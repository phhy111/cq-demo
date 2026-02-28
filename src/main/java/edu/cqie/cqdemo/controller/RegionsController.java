package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.service.RegionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/regions")
public class RegionsController {
    /**
     * 获取区域id与区域名称
     * @return
     */
    @Autowired
    private RegionsService regionsService;

    @GetMapping("/getRegionsIdAndName")
    public Result<Object> getRegionsIdAndName() {
        if (regionsService.getRegionsIdAndName() != null){
            return Result.success(regionsService.getRegionsIdAndName());
        }else {
            return Result.error("获取区域id与区域名称失败");
        }
    }

    /**
     * 区域热度对比数据
     * @return
     */
    @GetMapping("/popularityComparison")
    public List<Map<String, Object>> popularityComparison(){
        if (regionsService.popularityComparison() != null){
            return regionsService.popularityComparison();

        }else {
            return null;
        }
    }
}
