package edu.cqie.cqdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 评论表
 * @TableName comments
 */
@TableName(value ="comments")
@Data
public class Comments {
    /**
     * 评论ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 用户ID（修正为和users.id一致的类型）
     */
    @TableField(value = "user_id")
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
    @NotNull(message = "评论内容不能为空")
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
     * 预留字段1
     */
    private String ext1;

    /**
     * 预留字段2
     */
    private String ext2;

    /**
     * 预留字段3
     */
    private String ext3;

    /**
     * 预留字段4
     */
    private String ext4;

    /**
     * 预留字段5
     */
    private String ext5;
}