package edu.cqie.cqdemo.service.impl;

import edu.cqie.cqdemo.entity.LoginUser;
import edu.cqie.cqdemo.entity.Shop;
import edu.cqie.cqdemo.mapper.ShopMapper;
import edu.cqie.cqdemo.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;
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
        long userId = getCurrentUserId();
        // 先查询是否已经点赞过
        int exists = shopMapper.selectshoplike(userId, id);
        if (exists == 0) {
            // 未点赞才添加历史记录
            addlikehistory(userId, id);
        }
        return shopMapper.addlike(id);
    }
    /**
     * 删除点赞数并删除点赞历史
     * @param id
     * @return
     */
    @Override
    public int deletelike(Integer id) {
        //调用删除点赞历史
        return shopMapper.deletelike(id);
    }

    @Override
    public Shop selectshopdetails(Integer id) {
        Shop shopdetails=shopMapper.selectshopdetails(id);
        if (shopdetails!=null){
            return shopdetails;
        }else {
            return null;
        }
    }
    //添加点赞历史
    @Override
    public int addlikehistory(long userid, Integer targetid) {
        Date date=new Date();
        return shopMapper.addshoplike(userid,targetid,date);
    }
//删除点赞历史
    @Override
    public int deletehistory(long userid, Integer targetid) {
        return shopMapper.deleteshoplike(userid,targetid);
    }
//查询点赞历史
    @Override
    public int selecthistory( Integer targetid) {
        return  shopMapper.selectshoplike(getCurrentUserId(),targetid);
    }

    @Override
    public List<Shop> selectallshop() {
        return shopMapper.selectallshop();
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof LoginUser) {
            return ((LoginUser) principal).getId();
        }
        throw new RuntimeException("用户未登录或令牌无效");
    }
}
