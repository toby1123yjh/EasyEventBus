package com.tobyang.demo.listener;

import com.tobyang.demo.event.TestEvent;
import com.tobyang.easyeventbus.guava.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 测试事件监听器 - 验证所有核心功能
 */
@Component
public class TestEventListener {

    private static final Logger logger = LoggerFactory.getLogger(TestEventListener.class);

    // 模拟已处理的事件ID
    private final ConcurrentHashMap<String, Boolean> processedEvents = new ConcurrentHashMap<>();

    /**
     * 幂等性检查
     */
    @Idempotent
    public boolean checkNotProcessed(TestEvent event) {
        boolean notProcessed = !processedEvents.containsKey(event.getId());
        logger.info("🔍 幂等性检查 - 事件ID: {}, 结果: {}", event.getId(), notProcessed ? "未处理" : "已处理");
        return notProcessed;
    }

    /**
     * 正常处理（带重试）
     */
    @Subscribe
    @FailRetry(retries = 2, interval = 1, timeUnit = TimeUnit.SECONDS)
    public void handleTestEvent(TestEvent event) {
        logger.info("🚀 开始处理事件: {}", event);

        // 模拟处理失败
        if (event.shouldFail()) {
            throw new RuntimeException("模拟处理失败: " + event.getMessage());
        }

        // 模拟处理耗时
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 标记为已处理
        processedEvents.put(event.getId(), true);
        logger.info("✅ 事件处理成功: {}", event.getId());
    }

    /**
     * 失败处理（支持FailureContext）
     */
    @FailSubscribe
    public void handleTestEventFailure(TestEvent event, FailureContext context) {
        logger.error("❌ 事件处理失败: {}", event.getId());
        logger.error("   失败原因: {}", context.getFailureMessage());
        logger.error("   重试次数: {}", context.getTotalRetries());
        logger.error("   总耗时: {}ms", context.getTotalDuration());
        logger.error("   失败类型: {}", context.getFailureType());
    }

    /**
     * 获取已处理事件数量
     */
    public int getProcessedCount() {
        return processedEvents.size();
    }

    /**
     * 清空已处理事件
     */
    public void clear() {
        processedEvents.clear();
    }
}
