package com.cqchat.netty.handler;

import com.alibaba.fastjson.JSON;
import com.cqchat.entity.ChatMsg;
import com.cqchat.netty.session.UserChannelManager;
import com.cqchat.protocol.ChatMessage;
import com.cqchat.protocol.MessageType;
import com.cqchat.service.ChatService;
import com.cqchat.service.OfflineMsgService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class WebSocketHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final ChatService chatService;
    private final OfflineMsgService offlineMsgService;
    private final UserChannelManager userChannelManager;

    public WebSocketHandler(ChatService chatService, OfflineMsgService offlineMsgService,
                            UserChannelManager userChannelManager) {
        this.chatService = chatService;
        this.offlineMsgService = offlineMsgService;
        this.userChannelManager = userChannelManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame textFrame) {
            String json = textFrame.text();
            log.debug("收到消息: {}", json);

            ChatMessage msg;
            try {
                msg = JSON.parseObject(json, ChatMessage.class);
            } catch (Exception e) {
                log.error("消息解析失败: {}", json);
                return;
            }

            Long userId = userChannelManager.getUserId(ctx.channel());
            if (userId == null) {
                log.warn("未认证的 Channel 发送消息");
                ctx.close();
                return;
            }

            // 设置发送者 ID（以服务端认证为准，防止伪造）
            msg.setSenderId(userId);

            handleMessage(ctx.channel(), msg);
        }
    }

    private void handleMessage(Channel channel, ChatMessage msg) {
        String type = msg.getType();
        if (type == null) {
            log.warn("消息类型为空");
            return;
        }

        switch (MessageType.valueOf(type)) {
            case TEXT, IMAGE, VOICE -> chatService.handleSendMessage(msg);
            case READ_ACK -> chatService.handleReadAck(msg, msg.getSenderId());
            case HEARTBEAT -> handleHeartbeat(channel);
            default -> log.warn("未知消息类型: {}", type);
        }
    }

    private void handleHeartbeat(Channel channel) {
        ChatMessage pong = new ChatMessage();
        pong.setType(MessageType.HEARTBEAT.name());
        pong.setTimestamp(System.currentTimeMillis());
        channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(pong)));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
            Long userId = userChannelManager.getUserId(ctx.channel());
            if (userId != null) {
                log.info("用户 {} WebSocket 握手完成，推送离线消息", userId);
                pushOfflineMessages(ctx.channel(), userId);
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    private void pushOfflineMessages(Channel channel, Long userId) {
        try {
            List<ChatMsg> offlineMessages = offlineMsgService.getUndeliveredMessages(userId);
            if (!offlineMessages.isEmpty()) {
                for (ChatMsg msg : offlineMessages) {
                    ChatMessage pushMsg = new ChatMessage();
                    pushMsg.setType(msg.getMsgType());
                    pushMsg.setMsgId(msg.getMsgId());
                    pushMsg.setSenderId(msg.getSenderId());
                    pushMsg.setReceiverId(msg.getReceiverId());
                    pushMsg.setContent(msg.getContent());
                    pushMsg.setMediaUrl(msg.getMediaUrl());
                    pushMsg.setVoiceDuration(msg.getVoiceDuration());
                    pushMsg.setTimestamp(msg.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
                    channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(pushMsg)));
                }
                log.info("用户 {} 上线，推送了 {} 条离线消息", userId, offlineMessages.size());
            }
        } catch (Exception e) {
            log.error("推送离线消息失败: userId={}", userId, e);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("TCP 连接建立: {}", ctx.channel().id().asShortText());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Long userId = userChannelManager.getUserId(ctx.channel());
        userChannelManager.unbindUser(ctx.channel());
        log.info("WebSocket 连接断开: userId={}", userId);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebSocket 异常: {}", cause.getMessage(), cause);
        ctx.close();
    }
}
