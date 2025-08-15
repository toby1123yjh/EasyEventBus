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
 * 事件处理器订阅者工厂
 *
 * 分析监听器类，按事件类型分组方法，创建三阶段处理器：
 * 1. @Idempotent - 幂等性检查（可选）
 * 2. @Subscribe - 主要处理逻辑（必需）
 * 3. @FailSubscribe - 失败处理（可选）
 */
final class EventProcessorSubscriberFactory {

    private EventProcessorSubscriberFactory() {}

    /** 从监听器对象创建事件处理器订阅者 */
    static com.google.common.collect.Multimap<Class<?>, Subscriber> createEventProcessorSubscribers(
            EventBus bus, Object listener) {
        return createEventProcessorSubscribers(bus, listener, new InterceptorChain(java.util.Collections.emptyList()));
    }

    /** 从监听器对象创建带拦截器的事件处理器订阅者 */
    static com.google.common.collect.Multimap<Class<?>, Subscriber> createEventProcessorSubscribers(
            EventBus bus, Object listener, InterceptorChain interceptorChain) {

        com.google.common.collect.Multimap<Class<?>, Subscriber> subscribers =
            com.google.common.collect.HashMultimap.create();

        // 按事件类型分组方法
        java.util.Map<Class<?>, EventProcessorGroup> processorGroups =
            groupMethodsByEventType(listener);

        // 为每个有@Subscribe方法的组创建订阅者
        for (EventProcessorGroup group : processorGroups.values()) {
            if (group.hasSubscribeMethod()) {
                EventProcessorSubscriber subscriber = new EventProcessorSubscriber(bus, group, interceptorChain);
                subscribers.put(group.getEventType(), subscriber);
            }
        }

        return subscribers;
    }

    /** 按事件类型分组方法 */
    private static java.util.Map<Class<?>, EventProcessorGroup> groupMethodsByEventType(Object listener) {
        java.util.Map<Class<?>, EventProcessorGroup> groups = new java.util.HashMap<>();

        Class<?> listenerClass = listener.getClass();
        for (java.lang.reflect.Method method : listenerClass.getDeclaredMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();

            // 跳过参数不是1个的方法
            if (parameterTypes.length != 1) {
                continue;
            }

            Class<?> eventType = parameterTypes[0];

            // 跳过基本类型
            if (eventType.isPrimitive()) {
                continue;
            }

            // 获取或创建该事件类型的处理器组
            EventProcessorGroup group = groups.computeIfAbsent(eventType,
                type -> new EventProcessorGroup(listener, type));

            // 将方法添加到对应阶段
            if (method.isAnnotationPresent(Idempotent.class)) {
                if (group.hasIdempotentMethod()) {
                    throw new IllegalArgumentException(
                        String.format("事件类型%s在类%s中发现多个@Idempotent方法",
                            eventType.getSimpleName(), listenerClass.getSimpleName()));
                }
                group.setIdempotentMethod(method);
            }

            if (method.isAnnotationPresent(Subscribe.class)) {
                if (group.hasSubscribeMethod()) {
                    throw new IllegalArgumentException(
                        String.format("事件类型%s在类%s中发现多个@Subscribe方法",
                            eventType.getSimpleName(), listenerClass.getSimpleName()));
                }
                group.setSubscribeMethod(method);
            }

            if (method.isAnnotationPresent(FailSubscribe.class)) {
                if (group.hasFailSubscribeMethod()) {
                    throw new IllegalArgumentException(
                        String.format("事件类型%s在类%s中发现多个@FailSubscribe方法",
                            eventType.getSimpleName(), listenerClass.getSimpleName()));
                }
                group.setFailSubscribeMethod(method);
            }
        }

        return groups;
    }
}
