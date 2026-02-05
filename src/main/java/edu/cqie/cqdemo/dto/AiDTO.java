package edu.cqie.cqdemo.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 原有字段保留 + 新增message + 实现序列化 + 重载拼接方法
 * 序列化：支持Redis存储对象
 * message：接收后续仅传的userInput（纯输入要求）
 */
@Data
public class AiDTO implements Serializable {
    // 原有核心必填字段
    private String peopleNum;    // 出行人数
    private String planDays;     // 旅游天数
    private String travelType;   // 出行类型（亲子/情侣/朋友等）
    // 原有可选字段
    private String travelWay;    // 出行方式
    private List<String> foods;  // 想吃的美食
    private List<String> scenicSpots; // 想去的景点
    private String userExtra;    // 首次额外要求（保留，兼容原有）
    // 新增：后续仅传的纯输入要求（核心新增字段）
    private String message;      // 接收前端后续的userInput

    /**
     * 原有拼接方法：首次全量模式使用（拼接所有表单字段）
     * 保留原有逻辑，不修改，兼容首次传参
     */
    public String userMessages() {
        StringBuilder sb = new StringBuilder("请为我生成一份旅行计划，要求如下：");
        sb.append("出行人数：").append(peopleNum).append("，");
        sb.append("旅游天数：").append(planDays).append("，");
        sb.append("出行类型：").append(travelType).append("，");
        sb.append("出行方式：").append(travelWay == null || travelWay.isBlank() ? "无" : travelWay).append("，");
        sb.append("想吃的美食：").append(foods == null || foods.isEmpty() ? "无" : String.join("、", foods)).append("，");
        sb.append("想去的景点：").append(scenicSpots == null || scenicSpots.isEmpty() ? "无" : String.join("、", scenicSpots)).append("，");
        sb.append("额外要求：").append(userExtra == null || userExtra.isBlank() ? "无" : userExtra);
        return sb.toString();
    }

    /**
     * 新增重载方法：后续仅输入模式使用（历史核心数据 + 新message）
     * @param historyDTO 从Redis缓存读取的用户历史核心AiDTO
     * @param newMessage 前端新传的纯输入要求（message）
     * @return 拼接后的优化要求消息
     */
    public String userMessages(AiDTO historyDTO, String newMessage) {
        StringBuilder sb = new StringBuilder("基于之前的旅行计划（");
        sb.append(historyDTO.getPeopleNum()).append("人/");
        sb.append(historyDTO.getPlanDays()).append("/");
        sb.append(historyDTO.getTravelType()).append("），");
        sb.append("请按照我的新要求优化：").append(newMessage);
        return sb.toString();
    }
}