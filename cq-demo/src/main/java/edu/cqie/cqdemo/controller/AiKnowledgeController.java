package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.entity.LoginUser;
import edu.cqie.cqdemo.service.AiService;
import edu.cqie.cqdemo.service.DocumentService;
import edu.cqie.cqdemo.service.impl.AiServiceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/knowledge")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AiKnowledgeController {

    @Autowired
    private AiServiceFactory aiServiceFactory;

    @Autowired
    private DocumentService documentService;

    /**
     * 基于知识库的问答接口
     * 使用专门的知识库问答服务，不使用工具调用，专注于检索问答
     */
    @PostMapping(
            value = "/chat",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<String> knowledgeChat(@RequestBody Map<String, String> request) {
        try {
            String question = request.get("question");
            
            if (question == null || question.isBlank()) {
                return Flux.error(new IllegalArgumentException("问题不能为空"));
            }

            LoginUser loginUser = getLoginUser();
            Long userId = loginUser.getId();

            log.info("用户 {} 使用知识库问答：{}", userId, question);

            AiService knowledgeService = aiServiceFactory.createKnowledgeService(question);

            return knowledgeService.answerQuestion(userId, question);
            
        } catch (IllegalAccessException e) {
            log.error("认证失败", e);
            return Flux.error(e);
        } catch (Exception e) {
            log.error("知识库问答异常", e);
            return Flux.error(e);
        }
    }

    /**
     * 检查知识库状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> checkKnowledgeStatus() {
        try {
            int documentCount = documentService.getDocumentCount();
            
            Map<String, Object> status = new HashMap<>();
            status.put("serviceStatus", "running");
            status.put("documentCount", documentCount);
            status.put("description", "基于Redis向量存储的RAG知识库服务");
            
            return Result.success(status);
        } catch (Exception e) {
            log.error("检查知识库状态失败", e);
            return Result.error("知识库服务异常：" + e.getMessage());
        }
    }

    /**
     * 重建知识库索引
     */
    @PostMapping("/rebuild")
    public Result<String> rebuildKnowledgeBase() {
        try {
            log.info("开始重建知识库索引");
            
            documentService.clearEmbeddingStore();
            documentService.processAllDocuments();
            
            return Result.success("知识库索引重建成功");
        } catch (Exception e) {
            log.error("重建知识库索引失败", e);
            return Result.error("知识库索引重建失败：" + e.getMessage());
        }
    }

    private LoginUser getLoginUser() throws IllegalAccessException {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof LoginUser)) {
            throw new IllegalAccessException("用户未登录或令牌无效");
        }
        return (LoginUser) principal;
    }
}
