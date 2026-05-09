package edu.cqie.cqdemo.ai.map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MapService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${amap.api-key:}")
    private String apiKey;

    @Value("${amap.api-url:https://restapi.amap.com/v3}")
    private String apiUrl;

    private static final String MAP_CACHE_KEY_PREFIX = "map:";
    private static final long MAP_CACHE_EXPIRE = 24;

    public Map<String, Object> geocode(String address) {
        String cacheKey = MAP_CACHE_KEY_PREFIX + "geocode:" + address;
        Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("从缓存获取地理编码: {}", address);
            return cached;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("高德地图API Key未配置，返回模拟数据");
            return getMockGeocode(address);
        }

        try {
            String url = apiUrl + "/geocode/geo?key=" + apiKey + "&address=" + address + "&city=重庆";
            String response = restTemplate.getForObject(url, String.class);
            
            JSONObject json = JSONObject.parseObject(response);
            JSONArray geocodes = json.getJSONArray("geocodes");
            
            if (geocodes == null || geocodes.isEmpty()) {
                log.warn("未找到地址: {}", address);
                return null;
            }

            JSONObject geocode = geocodes.getJSONObject(0);
            String location = geocode.getString("location");
            String[] lngLat = location.split(",");

            Map<String, Object> result = new HashMap<>();
            result.put("address", geocode.getString("formatted_address"));
            result.put("longitude", Double.parseDouble(lngLat[0]));
            result.put("latitude", Double.parseDouble(lngLat[1]));
            result.put("province", geocode.getString("province"));
            result.put("city", geocode.getString("city"));
            result.put("district", geocode.getString("district"));

            redisTemplate.opsForValue().set(cacheKey, result, MAP_CACHE_EXPIRE, TimeUnit.HOURS);
            
            log.info("地理编码成功: {} -> {},{}", address, lngLat[0], lngLat[1]);
            return result;
        } catch (Exception e) {
            log.error("地理编码失败: {}", e.getMessage());
            return null;
        }
    }

    public Map<String, Object> getRoute(List<String> waypoints) {
        if (waypoints == null || waypoints.size() < 2) {
            log.warn("路线规划需要至少2个途经点");
            return null;
        }

        String cacheKey = MAP_CACHE_KEY_PREFIX + "route:" + String.join(",", waypoints);
        Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("从缓存获取路线规划");
            return cached;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("高德地图API Key未配置，返回模拟数据");
            return getMockRoute(waypoints);
        }

        try {
            List<Map<String, Object>> geocodedPoints = new ArrayList<>();
            for (String point : waypoints) {
                Map<String, Object> geocode = geocode(point);
                if (geocode != null) {
                    geocodedPoints.add(geocode);
                }
            }

            if (geocodedPoints.size() < 2) {
                log.warn("无法获取足够的地理编码点");
                return null;
            }

            StringBuilder origin = new StringBuilder();
            origin.append(geocodedPoints.get(0).get("longitude"))
                  .append(",")
                  .append(geocodedPoints.get(0).get("latitude"));

            StringBuilder destination = new StringBuilder();
            destination.append(geocodedPoints.get(geocodedPoints.size() - 1).get("longitude"))
                      .append(",")
                      .append(geocodedPoints.get(geocodedPoints.size() - 1).get("latitude"));

            StringBuilder waypointsStr = new StringBuilder();
            for (int i = 1; i < geocodedPoints.size() - 1; i++) {
                if (i > 1) {
                    waypointsStr.append(";");
                }
                waypointsStr.append(geocodedPoints.get(i).get("longitude"))
                           .append(",")
                           .append(geocodedPoints.get(i).get("latitude"));
            }

            String url = apiUrl + "/direction/driving?key=" + apiKey 
                    + "&origin=" + origin 
                    + "&destination=" + destination;
            
            if (waypointsStr.length() > 0) {
                url += "&waypoints=" + waypointsStr;
            }

            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = JSONObject.parseObject(response);
            JSONObject route = json.getJSONObject("route");

            Map<String, Object> result = new HashMap<>();
            result.put("origin", waypoints.get(0));
            result.put("destination", waypoints.get(waypoints.size() - 1));
            result.put("distance", route.getString("distance") + "米");
            result.put("duration", formatDuration(Integer.parseInt(route.getString("duration"))));

            JSONArray paths = route.getJSONArray("paths");
            if (paths != null && !paths.isEmpty()) {
                JSONObject path = paths.getJSONObject(0);
                result.put("tolls", path.getString("tolls") + "元");
                result.put("trafficLights", path.getString("traffic_lights"));
            }

            List<Map<String, String>> waypointList = new ArrayList<>();
            for (int i = 0; i < waypoints.size(); i++) {
                Map<String, String> wp = new HashMap<>();
                wp.put("name", waypoints.get(i));
                wp.put("order", String.valueOf(i + 1));
                waypointList.add(wp);
            }
            result.put("waypoints", waypointList);

            redisTemplate.opsForValue().set(cacheKey, result, MAP_CACHE_EXPIRE, TimeUnit.HOURS);
            
            log.info("路线规划成功: {} -> {}, 距离: {}, 时间: {}", 
                    waypoints.get(0), waypoints.get(waypoints.size() - 1),
                    result.get("distance"), result.get("duration"));
            return result;
        } catch (Exception e) {
            log.error("路线规划失败: {}", e.getMessage());
            return null;
        }
    }

    public Map<String, Object> searchNearby(String keyword, double longitude, double latitude, int radius) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("高德地图API Key未配置，返回模拟数据");
            return getMockNearby(keyword);
        }

        try {
            String url = apiUrl + "/place/around?key=" + apiKey 
                    + "&location=" + longitude + "," + latitude
                    + "&keywords=" + keyword
                    + "&radius=" + radius
                    + "&offset=10";

            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = JSONObject.parseObject(response);
            JSONArray pois = json.getJSONArray("pois");

            Map<String, Object> result = new HashMap<>();
            result.put("keyword", keyword);
            result.put("count", json.getString("count"));

            List<Map<String, String>> poiList = new ArrayList<>();
            if (pois != null) {
                for (int i = 0; i < Math.min(pois.size(), 10); i++) {
                    JSONObject poi = pois.getJSONObject(i);
                    Map<String, String> poiInfo = new HashMap<>();
                    poiInfo.put("name", poi.getString("name"));
                    poiInfo.put("address", poi.getString("address"));
                    poiInfo.put("distance", poi.getString("distance") + "米");
                    poiInfo.put("type", poi.getString("type"));
                    poiList.add(poiInfo);
                }
            }
            result.put("pois", poiList);

            log.info("搜索周边成功: {}, 找到{}个结果", keyword, result.get("count"));
            return result;
        } catch (Exception e) {
            log.error("搜索周边失败: {}", e.getMessage());
            return null;
        }
    }

    public String generateRouteVisualization(List<String> waypoints) {
        if (waypoints == null || waypoints.isEmpty()) {
            return "";
        }

        StringBuilder visualization = new StringBuilder();
        visualization.append("## 📍 路线可视化\n\n");
        visualization.append("```\n");
        
        for (int i = 0; i < waypoints.size(); i++) {
            visualization.append(waypoints.get(i));
            if (i < waypoints.size() - 1) {
                visualization.append("\n    ↓\n");
            }
        }
        
        visualization.append("\n```\n\n");
        
        Map<String, Object> route = getRoute(waypoints);
        if (route != null) {
            visualization.append("**路线信息：**\n");
            visualization.append("- 总距离：").append(route.get("distance")).append("\n");
            visualization.append("- 预计时间：").append(route.get("duration")).append("\n");
            if (route.containsKey("tolls")) {
                visualization.append("- 过路费：").append(route.get("tolls")).append("\n");
            }
        }

        return visualization.toString();
    }

    private String formatDuration(int seconds) {
        if (seconds < 60) {
            return seconds + "秒";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分钟";
        } else {
            int hours = seconds / 3600;
            int minutes = (seconds % 3600) / 60;
            return hours + "小时" + (minutes > 0 ? minutes + "分钟" : "");
        }
    }

    private Map<String, Object> getMockGeocode(String address) {
        Map<String, Object> result = new HashMap<>();
        result.put("address", address);
        result.put("longitude", 106.5 + Math.random() * 0.5);
        result.put("latitude", 29.5 + Math.random() * 0.5);
        result.put("province", "重庆市");
        result.put("city", "重庆市");
        result.put("district", "渝中区");
        result.put("note", "模拟数据");
        return result;
    }

    private Map<String, Object> getMockRoute(List<String> waypoints) {
        Map<String, Object> result = new HashMap<>();
        result.put("origin", waypoints.get(0));
        result.put("destination", waypoints.get(waypoints.size() - 1));
        result.put("distance", "约15公里");
        result.put("duration", "约40分钟");
        result.put("note", "模拟数据");
        
        List<Map<String, String>> waypointList = new ArrayList<>();
        for (int i = 0; i < waypoints.size(); i++) {
            Map<String, String> wp = new HashMap<>();
            wp.put("name", waypoints.get(i));
            wp.put("order", String.valueOf(i + 1));
            waypointList.add(wp);
        }
        result.put("waypoints", waypointList);
        return result;
    }

    private Map<String, Object> getMockNearby(String keyword) {
        Map<String, Object> result = new HashMap<>();
        result.put("keyword", keyword);
        result.put("count", "3");
        result.put("note", "模拟数据");
        return result;
    }
}
