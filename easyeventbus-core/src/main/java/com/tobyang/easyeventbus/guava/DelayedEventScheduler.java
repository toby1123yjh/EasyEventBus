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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 延迟事件调度器
 *
 * 负责管理延迟事件的调度和执行，使用ScheduledExecutorService来实现延迟功能。
 * 支持延迟发布事件到EventBus。
 */
final class DelayedEventScheduler {

    private static final Logger logger = Logger.getLogger(DelayedEventScheduler.class.getName());

    /** 调度器配置 */
    private final DelayedEventConfig config;

    /** 调度器实例 */
    private final ScheduledExecutorService scheduler;

    /** 是否由本类创建的调度器（需要负责关闭） */
    private final boolean ownedScheduler;

    /**
     * 使用默认配置创建延迟事件调度器
     */
    DelayedEventScheduler() {
        this(DelayedEventConfig.defaultConfig());
    }

    /**
     * 使用指定配置创建延迟事件调度器
     *
     * @param config 调度器配置
     */
    DelayedEventScheduler(DelayedEventConfig config) {
        this.config = checkNotNull(config, "config");
        this.scheduler = createScheduler(config);
        this.ownedScheduler = true;
    }

    /**
     * 使用外部调度器创建延迟事件调度器
     *
     * @param scheduler 外部提供的调度器
     */
    DelayedEventScheduler(ScheduledExecutorService scheduler) {
        this.config = DelayedEventConfig.defaultConfig();
        this.scheduler = checkNotNull(scheduler, "scheduler");
        this.ownedScheduler = false;
    }

    /**
     * 调度延迟事件
     * 
     * @param eventBus 目标事件总线
     * @param event 要发布的事件
     * @param delay 延迟时间
     * @param timeUnit 时间单位
     */
    void scheduleDelayedEvent(EventBus eventBus, Object event, long delay, TimeUnit timeUnit) {
        checkNotNull(eventBus, "eventBus");
        checkNotNull(event, "event");
        checkNotNull(timeUnit, "timeUnit");

        if (delay <= 0) {
            // 无延迟，直接发布
            eventBus.post(event);
            return;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(String.format("调度延迟事件: %s, 延迟: %d %s", 
                event.getClass().getSimpleName(), delay, timeUnit));
        }

        scheduler.schedule(() -> {
            try {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("执行延迟事件: " + event.getClass().getSimpleName());
                }
                eventBus.post(event);
            } catch (Exception e) {
                if (logger.isLoggable(Level.SEVERE)) {
                    logger.severe("延迟事件执行失败: " + event + ", 错误: " + e);
                }
                // 不重新抛出异常，避免影响调度器
            }
        }, delay, timeUnit);
    }

    /**
     * 关闭调度器
     *
     * 只有当调度器是由本类创建时才会关闭
     */
    void shutdown() {
        if (ownedScheduler && !scheduler.isShutdown()) {
            if (logger.isLoggable(Level.INFO)) {
                logger.info("关闭延迟事件调度器");
            }
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 检查调度器是否已关闭
     */
    boolean isShutdown() {
        return scheduler.isShutdown();
    }

    /**
     * 根据配置创建调度器
     */
    private static ScheduledExecutorService createScheduler(DelayedEventConfig config) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
            config.getCorePoolSize(),
            new DelayedEventThreadFactory(config)
        );

        // 配置调度器属性（使用合理的默认值）
        executor.setRemoveOnCancelPolicy(true);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);

        return executor;
    }

    /**
     * 延迟事件线程工厂
     */
    private static final class DelayedEventThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final DelayedEventConfig config;

        DelayedEventThreadFactory(DelayedEventConfig config) {
            this.config = config;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, config.getThreadNamePrefix() + threadNumber.getAndIncrement());
            thread.setDaemon(true);  // 默认为守护线程
            thread.setPriority(Thread.NORM_PRIORITY);  // 默认优先级
            return thread;
        }
    }
}
