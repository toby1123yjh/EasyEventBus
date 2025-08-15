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

import java.time.LocalDateTime;

/**
 * 失败上下文
 * 
 * 包含事件处理失败时的详细信息，用于@FailSubscribe方法获取失败原因和处理统计。
 */
public final class FailureContext {

    /** 原始事件 */
    private final Object originalEvent;

    /** 失败原因（最后一次异常） */
    private final Throwable failureCause;

    /** 总重试次数 */
    private final int totalRetries;

    /** 第一次尝试时间 */
    private final LocalDateTime firstAttemptTime;

    /** 最后一次尝试时间 */
    private final LocalDateTime lastAttemptTime;

    /** 总处理耗时（毫秒） */
    private final long totalDuration;

    /** 失败类型 */
    private final FailureType failureType;

    public FailureContext(Object originalEvent, 
                         Throwable failureCause, 
                         int totalRetries,
                         LocalDateTime firstAttemptTime,
                         LocalDateTime lastAttemptTime,
                         long totalDuration,
                         FailureType failureType) {
        this.originalEvent = originalEvent;
        this.failureCause = failureCause;
        this.totalRetries = totalRetries;
        this.firstAttemptTime = firstAttemptTime;
        this.lastAttemptTime = lastAttemptTime;
        this.totalDuration = totalDuration;
        this.failureType = failureType;
    }

    /**
     * 获取原始事件
     */
    public Object getOriginalEvent() {
        return originalEvent;
    }

    /**
     * 获取失败原因
     */
    public Throwable getFailureCause() {
        return failureCause;
    }

    /**
     * 获取总重试次数
     */
    public int getTotalRetries() {
        return totalRetries;
    }

    /**
     * 获取第一次尝试时间
     */
    public LocalDateTime getFirstAttemptTime() {
        return firstAttemptTime;
    }

    /**
     * 获取最后一次尝试时间
     */
    public LocalDateTime getLastAttemptTime() {
        return lastAttemptTime;
    }

    /**
     * 获取总处理耗时（毫秒）
     */
    public long getTotalDuration() {
        return totalDuration;
    }

    /**
     * 获取失败类型
     */
    public FailureType getFailureType() {
        return failureType;
    }

    /**
     * 获取失败原因的简短描述
     */
    public String getFailureMessage() {
        return failureCause != null ? failureCause.getMessage() : "未知错误";
    }

    /**
     * 获取失败原因的类型名称
     */
    public String getFailureCauseType() {
        return failureCause != null ? failureCause.getClass().getSimpleName() : "Unknown";
    }

    /**
     * 是否有重试
     */
    public boolean hasRetries() {
        return totalRetries > 0;
    }

    @Override
    public String toString() {
        return "FailureContext{" +
                "originalEvent=" + originalEvent.getClass().getSimpleName() +
                ", failureCause=" + getFailureCauseType() +
                ", failureMessage='" + getFailureMessage() + '\'' +
                ", totalRetries=" + totalRetries +
                ", totalDuration=" + totalDuration + "ms" +
                ", failureType=" + failureType +
                '}';
    }

    /**
     * 失败类型枚举
     */
    public enum FailureType {
        /** 处理异常（业务逻辑异常） */
        PROCESSING_EXCEPTION,
        
        /** 重试耗尽（达到最大重试次数） */
        RETRY_EXHAUSTED,
        
        /** 系统异常（如反射调用失败） */
        SYSTEM_EXCEPTION
    }

    /**
     * FailureContext构建器
     */
    public static class Builder {
        private Object originalEvent;
        private Throwable failureCause;
        private int totalRetries;
        private LocalDateTime firstAttemptTime;
        private LocalDateTime lastAttemptTime;
        private long totalDuration;
        private FailureType failureType = FailureType.PROCESSING_EXCEPTION;

        public Builder originalEvent(Object originalEvent) {
            this.originalEvent = originalEvent;
            return this;
        }

        public Builder failureCause(Throwable failureCause) {
            this.failureCause = failureCause;
            return this;
        }

        public Builder totalRetries(int totalRetries) {
            this.totalRetries = totalRetries;
            return this;
        }

        public Builder firstAttemptTime(LocalDateTime firstAttemptTime) {
            this.firstAttemptTime = firstAttemptTime;
            return this;
        }

        public Builder lastAttemptTime(LocalDateTime lastAttemptTime) {
            this.lastAttemptTime = lastAttemptTime;
            return this;
        }

        public Builder totalDuration(long totalDuration) {
            this.totalDuration = totalDuration;
            return this;
        }

        public Builder failureType(FailureType failureType) {
            this.failureType = failureType;
            return this;
        }

        public FailureContext build() {
            return new FailureContext(originalEvent, failureCause, totalRetries, 
                firstAttemptTime, lastAttemptTime, totalDuration, failureType);
        }
    }
}
