package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.entity.Routes;
import edu.cqie.cqdemo.service.RoutesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RoutesController {
    /**
     * 查询路线信息
     * @return
     */

    @Autowired
    private RoutesService routesService;

    //查询的前五
    @RequestMapping("/GetRoutesListInfo")
    public Result<List<Routes>> getRoutesListInfo() {
        List<Routes> routesList = routesService.getRoutesListInfo();
        if (routesList != null){
            return Result.success(routesList);
        }else {
            return Result.error("查询失败");
        }
    }
    //时间升序
    @GetMapping("/getRoutesMessageTimeS")
    public Result<List<Routes>> getRouesAllMessageS(){
        List<Routes> allRoutes = routesService.getRoutesListInfoTimeS();
        return Result.success(allRoutes);
    }
    //时间降序
    @GetMapping("/getRoutesMessageTimeJ")
    public Result<List<Routes>> getRouesAllMessageJ()
    {
        List<Routes> allRoutes = routesService.getRoutesListInfoTimeJ();
        return Result.success(allRoutes);
    }

    @GetMapping("/getRoutesHeatS")
    public Result<List<Routes>> getRoutesHeatS(){
        List<Routes> allHeatS =routesService.getAllHeatS();
        return Result.success(allHeatS);
    }

    @GetMapping("/getRoutesHeatJ")
    public Result<List<Routes>> getRoutesHeatJ(){
        List<Routes> allHeatJ =routesService.getAllHeatJ();
        return Result.success(allHeatJ);
    }
}
