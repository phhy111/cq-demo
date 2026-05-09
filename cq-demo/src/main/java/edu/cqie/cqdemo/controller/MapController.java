package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.ai.map.MapService;
import edu.cqie.cqdemo.annotation.RateLimit;
import edu.cqie.cqdemo.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/map")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class MapController {

    @Autowired
    private MapService mapService;

    @GetMapping("/geocode")
    @RateLimit(limit = 60, timeoutSeconds = 60, key = "map-geocode")
    public Result<Map<String, Object>> geocode(@RequestParam String address) {
        try {
            if (address == null || address.isBlank()) {
                return Result.error("地址不能为空");
            }
            Map<String, Object> result = mapService.geocode(address);
            if (result == null) {
                return Result.error("未找到该地址");
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("地理编码失败", e);
            return Result.error("地理编码失败：" + e.getMessage());
        }
    }

    @PostMapping("/route")
    @RateLimit(limit = 30, timeoutSeconds = 60, key = "map-route")
    public Result<Map<String, Object>> getRoute(@RequestBody List<String> waypoints) {
        try {
            if (waypoints == null || waypoints.size() < 2) {
                return Result.error("路线规划需要至少2个途经点");
            }
            Map<String, Object> result = mapService.getRoute(waypoints);
            if (result == null) {
                return Result.error("路线规划失败");
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("路线规划失败", e);
            return Result.error("路线规划失败：" + e.getMessage());
        }
    }

    @GetMapping("/nearby")
    @RateLimit(limit = 60, timeoutSeconds = 60, key = "map-nearby")
    public Result<Map<String, Object>> searchNearby(
            @RequestParam String keyword,
            @RequestParam double longitude,
            @RequestParam double latitude,
            @RequestParam(defaultValue = "3000") int radius) {
        try {
            if (keyword == null || keyword.isBlank()) {
                return Result.error("搜索关键词不能为空");
            }
            Map<String, Object> result = mapService.searchNearby(keyword, longitude, latitude, radius);
            if (result == null) {
                return Result.error("搜索周边失败");
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("搜索周边失败", e);
            return Result.error("搜索周边失败：" + e.getMessage());
        }
    }

    @PostMapping("/visualization")
    @RateLimit(limit = 30, timeoutSeconds = 60, key = "map-visualization")
    public Result<Map<String, String>> getRouteVisualization(@RequestBody List<String> waypoints) {
        try {
            if (waypoints == null || waypoints.isEmpty()) {
                return Result.error("途经点不能为空");
            }
            String visualization = mapService.generateRouteVisualization(waypoints);
            Map<String, String> result = new java.util.HashMap<>();
            result.put("visualization", visualization);
            return Result.success(result);
        } catch (Exception e) {
            log.error("生成路线可视化失败", e);
            return Result.error("生成路线可视化失败：" + e.getMessage());
        }
    }
}
