package edu.cqie.cqdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.cqie.cqdemo.entity.Collections;
import edu.cqie.cqdemo.entity.FoodCategories;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface FoodCategoriesMapper extends BaseMapper<FoodCategories> {
    List<FoodCategories> getAllStatus();
    List<FoodCategories>selectFoods();
    FoodCategories selectone( Integer id);
    int addfoodselcet(@Param("userid") Integer userid , @Param("targetid") Integer targetid, @Param("createat") Date date);
}
