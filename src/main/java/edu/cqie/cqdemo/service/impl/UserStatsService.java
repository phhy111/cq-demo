package edu.cqie.cqdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.cqie.cqdemo.dto.UserGrowthDTO;
import edu.cqie.cqdemo.entity.Users;
import edu.cqie.cqdemo.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserStatsService extends ServiceImpl<UserMapper, Users> {

    @Resource
    private UserMapper userMapper;

    /**
     * 获取近7天用户增长数据
     */
    public UserGrowthDTO getUserGrowthData() {
        UserGrowthDTO dto = new UserGrowthDTO();
        List<String> dateList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> activeUserList = new ArrayList<>();

        // 1. 生成近7天日期（包含今天）
        LocalDate today = LocalDate.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            dateList.add(date.format(dateFormatter));
        }

        // 2. 计算时间范围：7天前 00:00:00 到 今天 23:59:59
        LocalDateTime startTime = today.minusDays(6).atStartOfDay();
        LocalDateTime endTime = today.plusDays(1).atStartOfDay(); // 明天0点，避免漏掉今天数据

        // 3. 查询新增用户数据
        List<Map<String, Object>> newUserResult = userMapper.countNewUserByDay(startTime, endTime);
        Map<String, Integer> newUserMap = new HashMap<>();
        if (newUserResult != null) {
            for (Map<String, Object> row : newUserResult) {
                Object dateObj = row.get("date");
                Object countObj = row.get("count");
                if (dateObj != null && countObj != null) {
                    newUserMap.put(dateObj.toString(), ((Number) countObj).intValue());
                }
            }
        }

        // 4. 查询活跃用户数据
        List<Map<String, Object>> activeUserResult = userMapper.countActiveUserByDay(startTime, endTime);
        Map<String, Integer> activeUserMap = new HashMap<>();
        if (activeUserResult != null) {
            for (Map<String, Object> row : activeUserResult) {
                Object dateObj = row.get("date");
                Object countObj = row.get("count");
                if (dateObj != null && countObj != null) {
                    activeUserMap.put(dateObj.toString(), ((Number) countObj).intValue());
                }
            }
        }

        // 5. 填充数据（无数据的日期填0）
        for (String date : dateList) {
            newUserList.add(newUserMap.getOrDefault(date, 0));
            activeUserList.add(activeUserMap.getOrDefault(date, 0));
        }

        // 6. 组装返回数据
        dto.setDates(dateList);
        dto.setNewUser(newUserList);
        dto.setActiveUser(activeUserList);

        return dto;
    }
}