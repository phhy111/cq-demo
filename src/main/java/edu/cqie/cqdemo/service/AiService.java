package edu.cqie.cqdemo.service;

import dev.langchain4j.service.MemoryId; // 关键：替换@Id为@MemoryId
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface AiService {
    @SystemMessage("你是一个得力的助手，回答简洁、友好")
        // 修复：@Id → @MemoryId（langchain4j 1.1.0-beta7的正确注解）
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);
}