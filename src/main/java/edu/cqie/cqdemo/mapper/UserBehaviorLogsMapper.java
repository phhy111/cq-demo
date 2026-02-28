package edu.cqie.cqdemo.mapper;

import edu.cqie.cqdemo.entity.UserBehaviorLogs;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
* @author
* @description 针对表【user_behavior_logs(用户行为日志表（记录活跃行为）)】的数据库操作Mapper
* @createDate 2026-02-23 21:22:28
* @Entity edu.cqie.cqdemo.entity.UserBehaviorLogs
*/
public interface UserBehaviorLogsMapper extends BaseMapper<UserBehaviorLogs> {


}




