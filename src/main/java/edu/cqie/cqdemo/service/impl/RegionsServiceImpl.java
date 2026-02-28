package edu.cqie.cqdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.cqie.cqdemo.entity.Regions;
import edu.cqie.cqdemo.service.RegionsService;
import edu.cqie.cqdemo.mapper.RegionsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
* @author
* @description 针对表【regions(区域表)】的数据库操作Service实现
* @createDate 2026-01-31 21:31:23
*/
@Service
public class RegionsServiceImpl extends ServiceImpl<RegionsMapper, Regions>
    implements RegionsService{
    /**
     * 获取区域id与区域名称
     * @return
     */
    @Autowired
    private RegionsMapper regionsMapper;
    @Override
    public List<Regions> getRegionsIdAndName() {
        return regionsMapper.getRegionsIdAndName();
    }

    @Override
    public List<Map<String, Object>> popularityComparison() {
        return regionsMapper.popularityComparison();
    }
}




