package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.entity.Routes;
import edu.cqie.cqdemo.service.RoutesService;
import org.springframework.beans.factory.annotation.Autowired;
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
    @RequestMapping("/GetRoutesListInfo")
    public Result<List<Routes>> getRoutesListInfo() {
        List<Routes> routesList = routesService.getRoutesListInfo();
        return Result.success(routesList);
    }
}
