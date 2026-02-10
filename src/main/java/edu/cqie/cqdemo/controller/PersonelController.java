package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.entity.Users;
import edu.cqie.cqdemo.service.PersonerlService;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

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
    @PostMapping("/updatePhone")
    public Map<String,Object> updateUserphone(@RequestBody Users request){
        System.out.println("获取前端的id"+request.getId());
        System.out.println("获取前端的phone"+request.getPhone());
        int result=personerlService.updateUserphone(request.getId(),request.getPhone());
        Map<String,Object> response=new HashMap<>();
        if(result>0){
            response.put("code",200);
            response.put("message","修改成功");
        }else {
            response.put("code",500);
            response.put("message","修改失败");
        }
        return response;
    }
}
