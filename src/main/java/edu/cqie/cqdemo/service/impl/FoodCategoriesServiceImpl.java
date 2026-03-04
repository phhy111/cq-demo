package edu.cqie.cqdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.cqie.cqdemo.entity.FoodCategories;
import edu.cqie.cqdemo.mapper.FoodCategoriesMapper;
import edu.cqie.cqdemo.service.FoodCategoriesService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FoodCategoriesServiceImpl extends ServiceImpl<FoodCategoriesMapper, FoodCategories> implements FoodCategoriesService {
    @Autowired
    private FoodCategoriesMapper foodCategoriesMapper;
    @Override
    public List<FoodCategories> getAllStatus() {
        return list();
    }

    @Override
    public List<FoodCategories> selectFoods() {
      List<FoodCategories>foodCategories=foodCategoriesMapper.selectFoods();
      if (foodCategories!=null){
          return foodCategories;}else {
          return null;
      }
    }

    @Override
    public FoodCategories selectone(Integer id) {
        return foodCategoriesMapper.selectone(id);
    }
}
