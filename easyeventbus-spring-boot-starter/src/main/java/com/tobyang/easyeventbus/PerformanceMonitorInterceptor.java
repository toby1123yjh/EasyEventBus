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

package com.tobyang.easyeventbus;

import com.tobyang.easyeventbus.guava.EventInterceptor;
import com.tobyang.easyeventbus.guava.InterceptorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 性能监控拦截器
 * 
 * 收集事件处理的性能统计信息，包括处理次数、平均耗时、成功率等。
 */
public class PerformanceMonitorInterceptor implements EventInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitorInterceptor.class);

    /** 事件处理统计信息 */
    private final ConcurrentMap<String, EventStats> eventStatsMap = new ConcurrentHashMap<>();

    @Override
    public void beforeProcessing(Object event, InterceptorContext context) {
        // 记录开始时间已经在context中了，这里不需要额外处理
        context.setAttribute("eventType", event.getClass().getSimpleName());
    }

    @Override
    public void afterProcessingSuccess(Object event, InterceptorContext context) {
        String eventType = context.getAttribute("eventType");
        EventStats stats = eventStatsMap.computeIfAbsent(eventType, k -> new EventStats());
        
        stats.totalCount.increment();
        stats.successCount.increment();
        stats.totalDuration.add(context.getDuration());
        
        // 如果处理时间过长，记录警告
        if (context.getDuration() > 1000) { // 超过1秒
            logger.warn("事件处理耗时过长: {}, 耗时: {}ms", eventType, context.getDuration());
        }
    }

    @Override
    public void afterProcessingFailure(Object event, Throwable exception, InterceptorContext context) {
        String eventType = context.getAttribute("eventType");
        EventStats stats = eventStatsMap.computeIfAbsent(eventType, k -> new EventStats());
        
        stats.totalCount.increment();
        stats.failureCount.increment();
        stats.totalDuration.add(context.getDuration());
        
        logger.warn("事件处理失败: {}, 耗时: {}ms, 异常: {}", 
            eventType, context.getDuration(), exception.getMessage());
    }

    /**
     * 获取事件处理统计信息
     */
    public EventStats getEventStats(String eventType) {
        return eventStatsMap.get(eventType);
    }

    /**
     * 获取所有事件类型的统计信息
     */
    public ConcurrentMap<String, EventStats> getAllEventStats() {
        return new ConcurrentHashMap<>(eventStatsMap);
    }

    /**
     * 清空统计信息
     */
    public void clearStats() {
        eventStatsMap.clear();
    }

    @Override
    public int getOrder() {
        return 50; // 中等优先级
    }

    /**
     * 事件处理统计信息
     */
    public static class EventStats {
        private final LongAdder totalCount = new LongAdder();
        private final LongAdder successCount = new LongAdder();
        private final LongAdder failureCount = new LongAdder();
        private final LongAdder totalDuration = new LongAdder();

        public long getTotalCount() {
            return totalCount.sum();
        }

        public long getSuccessCount() {
            return successCount.sum();
        }

        public long getFailureCount() {
            return failureCount.sum();
        }

        public long getTotalDuration() {
            return totalDuration.sum();
        }

        public double getAverageDuration() {
            long total = getTotalCount();
            return total > 0 ? (double) getTotalDuration() / total : 0.0;
        }

        public double getSuccessRate() {
            long total = getTotalCount();
            return total > 0 ? (double) getSuccessCount() / total : 0.0;
        }

        @Override
        public String toString() {
            return String.format("EventStats{total=%d, success=%d, failure=%d, avgDuration=%.2fms, successRate=%.2f%%}",
                getTotalCount(), getSuccessCount(), getFailureCount(), 
                getAverageDuration(), getSuccessRate() * 100);
        }
    }
}
