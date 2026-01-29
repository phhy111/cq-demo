package edu.cqie.cqdemo.dto;

import lombok.Data;

/**
 * 注册请求参数
 */
@Data
public class RegisterDTO {
    private String username;
    private String password;
    private String ext1;
}