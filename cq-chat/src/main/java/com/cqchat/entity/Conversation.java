package com.cqchat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation")
public class Conversation {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_a_id")
    private Long userAId;

    @TableField("user_b_id")
    private Long userBId;

    @TableField("last_msg_id")
    private Long lastMsgId;

    @TableField("last_msg_time")
    private LocalDateTime lastMsgTime;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
