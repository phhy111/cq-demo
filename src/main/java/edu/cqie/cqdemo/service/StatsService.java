package edu.cqie.cqdemo.service;

import edu.cqie.cqdemo.dto.ContentInteractionStatsDTO;

public interface StatsService {
    /**
     * 获取近一个月的内容发布数以及互动量
     */
    ContentInteractionStatsDTO getContentInteractionStats();
}
