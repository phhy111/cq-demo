package edu.cqie.cqdemo.service;

import edu.cqie.cqdemo.entity.Users;
import org.apache.ibatis.annotations.Param;

public interface PersonerlService {
    public Users getUser(Long id);
    public int updateUserphone(Long id);
    public int updateUserpassword(Long id);
    public int updateUseremail(Long id);
}
