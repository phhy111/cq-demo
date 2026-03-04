package edu.cqie.cqdemo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

//美食分类
@TableName("categories")
@Data
public class Categories {
    @TableField("id")
    private int id;
    @TableField("type")
    private int type;
    @TableField("code")
    private int code;
    @TableField("name")
    private String name;
    @TableField("icon")
    private String icon;
    @TableField("sort_order")
    private int sortOrder;
    @TableField("status")
    private int status;
    @TableField("created_at")
    private Date createdAt;
    @TableField("updated_at")
    private Date updatedAt;
}
