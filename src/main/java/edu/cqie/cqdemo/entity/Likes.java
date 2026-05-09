package edu.cqie.cqdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@TableName(value ="likes")
@Data
public class Likes {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("user_id")
    private Long userId;

    @TableField("target_id")
    private Long targetId;

    @TableField("target_type")
    private Integer targetType;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField("ext1")
    private String ext1;

    @TableField("ext2")
    private String ext2;

    @TableField("ext3")
    private String ext3;

    @TableField("ext4")
    private String ext4;

    @TableField("ext5")
    private String ext5;
}
