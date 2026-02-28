package edu.cqie.cqdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class Users {
    /**
     * 用户唯一ID（主键）
     * 对应数据库字段：id (BIGINT UNSIGNED)
     */
    @TableId(type = IdType.AUTO) // 主键自增，匹配数据库AUTO_INCREMENT
    private Long id;

    /**
     * 用户名（登录/展示用）
     * 对应数据库字段：username (VARCHAR(50))
     */
    @TableField(value = "username")
    private String username;

    /**
     * 用户邮箱（唯一）
     * 对应数据库字段：email (VARCHAR(100))
     */
    @TableField(value = "email")
    private String email;

    /**
     * 头像URL地址
     * 对应数据库字段：avatar_url (VARCHAR(255))
     */
    @TableField(value = "avatar_url")
    private String avatarUrl;

    /**
     * 用户角色：0-普通用户，1-管理员 2-vip
     * 对应数据库字段：role (TINYINT UNSIGNED)
     */
    @TableField(value = "role")
    private Integer role;

    /**
     * 加密后的密码（如bcrypt/MD5+盐）
     * 对应数据库字段：password (VARCHAR(100))
     */
    @TableField(value = "password")
    private String password;

    /**
     * 性别：0-未知，1-男，2-女
     * 对应数据库字段：gender (TINYINT UNSIGNED)
     */
    @TableField(value = "gender")
    private Integer gender;

    /**
     * 创建时间（用户注册时间）
     * 对应数据库字段：created_at (DATETIME)
     * 数据库自动赋值，插入时无需手动设置
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT) // MyBatis-Plus自动填充（插入时）
    private LocalDateTime createdAt;

    /**
     * 备用字符串字段1（可存手机号/地址等）
     * 对应数据库字段：ext1 (VARCHAR(255))
     */
    @TableField(value = "phone")
    private String phone;

    /**
     * 个人签名
     * 对应数据库字段：per_signature (VARCHAR(500))
     */
    @TableField(value = "per_signature")
    private String perSignature;

    /**
     * 备用小整数字段（可存开关/状态等）
     * 对应数据库字段：ext3 (TINYINT UNSIGNED)
     */
    @TableField(value = "ext3")
    private Integer ext3;

    /**
     * 备用时间字段（可存会员到期时间等）
     * 对应数据库字段：ext4 (DATETIME)
     */
    @TableField(value = "ext4")
    private LocalDateTime ext4;

    /**
     * 备用结构化字段（可存复杂配置/偏好等）
     * 对应数据库字段：ext5 (JSON)
     * 用String接收，业务中可转JSON对象（如FastJSON/Jackson）
     */
    @TableField(value = "ext5")
    private String ext5;
}
