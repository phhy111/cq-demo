package edu.cqie.cqdemo.mapper;

import edu.cqie.cqdemo.entity.Users;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PersonerlMapper {
    Users selectuser(@Param("id") long id);
    Users selectUserByUsername(@Param("username") String username);
}
