package edu.cqie.cqdemo.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

/**
 * 精准查询重庆景点地理位置（Spring Bean版，无编译错误）
 * 功能：输入景点名，返回格式化的位置信息字符串（适配AI工具调用）
 */
@Component // 仅保留@Component，去掉无用的@Bean注解
public class ScenicSpotLocationUtil {
    // 高德Web服务API Key（建议后续移到application.yml配置文件）
    private static final String AMAP_KEY = "a976638adafc15d9f874bbd09f119798";
    // 高德POI搜索API（专门查景点/商圈等兴趣点）
    private static final String POI_SEARCH_URL = "https://restapi.amap.com/v3/place/text";

    /**
     * 精准查询重庆景点的地理位置
     *
     * @param spotName 景点名称（如：洪崖洞、解放碑、磁器口古镇）
     * @return 格式化的位置信息字符串（失败返回错误提示，不再返回null）
     */
    public String getSpotLocation(String spotName) {
        // 前置校验：景点名不能为空
        if (spotName == null || spotName.trim().isEmpty()) {
            return "景点名称不能为空，请输入有效的景点名称！";
        }

        try {
            // 1. 构建精准查询参数
            String params = "keywords=" + spotName  // 景点关键词
                    + "&city=重庆"                 // 严格限定城市为重庆
                    + "&citylimit=true"            // 强制只返回重庆的结果
                    + "&types=110000"              // 类型：风景名胜
                    + "&output=json"
                    + "&key=" + AMAP_KEY;

            String fullUrl = POI_SEARCH_URL + "?" + params;

            // 2. 发送GET请求（添加超时时间，避免请求挂起）
            HttpResponse response = HttpRequest.get(fullUrl)
                    .timeout(5000) // 5秒超时
                    .execute();

            if (!response.isOk()) {
                return "请求高德地图API失败，状态码：" + response.getStatus();
            }

            // 3. 解析JSON响应
            String result = response.body();
            JSONObject jsonResult = JSON.parseObject(result);

            // 4. 校验返回结果是否成功
            if (!"1".equals(jsonResult.getString("status"))) {
                return "查询失败：" + jsonResult.getString("info");
            }

            // 5. 检查是否有匹配结果
            if (jsonResult.getInteger("count") == 0) {
                return "未找到重庆的「" + spotName + "」相关景点，请确认景点名称是否正确！";
            }

            // 6. 提取第一个精准匹配的结果并格式化
            JSONObject firstResult = jsonResult.getJSONArray("pois").getJSONObject(0);
            String lngLat = firstResult.getString("location");
            String address = firstResult.getString("address");
            String district = firstResult.getString("adname");
            String officialName = firstResult.getString("name");

            // 格式化返回字符串（AI工具调用时更友好）
            return String.format(
                    "【%s 位置信息】\n官方名称：%s\n所属区域：%s\n详细地址：%s\n经纬度：%s",
                    spotName, officialName, district, address, lngLat
            );

        } catch (Exception e) {
            String errorMsg = "查询「" + spotName + "」位置异常：" + e.getMessage();
            System.err.println(errorMsg);
            return errorMsg;
        }
    }
}