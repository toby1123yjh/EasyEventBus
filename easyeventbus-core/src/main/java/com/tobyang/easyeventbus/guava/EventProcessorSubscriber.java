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

import com.google.common.annotations.VisibleForTesting;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 三阶段事件处理订阅者
 *
 * 协调执行三个阶段：
 * 1. 幂等性检查 (@Idempotent)
 * 2. 正常处理 (@Subscribe + 重试)
 * 3. 失败处理 (@FailSubscribe)
 */
final class EventProcessorSubscriber extends Subscriber {

    private static final Logger logger = Logger.getLogger(EventProcessorSubscriber.class.getName());

    /** 事件处理器组，包含三阶段方法 */
    private final EventProcessorGroup processorGroup;

    /** 重试配置 */
    private final RetryConfig retryConfig;

    /** 拦截器链 */
    private final InterceptorChain interceptorChain;

    /** 第一次尝试时间（用于FailureContext） */
    private volatile LocalDateTime firstAttemptTime;

    /** 创建事件处理器订阅者 */
    EventProcessorSubscriber(EventBus bus, EventProcessorGroup processorGroup) {
        this(bus, processorGroup, new InterceptorChain(java.util.Collections.emptyList()));
    }

    /** 创建带拦截器的事件处理器订阅者 */
    EventProcessorSubscriber(EventBus bus, EventProcessorGroup processorGroup, InterceptorChain interceptorChain) {
        super(bus, processorGroup.getTarget(), processorGroup.getSubscribeMethod());
        this.processorGroup = checkNotNull(processorGroup);
        this.retryConfig = extractRetryConfig(processorGroup.getSubscribeMethod());
        this.interceptorChain = checkNotNull(interceptorChain);

        processorGroup.validate();
    }

    @Override
    @VisibleForTesting
    void invokeSubscriberMethod(Object event) throws InvocationTargetException {
        InterceptorContext context = new InterceptorContext();
        firstAttemptTime = LocalDateTime.now(); // 记录第一次尝试时间

        try {
            // 拦截器：处理开始前
            interceptorChain.beforeProcessing(event, context);

            // 阶段1：幂等性检查
            if (processorGroup.hasIdempotentMethod()) {
                boolean shouldProcess = performIdempotencyCheck(event);
                if (!shouldProcess) {
                    context.setSkipped(true);
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("跳过事件处理，幂等性检查失败: " + event);
                    }
                    return; // 跳过处理
                }
            }

            // 阶段2：正常处理（带重试）
            performNormalProcessing(event, context);

            // 拦截器：处理成功后
            context.setEndTime(System.currentTimeMillis());
            interceptorChain.afterProcessingSuccess(event, context);

        } catch (InvocationTargetException e) {
            // 阶段3：失败处理（带FailureContext）
            performFailureProcessing(event, e, context);

            // 拦截器：处理失败后
            context.setEndTime(System.currentTimeMillis());
            interceptorChain.afterProcessingFailure(event, e.getCause(), context);

            throw e; // 重新抛出异常
        }
    }

    /** 执行幂等性检查 */
    private boolean performIdempotencyCheck(Object event) throws InvocationTargetException {
        Method idempotentMethod = processorGroup.getIdempotentMethod();
        try {
            idempotentMethod.setAccessible(true);
            Object result = idempotentMethod.invoke(processorGroup.getTarget(), event);
            return (Boolean) result;
        } catch (IllegalAccessException e) {
            throw new Error("幂等性方法不可访问: " + idempotentMethod, e);
        } catch (IllegalArgumentException e) {
            throw new Error("幂等性方法参数错误: " + event, e);
        }
    }

    /** 执行正常处理（带重试逻辑） */
    private void performNormalProcessing(Object event, InterceptorContext context) throws InvocationTargetException {
        Method subscribeMethod = processorGroup.getSubscribeMethod();
        InvocationTargetException lastException = null;

        int maxAttempts = retryConfig != null ? retryConfig.getMaxRetries() + 1 : 1;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                subscribeMethod.setAccessible(true);
                subscribeMethod.invoke(processorGroup.getTarget(), event);
                
                if (logger.isLoggable(Level.FINE) && attempt > 1) {
                    logger.fine(String.format("第%d次尝试处理成功: %s", attempt, event));
                }
                return; // 成功
                
            } catch (InvocationTargetException e) {
                lastException = e;
                context.setRetryCount(attempt - 1); // 更新重试次数

                if (attempt < maxAttempts && retryConfig != null) {
                    if (logger.isLoggable(Level.WARNING)) {
                        logger.warning(String.format("第%d次尝试失败，准备重试: %s",
                            attempt, e.getCause()));
                    }

                    // 等待后重试
                    try {
                        Thread.sleep(retryConfig.getRetryIntervalMs());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试等待被中断", ie);
                    }
                } else {
                    if (logger.isLoggable(Level.SEVERE)) {
                        logger.severe(String.format("处理失败，已尝试%d次: %s",
                            attempt, e.getCause()));
                    }
                }
            } catch (IllegalAccessException e) {
                throw new Error("订阅方法不可访问: " + subscribeMethod, e);
            } catch (IllegalArgumentException e) {
                throw new Error("订阅方法参数错误: " + event, e);
            }
        }

        // 所有尝试都失败
        throw lastException;
    }

    /** 执行失败处理（带FailureContext） */
    private void performFailureProcessing(Object event, InvocationTargetException originalException, InterceptorContext context) {
        if (!processorGroup.hasFailSubscribeMethod()) {
            return; // 没有失败处理方法
        }

        Method failSubscribeMethod = processorGroup.getFailSubscribeMethod();
        try {
            failSubscribeMethod.setAccessible(true);

            // 检查方法参数，决定是否传递FailureContext
            Class<?>[] parameterTypes = failSubscribeMethod.getParameterTypes();
            if (parameterTypes.length == 2 && parameterTypes[1] == FailureContext.class) {
                // 方法接受FailureContext参数
                FailureContext failureContext = createFailureContext(event, originalException, context);
                failSubscribeMethod.invoke(processorGroup.getTarget(), event, failureContext);
            } else {
                // 传统方法，只接受事件参数
                failSubscribeMethod.invoke(processorGroup.getTarget(), event);
            }

            if (logger.isLoggable(Level.INFO)) {
                logger.info("失败处理完成: " + event);
            }
        } catch (Exception e) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.severe("失败处理本身失败: " + event + ", 错误: " + e);
            }
            // 不抛出异常 - 失败处理是尽力而为
        }
    }

    /** 创建失败上下文 */
    private FailureContext createFailureContext(Object event, InvocationTargetException originalException, InterceptorContext context) {
        LocalDateTime lastAttemptTime = LocalDateTime.now();
        long totalDuration = context.getDuration();
        int totalRetries = context.getRetryCount();

        // 确定失败类型
        FailureContext.FailureType failureType;
        if (originalException.getCause() instanceof IllegalAccessException ||
            originalException.getCause() instanceof IllegalArgumentException) {
            failureType = FailureContext.FailureType.SYSTEM_EXCEPTION;
        } else if (totalRetries > 0) {
            failureType = FailureContext.FailureType.RETRY_EXHAUSTED;
        } else {
            failureType = FailureContext.FailureType.PROCESSING_EXCEPTION;
        }

        return new FailureContext.Builder()
                .originalEvent(event)
                .failureCause(originalException.getCause())
                .totalRetries(totalRetries)
                .firstAttemptTime(firstAttemptTime)
                .lastAttemptTime(lastAttemptTime)
                .totalDuration(totalDuration)
                .failureType(failureType)
                .build();
    }

    /** 提取重试配置 */
    private static RetryConfig extractRetryConfig(Method method) {
        FailRetry failRetry = method.getAnnotation(FailRetry.class);
        if (failRetry == null) {
            return null;
        }
        return new RetryConfig(failRetry.retries(), failRetry.interval(), failRetry.timeUnit());
    }

    /** 重试配置 */
    private static final class RetryConfig {
        private final int maxRetries;
        private final long retryIntervalMs;

        RetryConfig(int maxRetries, long interval, TimeUnit timeUnit) {
            this.maxRetries = maxRetries;
            this.retryIntervalMs = timeUnit.toMillis(interval);
        }

        int getMaxRetries() {
            return maxRetries;
        }

        long getRetryIntervalMs() {
            return retryIntervalMs;
        }
    }
}
