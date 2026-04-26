package edu.cqie.cqdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.cqie.cqdemo.dto.ContentInteractionStatsDTO;
import edu.cqie.cqdemo.entity.Scenics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StatsMapper extends BaseMapper<Scenics> {

    ContentInteractionStatsDTO getContentInteractionStats();
}
