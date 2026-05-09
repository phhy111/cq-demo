package edu.cqie.cqdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 区域表
 * @TableName regions
 */
@TableName(value ="regions")
@Data
public class Regions {
    /**
     * 区域ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 父区域ID
     */
    private Integer parentId;

    /**
     * 区域名称
     */
    private String name;

    /**
     * 1-市，2-区县，3-街道
     */
    private Integer level;

    /**
     * 封面图
     */
    private String coverImg;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 特色标签，逗号分隔
     */
    private String tags;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 1-启用，0-停用
     */
    private Integer status;

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