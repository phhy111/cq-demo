package edu.cqie.cqdemo.ai;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.cqie.cqdemo.util.ScenicSpotLocationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * AI工具类：航班查询 + 重庆景点位置查询 + 联网搜索
 * 联网搜索使用 SerpAPI 接入真实 Google 搜索结果
 */
@Slf4j
@Component
public class TravelTools {

    @Autowired
    private ScenicSpotLocationUtil spotLocationUtil;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${serpapi.key:}")
    private String serpApiKey;

    private static final String SERPAPI_URL = "https://serpapi.com/search";

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

    @Tool("联网搜索信息，获取最新的网络数据，如实时新闻、活动信息、票价等")
    public String webSearch(
            @P("搜索关键词") String keyword
    ) {
        if (keyword == null || keyword.isEmpty()) {
            return "搜索关键词不能为空，请提供有效的搜索词！";
        }

        // 如果没有配置 SerpAPI Key，使用模拟数据
        if (serpApiKey == null || serpApiKey.isEmpty()) {
            log.warn("[联网搜索] SerpAPI Key 未配置，返回模拟数据");
            return mockSearchResult(keyword);
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(SERPAPI_URL)
                    .queryParam("q", keyword)
                    .queryParam("api_key", serpApiKey)
                    .queryParam("engine", "google")
                    .queryParam("gl", "cn")
                    .queryParam("hl", "zh-cn")
                    .queryParam("num", 5)
                    .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String result = parseSerpApiResult(response.getBody());
                log.info("[联网搜索工具] 搜索关键词: {}, 结果长度: {}", keyword, result.length());
                return result;
            } else {
                log.error("[联网搜索工具] SerpAPI 请求失败，状态码: {}", response.getStatusCode());
                return "搜索服务暂时不可用，请稍后重试";
            }
        } catch (Exception e) {
            log.error("[联网搜索工具] 搜索失败: {}", e.getMessage());
            return "搜索失败: " + e.getMessage() + "，请稍后重试";
        }
    }

    /**
     * 解析 SerpAPI 返回的 JSON 结果
     */
    private String parseSerpApiResult(String jsonBody) {
        try {
            JSONObject json = JSONObject.parseObject(jsonBody);
            StringBuilder result = new StringBuilder();

            // 提取搜索结果
            JSONArray organicResults = json.getJSONArray("organic_results");
            if (organicResults != null && !organicResults.isEmpty()) {
                result.append("搜索结果：\n\n");
                for (int i = 0; i < Math.min(organicResults.size(), 5); i++) {
                    JSONObject item = organicResults.getJSONObject(i);
                    String title = item.getString("title");
                    String snippet = item.getString("snippet");
                    String link = item.getString("link");

                    if (title != null) {
                        result.append(i + 1).append(". ").append(title).append("\n");
                    }
                    if (snippet != null) {
                        result.append("   ").append(snippet).append("\n");
                    }
                    if (link != null) {
                        result.append("   链接: ").append(link).append("\n");
                    }
                    result.append("\n");
                }
            }

            // 提取知识图谱信息
            JSONObject knowledgeGraph = json.getJSONObject("knowledge_graph");
            if (knowledgeGraph != null) {
                String kgTitle = knowledgeGraph.getString("title");
                String kgDescription = knowledgeGraph.getString("description");
                if (kgTitle != null && kgDescription != null) {
                    result.append("【知识卡片】\n");
                    result.append(kgTitle).append("\n");
                    result.append(kgDescription).append("\n\n");
                }
            }

            if (result.length() == 0) {
                return "未找到相关搜索结果";
            }

            return result.toString();
        } catch (Exception e) {
            log.error("解析 SerpAPI 结果失败: {}", e.getMessage());
            return "搜索结果解析失败";
        }
    }

    /**
     * 模拟搜索结果（当 SerpAPI Key 未配置时使用）
     */
    private String mockSearchResult(String keyword) {
        return "【模拟搜索结果】关键词: " + keyword + "\n\n"
                + "注意：当前使用的是模拟数据。要获取真实搜索结果，请在 application.yml 中配置 serpapi.key。\n\n"
                + "1. " + keyword + "相关资讯\n"
                + "   这是关于" + keyword + "的模拟搜索结果。实际使用时，将调用 SerpAPI 获取真实的 Google 搜索结果。\n"
                + "   链接: https://www.example.com/search?q=" + keyword + "\n\n";
    }
}
