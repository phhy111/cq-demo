package edu.cqie.cqdemo.ai;

import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.cqie.cqdemo.util.ScenicSpotLocationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component; // 声明为Spring Bean

/**
 * AI工具类：航班查询 + 重庆景点位置查询
 * 注意：类名首字母大写（规范），避免方法名冲突
 */
@Slf4j
@Component 
public class TravelTools { 

    @Autowired
    private ScenicSpotLocationUtil spotLocationUtil;

    @Tool("查询航班信息，必须提供出发城市和目的城市才能查询")
    public String searchFlights(
            @P("航班的出发城市（如北京、上海）") String departureCity,
            @P("航班的目的城市（如广州、深圳）") String targetCity
    ) {
        if (departureCity == null || departureCity.isEmpty()
                || targetCity == null || targetCity.isEmpty()) {
            return "出发城市和目的城市不能为空，请提供有效的城市名称！";
        }
        String flightInfo = departureCity + "到" + targetCity + "的航班信息：\n"
                + "1. CA1234 08:00起飞 10:30到达\n"
                + "2. MU5678 09:10起飞 11:40到达";
        log.info("[航班查询工具] {}", flightInfo);
        return flightInfo;
    }

    @Tool("查询景点地理位置，确定行程安排，如果有多个想去景点多次调用该工具，必须提供相关景点")
    public String searchScenicSpotLocation(
            @P("景点名字（如洪崖洞，金佛山）") String attraction
    ) {
        if (attraction == null || attraction.isEmpty()) {
            return "景点名称信息不能为空，请提供有效的景点名称！";
        }
        return spotLocationUtil.getSpotLocation(attraction);
    }
}