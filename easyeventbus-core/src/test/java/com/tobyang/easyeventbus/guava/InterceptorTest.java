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

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * 拦截器功能测试
 */
public class InterceptorTest {

    /** 测试事件类 */
    public static class TestEvent {
        private final String message;

        public TestEvent(String message) {
            this.message = message;
        }

        public String getMessage() { return message; }

        @Override
        public String toString() {
            return "TestEvent{message='" + message + "'}";
        }
    }

    /** 测试拦截器 */
    public static class TestInterceptor implements EventInterceptor {
        private final AtomicInteger beforeCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final int order;

        public TestInterceptor(int order) {
            this.order = order;
        }

        @Override
        public void beforeProcessing(Object event, InterceptorContext context) {
            beforeCount.incrementAndGet();
            context.setAttribute("interceptor-" + order + "-before", true);
        }

        @Override
        public void afterProcessingSuccess(Object event, InterceptorContext context) {
            successCount.incrementAndGet();
            context.setAttribute("interceptor-" + order + "-success", true);
        }

        @Override
        public void afterProcessingFailure(Object event, Throwable exception, InterceptorContext context) {
            failureCount.incrementAndGet();
            context.setAttribute("interceptor-" + order + "-failure", true);
        }

        @Override
        public int getOrder() {
            return order;
        }

        public int getBeforeCount() { return beforeCount.get(); }
        public int getSuccessCount() { return successCount.get(); }
        public int getFailureCount() { return failureCount.get(); }
        public void reset() {
            beforeCount.set(0);
            successCount.set(0);
            failureCount.set(0);
        }
    }

    /** 测试监听器 */
    public static class TestEventListener {
        private final CountDownLatch latch;
        private final boolean shouldFail;

        public TestEventListener(int expectedEvents, boolean shouldFail) {
            this.latch = new CountDownLatch(expectedEvents);
            this.shouldFail = shouldFail;
        }

        @Subscribe
        public void handleTestEvent(TestEvent event) {
            if (shouldFail) {
                throw new RuntimeException("模拟处理失败");
            }
            latch.countDown();
        }

        public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }
    }

    @Test
    public void testInterceptorChainExecution() throws InterruptedException {
        // 创建拦截器
        TestInterceptor interceptor1 = new TestInterceptor(1);
        TestInterceptor interceptor2 = new TestInterceptor(2);
        InterceptorChain chain = new InterceptorChain(Arrays.asList(interceptor2, interceptor1)); // 故意乱序

        // 创建EventBus和监听器
        AsyncEventBus eventBus = new AsyncEventBus("test", 
            java.util.concurrent.Executors.newFixedThreadPool(2));
        TestEventListener listener = new TestEventListener(1, false);

        // 注册监听器（带拦截器）
        eventBus.register(listener);

        // 发布事件
        eventBus.post(new TestEvent("test"));

        // 等待处理完成
        assertTrue("事件应该被处理", listener.await(1000, TimeUnit.MILLISECONDS));

        // 验证拦截器执行
        assertEquals("interceptor1应该执行beforeProcessing", 1, interceptor1.getBeforeCount());
        assertEquals("interceptor1应该执行afterProcessingSuccess", 1, interceptor1.getSuccessCount());
        assertEquals("interceptor1不应该执行afterProcessingFailure", 0, interceptor1.getFailureCount());

        assertEquals("interceptor2应该执行beforeProcessing", 1, interceptor2.getBeforeCount());
        assertEquals("interceptor2应该执行afterProcessingSuccess", 1, interceptor2.getSuccessCount());
        assertEquals("interceptor2不应该执行afterProcessingFailure", 0, interceptor2.getFailureCount());
    }

    @Test
    public void testInterceptorWithFailure() throws InterruptedException {
        // 创建拦截器
        TestInterceptor interceptor = new TestInterceptor(1);
        InterceptorChain chain = new InterceptorChain(Arrays.asList(interceptor));

        // 创建EventBus和监听器（会失败）
        AsyncEventBus eventBus = new AsyncEventBus("test", 
            java.util.concurrent.Executors.newFixedThreadPool(2));
        TestEventListener listener = new TestEventListener(1, true);

        // 注册监听器（带拦截器）
        eventBus.register(listener);

        // 发布事件
        eventBus.post(new TestEvent("test"));

        // 等待一段时间让处理完成
        Thread.sleep(500);

        // 验证拦截器执行
        assertEquals("interceptor应该执行beforeProcessing", 1, interceptor.getBeforeCount());
        assertEquals("interceptor不应该执行afterProcessingSuccess", 0, interceptor.getSuccessCount());
        assertEquals("interceptor应该执行afterProcessingFailure", 1, interceptor.getFailureCount());
    }

    @Test
    public void testInterceptorContext() {
        InterceptorContext context = new InterceptorContext();
        
        // 测试基本功能
        assertTrue("开始时间应该大于0", context.getStartTime() > 0);
        assertEquals("初始重试次数应该为0", 0, context.getRetryCount());
        assertFalse("初始不应该跳过", context.isSkipped());

        // 测试属性设置
        context.setAttribute("test", "value");
        assertEquals("应该能获取设置的属性", "value", context.getAttribute("test"));
        assertTrue("应该包含设置的属性", context.hasAttribute("test"));

        context.removeAttribute("test");
        assertFalse("移除后不应该包含属性", context.hasAttribute("test"));

        // 测试状态设置
        context.setRetryCount(3);
        assertEquals("重试次数应该正确设置", 3, context.getRetryCount());

        context.setSkipped(true);
        assertTrue("跳过状态应该正确设置", context.isSkipped());

        context.setEndTime(System.currentTimeMillis());
        assertTrue("耗时应该大于0", context.getDuration() >= 0);
    }

    @Test
    public void testEmptyInterceptorChain() {
        InterceptorChain chain = new InterceptorChain(Arrays.asList());
        assertTrue("空拦截器链应该为空", chain.isEmpty());
        assertEquals("空拦截器链大小应该为0", 0, chain.size());

        // 调用方法不应该抛出异常
        InterceptorContext context = new InterceptorContext();
        chain.beforeProcessing(new TestEvent("test"), context);
        chain.afterProcessingSuccess(new TestEvent("test"), context);
        chain.afterProcessingFailure(new TestEvent("test"), new RuntimeException(), context);
    }
}
