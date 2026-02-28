package edu.cqie.cqdemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("business_history")
public class BrowsingHistory {
    /**
     * 历史记录ID（主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 浏览的业务ID
     */
    @TableField(value = "business_id")
    private Long businessId;

    /**
     * 类型：1-景点，2-美食，3-路线，4-攻略，6-店铺
     */
    @TableField(value = "type")
    private Integer type;

    /**
     * 浏览时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableField(value = "extra", fill = FieldFill.INSERT_UPDATE)
    private String extra;
}
