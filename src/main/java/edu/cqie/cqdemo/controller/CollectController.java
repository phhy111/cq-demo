package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;

import edu.cqie.cqdemo.entity.*;
import edu.cqie.cqdemo.service.CollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/collect")
@RequiredArgsConstructor
@Slf4j
public class CollectController {

    private final CollectService collectService;

    /**
     * 获取用户收藏的美食
     */
    @GetMapping("/foods")
    public Result<List<Food>> getUserCollectedFoods() {
        try {
            LoginUser loginUser = getLoginUser();
            Long userId = loginUser.getId();

            List<Food> foods = collectService.selectfood(userId);
            System.out.println("用户获取的数据"+foods);

            log.info("用户 {} 获取收藏美食成功，共{}个", userId, foods.size());
            return Result.success(foods);
        } catch (IllegalAccessException e) {
            log.error("认证失败", e);
            return Result.error("用户未登录或令牌无效");
        } catch (Exception e) {
            log.error("获取用户收藏美食失败", e);
            return Result.error("系统内部错误：" + e.getMessage());
        }
    }

    /**
     * 获取用户收藏的路线
     */
    @GetMapping("/routes")
    public Result<List<Routes>> getUserCollectedRoutes() {
        try {
            LoginUser loginUser = getLoginUser();
            Long userId = loginUser.getId();

            List<Routes> routes = collectService.seletctroute(userId);

            log.info("用户 {} 获取收藏路线成功，共{}个", userId, routes.size());
            return Result.success(routes);
        } catch (IllegalAccessException e) {
            log.error("认证失败", e);
            return Result.error("用户未登录或令牌无效");
        } catch (Exception e) {
            log.error("获取用户收藏路线失败", e);
            return Result.error("系统内部错误：" + e.getMessage());
        }
    }

    /**
     * 获取用户收藏的景点
     */
    @GetMapping("/scenics")
    public Result<List<Scenics>> getUserCollectedScenics() {
        try {
            LoginUser loginUser = getLoginUser();
            Long userId = loginUser.getId();

            List<Scenics> scenics = collectService.selectscenic(userId);

            log.info("用户 {} 获取收藏景点成功，共{}个", userId, scenics.size());
            return Result.success(scenics);
        } catch (IllegalAccessException e) {
            log.error("认证失败", e);
            return Result.error("用户未登录或令牌无效");
        } catch (Exception e) {
            log.error("获取用户收藏景点失败", e);
            return Result.error("系统内部错误：" + e.getMessage());
        }
    }

    /**
     * 获取当前登录用户信息
     */
    private LoginUser getLoginUser() throws IllegalAccessException {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof LoginUser)) {
            throw new IllegalAccessException("用户未登录或令牌无效");
        }
        return (LoginUser) principal;
    }
}
