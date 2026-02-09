package edu.cqie.cqdemo.service.impl;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import edu.cqie.cqdemo.service.AiService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiServiceFactory {

    @Resource
    private ChatModel qwenChatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Bean
    public AiService aiService() {
        return AiServices.builder(AiService.class)
                .streamingChatModel(streamingChatModel)
                .chatModel(qwenChatModel)
                // ✅ 启用基于 memoryId 的会话记忆（最多保留10条）
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}