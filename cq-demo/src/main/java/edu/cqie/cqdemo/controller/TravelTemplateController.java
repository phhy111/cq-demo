package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.ai.template.TravelTemplateService;
import edu.cqie.cqdemo.annotation.RateLimit;
import edu.cqie.cqdemo.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/templates")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class TravelTemplateController {

    @Autowired
    private TravelTemplateService travelTemplateService;

    @GetMapping
    @RateLimit(limit = 60, timeoutSeconds = 60, key = "template-list")
    public Result<List<Map<String, String>>> getAllTemplates() {
        try {
            List<Map<String, String>> templates = travelTemplateService.getAllTemplates();
            return Result.success(templates);
        } catch (Exception e) {
            log.error("获取模板列表失败", e);
            return Result.error("获取模板列表失败：" + e.getMessage());
        }
    }

    @GetMapping("/{code}")
    @RateLimit(limit = 60, timeoutSeconds = 60, key = "template-detail")
    public Result<Map<String, String>> getTemplateByCode(@PathVariable String code) {
        try {
            String prompt = travelTemplateService.getTemplatePrompt(code);
            if (prompt == null) {
                return Result.error("未找到指定模板");
            }
            
            Map<String, String> templateInfo = new java.util.HashMap<>();
            templateInfo.put("code", code);
            templateInfo.put("prompt", prompt);
            return Result.success(templateInfo);
        } catch (Exception e) {
            log.error("获取模板详情失败", e);
            return Result.error("获取模板详情失败：" + e.getMessage());
        }
    }
}
