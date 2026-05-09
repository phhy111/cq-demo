package edu.cqie.cqdemo.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class CommentsDTO {
    /**
     * 评论ID
     */
    private Integer id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 目标对象ID
     */
    private Integer targetId;

    /**
     * 1-景点，2-美食，3-路线，4-攻略 5-评论
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
     * 用户名
     */
    private String username;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 子评论列表
     */
    private List<CommentsDTO> replies;

    /**
     * 被回复者用户名
     */
    private String replyToUsername;
}
