package edu.cqie.cqdemo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE（Server-Sent Events）控制器，用于实时数据推送
 */
@RestController
@RequestMapping("/api/sse")
public class SSEController {

    // 存储所有活跃的SSE连接
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * 建立SSE连接
     * @return SseEmitter对象
     */
    @GetMapping("/connect")
    public SseEmitter connect() {
        // 创建一个SSE发射器，设置超时时间为3600秒
        SseEmitter emitter = new SseEmitter(3600000L);
        
        // 添加到发射器列表
        emitters.add(emitter);
        
        // 注册断开连接回调
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));
        
        return emitter;
    }

    /**
     * 向所有客户端推送消息
     * @param message 消息内容
     * @throws IOException IO异常
     */
    public void sendMessage(String message) throws IOException {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(message));
            } catch (IOException e) {
                // 移除失败的发射器
                emitters.remove(emitter);
            }
        }
    }

    /**
     * 向指定客户端推送消息
     * @param emitter SSE发射器
     * @param message 消息内容
     * @throws IOException IO异常
     */
    public void sendMessage(SseEmitter emitter, String message) throws IOException {
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(message));
        } catch (IOException e) {
            // 移除失败的发射器
            emitters.remove(emitter);
        }
    }

    /**
     * 获取当前活跃连接数
     * @return 活跃连接数
     */
    @GetMapping("/count")
    public int getConnectionCount() {
        return emitters.size();
    }
}
