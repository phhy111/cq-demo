package com.cqchat.netty.handler;

import com.cqchat.netty.session.UserChannelManager;
import com.cqchat.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuthHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final JwtUtil jwtUtil;
    private final UserChannelManager userChannelManager;

    public AuthHandler(JwtUtil jwtUtil, UserChannelManager userChannelManager) {
        this.jwtUtil = jwtUtil;
        this.userChannelManager = userChannelManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        String uri = msg.uri();
        String token = extractToken(uri);

        if (token == null || token.isEmpty()) {
            log.warn("WebSocket 连接缺少 token");
            sendError(ctx, "缺少认证令牌");
            return;
        }

        try {
            Claims claims = jwtUtil.parseToken(token);
            Long userId = claims.get("id", Long.class);
            String username = claims.getSubject();

            if (userId == null) {
                log.warn("Token 中缺少用户ID");
                sendError(ctx, "无效的令牌");
                return;
            }

            // 绑定用户到 Channel
            userChannelManager.bindUser(userId, ctx.channel());
            log.info("用户认证成功: {} (ID={})", username, userId);

            // 将 URI 改为 /ws，去掉 query 参数，转发给下一个 handler
            msg.setUri("/ws");
            ctx.fireChannelRead(msg);

        } catch (Exception e) {
            log.error("Token 验证失败: {}", e.getMessage());
            sendError(ctx, "认证失败: " + e.getMessage());
        }
    }

    private String extractToken(String uri) {
        if (uri != null && uri.contains("?")) {
            String query = uri.substring(uri.indexOf('?') + 1);
            for (String param : query.split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2 && "token".equals(pair[0])) {
                    return pair[1];
                }
            }
        }
        return null;
    }

    private void sendError(ChannelHandlerContext ctx, String msg) {
        log.error("认证失败: {}", msg);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("AuthHandler 异常: {}", cause.getMessage());
        ctx.close();
    }
}
