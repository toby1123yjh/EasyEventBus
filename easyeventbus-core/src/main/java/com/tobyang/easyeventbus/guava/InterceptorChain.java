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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 拦截器链管理器
 * 
 * 管理所有注册的拦截器，按照order顺序执行拦截器方法。
 */
public final class InterceptorChain {

    private static final Logger logger = Logger.getLogger(InterceptorChain.class.getName());

    /** 注册的拦截器列表（按order排序） */
    private final List<EventInterceptor> interceptors;

    public InterceptorChain(List<EventInterceptor> interceptors) {
        this.interceptors = new ArrayList<>(interceptors);
        // 按order排序，order小的先执行
        this.interceptors.sort(Comparator.comparingInt(EventInterceptor::getOrder));
    }

    /**
     * 执行所有拦截器的beforeProcessing方法
     */
    public void beforeProcessing(Object event, InterceptorContext context) {
        for (EventInterceptor interceptor : interceptors) {
            try {
                interceptor.beforeProcessing(event, context);
            } catch (Exception e) {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.warning("拦截器beforeProcessing执行失败: " + interceptor.getClass().getSimpleName() + ", 错误: " + e);
                }
                // 拦截器异常不影响事件处理
            }
        }
    }

    /**
     * 执行所有拦截器的afterProcessingSuccess方法
     */
    public void afterProcessingSuccess(Object event, InterceptorContext context) {
        // 成功后按相反顺序执行（类似finally块的执行顺序）
        List<EventInterceptor> reversedInterceptors = new ArrayList<>(interceptors);
        Collections.reverse(reversedInterceptors);
        
        for (EventInterceptor interceptor : reversedInterceptors) {
            try {
                interceptor.afterProcessingSuccess(event, context);
            } catch (Exception e) {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.warning("拦截器afterProcessingSuccess执行失败: " + interceptor.getClass().getSimpleName() + ", 错误: " + e);
                }
                // 拦截器异常不影响事件处理
            }
        }
    }

    /**
     * 执行所有拦截器的afterProcessingFailure方法
     */
    public void afterProcessingFailure(Object event, Throwable exception, InterceptorContext context) {
        // 失败后按相反顺序执行
        List<EventInterceptor> reversedInterceptors = new ArrayList<>(interceptors);
        Collections.reverse(reversedInterceptors);
        
        for (EventInterceptor interceptor : reversedInterceptors) {
            try {
                interceptor.afterProcessingFailure(event, exception, context);
            } catch (Exception e) {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.warning("拦截器afterProcessingFailure执行失败: " + interceptor.getClass().getSimpleName() + ", 错误: " + e);
                }
                // 拦截器异常不影响事件处理
            }
        }
    }

    /**
     * 获取拦截器数量
     */
    public int size() {
        return interceptors.size();
    }

    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return interceptors.isEmpty();
    }
}
