package edu.cqie.cqdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
//店铺表
@TableName("foods")
@Data
public class Shop {
    @TableId(value = "id",type = IdType.AUTO)
    private  int id ;

    @TableField("region_id")
    private int region_id;

    @TableField("name")
    private String name;
    @TableField("subtitle")
    private String subtitle;
    @TableField("cover_img")
    private String cover_img;
    @TableField("address")
    private String address;
    @TableField("description")
    private String description;
    @TableField("avg_price")
    private BigDecimal avg_price;
    @TableField("open_time")
    private String open_time;
    @TableField("contact_phone")
    private String contact_phone;
    @TableField("score")
    private BigDecimal score;
    @TableField("collect_count")
    private int collect_count;
    @TableField("comment_count")
    private int comment_count;
    @TableField("view_count")
    private  int view_count;
    @TableField("like_count")
    private int like_count;

}
