package com.tobyang.easyeventbus;

import com.tobyang.easyeventbus.guava.AsyncEventBus;
import com.tobyang.easyeventbus.guava.EventBus;
import com.tobyang.easyeventbus.guava.Subscribe;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * CoreFunctionalityTest - 验证核心模块的Guava EventBus迁移功能
 * 
 * 这个测试类验证从Google Guava迁移过来的EventBus核心功能是否正常工作。
 */
public class CoreFunctionalityTest {

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

        @Override
        public String toString() {
            return "TestEvent{message='" + message + "', timestamp=" + timestamp + "}";
        }
    }

    /**
     * 测试订阅者类
     */
    public static class TestSubscriber {
        private volatile Object lastEvent;
        private volatile int eventCount = 0;

        @Subscribe
        public void handleTestEvent(TestEvent event) {
            this.lastEvent = event;
            this.eventCount++;
        }

        @Subscribe
        public void handleAnyEvent(Object event) {
            if (!(event instanceof TestEvent)) {
                this.lastEvent = event;
                this.eventCount++;
            }
        }

        public Object getLastEvent() {
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
    public void testBasicEventBusFunctionality() {
        // 创建EventBus实例
        EventBus eventBus = new EventBus("test-bus");
        
        // 验证标识符
        Assert.assertEquals("test-bus", eventBus.identifier());
        
        // 创建订阅者
        TestSubscriber subscriber = new TestSubscriber();
        
        // 注册订阅者
        eventBus.register(subscriber);
        
        // 发布事件
        TestEvent event = new TestEvent("Hello World");
        eventBus.post(event);
        
        // 验证事件被接收
        Assert.assertEquals(event, subscriber.getLastEvent());
        Assert.assertEquals(1, subscriber.getEventCount());
    }

    @Test
    public void testEventBusUnregister() {
        EventBus eventBus = new EventBus("test-unregister");
        TestSubscriber subscriber = new TestSubscriber();
        
        // 注册并发布事件
        eventBus.register(subscriber);
        eventBus.post(new TestEvent("First Event"));
        Assert.assertEquals(1, subscriber.getEventCount());
        
        // 取消注册
        eventBus.unregister(subscriber);
        
        // 再次发布事件
        eventBus.post(new TestEvent("Second Event"));
        
        // 验证事件计数没有增加
        Assert.assertEquals(1, subscriber.getEventCount());
    }

    @Test
    public void testAsyncEventBus() throws InterruptedException {
        // 创建异步EventBus
        AsyncEventBus asyncEventBus = new AsyncEventBus("async-test", 
            Executors.newSingleThreadExecutor());
        
        TestSubscriber subscriber = new TestSubscriber();
        CountDownLatch latch = new CountDownLatch(1);
        
        // 创建异步订阅者
        Object asyncSubscriber = new Object() {
            @Subscribe
            public void handleEvent(TestEvent event) {
                subscriber.handleTestEvent(event);
                latch.countDown();
            }
        };
        
        // 注册并发布事件
        asyncEventBus.register(asyncSubscriber);
        asyncEventBus.post(new TestEvent("Async Event"));
        
        // 等待异步处理完成
        Assert.assertTrue("异步事件处理超时", latch.await(5, TimeUnit.SECONDS));
        Assert.assertEquals(1, subscriber.getEventCount());
    }

    @Test
    public void testMultipleSubscribers() {
        EventBus eventBus = new EventBus("multi-subscriber-test");
        
        TestSubscriber subscriber1 = new TestSubscriber();
        TestSubscriber subscriber2 = new TestSubscriber();
        
        // 注册多个订阅者
        eventBus.register(subscriber1);
        eventBus.register(subscriber2);
        
        // 发布事件
        TestEvent event = new TestEvent("Multi Subscriber Event");
        eventBus.post(event);
        
        // 验证所有订阅者都收到了事件
        Assert.assertEquals(event, subscriber1.getLastEvent());
        Assert.assertEquals(event, subscriber2.getLastEvent());
        Assert.assertEquals(1, subscriber1.getEventCount());
        Assert.assertEquals(1, subscriber2.getEventCount());
    }

    @Test
    public void testEventInheritance() {
        EventBus eventBus = new EventBus("inheritance-test");
        TestSubscriber subscriber = new TestSubscriber();
        
        eventBus.register(subscriber);
        
        // 发布String事件（会被handleAnyEvent处理）
        eventBus.post("String Event");
        
        // 验证事件被正确处理
        Assert.assertEquals("String Event", subscriber.getLastEvent());
        Assert.assertEquals(1, subscriber.getEventCount());
    }
}
