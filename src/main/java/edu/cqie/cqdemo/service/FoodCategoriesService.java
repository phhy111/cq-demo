package edu.cqie.cqdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.cqie.cqdemo.entity.FoodCategories;

import java.util.List;

public interface FoodCategoriesService extends IService<FoodCategories> {
    List<FoodCategories> getAllStatus();
    List<FoodCategories> selectFoods();
    FoodCategories selectone(Integer id);
}
