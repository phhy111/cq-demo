package edu.cqie.cqdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 公告表
 * @TableName announcements
 */
@TableName(value ="announcements")
@Data
public class Announcements {
    /**
     * 公告ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 公告标题
     */
    private String title;

    /**
     * 公告内容
     */
    private String content;

    /**
     * 1-通知，2-活动
     */
    private Integer type;

    /**
     * 封面图
     */
    private String coverImg;

    /**
     * 是否置顶
     */
    private Integer isTop;

    /**
     * 1-发布，0-草稿
     */
    private Integer status;

    /**
     * 浏览量
     */
    private Integer viewCount;

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


}