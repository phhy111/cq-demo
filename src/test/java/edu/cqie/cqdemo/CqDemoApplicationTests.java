package edu.cqie.cqdemo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class CqDemoApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testAiChatController() throws Exception {
        // 测试/api/ai/chat接口
        mockMvc.perform(
                        // 1. 设置请求地址和参数
                        get("/api/ai/chat")
                                .param("memoryId", "1")
                                .param("message", "你好，测试接口")
                                // 2. 关键：设置Accept头为JSON，匹配服务端的produces
                                .accept(MediaType.APPLICATION_JSON)
                )
                // 验证状态码为200
                .andExpect(status().isOk())
                // 可选：验证返回的JSON内容（确保响应正确）
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("对话成功"))
                .andExpect(jsonPath("$.data.memoryId").value("1"));
    }
}