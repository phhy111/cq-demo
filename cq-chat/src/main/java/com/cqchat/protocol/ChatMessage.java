package com.cqchat.protocol;

import lombok.Data;

@Data
public class ChatMessage {
    private String type;
    private String msgId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private String mediaUrl;
    private Integer voiceDuration;
    private Long timestamp;
}
