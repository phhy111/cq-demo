package edu.cqie.cqdemo.service.impl;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import edu.cqie.cqdemo.repository.RedisChatMemoryStore;
import edu.cqie.cqdemo.service.AiService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiServiceFactory {

    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> redisEmbeddingStore;

    @Bean
    public AiService aiService(
            StreamingChatLanguageModel streamingChatModel,
            ChatLanguageModel chatModel
    ) {
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(redisEmbeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(8)
                .minScore(0.7)
                .build();

        return AiServices.builder(AiService.class)
                .streamingChatLanguageModel(streamingChatModel)
                .chatLanguageModel(chatModel)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.builder()
                                .chatMemoryStore(redisChatMemoryStore)
                                .maxMessages(10)
                                .build()
                )
                .contentRetriever(contentRetriever)
                .build();
    }
}
