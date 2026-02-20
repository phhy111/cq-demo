package edu.cqie.cqdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.cqie.cqdemo.entity.Users;

public interface UserService extends IService<Users> {
    String getUserName(Long id);
    Users getUserById(Long id);
}
