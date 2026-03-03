package edu.cqie.cqdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.dto.ScenicsAndRegionDTO;
import edu.cqie.cqdemo.entity.Users;
import edu.cqie.cqdemo.mapper.UserBehaviorLogsMapper;
import edu.cqie.cqdemo.mapper.UserMapper;
import edu.cqie.cqdemo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper,Users>implements UserService {

    @Autowired
    private UserMapper userMapper;
    public String getUserName(Long id) {
        return userMapper.getUserName(id);
    }

    @Override
    public Users getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public List<Users> getUsersInfo() {
            return userMapper.getUsersInfo();
    }

    @Override
    public boolean toggleUserStatus(Long id) {
        Users users = this.getById( id);
        if (users == null){
            return false;
        }else {
            Integer currentStatus = users.getUserStatus();
            if (currentStatus == 1){
                users.setUserStatus(0);
            }else if (currentStatus == 0){
                users.setUserStatus(1);
            }else{
                return false;
            }
        }
        return this.updateById(users);
    }

    @Override
    public boolean resetPassword(Long id, String password) {
        return userMapper.resetPassword(id, password);
    }

    @Override
    public boolean batchDisable(List<Long> ids) {
        return userMapper.batchDisable(ids);
    }



}
