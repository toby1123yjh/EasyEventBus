package com.tobyang.demo;

import com.tobyang.demo.event.DemoEvent;
import com.tobyang.demo.subscriber.DemoSubscriber;
import com.tobyang.easyeventbus.guava.EventBus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * Demo应用基础测试
 * 
 * @author tobyang
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = com.tobyang.EasyEventBusApplication.class)
public class DemoApplicationTest {
    
    @Autowired
    private EventBus eventBus;
    
    @Autowired
    private DemoSubscriber demoSubscriber;
    
    @Test
    public void contextLoads() {
        // 验证Spring上下文能正常加载
        assertNotNull("EventBus should be injected", eventBus);
        assertNotNull("DemoSubscriber should be injected", demoSubscriber);
    }
    
    @Test
    public void testEventBusConfiguration() {
        // 验证EventBus配置正确
        assertEquals("EventBus identifier should match configuration", 
                    "demoEventBus", eventBus.identifier());
    }
    
    @Test
    public void testEventPublishing() {
        // 测试事件发布功能
        DemoEvent event = new DemoEvent("测试事件");
        
        // 发布事件不应该抛出异常
        assertDoesNotThrow(() -> {
            eventBus.post(event);
        });
        
        // 发布字符串事件
        assertDoesNotThrow(() -> {
            eventBus.post("测试字符串事件");
        });
    }
    
    /**
     * 断言不抛出异常的辅助方法（兼容JUnit 4）
     */
    private void assertDoesNotThrow(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            fail("Expected no exception, but got: " + e.getMessage());
        }
    }
}
