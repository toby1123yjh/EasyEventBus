package com.tobyang.demo.controller;

import com.tobyang.demo.event.TestEvent;
import com.tobyang.demo.listener.TestEventListener;
import com.tobyang.easyeventbus.guava.AsyncEventBus;
import com.tobyang.easyeventbus.guava.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 演示控制器 - 提供HTTP接口测试各种功能
 */
@RestController
@RequestMapping("/demo")
public class DemoController {

    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);

    @Autowired
    private EventBus eventBus;

    @Autowired
    private TestEventListener testEventListener;

    /**
     * 测试正常事件处理
     */
    @PostMapping("/success")
    public String testSuccess(@RequestParam(defaultValue = "test-success") String message) {
        String id = "success-" + System.currentTimeMillis();
        TestEvent event = new TestEvent(id, message, false);
        
        logger.info("📤 发布成功事件: {}", event);
        eventBus.post(event);
        
        return "✅ 成功事件已发布: " + id;
    }

    /**
     * 测试失败事件处理（会重试）
     */
    @PostMapping("/failure")
    public String testFailure(@RequestParam(defaultValue = "test-failure") String message) {
        String id = "failure-" + System.currentTimeMillis();
        TestEvent event = new TestEvent(id, message, true);
        
        logger.info("📤 发布失败事件: {}", event);
        eventBus.post(event);
        
        return "❌ 失败事件已发布: " + id + " (将会重试2次)";
    }

    /**
     * 测试幂等性（重复发送相同ID的事件）
     */
    @PostMapping("/idempotent")
    public String testIdempotent(@RequestParam String id, @RequestParam(defaultValue = "test-idempotent") String message) {
        TestEvent event = new TestEvent(id, message, false);
        
        logger.info("📤 发布幂等性测试事件: {}", event);
        eventBus.post(event);
        
        return "🔄 幂等性测试事件已发布: " + id;
    }

    /**
     * 测试延迟事件（仅AsyncEventBus支持）
     */
    @PostMapping("/delayed")
    public String testDelayed(@RequestParam(defaultValue = "5") int seconds, @RequestParam(defaultValue = "test-delayed") String message) {
        if (!(eventBus instanceof AsyncEventBus)) {
            return "⚠️ 当前使用同步EventBus，不支持延迟事件。请在配置中设置 easyeventbus.async-enabled=true";
        }

        String id = "delayed-" + System.currentTimeMillis();
        TestEvent event = new TestEvent(id, message, false);
        
        logger.info("📤 发布延迟事件: {} ({}秒后执行)", event, seconds);
        ((AsyncEventBus) eventBus).postDelayed(event, seconds, TimeUnit.SECONDS);
        
        return "⏰ 延迟事件已发布: " + id + " (将在" + seconds + "秒后执行)";
    }

    /**
     * 获取处理统计
     */
    @GetMapping("/stats")
    public String getStats() {
        int processedCount = testEventListener.getProcessedCount();
        String eventBusType = eventBus instanceof AsyncEventBus ? "AsyncEventBus" : "EventBus";
        
        return String.format("📊 统计信息:\n" +
                "- EventBus类型: %s\n" +
                "- 已处理事件数: %d\n" +
                "- 当前时间: %s", 
                eventBusType, processedCount, java.time.LocalDateTime.now());
    }

    /**
     * 清空统计
     */
    @PostMapping("/clear")
    public String clearStats() {
        testEventListener.clear();
        return "🧹 统计信息已清空";
    }

    /**
     * 批量测试
     */
    @PostMapping("/batch")
    public String batchTest(@RequestParam(defaultValue = "5") int count) {
        logger.info("📤 开始批量测试，数量: {}", count);
        
        for (int i = 1; i <= count; i++) {
            String id = "batch-" + System.currentTimeMillis() + "-" + i;
            boolean shouldFail = i % 3 == 0; // 每3个事件中有1个失败
            TestEvent event = new TestEvent(id, "批量测试消息-" + i, shouldFail);
            eventBus.post(event);
        }
        
        return "🚀 批量测试完成，发布了" + count + "个事件";
    }
}
