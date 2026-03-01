package edu.cqie.cqdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.cqie.cqdemo.entity.Users;

import java.util.List;
import java.util.Map;

public interface UserService extends IService<Users> {
    String getUserName(Long id);
    Users getUserById(Long id);
    List<Users> getUsersInfo();

}
