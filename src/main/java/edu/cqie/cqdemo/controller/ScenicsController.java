package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.dto.ScenicsDTO;
import edu.cqie.cqdemo.entity.Scenics;
import edu.cqie.cqdemo.service.ScenicsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    /**
     * 获取所有景点信息
     * @return
     */
    @GetMapping("/GetScenicsInfo")
    public Result<List<Scenics>> getScenicsInfo() {
       List<Scenics> scenicsList = scenicsService.list();
       if (scenicsList != null){
           return Result.success(scenicsList);
       }else {
           return Result.error("未查询到景点信息");
       }
    }
    /**
     * 获取轮播图信息
     * @return
     */
    @GetMapping("/GetSlideShowInfo")
    public Result<List<Scenics>> getSlideShowInfo() {
        List<Scenics> scenicsList = scenicsService.getSlideShowInfo();
        if (scenicsList != null){
            return Result.success(scenicsList);
        }else {
            return Result.error("未查询到轮播图信息");
        }
    }

    /**
     * 获取对应区域的景点信息
     * @return
     */
    @GetMapping("/GetScenicsInfoByRegionId")
    public Result<List<Scenics>> getScenicsInfoByRegionId(Integer regionId) {
        List<Scenics> scenicsList = scenicsService.getScenicsInfoByRegionId(regionId);
        if (scenicsList != null){
            return Result.success(scenicsList);
        }else {
            return Result.error("未查询到该区域下的景点信息");
        }
    }
    /**
     * 获取不同等级的景点信息
     * @return
     */
    @GetMapping("/GetScenicsInfoByLevel")
    public Result<List<Scenics>> getScenicsInfoByLevel(String level) {
        List<Scenics> scenicsList = scenicsService.getScenicsInfoByLevel(level);
        if (scenicsList != null){
            return Result.success(scenicsList);
        }else {
            return Result.error("未查询到该等级的景点信息");
        }
    }
    /**
     * 按评分高低获取景点信息
     * @return
     */
    @GetMapping("/GetScenicsInfoByScore")
    public Result<List<Scenics>> getScenicsInfoByScore() {
        List<Scenics> scenicsList = scenicsService.getScenicsInfoByScore();
        if (scenicsList != null){
            return Result.success(scenicsList);
        }else {
            return Result.error("未查询到该评分的景点信息");
        }
    }
    /**
     * 按点赞量排序
     * @return
     */
    @GetMapping("/GetScenicsInfoByLike")
    public Result<List<Scenics>> getScenicsInfoByLike() {
        List<Scenics> scenicsList = scenicsService.getScenicsInfoByLikeCount();
        if (scenicsList != null){
            return Result.success(scenicsList);
        }else {
            return Result.error("未查询到该点赞量的景点信息");
        }
    }
    /**
     * 添加景点浏览量
     * @return
     */
    @PostMapping("/AddViewCount")
    public Result<String> addViewCount(Integer id) {
        boolean result = scenicsService.addViewCount(id);
        if (result){
            return Result.success("添加成功");
        }else {
            return Result.error("添加失败");
        }
    }
    /**
     * 获取景点详情信息
     * @return
     */
    @GetMapping("/GetScenicsDetailInfoById")
    public Result<List<ScenicsDTO>> getScenicsDetailInfoById(Integer id) {
        List<ScenicsDTO> scenicsDTOList = scenicsService.getScenicsDetailInfoById(id);
        if (scenicsDTOList != null){
            return Result.success(scenicsDTOList);
        }else {
            return Result.error("未查询到该景点详情信息");
        }
    }
}
