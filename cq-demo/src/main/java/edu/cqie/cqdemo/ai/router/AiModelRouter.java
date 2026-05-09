package edu.cqie.cqdemo.ai.router;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * AI 模型动态路由器
 * 根据任务复杂度选择使用本地模型（Ollama）或云端模型（通义千问）
 * 简单任务（问候、简单问答）→ 本地模型，降低成本
 * 复杂任务（攻略生成、多轮对话）→ 云端模型，保证质量
 */
@Component
@Slf4j
public class AiModelRouter {

    @Autowired
    @Qualifier("dashscopeChatModel")
    private ChatLanguageModel dashscopeChatModel;

    @Autowired
    @Qualifier("dashscopeStreamingChatModel")
    private StreamingChatLanguageModel dashscopeStreamingChatModel;

    @Autowired(required = false)
    @Qualifier("ollamaChatModel")
    private ChatLanguageModel ollamaChatModel;

    @Autowired(required = false)
    @Qualifier("ollamaStreamingChatModel")
    private StreamingChatLanguageModel ollamaStreamingChatModel;

    // 简单任务关键词
    private static final String[] SIMPLE_TASK_KEYWORDS = {
            "你好", "您好", "hello", "hi",
            "谢谢", "再见", "拜拜",
            "你是谁", "你能做什么", "介绍一下",
            "天气", "时间", "日期"
    };

    /**
     * 判断是否为简单任务
     */
    public boolean isSimpleTask(String userMessage) {
        if (userMessage == null || userMessage.trim().length() < 10) {
            return true;
        }
        String lowerMsg = userMessage.toLowerCase();
        for (String keyword : SIMPLE_TASK_KEYWORDS) {
            if (lowerMsg.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取适合当前任务的 ChatModel
     */
    public ChatLanguageModel routeChatModel(String userMessage) {
        if (isSimpleTask(userMessage) && ollamaChatModel != null) {
            log.info("任务判定为【简单任务】，使用本地 Ollama 模型处理");
            return ollamaChatModel;
        }
        log.info("任务判定为【复杂任务】，使用云端通义千问模型处理");
        return dashscopeChatModel;
    }

    /**
     * 获取适合当前任务的 StreamingChatModel
     */
    public StreamingChatLanguageModel routeStreamingChatModel(String userMessage) {
        if (isSimpleTask(userMessage) && ollamaStreamingChatModel != null) {
            log.info("流式任务判定为【简单任务】，使用本地 Ollama 模型处理");
            return ollamaStreamingChatModel;
        }
        log.info("流式任务判定为【复杂任务】，使用云端通义千问模型处理");
        return dashscopeStreamingChatModel;
    }

    /**
     * 获取默认的 StreamingChatModel（用于无法判断任务类型的场景）
     */
    public StreamingChatLanguageModel getDefaultStreamingModel() {
        return dashscopeStreamingChatModel;
    }

    /**
     * 获取默认的 ChatModel
     */
    public ChatLanguageModel getDefaultChatModel() {
        return dashscopeChatModel;
    }
}
