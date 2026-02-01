package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.service.RegionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        return Result.success(regionsService.getRegionsIdAndName());
    }
}
