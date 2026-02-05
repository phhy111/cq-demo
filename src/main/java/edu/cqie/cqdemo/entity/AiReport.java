package edu.cqie.cqdemo.entity;

import lombok.Data;

import java.util.Date;

@Data
public class AiReport {
    String name;
    String guidesTitle;
    String route_intro;//路线简介
    String highlights;//亮点
    Date playDate;//游玩时间
    String content;//具体内容

}
