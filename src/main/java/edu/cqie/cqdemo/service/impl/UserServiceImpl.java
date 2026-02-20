package edu.cqie.cqdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.cqie.cqdemo.entity.Users;
import edu.cqie.cqdemo.mapper.UserMapper;
import edu.cqie.cqdemo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper,Users>implements UserService {

    @Autowired
    private UserMapper userMapper;

    public String getUserName(Long id)
    {
        return userMapper.getUserName(id);

    }

    @Override
    public Users getUserById(Long id) {
        return userMapper.selectById(id);
    }

}
