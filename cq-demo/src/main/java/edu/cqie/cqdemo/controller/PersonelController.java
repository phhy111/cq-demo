package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.entity.Users;
import edu.cqie.cqdemo.service.PersonerlService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/personel")
public class PersonelController {
    @Autowired
    private PersonerlService personerlService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    //获取前端请求，将用户数据发给前端
    @PostMapping("/getuser")
    public Users getUserbyid(@Param("id") Long id){
        Users user=personerlService.getUser(id);
        return user;
    }
    @PostMapping("/updatePhone")
    public Map<String,Object> updateUserphone(@RequestBody Users request){
        log.debug("获取前端的id: {}, phone: {}", request.getId(), request.getPhone());
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
    @PostMapping("/updatePassword")
    public Map<String,Object> updatepassword(@RequestBody Users request){
        log.debug("获取前端的id: {}", request.getId());
        int result=personerlService.updateUserpassword(request.getId(),request.getPassword());
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
    //    修改邮箱
    @PostMapping("/updateEmail")
    public Map<String,Object> updateemail(@RequestBody Users request){
        int result=personerlService.updateUseremail(request.getId(),request.getEmail());
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
    @PostMapping("/updateUserPassword")
    public Map<String, Object> updatePassword(@RequestBody Users request) {
        // 1. 获取原始密码
        String rawPassword = request.getPassword();

        // 2. 使用 PasswordEncoder 加密密码
        String encodedPassword = passwordEncoder.encode(rawPassword);
        // 3. 将加密后的密码设置回 request 对象
        request.setPassword(encodedPassword);

        // 4. 调用 service 层方法更新密码
        int result = personerlService.updateUserpassword(request.getId(), request.getPassword());

        // 5. 构造响应结果
        Map<String, Object> response = new HashMap<>();
        if (result > 0) {
            response.put("code", 200);
            response.put("message", "修改成功");
        } else {
            response.put("code", 500);
            response.put("message", "修改失败");
        }
        return response;
    }
    @PostMapping("/updateSex")
    public Map<String,Object> updatesex(@RequestBody Users request){
        int result=personerlService.updateUsersex(request.getId(),request.getGender());
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

    @PostMapping("/updateUserInfo")
    public Map<String,Object> updateUserInfo(@RequestBody Users request){
        int result=personerlService.updateUserInfo(request.getId(), request.getUsername(), request.getGender(), request.getPhone(), request.getEmail(), request.getPerSignature());
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
