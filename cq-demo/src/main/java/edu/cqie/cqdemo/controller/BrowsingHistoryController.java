package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.entity.BrowsingHistory;
import edu.cqie.cqdemo.service.BrowsingHistoryService;
import edu.cqie.cqdemo.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
public class BrowsingHistoryController {

    @Autowired
    private BrowsingHistoryService browsingHistoryService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 从请求头获取用户ID
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = jwtUtil.getTokenFromRequest(request);
        if (token == null) {
            return null;
        }
        try {
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 添加浏览历史
     */
    @PostMapping("/add")
    public Result addBrowsingHistory(HttpServletRequest request, @RequestParam Long businessId, @RequestParam Integer type) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error("用户未登录");
        }

        try {
            browsingHistoryService.addBrowsingHistory(userId, businessId, type);
            return Result.success("添加浏览历史成功");
        } catch (Exception e) {
            return Result.error("添加浏览历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户浏览历史（按类型）
     */
    @GetMapping("/byType")
    public Result getBrowsingHistoryByType(HttpServletRequest request, @RequestParam Integer type) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error("用户未登录");
        }

        try {
            List<BrowsingHistory> historyList = browsingHistoryService.getBrowsingHistoryByType(userId, type);
            return Result.success(historyList);
        } catch (Exception e) {
            return Result.error("获取浏览历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户所有浏览历史
     */
    @GetMapping("/all")
    public Result getAllBrowsingHistory(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error("用户未登录");
        }

        try {
            List<BrowsingHistory> historyList = browsingHistoryService.getAllBrowsingHistory(userId);
            return Result.success(historyList);
        } catch (Exception e) {
            return Result.error("获取浏览历史失败: " + e.getMessage());
        }
    }

    /**
     * 删除浏览历史
     */
    @DeleteMapping("/delete")
    public Result deleteBrowsingHistory(HttpServletRequest request, @RequestParam Long businessId, @RequestParam Integer type) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return Result.error("用户未登录");
        }

        try {
            browsingHistoryService.deleteBrowsingHistory(userId, businessId, type);
            return Result.success("删除浏览历史成功");
        } catch (Exception e) {
            return Result.error("删除浏览历史失败: " + e.getMessage());
        }
    }
}
