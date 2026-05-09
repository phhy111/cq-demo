package edu.cqie.cqdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.cqie.cqdemo.entity.UserBehaviorLogs;
import edu.cqie.cqdemo.service.UserBehaviorLogsService;
import edu.cqie.cqdemo.mapper.UserBehaviorLogsMapper;
import org.springframework.stereotype.Service;

/**
* @author
* @description 针对表【user_behavior_logs(用户行为日志表（记录活跃行为）)】的数据库操作Service实现
* @createDate 2026-02-23 21:22:28
*/
@Service
public class UserBehaviorLogsServiceImpl extends ServiceImpl<UserBehaviorLogsMapper, UserBehaviorLogs>
    implements UserBehaviorLogsService{

}




