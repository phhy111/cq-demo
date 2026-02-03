package edu.cqie.cqdemo.mapper;

import edu.cqie.cqdemo.entity.Users;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PersonerlMapper {
    Users selectuser();
}
