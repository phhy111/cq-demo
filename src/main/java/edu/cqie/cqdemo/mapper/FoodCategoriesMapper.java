package edu.cqie.cqdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.cqie.cqdemo.entity.FoodCategories;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FoodCategoriesMapper extends BaseMapper<FoodCategories> {
    List<FoodCategories> getAllStatus();
    List<FoodCategories>selectFoods();

    /**
     * 更新美食的点赞数、收藏数和评论数
     */

    FoodCategories selectone( Integer id);

    void updateLikeCountAndCollectCount();

}
