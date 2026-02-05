package edu.cqie.cqdemo.service.impl;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import edu.cqie.cqdemo.service.AiService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiServiceFactory {
    // 修正：拼写错误 qwengChatModel → qwenChatModel（和你的ChatModel Bean名称一致）
    @Resource
    private ChatModel qwenChatModel;

    // 修正：Bean名称规范 aiservice → aiService（小驼峰）
    @Bean
    public AiService aiService() {
        return AiServices.builder(AiService.class)
                .chatModel(qwenChatModel) // 绑定通义千问ChatModel
                // 记忆隔离：根据memoryId创建会话记忆（最多保留10条消息）
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}