package edu.cqie.cqdemo.mapper;

import edu.cqie.cqdemo.entity.Shop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
//美食
public interface ShopMapper {
    List<Shop> selectshops(@Param("id") Integer id);
    int addlike(@Param("id") Integer id);
    int deletelike(@Param("id") Integer id);
    Shop selectshopdetails(@Param("id") Integer id);
    int deleteshoplike(@Param("userid") long userid , @Param("targetid") Integer targetid);
    int selectshoplike(@Param("userid") long userid,@Param("targetid") Integer targetid);
    int addshoplike(@Param("userid") long userid,@Param("targetid") Integer targetid, @Param("createat") Date date);
    List<Shop> selectallshop();
}
