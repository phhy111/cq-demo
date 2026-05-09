package edu.cqie.cqdemo;


import edu.cqie.cqdemo.service.AiService;
import edu.cqie.cqdemo.service.FoodCategoriesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CqDemoApplicationTests {

    @Autowired
    private FoodCategoriesService foodCategoriesService;
    @Autowired
    private AiService aiService;

    @Test
    void contextLoads() {
    }

    @Test
    void testGenerateTravelPlan() {
        Long memoryId = 1L;
        String userMessage = "我想在重庆玩3天，喜欢美食和历史文化，预算2000元";
        
        System.out.println("开始测试AI生成旅行计划...");
        System.out.println("用户请求: " + userMessage);
        
        aiService.generateTravelPlan(memoryId, userMessage)
                .subscribe(
                        content -> {
                            System.out.println("AI响应: " + content);
                        },
                        error -> {
                            System.err.println("测试失败: " + error.getMessage());
                        },
                        () -> {
                            System.out.println("测试完成");
                        }
                );
        
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}