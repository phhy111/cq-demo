package edu.cqie.cqdemo.service;

import edu.cqie.cqdemo.entity.Shop;

import java.util.List;

public interface ShopService {
    List<Shop> selectShops(Integer id);
    int addlike(Integer id);
    int deletelike(Integer id);
    Shop selectshopdetails(Integer id);
}
