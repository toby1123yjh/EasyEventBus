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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 拦截器上下文
 * 
 * 提供拦截器执行过程中的上下文信息，包括事件处理的统计信息和自定义属性。
 */
public final class InterceptorContext {

    /** 事件处理开始时间 */
    private final long startTime;

    /** 事件处理结束时间 */
    private volatile long endTime;

    /** 重试次数 */
    private volatile int retryCount;

    /** 是否跳过了处理（幂等性检查返回false） */
    private volatile boolean skipped;

    /** 自定义属性 */
    private final ConcurrentMap<String, Object> attributes;

    public InterceptorContext() {
        this.startTime = System.currentTimeMillis();
        this.attributes = new ConcurrentHashMap<>();
    }

    /**
     * 获取事件处理开始时间
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * 获取事件处理结束时间
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * 设置事件处理结束时间
     */
    void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /**
     * 获取处理耗时（毫秒）
     */
    public long getDuration() {
        return endTime > 0 ? endTime - startTime : System.currentTimeMillis() - startTime;
    }

    /**
     * 获取重试次数
     */
    public int getRetryCount() {
        return retryCount;
    }

    /**
     * 设置重试次数
     */
    void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    /**
     * 是否跳过了处理
     */
    public boolean isSkipped() {
        return skipped;
    }

    /**
     * 设置是否跳过了处理
     */
    void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    /**
     * 设置自定义属性
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 获取自定义属性
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    /**
     * 移除自定义属性
     */
    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    /**
     * 检查是否包含指定属性
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    @Override
    public String toString() {
        return "InterceptorContext{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", duration=" + getDuration() + "ms" +
                ", retryCount=" + retryCount +
                ", skipped=" + skipped +
                ", attributes=" + attributes.keySet() +
                '}';
    }
}
