package com.tobyang.demo;

import com.tobyang.demo.event.DemoEvent;
import com.tobyang.easyeventbus.guava.EventBus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * Demo应用集成测试
 *
 * @author tobyang
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = com.tobyang.EasyEventBusApplication.class)
public class DemoApplicationIntegrationTest {

    @Autowired
    private EventBus eventBus;
    
    @Test
    public void testEventBusInjection() {
        assertNotNull("EventBus should be injected", eventBus);
        assertEquals("EventBus identifier should match configuration",
                    "demoEventBus", eventBus.identifier());
    }

    @Test
    public void testDemoEventPublishing() {
        DemoEvent event = new DemoEvent("测试消息");
        eventBus.post(event);

        // 如果没有异常，测试通过
        assertTrue("DemoEvent publishing should complete without exception", true);
    }

    @Test
    public void testStringEventPublishing() {
        String message = "这是一个字符串事件";
        eventBus.post(message);

        // 如果没有异常，测试通过
        assertTrue("String event publishing should complete without exception", true);
    }
}
