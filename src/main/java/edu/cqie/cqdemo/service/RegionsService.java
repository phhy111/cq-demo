package edu.cqie.cqdemo.service;

import edu.cqie.cqdemo.entity.Regions;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
* @author
* @description 针对表【regions(区域表)】的数据库操作Service
* @createDate 2026-01-31 21:31:23
*/
public interface RegionsService extends IService<Regions> {
    /**
     * 获取区域id与区域名称
     * @return
     */
    public List<Regions> getRegionsIdAndName();

    List<Map<String,Object>> popularityComparison();
}
