package edu.cqie.cqdemo.service;

import dev.langchain4j.service.MemoryId; // 关键：替换@Id为@MemoryId
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface AiService {
    @SystemMessage(fromResource ="aiHelp.txt" )
        // 修复：@Id → @MemoryId（langchain4j 1.1.0-beta7的正确注解）
    String chat(@MemoryId Long memoryId, @UserMessage String userMessage);


    //流式输出
    @SystemMessage(fromResource ="aiHelp.txt" )
    Flux<String> chatStream(@MemoryId Long memoryId, @UserMessage String userMessage);
}