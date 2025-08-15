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
 * 延迟事件调度器配置
 *
 * 简化版配置，只包含关键参数
 */
public final class DelayedEventConfig {

    /** 调度器核心线程池大小 */
    private final int corePoolSize;

    /** 线程名称前缀 */
    private final String threadNamePrefix;

    private DelayedEventConfig(Builder builder) {
        this.corePoolSize = builder.corePoolSize;
        this.threadNamePrefix = builder.threadNamePrefix;
    }

    /** 创建默认配置 */
    public static DelayedEventConfig defaultConfig() {
        return new Builder().build();
    }

    /** 创建构建器 */
    public static Builder builder() {
        return new Builder();
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    /** 配置构建器 */
    public static final class Builder {
        private int corePoolSize = 2;
        private String threadNamePrefix = "DelayedEvent-";

        public Builder corePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
            return this;
        }

        public Builder threadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
            return this;
        }

        public DelayedEventConfig build() {
            return new DelayedEventConfig(this);
        }
    }

    @Override
    public String toString() {
        return "DelayedEventConfig{" +
                "corePoolSize=" + corePoolSize +
                ", threadNamePrefix='" + threadNamePrefix + '\'' +
                '}';
    }
}
