package edu.cqie.cqdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.cqie.cqdemo.entity.Collections;
import edu.cqie.cqdemo.entity.FoodCategories;
import edu.cqie.cqdemo.entity.LoginUser;
import edu.cqie.cqdemo.mapper.FoodCategoriesMapper;
import edu.cqie.cqdemo.service.FoodCategoriesService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;
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
    public void updateLikeCountAndCollectCount() {
        // 调用 Mapper 中的更新方法
        foodCategoriesMapper.updateLikeCountAndCollectCount();
    }

    @Override
    public FoodCategories selectone(Integer id) {
        return foodCategoriesMapper.selectone(id);

    }

    @Override
    public int addfoodselcet( Integer targetid) {
        //当前的userid
        long userid=getCurrentUserId();
        //当前时间
        Date date=new Date();
        int result=foodCategoriesMapper.addfoodselcet(userid,targetid,date);
        if (result>0){
            return result;
        }else {
            return 0;
        }
    }

    @Override
    public int deletefoodselcet(Integer targetid) {
        long userid=getCurrentUserId();
        int result=foodCategoriesMapper.deletefoodselcet(userid,targetid);
        return result;
    }

    @Override
    public int selectfoodselcet(Integer targetid) {
        long userid=getCurrentUserId();
       int result= foodCategoriesMapper.selectfoodselcet(userid,targetid);
       return result;
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof LoginUser) {
            return ((LoginUser) principal).getId();
        }
        throw new RuntimeException("用户未登录或令牌无效");
    }

}
