package edu.cqie.cqdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.cqie.cqdemo.entity.AiFeedback;

import java.util.List;

public interface AiFeedbackService extends IService<AiFeedback> {

    AiFeedback submitFeedback(Long userId, String conversationId, String aiResponse,
                              Integer rating, String correctionContent,
                              String correctionType, String feedbackNote);

    List<AiFeedback> getFeedbackByUserId(Long userId);

    AiFeedback getFeedbackById(Long id);

    boolean updateFeedback(Long id, Integer rating, String correctionContent,
                           String correctionType, String feedbackNote);

    boolean deleteFeedback(Long id);
}
