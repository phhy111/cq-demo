package edu.cqie.cqdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
// 美食表
@TableName("food_categories")
public class Food {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 食物ID（可能是外键或其他用途）
     */
    @TableField("food_id")
    private Integer foodId;

    /**
     * 分类ID
     */
    @TableField("category_id")
    private Integer categoryId;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private Date createdAt;

    /**
     * 食物名称
     */
    @TableField("name")
    private String name;

    /**
     * 详细描述
     */
    @TableField("details")
    private String details;
    @TableField("image")
    private String image;
    private Categories categories;
}
