package edu.cqie.cqdemo.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 路线表
 * @TableName routes
 */
@TableName(value ="routes")
@Data
public class Routes {
    /**
     * 路线ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 用户ID（修正为和users.id一致的类型）
     */
    private Long userId;

    /**
     * 路线标题
     */
    private String title;

    /**
     * 副标题
     */
    private String subtitle;

    /**
     * 封面图
     */
    private String coverImg;

    /**
     * 路线描述
     */
    private String description;

    /**
     * 路线摘要
     */
    private String summary;

    /**
     * 天数
     */
    private Integer dayCount;

    /**
     * 类型：food,scenic,culture,mixed
     */
    private String type;

    /**
     * 预算说明
     */
    private String budgetInfo;

    /**
     * 最佳季节
     */
    private String bestSeason;

    /**
     * 是否官方
     */
    private Integer isOfficial;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 收藏数
     */
    private Integer collectCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 是否推荐
     */
    private Integer isRecommend;

    /**
     * 是否热门
     */
    private Integer isHot;

    /**
     * 1-发布，0-草稿
     */
    private Integer status;

    /**
     * 排序
     */
    private Integer sortOrder;

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
     * 用来代表平均评分
     */
    private String ext1;

    /**
     * 预留字段2
     * 用来代表用户个人评分
     */
    private String ext2;

    /**
     * 预留字段3
     * 用来代表路线游玩时长
     */
    private String ext3;

    /**
     * 预留字段4
     * 用来代表人均消费
     */
    private String ext4;

    /**
     * 预留字段5
     */
    private String ext5;


}