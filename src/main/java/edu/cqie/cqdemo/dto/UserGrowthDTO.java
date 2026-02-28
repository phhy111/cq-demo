package edu.cqie.cqdemo.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户增长数据DTO
 */
@Data
public class UserGrowthDTO {
    // 近7天日期列表（如：["2026-02-18", "2026-02-19"...]）
    private List<String> dates;
    // 对应日期的新增用户数
    private List<Integer> newUser;
    // 对应日期的活跃用户数
    private List<Integer> activeUser;
}