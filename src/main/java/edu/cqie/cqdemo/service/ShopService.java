package edu.cqie.cqdemo.service;

import edu.cqie.cqdemo.entity.Shop;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface ShopService {
    List<Shop> selectShops(Integer id);
    int addlike(Integer id);
    int deletelike(Integer id);
    Shop selectshopdetails(Integer id);
    //添加点赞记录
    int addlikehistory(long userid, Integer targetid);
    int deletehistory(long userid, Integer targetid);
    int selecthistory(Integer targetid);
    Shop selectallshop();
}
