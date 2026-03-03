package edu.cqie.cqdemo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.entity.*;
import edu.cqie.cqdemo.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容管理控制器
 * 处理内容审核、删除和获取待审核数量的请求
 * 支持：景点、美食、路线、攻略、店铺
 */
@RestController
@RequestMapping("/api/content")
public class ContentController {

    @Autowired
    private ScenicsService scenicsService;

    @Autowired
    private RoutesService routesService;

    @Autowired
    private GuidesService guidesService;

    @Autowired
    private FoodCategoriesService foodCategoriesService;

    private static final Map<String, Integer> TYPE_CODE_MAP = new HashMap<>();
    static {
        TYPE_CODE_MAP.put("scenic", 1);    // 景点
        TYPE_CODE_MAP.put("food", 2);      // 美食
        TYPE_CODE_MAP.put("route", 3);     // 路线
        TYPE_CODE_MAP.put("guide", 4);     // 攻略
        TYPE_CODE_MAP.put("shop", 6);      // 店铺
    }

    /**
     * 审核内容
     * @param target_id 内容 ID
     * @param target_type 内容类型：scenic-景点，food-美食，route-路线，guide-攻略，shop-店铺
     * @param status 审核状态：2-待审核 1-发布，0-草稿
     * @return 审核结果
     */
    @PostMapping("/review")
    public Result<?> reviewContent(@RequestParam Long target_id,
                                   @RequestParam String target_type,
                                   @RequestParam Integer status) {
        try {
            switch (target_type.toLowerCase()) {
                case "scenic":
                    Scenics scenic = scenicsService.getById(target_id.intValue());
                    if (scenic != null) {
                        scenic.setStatus(status);
                        scenicsService.updateById(scenic);
                    }
                    break;
                case "route":
                    Routes route = routesService.getById(target_id.intValue());
                    if (route != null) {
                        route.setStatus(status);
                        routesService.updateById(route);
                    }
                    break;
                case "guide":
                    Guides guide = guidesService.getById(target_id.intValue());
                    if (guide != null) {
                        guide.setStatus(status);
                        guidesService.updateById(guide);
                    }
                    break;
                case "food":
                case "shop":
                    // 美食和店铺使用 foods 表
                    // 这里可以根据需要实现具体逻辑
                    break;
                default:
                    return Result.error("不支持的内容类型");
            }
            return Result.success("审核成功");
        } catch (Exception e) {
            return Result.error("审核失败：" + e.getMessage());
        }
    }

    /**
     * 删除内容
     * @param target_id 内容 ID
     * @param target_type 内容类型：scenic-景点，food-美食，route-路线，guide-攻略，shop-店铺
     * @return 删除结果
     */
    @DeleteMapping("/{type}/{contentId}")
    public Result<?> deleteContent(@RequestParam Long target_id,
                                   @RequestParam String target_type) {
        try {
            switch (target_type.toLowerCase()) {
                case "scenic":
                    scenicsService.removeById(target_id.intValue());
                    break;
                case "route":
                    routesService.removeById(target_id.intValue());
                    break;
                case "guide":
                    guidesService.removeById(target_id.intValue());
                    break;
                case "food":
                case "shop":
                    // 美食和店铺删除逻辑
                    break;
                default:
                    return Result.error("不支持的内容类型");
            }
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 获取待审核数量
     * @param target_type 内容类型：scenic-景点，food-美食，route-路线，guide-攻略，shop-店铺
     * @return 待审核总数
     */
    @GetMapping("/pending-count")
    public Result<?> getPendingReviewCount(@RequestParam(required = false) String target_type){
        try {
            Map<String, Object> data = new HashMap<>();
            Long total = 0L;
            
            if (target_type == null || target_type.isEmpty()) {
                // 统计所有类型的待审核数量
                Long scenicCount = scenicsService.count(new LambdaQueryWrapper<Scenics>().eq(Scenics::getStatus, 2));
                Long routeCount = routesService.count(new LambdaQueryWrapper<Routes>().eq(Routes::getStatus, 2));
                Long guideCount = guidesService.count(new LambdaQueryWrapper<Guides>().eq(Guides::getStatus, 2));
                total = scenicCount + routeCount + guideCount;
                
                data.put("scenic", scenicCount);
                data.put("route", routeCount);
                data.put("guide", guideCount);
                data.put("food", 0);
                data.put("shop", 0);
            } else {
                // 统计指定类型的待审核数量
                switch (target_type.toLowerCase()) {
                    case "scenic":
                        total = scenicsService.count(new LambdaQueryWrapper<Scenics>().eq(Scenics::getStatus, 2));
                        break;
                    case "route":
                        total = routesService.count(new LambdaQueryWrapper<Routes>().eq(Routes::getStatus, 2));
                        break;
                    case "guide":
                        total = guidesService.count(new LambdaQueryWrapper<Guides>().eq(Guides::getStatus, 2));
                        break;
                    case "food":
                    case "shop":
                        total = 0L;
                        break;
                    default:
                        return Result.error("不支持的内容类型");
                }
            }
            
            data.put("total", total);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取待审核数量失败：" + e.getMessage());
        }
    }

    /**
     * 获取待审核列表
     * @param target_type 内容类型：scenic-景点，food-美食，route-路线，guide-攻略，shop-店铺
     * @param page 页码
     * @param pageSize 每页数量
     * @return 待审核列表
     */
    @GetMapping("/pending-list")
    public Result<?> getPendingReviewList(@RequestParam(required = false) String target_type,
                                          @RequestParam(required = false, defaultValue = "1") Integer page,
                                          @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            if (target_type == null || target_type.isEmpty()) {
                // 返回所有类型的待审核数据
                List<Scenics> scenics = scenicsService.list(new LambdaQueryWrapper<Scenics>()
                    .eq(Scenics::getStatus, 2)
                    .last("LIMIT " + ((page - 1) * pageSize) + ", " + pageSize));
                
                List<Routes> routes = routesService.list(new LambdaQueryWrapper<Routes>()
                    .eq(Routes::getStatus, 2)
                    .last("LIMIT " + ((page - 1) * pageSize) + ", " + pageSize));
                
                List<Guides> guides = guidesService.list(new LambdaQueryWrapper<Guides>()
                    .eq(Guides::getStatus, 2)
                    .last("LIMIT " + ((page - 1) * pageSize) + ", " + pageSize));
                
                result.put("scenic", scenics);
                result.put("route", routes);
                result.put("guide", guides);
                result.put("food", new HashMap<>());
                result.put("shop", new HashMap<>());
            } else {
                // 返回指定类型的待审核数据
                switch (target_type.toLowerCase()) {
                    case "scenic":
                        Page<Scenics> scenicPage = scenicsService.page(new Page<>(page, pageSize), 
                            new LambdaQueryWrapper<Scenics>().eq(Scenics::getStatus, 2));
                        result.put("list", scenicPage.getRecords());
                        result.put("total", scenicPage.getTotal());
                        break;
                    case "route":
                        Page<Routes> routePage = routesService.page(new Page<>(page, pageSize),
                            new LambdaQueryWrapper<Routes>().eq(Routes::getStatus, 2));
                        result.put("list", routePage.getRecords());
                        result.put("total", routePage.getTotal());
                        break;
                    case "guide":
                        Page<Guides> guidePage = guidesService.page(new Page<>(page, pageSize),
                            new LambdaQueryWrapper<Guides>().eq(Guides::getStatus, 2));
                        result.put("list", guidePage.getRecords());
                        result.put("total", guidePage.getTotal());
                        break;
                    case "food":
                    case "shop":
                        result.put("list", new HashMap<>());
                        result.put("total", 0);
                        break;
                    default:
                        return Result.error("不支持的内容类型");
                }
            }
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取待审核列表失败：" + e.getMessage());
        }
    }
}
