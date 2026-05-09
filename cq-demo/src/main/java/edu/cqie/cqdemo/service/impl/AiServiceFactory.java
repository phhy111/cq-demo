package edu.cqie.cqdemo.service.impl;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.Rag;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.QueryTransformer;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import edu.cqie.cqdemo.ai.rerank.PointwiseReranker;
import edu.cqie.cqdemo.ai.router.AiModelRouter;
import edu.cqie.cqdemo.ai.TravelTools;
import edu.cqie.cqdemo.repository.RedisChatMemoryStore;
import edu.cqie.cqdemo.service.AiService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI Service 工厂（多模型动态路由版）
 * 支持：通义千问（云端）+ Ollama（本地）动态切换
 * 集成 RAG 知识库检索 + AI 工具调用（航班查询、景点位置、联网搜索）
 */
@Configuration
@Slf4j
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

    @Autowired
    private PointwiseReranker pointwiseReranker;

    @Value("${rag.retriever.max-results:8}")
    private int maxResults;

    @Value("${rag.reranker.enabled:true}")
    private boolean rerankerEnabled;

    @Value("${rag.retriever.min-score:0.6}")
    private double minScore;

    @Value("${rag.retriever.max-context-tokens:4000}")
    private int maxContextTokens;

    @Value("${rag.chat-memory.max-messages:15}")
    private int maxMemoryMessages;

    /**
     * 创建默认 AI Service（使用云端通义千问模型）
     */
    @Bean
    public AiService aiService(
            StreamingChatLanguageModel streamingChatModel,
            ChatLanguageModel chatModel
    ) {
        log.info("创建默认 AI Service，RAG配置: maxResults={}, minScore={}, maxContextTokens={}", 
                maxResults, minScore, maxContextTokens);

        ContentRetriever contentRetriever = createContentRetriever();

        return AiServices.builder(AiService.class)
                .streamingChatLanguageModel(streamingChatModel)
                .chatLanguageModel(chatModel)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.builder()
                                .chatMemoryStore(redisChatMemoryStore)
                                .maxMessages(maxMemoryMessages)
                                .build()
                )
                .contentRetriever(contentRetriever)
                .contentCombiner(this::combineContents)
                .queryTransformer(createQueryTransformer())
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

        ContentRetriever contentRetriever = createContentRetriever();

        log.info("动态创建 AI Service，用户消息长度: {}，使用流式模型: {}", 
                userMessage.length(), streamingModel.getClass().getSimpleName());

        return AiServices.builder(AiService.class)
                .streamingChatLanguageModel(streamingModel)
                .chatLanguageModel(chatModel)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.builder()
                                .chatMemoryStore(redisChatMemoryStore)
                                .maxMessages(maxMemoryMessages)
                                .build()
                )
                .contentRetriever(contentRetriever)
                .contentCombiner(this::combineContents)
                .queryTransformer(createQueryTransformer())
                .tools(travelTools)
                .build();
    }

    /**
     * 创建增强版内容检索器（带Pointwise重排序）
     */
    private ContentRetriever createContentRetriever() {
        ContentRetriever baseRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(redisEmbeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();

        if (rerankerEnabled) {
            return query -> {
                List<Content> contents = baseRetriever.retrieve(query);
                log.info("基础检索返回 {} 条结果", contents.size());
                
                if (!contents.isEmpty()) {
                    List<Content> reranked = pointwiseReranker.rerank(contents, query.text());
                    log.info("Pointwise重排序完成，保留 {} 条结果", reranked.size());
                    return reranked;
                }
                
                return contents;
            };
        }

        return baseRetriever;
    }

    /**
     * 创建查询转换器（支持查询重写）
     */
    private QueryTransformer createQueryTransformer() {
        return new QueryTransformer() {
            @Override
            public Query transform(Query query, List<ChatMessage> chatMessages) {
                String enhancedQuery = enhanceQuery(query.text(), chatMessages);
                log.debug("查询重写: 原始='{}', 增强='{}'", query.text(), enhancedQuery);
                return Query.from(enhancedQuery);
            }
        };
    }

    /**
     * 增强查询：结合对话历史和当前问题
     */
    private String enhanceQuery(String currentQuery, List<ChatMessage> chatMessages) {
        if (chatMessages == null || chatMessages.isEmpty()) {
            return currentQuery;
        }

        StringBuilder context = new StringBuilder();
        
        List<String> recentQuestions = chatMessages.stream()
                .filter(m -> m.type() == ChatMessageType.USER)
                .map(ChatMessage::text)
                .limit(3)
                .collect(Collectors.toList());

        if (!recentQuestions.isEmpty()) {
            context.append("对话历史：");
            for (int i = 0; i < recentQuestions.size(); i++) {
                context.append(recentQuestions.get(i));
                if (i < recentQuestions.size() - 1) {
                    context.append("；");
                }
            }
            context.append("。");
        }

        return context + "当前问题：" + currentQuery;
    }

    /**
     * 内容组合器：压缩和整合检索到的内容
     */
    private String combineContents(List<Content> contents) {
        if (contents == null || contents.isEmpty()) {
            return "";
        }

        StringBuilder combined = new StringBuilder();
        int tokenCount = 0;

        for (int i = 0; i < contents.size(); i++) {
            Content content = contents.get(i);
            String text = content.text();
            
            if (text == null || text.isEmpty()) {
                continue;
            }

            int estimatedTokens = text.length() / 3;
            
            if (tokenCount + estimatedTokens > maxContextTokens && combined.length() > 0) {
                log.debug("上下文长度达到上限，停止添加更多内容");
                break;
            }

            if (combined.length() > 0) {
                combined.append("\n\n---\n\n");
            }

            combined.append("【资料").append(i + 1).append("】\n");
            combined.append(text);
            tokenCount += estimatedTokens;
        }

        log.debug("组合内容长度: {} 字符，约 {} tokens", combined.length(), tokenCount);
        return combined.toString();
    }

    /**
     * 内容组合器（带Pointwise重排序）
     */
    private String combineContentsWithRerank(List<Content> contents, String query) {
        if (contents == null || contents.isEmpty()) {
            return "";
        }

        List<Content> processedContents = contents;
        
        if (rerankerEnabled) {
            processedContents = pointwiseReranker.rerank(contents, query);
            log.info("Pointwise重排序完成，文档数量: {}", processedContents.size());
        }

        return combineContents(processedContents);
    }

    /**
     * 创建专门用于知识库问答的 AI Service
     */
    public AiService createKnowledgeService(String userMessage) {
        StreamingChatLanguageModel streamingModel = aiModelRouter.routeStreamingChatModel(userMessage);
        ChatLanguageModel chatModel = aiModelRouter.routeChatModel(userMessage);

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(redisEmbeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();

        return AiServices.builder(AiService.class)
                .streamingChatLanguageModel(streamingModel)
                .chatLanguageModel(chatModel)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.builder()
                                .chatMemoryStore(redisChatMemoryStore)
                                .maxMessages(5)
                                .build()
                )
                .contentRetriever(contentRetriever)
                .contentCombiner(this::combineContents)
                .queryTransformer(createQueryTransformer())
                .build();
    }
}
