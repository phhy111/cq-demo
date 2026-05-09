package com.cqchat.netty;

import com.cqchat.config.NettyConfig;
import com.cqchat.netty.handler.WebSocketHandler;
import com.cqchat.netty.session.UserChannelManager;
import com.cqchat.util.JwtUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NettyServer {

    private final NettyConfig nettyConfig;
    private final JwtUtil jwtUtil;
    private final UserChannelManager userChannelManager;
    private final WebSocketHandler webSocketHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyServer(NettyConfig nettyConfig, JwtUtil jwtUtil,
                       UserChannelManager userChannelManager,
                       WebSocketHandler webSocketHandler) {
        this.nettyConfig = nettyConfig;
        this.jwtUtil = jwtUtil;
        this.userChannelManager = userChannelManager;
        this.webSocketHandler = webSocketHandler;
    }

    @PostConstruct
    public void start() {
        new Thread(() -> {
            bossGroup = new NioEventLoopGroup(nettyConfig.getBossThreads());
            workerGroup = new NioEventLoopGroup(nettyConfig.getWorkerThreads());
            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new WebSocketChannelInitializer(
                                nettyConfig, jwtUtil, userChannelManager, webSocketHandler));

                ChannelFuture future = bootstrap.bind(nettyConfig.getPort()).sync();
                log.info("Netty WebSocket 服务启动成功，端口: {}", nettyConfig.getPort());
                future.channel().closeFuture().sync();
            } catch (Exception e) {
                log.error("Netty 服务启动失败", e);
            } finally {
                shutdown();
            }
        }, "netty-server-thread").start();
    }

    @PreDestroy
    public void shutdown() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("Netty 服务已关闭");
    }
}
