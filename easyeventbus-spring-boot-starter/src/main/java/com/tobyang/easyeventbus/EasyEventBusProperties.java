package com.tobyang.easyeventbus;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * EasyEventBus统一配置属性
 *
 * 包含EventBus的所有配置选项，包括基础配置、异步配置和延迟事件配置
 *
 * @author tobyang
 * @version 1.0
 */
@Data
@ConfigurationProperties(prefix = EasyEventBusProperties.PREFIX)
public class EasyEventBusProperties {

    public static final String PREFIX = "easyeventbus";

    /** 是否启用EasyEventBus */
    private boolean enable = false;

    /** EventBus标识符 */
    private String identifier = "default";

    /** 每个事件类型的最大订阅者数量 */
    private int maxSubscribersPerEvent = 1000;

    /** 是否启用异步事件处理 */
    private boolean asyncEnabled = false;

    /** 异步事件处理线程池大小 */
    private int asyncThreadPoolSize = 10;

    /** 延迟事件配置 */
    private DelayedEvent delayed = new DelayedEvent();

    /**
     * 延迟事件配置内部类
     *
     * 简化版配置，只包含关键参数
     */
    @Data
    public static class DelayedEvent {
        /** 是否启用延迟事件功能 */
        private boolean enabled = true;

        /** 调度器核心线程池大小 */
        private int corePoolSize = 2;

        /** 线程名称前缀 */
        private String threadNamePrefix = "DelayedEvent-";
    }
}
