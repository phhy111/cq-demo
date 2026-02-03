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
    public Users getUser() {
        return personerlMapper.selectuser();
    }
}
