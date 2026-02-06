package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.entity.Users;
import edu.cqie.cqdemo.service.PersonerlService;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/personel")
public class PersonelController {
    @Autowired
    private PersonerlService personerlService;

    //获取前端请求，将用户数据发给前端
    @PostMapping("/getuser")
    public Users getUserbyid(@Param("id") Long id){
        Users user=personerlService.getUser(id);
        return user;
    }
}
