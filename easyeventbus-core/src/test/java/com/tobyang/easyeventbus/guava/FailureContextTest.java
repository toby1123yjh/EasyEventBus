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

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * 失败上下文功能测试
 */
public class FailureContextTest {

    /** 测试事件类 */
    public static class FailureTestEvent {
        private final String message;

        public FailureTestEvent(String message) {
            this.message = message;
        }

        public String getMessage() { return message; }

        @Override
        public String toString() {
            return "FailureTestEvent{message='" + message + "'}";
        }
    }

    /** 测试监听器 - 支持FailureContext */
    public static class EnhancedFailureListener {
        private final CountDownLatch failureLatch;
        private final AtomicReference<FailureContext> capturedContext = new AtomicReference<>();
        private final boolean shouldFail;

        public EnhancedFailureListener(boolean shouldFail) {
            this.shouldFail = shouldFail;
            this.failureLatch = new CountDownLatch(1);
        }

        @Subscribe
        @FailRetry(retries = 2, interval = 10, timeUnit = TimeUnit.MILLISECONDS)
        public void handleEvent(FailureTestEvent event) {
            if (shouldFail) {
                throw new RuntimeException("模拟处理失败: " + event.getMessage());
            }
        }

        @FailSubscribe
        public void handleFailure(FailureTestEvent event, FailureContext context) {
            capturedContext.set(context);
            failureLatch.countDown();
        }

        public FailureContext getCapturedContext() {
            return capturedContext.get();
        }

        public boolean awaitFailure(long timeout, TimeUnit unit) throws InterruptedException {
            return failureLatch.await(timeout, unit);
        }
    }

    /** 传统失败监听器 - 不使用FailureContext */
    public static class TraditionalFailureListener {
        private final CountDownLatch failureLatch;
        private final AtomicReference<FailureTestEvent> capturedEvent = new AtomicReference<>();

        public TraditionalFailureListener() {
            this.failureLatch = new CountDownLatch(1);
        }

        @Subscribe
        @FailRetry(retries = 1, interval = 10, timeUnit = TimeUnit.MILLISECONDS)
        public void handleEvent(FailureTestEvent event) {
            throw new RuntimeException("模拟处理失败");
        }

        @FailSubscribe
        public void handleFailure(FailureTestEvent event) {
            capturedEvent.set(event);
            failureLatch.countDown();
        }

        public FailureTestEvent getCapturedEvent() {
            return capturedEvent.get();
        }

        public boolean awaitFailure(long timeout, TimeUnit unit) throws InterruptedException {
            return failureLatch.await(timeout, unit);
        }
    }

    @Test
    public void testFailureContextCreation() {
        LocalDateTime now = LocalDateTime.now();
        RuntimeException cause = new RuntimeException("测试异常");
        
        FailureContext context = new FailureContext.Builder()
                .originalEvent(new FailureTestEvent("test"))
                .failureCause(cause)
                .totalRetries(3)
                .firstAttemptTime(now.minusSeconds(5))
                .lastAttemptTime(now)
                .totalDuration(5000)
                .failureType(FailureContext.FailureType.RETRY_EXHAUSTED)
                .build();

        assertEquals("原始事件应该正确", "test", ((FailureTestEvent) context.getOriginalEvent()).getMessage());
        assertEquals("失败原因应该正确", cause, context.getFailureCause());
        assertEquals("重试次数应该正确", 3, context.getTotalRetries());
        assertEquals("总耗时应该正确", 5000, context.getTotalDuration());
        assertEquals("失败类型应该正确", FailureContext.FailureType.RETRY_EXHAUSTED, context.getFailureType());
        assertTrue("应该有重试", context.hasRetries());
        assertEquals("失败消息应该正确", "测试异常", context.getFailureMessage());
        assertEquals("失败类型名称应该正确", "RuntimeException", context.getFailureCauseType());
    }

    @Test
    public void testEnhancedFailureHandling() throws InterruptedException {
        AsyncEventBus eventBus = new AsyncEventBus("test", 
            java.util.concurrent.Executors.newFixedThreadPool(2));
        EnhancedFailureListener listener = new EnhancedFailureListener(true);
        
        eventBus.register(listener);
        
        FailureTestEvent event = new FailureTestEvent("enhanced-test");
        eventBus.post(event);
        
        // 等待失败处理完成
        assertTrue("失败处理应该被调用", listener.awaitFailure(2000, TimeUnit.MILLISECONDS));
        
        FailureContext context = listener.getCapturedContext();
        assertNotNull("应该捕获到FailureContext", context);
        assertEquals("原始事件应该正确", event, context.getOriginalEvent());
        assertEquals("重试次数应该正确", 2, context.getTotalRetries());
        assertEquals("失败类型应该是重试耗尽", FailureContext.FailureType.RETRY_EXHAUSTED, context.getFailureType());
        assertTrue("应该有重试", context.hasRetries());
        assertTrue("总耗时应该大于0", context.getTotalDuration() > 0);
        assertNotNull("第一次尝试时间不应该为null", context.getFirstAttemptTime());
        assertNotNull("最后一次尝试时间不应该为null", context.getLastAttemptTime());
        assertTrue("最后尝试时间应该晚于第一次尝试时间", 
            context.getLastAttemptTime().isAfter(context.getFirstAttemptTime()) || 
            context.getLastAttemptTime().isEqual(context.getFirstAttemptTime()));
    }

    @Test
    public void testTraditionalFailureHandling() throws InterruptedException {
        AsyncEventBus eventBus = new AsyncEventBus("test", 
            java.util.concurrent.Executors.newFixedThreadPool(2));
        TraditionalFailureListener listener = new TraditionalFailureListener();
        
        eventBus.register(listener);
        
        FailureTestEvent event = new FailureTestEvent("traditional-test");
        eventBus.post(event);
        
        // 等待失败处理完成
        assertTrue("失败处理应该被调用", listener.awaitFailure(2000, TimeUnit.MILLISECONDS));
        
        FailureTestEvent capturedEvent = listener.getCapturedEvent();
        assertNotNull("应该捕获到事件", capturedEvent);
        assertEquals("事件应该正确", event, capturedEvent);
    }

    @Test
    public void testFailureContextToString() {
        FailureTestEvent event = new FailureTestEvent("test");
        RuntimeException cause = new RuntimeException("测试异常");
        
        FailureContext context = new FailureContext.Builder()
                .originalEvent(event)
                .failureCause(cause)
                .totalRetries(2)
                .firstAttemptTime(LocalDateTime.now())
                .lastAttemptTime(LocalDateTime.now())
                .totalDuration(1000)
                .failureType(FailureContext.FailureType.PROCESSING_EXCEPTION)
                .build();

        String str = context.toString();
        assertTrue("toString应该包含事件类型", str.contains("FailureTestEvent"));
        assertTrue("toString应该包含异常类型", str.contains("RuntimeException"));
        assertTrue("toString应该包含重试次数", str.contains("totalRetries=2"));
        assertTrue("toString应该包含耗时", str.contains("1000ms"));
        assertTrue("toString应该包含失败类型", str.contains("PROCESSING_EXCEPTION"));
    }

    @Test
    public void testFailureTypeEnum() {
        assertEquals("处理异常类型", FailureContext.FailureType.PROCESSING_EXCEPTION, 
            FailureContext.FailureType.valueOf("PROCESSING_EXCEPTION"));
        assertEquals("重试耗尽类型", FailureContext.FailureType.RETRY_EXHAUSTED, 
            FailureContext.FailureType.valueOf("RETRY_EXHAUSTED"));
        assertEquals("系统异常类型", FailureContext.FailureType.SYSTEM_EXCEPTION, 
            FailureContext.FailureType.valueOf("SYSTEM_EXCEPTION"));
    }
}
