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

/**
 * 事件拦截器接口
 * 
 * 提供事件处理前后的拦截能力，支持日志记录、性能监控、审计跟踪等功能。
 * 拦截器按照order顺序执行。
 */
public interface EventInterceptor {

    /**
     * 在事件处理开始前执行
     * 
     * @param event 要处理的事件
     * @param context 拦截器上下文
     */
    default void beforeProcessing(Object event, InterceptorContext context) {
        // 默认空实现
    }

    /**
     * 在事件处理成功后执行
     * 
     * @param event 要处理的事件
     * @param context 拦截器上下文
     */
    default void afterProcessingSuccess(Object event, InterceptorContext context) {
        // 默认空实现
    }

    /**
     * 在事件处理失败后执行
     * 
     * @param event 要处理的事件
     * @param exception 最终的处理异常
     * @param context 拦截器上下文
     */
    default void afterProcessingFailure(Object event, Throwable exception, InterceptorContext context) {
        // 默认空实现
    }

    /**
     * 获取拦截器的执行顺序
     * 数值越小，执行顺序越靠前
     * 
     * @return 执行顺序，默认为0
     */
    default int getOrder() {
        return 0;
    }
}
