package edu.cqie.cqdemo.ai;

import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.cqie.cqdemo.util.ScenicSpotLocationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component; // 声明为Spring Bean
import org.springframework.web.client.RestTemplate;

/**
 * AI工具类：航班查询 + 重庆景点位置查询 + 联网搜索
 * 注意：类名首字母大写（规范），避免方法名冲突
 */
@Slf4j
@Component 
public class TravelTools { 

    @Autowired
    private ScenicSpotLocationUtil spotLocationUtil;

    @Autowired
    private RestTemplate restTemplate;

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

    @Tool("联网搜索信息，获取最新的网络数据")
    public String webSearch(
            @P("搜索关键词") String keyword
    ) {
        if (keyword == null || keyword.isEmpty()) {
            return "搜索关键词不能为空，请提供有效的搜索词！";
        }
        
        try {
            // 这里使用一个简单的搜索API示例，实际项目中需要替换为真实的搜索API
            String apiUrl = "https://api.example.com/search?q=" + keyword;
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            
            log.info("[联网搜索工具] 搜索关键词: {}, 结果: {}", keyword, response.getBody());
            return "搜索结果：\n" + response.getBody();
        } catch (Exception e) {
            log.error("[联网搜索工具] 搜索失败: {}", e.getMessage());
            return "搜索失败，请稍后重试！";
        }
    }
}
