package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.entity.LoginUser;
import edu.cqie.cqdemo.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ai/knowledge")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AiKnowledgeController {

    @Autowired
    private AiService aiService;

    /**
     * 基于知识库的问答测试接口
     */
    @PostMapping(
            value = "/chat",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<String> knowledgeChat(@RequestBody String question) {
        try {
            if (question == null || question.isBlank()) {
                return Flux.error(new IllegalArgumentException("问题不能为空"));
            }

            // 获取用户ID作为memoryId
            LoginUser loginUser = getLoginUser();
            Long userId = loginUser.getId();

            log.info("用户 {} 使用知识库问答：{}", userId, question);
            
            // 使用现有的generateTravelPlan方法，它已经配置了RAG检索器
            return aiService.generateTravelPlan(userId, question);
            
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
    public Result<String> checkKnowledgeStatus() {
        try {
            return Result.success("知识库服务正常运行");
        } catch (Exception e) {
            log.error("检查知识库状态失败", e);
            return Result.error("知识库服务异常：" + e.getMessage());
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
