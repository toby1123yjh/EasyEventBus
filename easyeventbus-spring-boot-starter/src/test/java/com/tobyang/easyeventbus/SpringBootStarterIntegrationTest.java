package com.tobyang.easyeventbus;

import com.tobyang.easyeventbus.guava.EventBus;
import com.tobyang.easyeventbus.guava.Subscribe;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * SpringBootStarterIntegrationTest - 集成测试Spring Boot Starter的完整功能
 * 
 * 这个测试类验证Spring Boot Starter的自动配置、事件发布订阅等完整功能。
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootStarterIntegrationTest.TestConfiguration.class)
@EnableAutoConfiguration
@TestPropertySource(properties = {
    "easyeventbus.enable=true",
    "easyeventbus.identifier=integrationTestBus"
})
public class SpringBootStarterIntegrationTest {

    @Autowired
    private EventBus eventBus;
    
    @Autowired
    private TestEventSubscriber testSubscriber;

    /**
     * 测试事件类
     */
    public static class TestEvent {
        private final String message;
        private final long timestamp;

        public TestEvent(String message) {
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public String getMessage() {
            return message;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    /**
     * 测试订阅者
     */
    public static class TestEventSubscriber {
        private volatile TestEvent lastEvent;
        private volatile int eventCount = 0;

        @Subscribe
        public void handleTestEvent(TestEvent event) {
            this.lastEvent = event;
            this.eventCount++;
        }

        public TestEvent getLastEvent() {
            return lastEvent;
        }

        public int getEventCount() {
            return eventCount;
        }

        public void reset() {
            this.lastEvent = null;
            this.eventCount = 0;
        }
    }

    @Test
    public void testEventBusInjection() {
        // 验证EventBus被正确注入
        assertNotNull("EventBus should be injected", eventBus);
        assertEquals("EventBus identifier should match configuration", 
                    "integrationTestBus", eventBus.identifier());
    }

    @Test
    public void testEventPublishAndSubscribe() {
        // 重置订阅者状态
        testSubscriber.reset();
        
        // 注册订阅者
        eventBus.register(testSubscriber);
        
        // 发布事件
        TestEvent event = new TestEvent("Integration Test Event");
        eventBus.post(event);
        
        // 验证事件被正确接收
        assertEquals("Event should be received", event, testSubscriber.getLastEvent());
        assertEquals("Event count should be 1", 1, testSubscriber.getEventCount());
        
        // 清理
        eventBus.unregister(testSubscriber);
    }

    @Test
    public void testMultipleEvents() {
        // 重置订阅者状态
        testSubscriber.reset();
        
        // 注册订阅者
        eventBus.register(testSubscriber);
        
        // 发布多个事件
        for (int i = 0; i < 5; i++) {
            eventBus.post(new TestEvent("Event " + i));
        }
        
        // 验证所有事件都被接收
        assertEquals("Should receive 5 events", 5, testSubscriber.getEventCount());
        assertEquals("Last event should be Event 4", "Event 4", testSubscriber.getLastEvent().getMessage());
        
        // 清理
        eventBus.unregister(testSubscriber);
    }

    @Test
    public void testUnregisterSubscriber() {
        // 重置订阅者状态
        testSubscriber.reset();
        
        // 注册订阅者
        eventBus.register(testSubscriber);
        
        // 发布第一个事件
        eventBus.post(new TestEvent("First Event"));
        assertEquals("Should receive first event", 1, testSubscriber.getEventCount());
        
        // 取消注册
        eventBus.unregister(testSubscriber);
        
        // 发布第二个事件
        eventBus.post(new TestEvent("Second Event"));
        
        // 验证第二个事件没有被接收
        assertEquals("Should still have only 1 event", 1, testSubscriber.getEventCount());
        assertEquals("Last event should still be First Event", "First Event", 
                    testSubscriber.getLastEvent().getMessage());
    }

    /**
     * 测试配置类
     */
    @Configuration
    public static class TestConfiguration {
        
        @Bean
        public TestEventSubscriber testEventSubscriber() {
            return new TestEventSubscriber();
        }
    }
}
