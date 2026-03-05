package edu.cqie.cqdemo.service.impl;

import edu.cqie.cqdemo.entity.Shop;
import edu.cqie.cqdemo.mapper.ShopMapper;
import edu.cqie.cqdemo.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class ShopServiceImpI implements ShopService {
    @Autowired
    private ShopMapper shopMapper;
    @Override
    public List<Shop> selectShops(Integer id) {
        List<Shop>selectshops=shopMapper.selectshops(id);
        if (selectshops!=null){
            return selectshops;
        }else {
            return null;
        }
    }
    /**
     * 添加点赞数
     * @param id
     * @return
     */
    @Override
    public int addlike(Integer id) {
        return  shopMapper.addlike(id);
    }
    /**
     * 删除点赞数
     * @param id
     * @return
     */
    @Override
    public int deletelike(Integer id) {
        return shopMapper.deletelike(id);
    }
}
