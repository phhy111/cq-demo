package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.entity.Shop;
import edu.cqie.cqdemo.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/api/shop")
@ResponseBody
public class ShopController {
    @Autowired
    private ShopService shopService;
    @GetMapping("/selectallshop/{id}")
    public Result<List<Shop>> index(@PathVariable Integer id) {
        return Result.success(shopService.selectShops(id));
    }
}
