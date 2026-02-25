package edu.cqie.cqdemo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

//收藏类
@Data
@TableName("collections")
public class Collections {
    @TableField("id")
    private int id;
    @TableField("user_id")
    private Long user_id;
    @TableField("target_id")
    private int target_id;
    @TableField("create_at")
    private Date at;
//    引入美食
    private Food food;
//    引入路线
    private Routes routes;
    //引入景点
    private Scenics scenics;
}
