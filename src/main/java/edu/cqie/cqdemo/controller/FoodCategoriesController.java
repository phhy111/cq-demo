package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.entity.FoodCategories;
import edu.cqie.cqdemo.service.FoodCategoriesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/foods")
public class FoodCategoriesController {

    @Autowired
    private FoodCategoriesService foodscategoriesService;

    /**
     * 查询所有状态的美食数据
     */
    @GetMapping("/GetFoodInfo")
    public Result<List<FoodCategories>> getFoodInfo() {
        List<FoodCategories> foodsList = foodscategoriesService.list();
        if (foodsList != null) {
            return Result.success(foodsList);
        } else {
            return Result.error("查询失败");
        }
    }

    /**
     * 更新美食状态，将待审核（2）状态更新为发布（1）状态
     */
    @PostMapping("/updateFoodStatus")
    public Result updateFoodStatus(@RequestParam Integer id) {
        try {
            FoodCategories food = foodscategoriesService.getById(id);
            if (food != null && food.getStatus() == 2) {
                food.setStatus(1);
                foodscategoriesService.updateById(food);
                return Result.success("审核通过成功");
            } else {
                return Result.error("美食不存在或状态不是待审核");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("审核通过失败：" + e.getMessage());
        }
    }

    /**
     * 保存美食数据（新增或编辑）
     */
    @PostMapping("/saveFood")
    public Result saveFood(@RequestBody FoodCategories food) {
        try {
            if (food.getId() != null) {
                // 编辑美食
                foodscategoriesService.updateById(food);
                return Result.success("编辑成功");
            } else {
                // 新增美食
                food.setStatus(2); // 默认状态为待审核
                food.setCreatedAt(new Date());
                foodscategoriesService.save(food);
                return Result.success("新增成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("保存失败：" + e.getMessage());
        }
    }
//    已发布的
    @GetMapping("/selectFoods")
    public Result<List<FoodCategories>> selectFoods(){
          List<FoodCategories> foods = foodscategoriesService.selectFoods();
          return Result.success(foods);}

    /**
     * 更新美食的点赞数、收藏数和评论数
     */
    @PostMapping("/updateLikeCountAndCollectCount")
    public Result updateLikeCountAndCollectCount() {
        try {
            foodscategoriesService.updateLikeCountAndCollectCount();
            return Result.success("更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新失败：" + e.getMessage());
        }
    }
    /**
     * 根据id查询美食数据
     */
    @PostMapping("/selectone/{id}")
    public Result<FoodCategories> selectone(@PathVariable Integer id){
        FoodCategories food = foodscategoriesService.selectone(id);
        return Result.success(food);
    }
    @PostMapping("/addfoodselcet")
    public Result<String> addfoodselcet(@RequestParam Integer targetid){
        System.out.println("传入的美食id"+targetid);
        int result=foodscategoriesService.addfoodselcet(targetid);
        if(result>0){
            return Result.success("成功添加收藏");
        }else {
            return Result.success("添加收藏失败");
        }
    }
    /**
     * 取消美食收藏
     */
    @PostMapping("/deletefoodselcet")
    public Result<String> deletefoodselcet(@RequestParam Integer targetid){
        int result=foodscategoriesService.deletefoodselcet(targetid);
        if(result>0){
            return Result.success("成功删除收藏");
        }else {
            return Result.success("删除收藏失败");
        }
    }
    /**
     * 判断当前美食是否已收藏
     */
    @PostMapping("/selectfoodselcet")
    public Result<Integer> selectfoodselcet(@RequestParam Integer targetid){
        int result=foodscategoriesService.selectfoodselcet(targetid);
        return Result.success(result);
    }
    @GetMapping("/getAllStatusFoods")
    public Result<List<FoodCategories>> getAllStatusFoods(){
        List<FoodCategories> foods = foodscategoriesService.selectallFoods();
        return Result.success(foods);
    }
    @PostMapping("/save")
    public Result save(@RequestBody FoodCategories foodCategories){
        try {
            // 设置默认值
            if (foodCategories.getStatus() == null) {
                foodCategories.setStatus(2); // 待审核
            }
            if (foodCategories.getCreatedAt() == null) {
                foodCategories.setCreatedAt(new Date());
            }

            int result = foodscategoriesService.saveFood(foodCategories);
            if (result > 0) {
                return Result.success("保存成功");
            } else {
                return Result.error("保存失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("保存失败：" + e.getMessage());
        }
    }
}