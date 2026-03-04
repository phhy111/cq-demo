package edu.cqie.cqdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.cqie.cqdemo.entity.Collections;
import edu.cqie.cqdemo.entity.FoodCategories;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface FoodCategoriesService extends IService<FoodCategories> {
    List<FoodCategories> getAllStatus();
    List<FoodCategories> selectFoods();
    FoodCategories selectone(Integer id);
    int addfoodselcet(Integer userid ,Integer targetid, Date date);
}
