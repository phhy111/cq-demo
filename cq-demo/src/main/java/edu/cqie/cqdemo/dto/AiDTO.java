package edu.cqie.cqdemo.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class AiDTO implements Serializable {
    private String peopleNum;
    private String planDays;
    private String travelType;
    private String travelWay;
    private List<String> foods;
    private List<String> scenicSpots;
    private String userExtra;
    private String message;
    private String templateCode;

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

    public String userMessages(AiDTO historyDTO, String newMessage) {
        StringBuilder sb = new StringBuilder("基于之前的旅行计划（");
        sb.append(historyDTO.getPeopleNum()).append("人/");
        sb.append(historyDTO.getPlanDays()).append("/");
        sb.append(historyDTO.getTravelType()).append("），");
        sb.append("请按照我的新要求优化：").append(newMessage);
        return sb.toString();
    }
}
