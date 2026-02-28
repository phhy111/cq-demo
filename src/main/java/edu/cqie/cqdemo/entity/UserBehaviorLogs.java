package edu.cqie.cqdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 用户行为日志表（记录活跃行为）
 * @TableName user_behavior_logs
 */
@TableName(value ="user_behavior_logs")
@Data
public class UserBehaviorLogs {
    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 行为类型：1-登录，2-浏览内容，3-发布内容，4-评论互动
     */
    private Integer behaviorType;

    /**
     * 行为发生时间
     */
    private Date behaviorTime;

    /**
     * 用户IP地址
     */
    private String ipAddress;

    /**
     * 设备信息
     */
    private String deviceInfo;
}