package edu.cqie.cqdemo.mapper;

import edu.cqie.cqdemo.entity.Users;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PersonerlMapper {
    Users selectuser(@Param("id") long id);
    Users selectUserByUsername(@Param("username") String username);
    //修改手机号
    int updateUserphone(@Param("id") long id ,@Param("phone") String phone);
    //修改密码
    int updateUserpassword(@Param("id") long id,@Param("password") String password);
    //修改邮箱
    int updateUseremail(@Param("id") long id ,@Param("email") String email);
    int updateUsersex(@Param("id") long id ,@Param("gender") int gender);
}
