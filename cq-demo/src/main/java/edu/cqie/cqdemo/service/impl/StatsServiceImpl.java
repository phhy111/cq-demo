package edu.cqie.cqdemo.service.impl;

import edu.cqie.cqdemo.dto.ContentInteractionStatsDTO;
import edu.cqie.cqdemo.mapper.StatsMapper;
import edu.cqie.cqdemo.service.StatsService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class StatsServiceImpl implements StatsService {

    @Resource
    private StatsMapper statsMapper;

    @Override
    public ContentInteractionStatsDTO getContentInteractionStats() {
        return statsMapper.getContentInteractionStats();
    }
}
