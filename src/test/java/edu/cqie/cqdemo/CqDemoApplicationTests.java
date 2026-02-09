// src/test/java/edu/cqie/cqdemo/AiServiceTest.java
package edu.cqie.cqdemo;

import edu.cqie.cqdemo.entity.AiReport;
import edu.cqie.cqdemo.service.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CqDemoApplicationTests {

    @Autowired
    private AiService aiService;

    @Test

    void contextLoads() {


    public void testGenerateTravelPlan() {
        // 模拟用户请求
        String userMessage = "请为我规划一个川西5日游行程，2人出行，喜欢自然风光和摄影，9月出发。";

        // 调用 AI 服务（使用 memoryId = 1L）
        AiReport report = aiService.generateTravelPlan(1L, userMessage);

        // 验证返回对象不为空
        assertThat(report).isNotNull();

        // 验证各字段非空
        assertThat(report.getName()).isNotBlank();
        assertThat(report.getStrategyTitle()).isNotBlank();
        assertThat(report.getRouteIntro()).isNotBlank();
        assertThat(report.getHighlights()).isNotBlank();
        assertThat(report.getContent()).isNotBlank();

        // 验证字数要求（可选加强）
        assertThat(report.getRouteIntro().length()).isGreaterThanOrEqualTo(150);
        assertThat(report.getHighlights().length()).isGreaterThanOrEqualTo(150);
        assertThat(report.getContent().length()).isGreaterThanOrEqualTo(650);

        System.out.println("✅ 路线名称: " + report.getName());
        System.out.println("✅ 攻略标题: " + report.getStrategyTitle());
        System.out.println("✅ 内容预览: " + report.getContent().substring(0, Math.min(200, report.getContent().length())) + "...");

    }

    @Test
    public void testMultiTurnWithMemory() {
        Long memoryId = 2L;

        // 第一轮：生成初始计划
        AiReport plan1 = aiService.generateTravelPlan(memoryId, "规划一个云南3日游");
        assertThat(plan1).isNotNull();
        System.out.println(plan1);

        // 第二轮：基于历史优化（例如修改天数）
        AiReport plan2 = aiService.generateTravelPlan(memoryId, "把行程延长到5天，并增加大理的停留时间");
        assertThat(plan2).isNotNull();
        System.out.println(plan2);

        // 可人工检查：plan2 是否体现了“延长到5天”和“大理停留更久”
        // （自动验证较难，但至少能确保不报错且返回有效对象）
    }
}}