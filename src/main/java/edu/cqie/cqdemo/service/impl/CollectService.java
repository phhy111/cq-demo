package edu.cqie.cqdemo.service.impl;

import edu.cqie.cqdemo.entity.Collections;
import edu.cqie.cqdemo.entity.Food;
import edu.cqie.cqdemo.entity.Routes;
import edu.cqie.cqdemo.entity.Scenics;
import edu.cqie.cqdemo.mapper.CollectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CollectService implements edu.cqie.cqdemo.service.CollectService {
    @Autowired
    private CollectMapper collectMapper;


    @Override
    public List <Food>  selectfood(Long user_id) {
        List<Food>seletctfood=collectMapper.seletctfood(user_id);
        if (seletctfood.size()>0){
            System.out.println("用户有美食收藏");
            System.out.println(seletctfood);
            return seletctfood;
        }else {
            System.out.println("用户无美食收藏");
            return null;
        }
    }

    @Override
    public List <Routes>  seletctroute(Long user_id) {
        return null;
    }

    @Override
    public List <Scenics> selectscenic(Long user_id) {
        return null;
    }
}
