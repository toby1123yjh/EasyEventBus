/*
 * Copyright (C) 2024 tobyang
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.tobyang.easyeventbus.guava;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * 延迟事件发布功能测试
 */
public class DelayedEventTest {

    /** 测试事件类 */
    public static class DelayedTestEvent {
        private final String message;
        private final long timestamp;

        public DelayedTestEvent(String message) {
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return "DelayedTestEvent{message='" + message + "', timestamp=" + timestamp + '}';
        }
    }

    /** 测试监听器 */
    public static class DelayedEventListener {
        private final AtomicInteger eventCount = new AtomicInteger(0);
        private final AtomicLong lastEventTime = new AtomicLong(0);
        private final CountDownLatch latch;

        public DelayedEventListener(int expectedEvents) {
            this.latch = new CountDownLatch(expectedEvents);
        }

        @Subscribe
        public void handleDelayedEvent(DelayedTestEvent event) {
            eventCount.incrementAndGet();
            lastEventTime.set(System.currentTimeMillis());
            latch.countDown();
        }

        public int getEventCount() { return eventCount.get(); }
        public long getLastEventTime() { return lastEventTime.get(); }
        public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }
    }

    @Test
    public void testImmediatePost() throws InterruptedException {
        AsyncEventBus eventBus = new AsyncEventBus("test",
            java.util.concurrent.Executors.newFixedThreadPool(2));
        DelayedEventListener listener = new DelayedEventListener(1);

        eventBus.register(listener);

        // 延迟时间为0，应该立即发布
        eventBus.postDelayed(new DelayedTestEvent("immediate"), 0, TimeUnit.MILLISECONDS);

        assertTrue("事件应该立即处理", listener.await(100, TimeUnit.MILLISECONDS));
        assertEquals("应该处理1个事件", 1, listener.getEventCount());
    }

    @Test
    public void testDelayedPost() throws InterruptedException {
        AsyncEventBus eventBus = new AsyncEventBus("test",
            java.util.concurrent.Executors.newFixedThreadPool(2));
        DelayedEventListener listener = new DelayedEventListener(1);
        
        eventBus.register(listener);
        
        long startTime = System.currentTimeMillis();
        long delayMs = 200;
        
        // 延迟200毫秒发布
        eventBus.postDelayed(new DelayedTestEvent("delayed"), delayMs, TimeUnit.MILLISECONDS);
        
        // 立即检查，应该还没有事件
        Thread.sleep(50);
        assertEquals("延迟期间不应该有事件", 0, listener.getEventCount());
        
        // 等待事件处理
        assertTrue("延迟事件应该被处理", listener.await(500, TimeUnit.MILLISECONDS));
        assertEquals("应该处理1个事件", 1, listener.getEventCount());
        
        long actualDelay = listener.getLastEventTime() - startTime;
        assertTrue("实际延迟应该大于等于设定延迟", actualDelay >= delayMs);
        assertTrue("实际延迟不应该过长", actualDelay < delayMs + 100);
    }

    @Test
    public void testMultipleDelayedEvents() throws InterruptedException {
        AsyncEventBus eventBus = new AsyncEventBus("test",
            java.util.concurrent.Executors.newFixedThreadPool(2));
        DelayedEventListener listener = new DelayedEventListener(3);
        
        eventBus.register(listener);
        
        // 发布多个延迟事件
        eventBus.postDelayed(new DelayedTestEvent("event1"), 50, TimeUnit.MILLISECONDS);
        eventBus.postDelayed(new DelayedTestEvent("event2"), 100, TimeUnit.MILLISECONDS);
        eventBus.postDelayed(new DelayedTestEvent("event3"), 150, TimeUnit.MILLISECONDS);
        
        // 等待所有事件处理完成
        assertTrue("所有延迟事件应该被处理", listener.await(500, TimeUnit.MILLISECONDS));
        assertEquals("应该处理3个事件", 3, listener.getEventCount());
    }

    @Test
    public void testDelayedEventWithAsyncEventBus() throws InterruptedException {
        AsyncEventBus eventBus = new AsyncEventBus("test", 
            java.util.concurrent.Executors.newFixedThreadPool(2));
        DelayedEventListener listener = new DelayedEventListener(1);
        
        eventBus.register(listener);
        
        long startTime = System.currentTimeMillis();
        long delayMs = 100;
        
        // 在异步EventBus上测试延迟发布
        eventBus.postDelayed(new DelayedTestEvent("async-delayed"), delayMs, TimeUnit.MILLISECONDS);
        
        assertTrue("异步延迟事件应该被处理", listener.await(500, TimeUnit.MILLISECONDS));
        assertEquals("应该处理1个事件", 1, listener.getEventCount());
        
        long actualDelay = listener.getLastEventTime() - startTime;
        assertTrue("实际延迟应该大于等于设定延迟", actualDelay >= delayMs);
    }

    @Test
    public void testNegativeDelay() throws InterruptedException {
        AsyncEventBus eventBus = new AsyncEventBus("test",
            java.util.concurrent.Executors.newFixedThreadPool(2));
        DelayedEventListener listener = new DelayedEventListener(1);
        
        eventBus.register(listener);
        
        // 负延迟应该立即发布
        eventBus.postDelayed(new DelayedTestEvent("negative"), -100, TimeUnit.MILLISECONDS);
        
        assertTrue("负延迟事件应该立即处理", listener.await(100, TimeUnit.MILLISECONDS));
        assertEquals("应该处理1个事件", 1, listener.getEventCount());
    }

    @Test(expected = NullPointerException.class)
    public void testNullEvent() {
        AsyncEventBus eventBus = new AsyncEventBus("test",
            java.util.concurrent.Executors.newFixedThreadPool(2));
        eventBus.postDelayed(null, 100, TimeUnit.MILLISECONDS);
    }

    @Test(expected = NullPointerException.class)
    public void testNullTimeUnit() {
        AsyncEventBus eventBus = new AsyncEventBus("test",
            java.util.concurrent.Executors.newFixedThreadPool(2));
        eventBus.postDelayed(new DelayedTestEvent("test"), 100, null);
    }
}
