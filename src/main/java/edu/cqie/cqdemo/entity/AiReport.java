package edu.cqie.cqdemo.entity;

import lombok.Data;

import java.util.Date;

@Data
public class AiReport {
    String name;//路线名字
    String strategyTitle;//攻略标题
    String route_intro;//路线简介
    String highlights;//亮点
    String content;//具体内容

}
