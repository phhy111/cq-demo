package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.entity.LoginUser;
import edu.cqie.cqdemo.entity.Shop;
import edu.cqie.cqdemo.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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
    @PostMapping("/addlike/{id}")
    public Result<String> addLike(@PathVariable Integer id) {
        int result = shopService.addlike(id);
        if (result>0){
            return Result.success("添加成功");
        }else {
            return Result.error("添加失败");
        }
    }
    @PostMapping("/deletelike/{id}")
    public Result<String> deleteLike(@PathVariable Integer id) {
        int result = shopService.deletelike(id);
        if (result>0){
            return Result.success("删除成功");
        }else {
            return Result.error("删除失败");
        }
    }
    //查询店铺详情
    @GetMapping("/selectshopdetails/{id}")
    public Result<Shop> selectshopdetails(@PathVariable Integer id) {
        return Result.success(shopService.selectshopdetails(id));
    }
    //查询用户是否已经点赞
    @PostMapping("/selecthistory/{id}")
    public Result<Integer> selecthistory(@PathVariable Integer targetid) {
        return Result.success(shopService.selecthistory(targetid));
    }
    @PostMapping("/selectallshop")
    public Result<Shop> selectallshop() {
        return Result.success(shopService.selectallshop());
    }
}


