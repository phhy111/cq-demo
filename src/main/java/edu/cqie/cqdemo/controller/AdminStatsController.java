package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.dto.ContentInteractionStatsDTO;
import edu.cqie.cqdemo.dto.UserGrowthDTO;
import edu.cqie.cqdemo.service.StatsService;
import edu.cqie.cqdemo.service.impl.UserStatsService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 管理员统计接口
 */
@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    @Resource
    private UserStatsService userStatsService;

    @Resource
    private StatsService statsService;

    /**
     * 获取用户增长趋势数据（近7天）
     */
    @GetMapping("/user-growth")
    public UserGrowthDTO getUserGrowth() {
        return userStatsService.getUserGrowthData();
    }

    /**
     * 获取近一个月的内容发布数以及互动量
     */
    @GetMapping("/content-interaction")
    public ContentInteractionStatsDTO getContentInteractionStats() {
        return statsService.getContentInteractionStats();
    }
}