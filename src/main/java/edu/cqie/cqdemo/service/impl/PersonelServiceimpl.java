package edu.cqie.cqdemo.service.impl;

import edu.cqie.cqdemo.entity.Users;
import edu.cqie.cqdemo.mapper.PersonerlMapper;
import edu.cqie.cqdemo.service.PersonerlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PersonelServiceimpl implements PersonerlService {
    @Autowired
    private PersonerlMapper personerlMapper;
    @Override
    public Users getUser(Long id) {
        return personerlMapper.selectuser(id);
    }
    //修改用户手机号

    @Override
    public int updateUserphone(Long id) {
        int result=personerlMapper.updateUserphone(id);
        return result;
    }

    @Override
    public int updateUserpassword(Long id) {
        return 0;
    }

    @Override
    public int updateUseremail(Long id) {
        return 0;
    }
}
