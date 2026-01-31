package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.entity.Scenics;
import edu.cqie.cqdemo.service.ScenicsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/scenics")
public class ScenicsController {
    /**
     * 查询景点信息
     * @return
     */
    @Autowired
    private ScenicsService scenicsService;
    @GetMapping("/GetScenicsInfo")
    public Result<List<Scenics>> getScenicsInfo() {
       List<Scenics> scenicsList = scenicsService.list();
       return Result.success(scenicsList);
    }
    /**
     * 获取轮播图信息
     * @return
     */
    @GetMapping("/GetSlideShowInfo")
    public Result<List<Scenics>> getSlideShowInfo() {
        List<Scenics> scenicsList = scenicsService.getSlideShowInfo();
        return Result.success(scenicsList);
    }

}
