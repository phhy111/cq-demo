package edu.cqie.cqdemo.dto;

import lombok.Data;

@Data
public class ResetPasswordDTO {
    private Long userId;
    private String oldPassword;
    private String newPassword;
}
