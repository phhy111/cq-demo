package edu.cqie.cqdemo.mapper;

import edu.cqie.cqdemo.entity.Shop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
//美食
public interface ShopMapper {
    List<Shop> selectshops(@Param("id") Integer id);
    int addlike(@Param("id") Integer id);
    int deletelike(@Param("id") Integer id);
}
