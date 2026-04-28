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
import edu.cqie.cqdemo.ai.router.AiModelRouter;
import edu.cqie.cqdemo.ai.TravelTools;
import edu.cqie.cqdemo.repository.RedisChatMemoryStore;
import edu.cqie.cqdemo.service.AiService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI Service 工厂（多模型动态路由版）
 * 支持：通义千问（云端）+ Ollama（本地）动态切换
 * 集成 RAG 知识库检索 + AI 工具调用（航班查询、景点位置、联网搜索）
 */
@Configuration
public class AiServiceFactory {

    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> redisEmbeddingStore;

    @Autowired
    private AiModelRouter aiModelRouter;

    @Autowired
    private TravelTools travelTools;

    /**
     * 创建默认 AI Service（使用云端通义千问模型）
     */
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
                .tools(travelTools)
                .build();
    }

    /**
     * 根据用户消息动态路由创建 AI Service
     * 简单任务 → Ollama 本地模型（降低成本）
     * 复杂任务 → 通义千问云端模型（保证质量）
     */
    public AiService createAiService(String userMessage) {
        StreamingChatLanguageModel streamingModel = aiModelRouter.routeStreamingChatModel(userMessage);
        ChatLanguageModel chatModel = aiModelRouter.routeChatModel(userMessage);

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(redisEmbeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(8)
                .minScore(0.7)
                .build();

        return AiServices.builder(AiService.class)
                .streamingChatLanguageModel(streamingModel)
                .chatLanguageModel(chatModel)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.builder()
                                .chatMemoryStore(redisChatMemoryStore)
                                .maxMessages(10)
                                .build()
                )
                .contentRetriever(contentRetriever)
                .tools(travelTools)
                .build();
    }
}
