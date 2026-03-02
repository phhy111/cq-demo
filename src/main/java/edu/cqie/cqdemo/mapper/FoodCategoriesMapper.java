package edu.cqie.cqdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.cqie.cqdemo.entity.FoodCategories;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FoodCategoriesMapper extends BaseMapper<FoodCategories> {
    List<FoodCategories> getAllStatus();
    List<FoodCategories>selectFoods();
}
