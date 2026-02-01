package edu.cqie.cqdemo.mapper;

import edu.cqie.cqdemo.entity.Regions;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* @author
* @description 针对表【regions(区域表)】的数据库操作Mapper
* @createDate 2026-01-31 21:31:23
* @Entity edu.cqie.cqdemo.entity.Regions
*/
@Mapper
public interface RegionsMapper extends BaseMapper<Regions> {
    /**
     * 获取区域id与区域名称
     * @return
     */
    public List<Regions> getRegionsIdAndName();
}




