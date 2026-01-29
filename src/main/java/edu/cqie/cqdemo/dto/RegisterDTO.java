package edu.cqie.cqdemo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 注册请求参数
 */
@Data
public class RegisterDTO {

    /**
     * 用户名（前端必填）
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度需在2-20个字符之间")
    private String username;

    /**
     * 邮箱（前端必填+格式校验）
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 验证码（前端必填）
     */
    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码需为6位字符")
    private String verificationCode;

    /**
     * 密码（前端必填）
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在6-32个字符之间")
    private String password;

    /**
     * 性别（前端默认0：未知，1：男，2：女）
     */
    private Integer gender = 0;

    /**
     * 手机号（前端非必填，但有格式校验）
     */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 备用字段1（数字类型，前端为null）
     */
    private Long ext2;

    /**
     * 备用字段2（小整数类型，前端为null）
     */
    private Integer ext3;

    /**
     * 备用字段3（时间类型，前端为null，可接收yyyy-MM-dd HH:mm:ss格式）
     */
    private String ext4;

    /**
     * 备用字段4（结构化字段，前端为null，可接收JSON字符串）
     */
    private String ext5;

    /**
     * 头像文件（前端非必填，MultipartFile接收二进制文件）
     */
    private MultipartFile avatar;
}