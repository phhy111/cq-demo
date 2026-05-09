package edu.cqie.cqdemo.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ScenicsDTO {
    /**
     * 景点ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 区域ID
     */
    private Integer regionId;

    /**
     * 景点名称
     */
    private String name;

    /**
     * 副标题
     */
    private String subtitle;

    /**
     * 封面图
     */
    private String coverImg;

    /**
     * 详细地址
     */
    private String address;

    /**
     * 景区等级
     */
    private String level;

    /**
     * 价格
     */
    private String price;

    /**
     * 开放时间
     */
    private String openTime;

    /**
     * 景点描述
     */
    private String description;

    /**
     * 综合评分
     */
    private BigDecimal score;

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
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 是否推荐
     */
    private Integer isRecommend;

    /**
     * 是否热门
     */
    private Integer isHot;

    /**
     * 1-上架，0-下架
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
    /**
     * 景点图片地址
     */
    private String imageUrl;
}
