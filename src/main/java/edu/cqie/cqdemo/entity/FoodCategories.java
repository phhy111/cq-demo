package edu.cqie.cqdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 美食表
 * @TableName food_categories
 */
@TableName(value ="food_categories")
@Data
public class FoodCategories {
    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 店铺ID
     */
    private Integer foodId;

    /**
     * 分类ID
     */
    private Integer categoryId;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 美食名字
     */
    private String name;

    /**
     * 简介
     */
    private String details;

    /**
     * 图片
     */
    private String image;

    /**
     * 2-待审核 1-发布，0-草稿
     */
    private Integer status;

    /**
     * 预留字段5
     */
    @TableField(exist = false)
    private Categories categories;

}
