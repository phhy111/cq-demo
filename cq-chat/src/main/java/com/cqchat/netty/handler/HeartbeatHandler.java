package com.cqchat.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class HeartbeatHandler extends IdleStateHandler {

    public HeartbeatHandler(int idleSeconds) {
        super(0, 0, idleSeconds, TimeUnit.SECONDS);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) {
        log.info("心跳超时，关闭连接: {}", ctx.channel().id().asShortText());
        ctx.close();
    }
}
