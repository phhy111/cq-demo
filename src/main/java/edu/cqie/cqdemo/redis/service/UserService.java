package edu.cqie.cqdemo.redis.service;

import edu.cqie.cqdemo.redis.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    @Autowired
    private RedisUtil redisUtil;

    public void cacheUserInfo(String userId, String userInfo) {
        // 调用Redis工具类，缓存用户信息（1小时过期）
        redisUtil.set("user:" + userId, userInfo, 1, TimeUnit.HOURS);
    }
}
