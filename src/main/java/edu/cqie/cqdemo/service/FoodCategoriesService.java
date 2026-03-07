package edu.cqie.cqdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.cqie.cqdemo.entity.FoodCategories;

import java.util.Date;
import java.util.List;

public interface FoodCategoriesService extends IService<FoodCategories> {
    List<FoodCategories> getAllStatus();
    List<FoodCategories> selectFoods();
    /**
     * 更新美食的点赞数、收藏数和评论数
     */
    void updateLikeCountAndCollectCount();

    FoodCategories selectone(Integer id);

    int addfoodselcet( Integer targetid);
    int deletefoodselcet(Integer targetid);
    int selectfoodselcet(Integer targetid);
    List<FoodCategories>selectallFoods();
    int saveFood(FoodCategories foodCategories);

}
