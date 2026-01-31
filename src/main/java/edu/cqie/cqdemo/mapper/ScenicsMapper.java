package edu.cqie.cqdemo.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.cqie.cqdemo.entity.Scenics;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author
 * @description 针对表【scenics(景点表)】的数据库操作Mapper
 * @createDate 2026-01-30 19:29:27
 * @Entity generator.domain.Scenics
 */
@Mapper
public interface ScenicsMapper extends BaseMapper<Scenics> {
    public List<Scenics> getSlideShowInfo();
}
