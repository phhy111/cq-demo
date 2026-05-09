package edu.cqie.cqdemo.dto;

import lombok.Data;

@Data
public class AiFeedbackDTO {

    private String conversationId;

    private String aiResponse;

    private Integer rating;

    private String correctionContent;

    private String correctionType;

    private String feedbackNote;
}
