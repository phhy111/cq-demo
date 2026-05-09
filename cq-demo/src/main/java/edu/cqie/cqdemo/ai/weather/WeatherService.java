package edu.cqie.cqdemo.ai.weather;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class WeatherService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${weather.api-key:}")
    private String apiKey;

    @Value("${weather.api-url:https://api.seniverse.com/v3}")
    private String apiUrl;

    private static final String WEATHER_CACHE_KEY_PREFIX = "weather:chongqing:";
    private static final long WEATHER_CACHE_EXPIRE = 3;

    public Map<String, Object> getChongqingWeather() {
        String cacheKey = WEATHER_CACHE_KEY_PREFIX + "current";
        Map<String, Object> cachedWeather = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedWeather != null) {
            log.info("从缓存获取天气数据");
            return cachedWeather;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("天气API Key未配置，返回模拟数据");
            return getMockWeather();
        }

        try {
            String url = apiUrl + "/weather/now.json?key=" + apiKey + "&location=chongqing&language=zh-Hans&unit=c";
            String response = restTemplate.getForObject(url, String.class);
            
            JSONObject json = JSONObject.parseObject(response);
            JSONObject results = json.getJSONArray("results").getJSONObject(0);
            JSONObject now = results.getJSONObject("now");

            Map<String, Object> weather = new HashMap<>();
            weather.put("temperature", now.getString("temperature"));
            weather.put("description", now.getString("text"));
            weather.put("humidity", now.getString("humidity"));
            weather.put("windDirection", now.getString("wind_direction"));
            weather.put("windScale", now.getString("wind_scale"));
            weather.put("updateTime", results.getJSONObject("last_update").getString("loc"));

            redisTemplate.opsForValue().set(cacheKey, weather, WEATHER_CACHE_EXPIRE, TimeUnit.HOURS);
            
            log.info("获取天气数据成功: {}°C, {}", weather.get("temperature"), weather.get("description"));
            return weather;
        } catch (Exception e) {
            log.error("获取天气数据失败: {}", e.getMessage());
            return getMockWeather();
        }
    }

    public Map<String, Object> getWeatherForecast(int days) {
        String cacheKey = WEATHER_CACHE_KEY_PREFIX + "forecast:" + days;
        Map<String, Object> cachedForecast = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedForecast != null) {
            log.info("从缓存获取天气预报数据");
            return cachedForecast;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("天气API Key未配置，返回模拟数据");
            return getMockForecast(days);
        }

        try {
            String url = apiUrl + "/weather/daily.json?key=" + apiKey + "&location=chongqing&language=zh-Hans&unit=c&start=0&days=" + days;
            String response = restTemplate.getForObject(url, String.class);
            
            JSONObject json = JSONObject.parseObject(response);
            JSONObject results = json.getJSONArray("results").getJSONObject(0);
            var daily = results.getJSONArray("daily");

            Map<String, Object> forecast = new HashMap<>();
            forecast.put("location", "重庆");
            forecast.put("days", days);

            java.util.List<Map<String, String>> dailyList = new java.util.ArrayList<>();
            for (int i = 0; i < daily.size(); i++) {
                JSONObject day = daily.getJSONObject(i);
                Map<String, String> dayInfo = new HashMap<>();
                dayInfo.put("date", day.getString("date"));
                dayInfo.put("high", day.getString("high"));
                dayInfo.put("low", day.getString("low"));
                dayInfo.put("dayDescription", day.getString("text_day"));
                dayInfo.put("nightDescription", day.getString("text_night"));
                dayInfo.put("windDirection", day.getString("wind_direction"));
                dayInfo.put("windScale", day.getString("wind_scale"));
                dailyList.add(dayInfo);
            }
            forecast.put("daily", dailyList);

            redisTemplate.opsForValue().set(cacheKey, forecast, WEATHER_CACHE_EXPIRE, TimeUnit.HOURS);
            
            log.info("获取天气预报成功，天数: {}", days);
            return forecast;
        } catch (Exception e) {
            log.error("获取天气预报失败: {}", e.getMessage());
            return getMockForecast(days);
        }
    }

    public String getWeatherAdvice(String travelDate) {
        Map<String, Object> weather = getChongqingWeather();
        if (weather == null) {
            return "无法获取天气信息，建议出行前查看最新天气预报。";
        }

        String description = (String) weather.get("description");
        String temperature = (String) weather.get("temperature");

        StringBuilder advice = new StringBuilder();
        advice.append("当前重庆天气：").append(description).append("，").append(temperature).append("°C\n");
        
        int temp = Integer.parseInt(temperature);
        if (temp > 35) {
            advice.append("⚠️ 高温预警：请做好防暑降温，携带防晒霜、遮阳帽，多补充水分。");
        } else if (temp > 28) {
            advice.append("☀️ 天气较热：建议穿着轻薄透气衣物，注意防晒。");
        } else if (temp > 20) {
            advice.append("🌤️ 天气舒适：适合户外活动，建议携带薄外套备用。");
        } else if (temp > 10) {
            advice.append("🧥 天气较凉：建议穿着外套或薄毛衣。");
        } else {
            advice.append("❄️ 天气寒冷：请做好保暖措施，穿着厚外套。");
        }

        if (description.contains("雨")) {
            advice.append("\n🌧️ 有雨：请携带雨具，注意地面湿滑。");
        }

        return advice.toString();
    }

    private Map<String, Object> getMockWeather() {
        Map<String, Object> weather = new HashMap<>();
        weather.put("temperature", "25");
        weather.put("description", "多云");
        weather.put("humidity", "65");
        weather.put("windDirection", "东南风");
        weather.put("windScale", "2");
        weather.put("updateTime", "模拟数据");
        return weather;
    }

    private Map<String, Object> getMockForecast(int days) {
        Map<String, Object> forecast = new HashMap<>();
        forecast.put("location", "重庆");
        forecast.put("days", days);
        forecast.put("note", "天气API Key未配置，这是模拟数据");
        return forecast;
    }
}
