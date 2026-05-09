package edu.cqie.cqdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.cqie.cqdemo.entity.AiFeedback;
import edu.cqie.cqdemo.mapper.AiFeedbackMapper;
import edu.cqie.cqdemo.service.AiFeedbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class AiFeedbackServiceImpl extends ServiceImpl<AiFeedbackMapper, AiFeedback> implements AiFeedbackService {

    @Override
    public AiFeedback submitFeedback(Long userId, String conversationId, String aiResponse,
                                     Integer rating, String correctionContent,
                                     String correctionType, String feedbackNote) {
        AiFeedback feedback = new AiFeedback();
        feedback.setUserId(userId);
        feedback.setConversationId(conversationId);
        feedback.setAiResponse(aiResponse);
        feedback.setRating(rating);
        feedback.setCorrectionContent(correctionContent);
        feedback.setCorrectionType(correctionType);
        feedback.setFeedbackNote(feedbackNote);
        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setUpdatedAt(LocalDateTime.now());

        save(feedback);
        log.info("用户 {} 提交了AI反馈，对话ID: {}, 评分: {}", userId, conversationId, rating);
        return feedback;
    }

    @Override
    public List<AiFeedback> getFeedbackByUserId(Long userId) {
        LambdaQueryWrapper<AiFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiFeedback::getUserId, userId)
                .orderByDesc(AiFeedback::getCreatedAt);
        return list(wrapper);
    }

    @Override
    public AiFeedback getFeedbackById(Long id) {
        return getById(id);
    }

    @Override
    public boolean updateFeedback(Long id, Integer rating, String correctionContent,
                                  String correctionType, String feedbackNote) {
        AiFeedback feedback = getById(id);
        if (feedback == null) {
            return false;
        }

        if (rating != null) {
            feedback.setRating(rating);
        }
        if (correctionContent != null) {
            feedback.setCorrectionContent(correctionContent);
        }
        if (correctionType != null) {
            feedback.setCorrectionType(correctionType);
        }
        if (feedbackNote != null) {
            feedback.setFeedbackNote(feedbackNote);
        }
        feedback.setUpdatedAt(LocalDateTime.now());

        return updateById(feedback);
    }

    @Override
    public boolean deleteFeedback(Long id) {
        return removeById(id);
    }
}
