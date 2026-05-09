package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.entity.Categories;
import edu.cqie.cqdemo.service.impl.CategoriesServiceimpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/api/categories")  // 确认路径
public class CategoriesController {

    @Autowired
    private CategoriesServiceimpl categoriesService;

    @GetMapping("/getcategories")  // 完整路径：/api/categories/getcategories
    @ResponseBody  // 确保添加此注解
    public Result<List<Categories>> getCategories() {
        List<Categories> categories = categoriesService.selectcategories();
        return Result.success(categories);
    }
}

