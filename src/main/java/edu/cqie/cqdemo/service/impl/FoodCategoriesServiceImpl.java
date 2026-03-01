package edu.cqie.cqdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.cqie.cqdemo.entity.FoodCategories;
import edu.cqie.cqdemo.mapper.FoodCategoriesMapper;
import edu.cqie.cqdemo.service.FoodCategoriesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FoodCategoriesServiceImpl extends ServiceImpl<FoodCategoriesMapper, FoodCategories> implements FoodCategoriesService {

    @Autowired
    private FoodCategoriesMapper foodCategoriesMapper;

    public List<FoodCategories> getAllStatus()
    {
        List<FoodCategories> foodCategoriesList=foodCategoriesMapper.getAllStatus();

        return foodCategoriesList;
    }
}
