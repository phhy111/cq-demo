package edu.cqie.cqdemo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisLog implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 日志类型：INFO-信息，ERROR-错误，WARN-警告，DEBUG-调试
     */
    private String logType;
    
    /**
     * 所属模块：SCENIC-景点，USER-用户，ORDER-订单等
     */
    private String module;
    
    /**
     * 操作人 ID
     */
    private Long operatorId;
    
    /**
     * 操作人名称
     */
    private String operatorName;
    
    /**
     * 关联业务 ID
     */
    private Long businessId;
    
    /**
     * 请求 URL
     */
    private String requestUrl;
    
    /**
     * 请求方法：GET, POST, PUT, DELETE 等
     */
    private String requestMethod;
    
    /**
     * 请求参数
     */
    private String requestParams;
    
    /**
     * 响应数据
     */
    private String responseData;
    
    /**
     * 日志记录时间
     */
    private Date logTime;
    
    /**
     * 执行时长（毫秒）
     */
    private Long executeTime;
    
    /**
     * IP 地址
     */
    private String ipAddress;
}
