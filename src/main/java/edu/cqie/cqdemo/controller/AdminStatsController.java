package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.dto.UserGrowthDTO;
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

    /**
     * 获取用户增长趋势数据（近7天）
     */
    @GetMapping("/user-growth")
    public UserGrowthDTO getUserGrowth() {
        return userStatsService.getUserGrowthData();
    }
}