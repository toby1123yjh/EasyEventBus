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

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * 同一事件类型的三阶段处理方法组
 *
 * 包含三个阶段：
 * 1. 幂等性检查 (@Idempotent) - 返回boolean
 * 2. 正常处理 (@Subscribe) - 主要业务逻辑
 * 3. 失败处理 (@FailSubscribe) - 失败补偿逻辑
 */
final class EventProcessorGroup {

    /** 目标监听器对象 */
    private final Object target;

    /** 事件类型 */
    private final Class<?> eventType;

    /** 幂等性检查方法（可选，必须返回boolean） */
    @Nullable
    private Method idempotentMethod;

    /** 正常处理方法（必需） */
    @Nullable
    private Method subscribeMethod;

    /** 失败处理方法（可选） */
    @Nullable
    private Method failSubscribeMethod;

    /** 创建事件处理器组 */
    EventProcessorGroup(Object target, Class<?> eventType) {
        this.target = target;
        this.eventType = eventType;
    }

    /** 设置幂等性检查方法 */
    void setIdempotentMethod(Method method) {
        if (method.getReturnType() != boolean.class) {
            throw new IllegalArgumentException(
                String.format("@Idempotent方法%s必须返回boolean，但返回%s",
                    method.getName(), method.getReturnType().getSimpleName()));
        }
        this.idempotentMethod = method;
    }

    /** 设置正常处理方法 */
    void setSubscribeMethod(Method method) {
        this.subscribeMethod = method;
    }

    /** 设置失败处理方法 */
    void setFailSubscribeMethod(Method method) {
        this.failSubscribeMethod = method;
    }

    Object getTarget() {
        return target;
    }

    Class<?> getEventType() {
        return eventType;
    }

    @Nullable
    Method getIdempotentMethod() {
        return idempotentMethod;
    }

    @Nullable
    Method getSubscribeMethod() {
        return subscribeMethod;
    }

    @Nullable
    Method getFailSubscribeMethod() {
        return failSubscribeMethod;
    }

    boolean hasSubscribeMethod() {
        return subscribeMethod != null;
    }

    boolean hasIdempotentMethod() {
        return idempotentMethod != null;
    }

    boolean hasFailSubscribeMethod() {
        return failSubscribeMethod != null;
    }

    /** 验证组的有效性 */
    void validate() {
        if (!hasSubscribeMethod()) {
            throw new IllegalArgumentException(
                String.format("事件类型%s的处理器组必须至少有一个@Subscribe方法",
                    eventType.getSimpleName()));
        }
    }

    @Override
    public String toString() {
        return String.format("EventProcessorGroup{eventType=%s, hasIdempotent=%s, hasSubscribe=%s, hasFailSubscribe=%s}",
            eventType.getSimpleName(), hasIdempotentMethod(), hasSubscribeMethod(), hasFailSubscribeMethod());
    }
}
