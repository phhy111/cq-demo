package edu.cqie.cqdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@TableName(value ="guides")
@Data
public class Guides{

    private static final long serialVersionUID = 1L;

    /**
     * 攻略ID（主键，自增）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 用户ID（关联users表id）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 攻略标题
     */
    @TableField("title")
    private String title;

    /**
     * 副标题
     */
    @TableField("subtitle")
    private String subtitle;

    /**
     * 封面图（存储图片URL/路径）
     */
    @TableField("cover_img")
    private String coverImg;

    /**
     * 摘要
     */
    @TableField("summary")
    private String summary;

    /**
     * 攻略内容（文本类型，存储富文本/Markdown）
     */
    @TableField("content")
    private String content;

    /**
     * 分类（traffic-交通, food-美食, scenic-景点等）
     */
    @TableField("category")
    private String category;

    /**
     * 关联区域ID
     */
    @TableField("region_id")
    private Integer regionId;

    /**
     * 浏览量
     */
    @TableField("view_count")
    private Integer viewCount;

    /**
     * 收藏数
     */
    @TableField("collect_count")
    private Integer collectCount;

    /**
     * 评论数
     */
    @TableField("comment_count")
    private Integer commentCount;

    /**
     * 点赞数
     */
    @TableField("like_count")
    private Integer likeCount;

    /**
     * 是否推荐（0-否，1-是）
     */
    @TableField("is_recommend")
    private Integer isRecommend;

    /**
     * 是否热门（0-否，1-是）
     */
    @TableField("is_hot")
    private Integer isHot;

    /**
     * 状态（0-草稿，1-发布）
     */
    @TableField("status")
    private Integer status;

    /**
     * 排序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 路线ID
     */
    @TableField("routes_id")
    private Integer routesId;

    /**
     * 预算
     */
    @TableField("budget_info")
    private Double budgetInfo;

    /**
     * 出行方式
     */
    @TableField("travel_way")
    private String travelWay;

    /**
     * 预留字段4
     */
    @TableField("ext4")
    private String ext4;

    /**
     * 预留字段5
     */
    @TableField("ext5")
    private String ext5;
}
