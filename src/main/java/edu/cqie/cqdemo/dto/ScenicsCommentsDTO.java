package edu.cqie.cqdemo.dto;


import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

@Data
public class ScenicsCommentsDTO {
    /**
     * 评论ID
     */
    private Integer id;

    /**
     * 用户ID（修正为和users.id一致的类型）
     */
    private Long userId;

    /**
     * 目标对象ID
     */
    private Integer targetId;

    /**
     * 1-景点，2-美食，3-路线，4-攻略
     */
    private Integer targetType;

    /**
     * 父评论ID
     */
    private Integer parentId;

    /**
     * 回复给用户ID
     */
    private Integer replyToId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 图片URL，JSON数组
     */
    private String images;

    /**
     * 评分（1-5）
     */
    private Integer rating;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 回复数
     */
    private Integer replyCount;

    /**
     * 1-正常，0-删除
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

    /**
     * 用户名（登录/展示用）
     * 对应数据库字段：username (VARCHAR(50))
     */
    private String username;
    /**
     * 头像URL地址
     * 对应数据库字段：avatar_url (VARCHAR(255))
     */
    private String avatarUrl;

    private Integer commentId;


}
