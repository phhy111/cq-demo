package com.cqchat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_msg")
public class ChatMsg {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("msg_id")
    private String msgId;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("sender_id")
    private Long senderId;

    @TableField("receiver_id")
    private Long receiverId;

    @TableField("msg_type")
    private String msgType;

    @TableField("content")
    private String content;

    @TableField("media_url")
    private String mediaUrl;

    @TableField("voice_duration")
    private Integer voiceDuration;

    @TableField("is_read")
    private Integer isRead;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
