// LoginDTO.java
package edu.cqie.cqdemo.dto;

import lombok.Data;

/**
 * 登录请求参数
 */
@Data
public class LoginDTO {
    private String username;
    private String password;
}