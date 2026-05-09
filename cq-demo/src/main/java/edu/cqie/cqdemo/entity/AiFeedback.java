package edu.cqie.cqdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_feedback")
public class AiFeedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("conversation_id")
    private String conversationId;

    @TableField("ai_response")
    private String aiResponse;

    @TableField("rating")
    private Integer rating;

    @TableField("correction_content")
    private String correctionContent;

    @TableField("correction_type")
    private String correctionType;

    @TableField("feedback_note")
    private String feedbackNote;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
