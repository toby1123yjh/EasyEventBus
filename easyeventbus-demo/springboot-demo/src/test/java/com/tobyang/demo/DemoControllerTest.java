package com.tobyang.demo;

import com.tobyang.demo.controller.DemoController;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Demo控制器测试
 * 
 * 测试REST API的功能。
 * 
 * @author tobyang
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = com.tobyang.EasyEventBusApplication.class)
public class DemoControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DemoController demoController;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }
    
    @Test
    public void testControllerInjection() {
        // 验证控制器被正确注入
        org.junit.Assert.assertNotNull("DemoController should be injected", demoController);
    }
    
    @Test
    public void testGetEventBusInfo() throws Exception {
        mockMvc.perform(get("/api/demo/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value("demoEventBus"))
                .andExpect(jsonPath("$.status").value("active"));
    }

    @Test
    public void testPublishDemoEvent() throws Exception {
        mockMvc.perform(post("/api/demo/event")
                .param("message", "测试消息"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("DemoEvent发布成功"));
    }

    @Test
    public void testPublishStringEvent() throws Exception {
        mockMvc.perform(post("/api/demo/string")
                .param("message", "测试字符串"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("字符串事件发布成功"));
    }
}
