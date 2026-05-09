package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.ai.weather.WeatherService;
import edu.cqie.cqdemo.annotation.RateLimit;
import edu.cqie.cqdemo.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/weather")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class WeatherController {

    @Autowired
    private WeatherService weatherService;

    @GetMapping("/current")
    @RateLimit(limit = 60, timeoutSeconds = 60, key = "weather-current")
    public Result<Map<String, Object>> getCurrentWeather() {
        try {
            Map<String, Object> weather = weatherService.getChongqingWeather();
            return Result.success(weather);
        } catch (Exception e) {
            log.error("获取当前天气失败", e);
            return Result.error("获取天气失败：" + e.getMessage());
        }
    }

    @GetMapping("/forecast")
    @RateLimit(limit = 60, timeoutSeconds = 60, key = "weather-forecast")
    public Result<Map<String, Object>> getWeatherForecast(
            @RequestParam(defaultValue = "3") int days) {
        try {
            if (days < 1 || days > 15) {
                return Result.error("天数范围应在1-15之间");
            }
            Map<String, Object> forecast = weatherService.getWeatherForecast(days);
            return Result.success(forecast);
        } catch (Exception e) {
            log.error("获取天气预报失败", e);
            return Result.error("获取天气预报失败：" + e.getMessage());
        }
    }

    @GetMapping("/advice")
    @RateLimit(limit = 60, timeoutSeconds = 60, key = "weather-advice")
    public Result<Map<String, String>> getWeatherAdvice(
            @RequestParam(required = false) String travelDate) {
        try {
            String advice = weatherService.getWeatherAdvice(travelDate);
            Map<String, String> result = new java.util.HashMap<>();
            result.put("advice", advice);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取天气建议失败", e);
            return Result.error("获取天气建议失败：" + e.getMessage());
        }
    }
}
