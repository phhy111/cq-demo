package edu.cqie.cqdemo.ai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * AI 多模型动态路由配置
 * 支持：通义千问（云端）+ Ollama（本地部署）
 * 简单任务走本地模型，复杂任务走云端模型
 */
@Configuration
@Slf4j
public class AiModelRouterConfig {

    // ==================== 通义千问配置 ====================
    @Value("${langchain4j.community.dashscope.chat-model.api-key:}")
    private String dashscopeApiKey;

    @Value("${langchain4j.community.dashscope.chat-model.model-name:qwen-max}")
    private String dashscopeModelName;

    // ==================== Ollama 本地模型配置 ====================
    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model-name:qwen2.5:7b}")
    private String ollamaModelName;

    /**
     * 通义千问 ChatModel（云端，用于复杂任务）
     */
    @Bean(name = "dashscopeChatModel")
    @Primary
    public ChatLanguageModel dashscopeChatModel() {
        log.info("初始化通义千问 ChatModel，模型：{}", dashscopeModelName);
        return QwenChatModel.builder()
                .apiKey(dashscopeApiKey)
                .modelName(dashscopeModelName)
                .build();
    }

    /**
     * 通义千问 StreamingChatModel（云端，用于复杂任务的流式输出）
     */
    @Bean(name = "dashscopeStreamingChatModel")
    @Primary
    public StreamingChatLanguageModel dashscopeStreamingChatModel() {
        log.info("初始化通义千问 StreamingChatModel，模型：{}", dashscopeModelName);
        return QwenStreamingChatModel.builder()
                .apiKey(dashscopeApiKey)
                .modelName(dashscopeModelName)
                .build();
    }

    /**
     * Ollama ChatModel（本地，用于简单任务）
     */
    @Bean(name = "ollamaChatModel")
    public ChatLanguageModel ollamaChatModel() {
        log.info("初始化 Ollama ChatModel，baseUrl：{}，模型：{}", ollamaBaseUrl, ollamaModelName);
        try {
            return OllamaChatModel.builder()
                    .baseUrl(ollamaBaseUrl)
                    .modelName(ollamaModelName)
                    .timeout(Duration.ofSeconds(60))
                    .build();
        } catch (Exception e) {
            log.warn("Ollama 本地模型初始化失败，将仅使用云端模型：{}", e.getMessage());
            return null;
        }
    }

    /**
     * Ollama StreamingChatModel（本地，用于简单任务的流式输出）
     */
    @Bean(name = "ollamaStreamingChatModel")
    public StreamingChatLanguageModel ollamaStreamingChatModel() {
        log.info("初始化 Ollama StreamingChatModel，baseUrl：{}，模型：{}", ollamaBaseUrl, ollamaModelName);
        try {
            return OllamaStreamingChatModel.builder()
                    .baseUrl(ollamaBaseUrl)
                    .modelName(ollamaModelName)
                    .timeout(Duration.ofSeconds(60))
                    .build();
        } catch (Exception e) {
            log.warn("Ollama 本地流式模型初始化失败，将仅使用云端模型：{}", e.getMessage());
            return null;
        }
    }
}
