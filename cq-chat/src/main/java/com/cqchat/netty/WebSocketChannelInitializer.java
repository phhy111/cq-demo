package com.cqchat.netty;

import com.cqchat.config.NettyConfig;
import com.cqchat.netty.handler.AuthHandler;
import com.cqchat.netty.handler.HeartbeatHandler;
import com.cqchat.netty.handler.WebSocketHandler;
import com.cqchat.netty.session.UserChannelManager;
import com.cqchat.util.JwtUtil;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class WebSocketChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final NettyConfig nettyConfig;
    private final JwtUtil jwtUtil;
    private final UserChannelManager userChannelManager;
    private final WebSocketHandler webSocketHandler;

    public WebSocketChannelInitializer(NettyConfig nettyConfig, JwtUtil jwtUtil,
                                        UserChannelManager userChannelManager,
                                        WebSocketHandler webSocketHandler) {
        this.nettyConfig = nettyConfig;
        this.jwtUtil = jwtUtil;
        this.userChannelManager = userChannelManager;
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        // HTTP 编解码
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new HttpObjectAggregator(65536));

        // 认证（在 WebSocket 握手之前）
        pipeline.addLast(new AuthHandler(jwtUtil, userChannelManager));

        // WebSocket 协议处理
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));

        // 心跳检测
        pipeline.addLast(new HeartbeatHandler(nettyConfig.getHeartbeatIdle()));

        // 业务消息处理
        pipeline.addLast(webSocketHandler);
    }
}
