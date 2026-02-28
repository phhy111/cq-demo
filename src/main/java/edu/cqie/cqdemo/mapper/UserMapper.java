package edu.cqie.cqdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.cqie.cqdemo.entity.Users;
import org.apache.ibatis.annotations.Mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper extends BaseMapper<Users> {

    //获取用户名
    String getUserName(Long id);

    /**
     * 查询指定时间范围内每天的新增用户数
     */
    @Select("""
        SELECT DATE(created_at) as date, COUNT(id) as count 
        FROM users 
        WHERE created_at >= #{startTime} AND created_at < #{endTime}
        GROUP BY DATE(created_at)
        ORDER BY DATE(created_at)
    """)
    List<Map<String, Object>> countNewUserByDay(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定时间范围内每天的活跃用户数（去重）
     */
    @Select("""
        SELECT DATE(ubl.behavior_time) as date, COUNT(DISTINCT ubl.user_id) as count 
        FROM user_behavior_logs ubl
        WHERE ubl.behavior_time >= #{startTime} AND ubl.behavior_time < #{endTime}
        GROUP BY DATE(ubl.behavior_time)
        ORDER BY DATE(ubl.behavior_time)
    """)
    List<Map<String, Object>> countActiveUserByDay(@Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);
}
