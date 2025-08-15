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

/**
 * 日志记录拦截器
 * 
 * 记录事件处理的开始、成功、失败等信息，用于调试和监控。
 */
public class LoggingEventInterceptor implements EventInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoggingEventInterceptor.class);

    @Override
    public void beforeProcessing(Object event, InterceptorContext context) {
        if (logger.isDebugEnabled()) {
            logger.debug("开始处理事件: {}", event.getClass().getSimpleName());
        }
    }

    @Override
    public void afterProcessingSuccess(Object event, InterceptorContext context) {
        if (logger.isDebugEnabled()) {
            logger.debug("事件处理成功: {}, 耗时: {}ms, 重试次数: {}, 是否跳过: {}", 
                event.getClass().getSimpleName(), 
                context.getDuration(),
                context.getRetryCount(),
                context.isSkipped());
        }
    }

    @Override
    public void afterProcessingFailure(Object event, Throwable exception, InterceptorContext context) {
        if (logger.isWarnEnabled()) {
            logger.warn("事件处理失败: {}, 耗时: {}ms, 重试次数: {}, 异常: {}", 
                event.getClass().getSimpleName(), 
                context.getDuration(),
                context.getRetryCount(),
                exception.getMessage());
        }
    }

    @Override
    public int getOrder() {
        return 100; // 较低优先级，最后执行
    }
}
