// edu.cqie.cqdemo.entity.AiReport.java
package edu.cqie.cqdemo.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AiReport {
    @JsonProperty("name")
    private String name; // 路线名字

    @JsonProperty("strategyTitle")
    private String strategyTitle; // 攻略标题

    @JsonProperty("routeIntro")
    private String routeIntro; // 注意：Java 驼峰，JSON 下划线

    @JsonProperty("highlights")
    private String highlights; // 亮点

    @JsonProperty("content")
    private String content; // 具体内容（Markdown 字符串）
}