package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.dto.AiFeedbackDTO;
import edu.cqie.cqdemo.entity.AiFeedback;
import edu.cqie.cqdemo.entity.LoginUser;
import edu.cqie.cqdemo.service.AiFeedbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/feedback")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AiFeedbackController {

    @Autowired
    private AiFeedbackService aiFeedbackService;

    @PostMapping("/submit")
    public Result<AiFeedback> submitFeedback(@RequestBody AiFeedbackDTO dto) {
        try {
            LoginUser loginUser = getLoginUser();
            Long userId = loginUser.getId();

            if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
                return Result.error("评分必须在1-5之间");
            }

            AiFeedback feedback = aiFeedbackService.submitFeedback(
                    userId,
                    dto.getConversationId(),
                    dto.getAiResponse(),
                    dto.getRating(),
                    dto.getCorrectionContent(),
                    dto.getCorrectionType(),
                    dto.getFeedbackNote()
            );

            return Result.success(feedback);
        } catch (IllegalAccessException e) {
            log.error("用户未登录", e);
            return Result.error("用户未登录或令牌无效");
        } catch (Exception e) {
            log.error("提交反馈失败", e);
            return Result.error("提交反馈失败：" + e.getMessage());
        }
    }

    @GetMapping("/my")
    public Result<List<AiFeedback>> getMyFeedback() {
        try {
            LoginUser loginUser = getLoginUser();
            Long userId = loginUser.getId();

            List<AiFeedback> feedbackList = aiFeedbackService.getFeedbackByUserId(userId);
            return Result.success(feedbackList);
        } catch (IllegalAccessException e) {
            log.error("用户未登录", e);
            return Result.error("用户未登录或令牌无效");
        } catch (Exception e) {
            log.error("获取反馈列表失败", e);
            return Result.error("获取反馈列表失败：" + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<AiFeedback> getFeedbackById(@PathVariable Long id) {
        try {
            AiFeedback feedback = aiFeedbackService.getFeedbackById(id);
            if (feedback == null) {
                return Result.error("反馈不存在");
            }
            return Result.success(feedback);
        } catch (Exception e) {
            log.error("获取反馈详情失败", e);
            return Result.error("获取反馈详情失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Boolean> updateFeedback(@PathVariable Long id, @RequestBody AiFeedbackDTO dto) {
        try {
            boolean success = aiFeedbackService.updateFeedback(
                    id,
                    dto.getRating(),
                    dto.getCorrectionContent(),
                    dto.getCorrectionType(),
                    dto.getFeedbackNote()
            );

            if (success) {
                return Result.success(true);
            } else {
                return Result.error("更新失败，反馈不存在");
            }
        } catch (Exception e) {
            log.error("更新反馈失败", e);
            return Result.error("更新反馈失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteFeedback(@PathVariable Long id) {
        try {
            boolean success = aiFeedbackService.deleteFeedback(id);
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("删除失败，反馈不存在");
            }
        } catch (Exception e) {
            log.error("删除反馈失败", e);
            return Result.error("删除反馈失败：" + e.getMessage());
        }
    }

    private LoginUser getLoginUser() throws IllegalAccessException {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof LoginUser) {
            return (LoginUser) principal;
        }
        throw new IllegalAccessException("用户未登录或令牌无效");
    }
}
