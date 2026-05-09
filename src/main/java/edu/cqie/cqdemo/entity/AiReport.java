// edu.cqie.cqdemo.entity.AiReport.java
package edu.cqie.cqdemo.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AiReport {
    private String routesTitle;//路线名字
    private String description;//路线描述
    private String traveType;//旅行类型
    private String bestSeason;//最佳季节
    private String guidesTitle;//攻略标题
    private String summary;//摘要
    private String budgetInfo;//预算
}