package edu.cqie.cqdemo.mapper;

import edu.cqie.cqdemo.entity.Collections;
import edu.cqie.cqdemo.entity.Food;
import edu.cqie.cqdemo.entity.Routes;
import edu.cqie.cqdemo.entity.Scenics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CollectMapper {
    List<Food> seletctfood(@Param("user_id") Long user_id);
    List<Routes> seletctroute(@Param("user_id") Long user_id);
    List<Scenics> seletctscenic(@Param("user_id") Long user_id);

}
