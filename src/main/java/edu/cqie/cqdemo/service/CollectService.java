package edu.cqie.cqdemo.service;

import edu.cqie.cqdemo.entity.Collections;
import edu.cqie.cqdemo.entity.Food;
import edu.cqie.cqdemo.entity.Routes;
import edu.cqie.cqdemo.entity.Scenics;

import java.util.List;

public interface CollectService {
   List <Food> selectfood(Long user_id);
    List <Routes>  seletctroute(Long user_id);
    List <Scenics>  selectscenic(Long user_id);
}
