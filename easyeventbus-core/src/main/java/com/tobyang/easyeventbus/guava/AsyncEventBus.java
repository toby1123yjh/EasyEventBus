/*
 * Copyright (C) 2007 The Guava Authors
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

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An {@link EventBus} that takes the Executor of your choice and uses it to dispatch events,
 * allowing dispatch to occur asynchronously.
 *
 * @author Cliff Biffle
 * @since 10.0
 */
public class AsyncEventBus extends EventBus {

  /** 延迟事件调度器（懒加载） */
  private volatile DelayedEventScheduler delayedEventScheduler;

  /** 延迟事件配置 */
  private final DelayedEventConfig delayedEventConfig;

  /**
   * Creates a new AsyncEventBus that will use {@code executor} to dispatch events. Assigns {@code
   * identifier} as the bus's name for logging purposes.
   *
   * @param identifier short name for the bus, for logging purposes.
   * @param executor Executor to use to dispatch events. It is the caller's responsibility to shut
   *     down the executor after the last event has been posted to this event bus.
   */
  public AsyncEventBus(String identifier, Executor executor) {
    this(identifier, executor, DelayedEventConfig.defaultConfig());
  }

  /**
   * Creates a new AsyncEventBus with delayed event config.
   *
   * @param identifier short name for the bus
   * @param executor Executor to use to dispatch events
   * @param delayedEventConfig configuration for delayed event scheduler
   */
  public AsyncEventBus(String identifier, Executor executor, DelayedEventConfig delayedEventConfig) {
    super(identifier, executor, Dispatcher.legacyAsync(), LoggingHandler.INSTANCE);
    this.delayedEventConfig = checkNotNull(delayedEventConfig);
  }

  /**
   * 延迟发布事件到所有注册的订阅者
   *
   * 事件将在指定的延迟时间后发布。如果延迟时间小于等于0，则立即发布。
   * 注意：这是异步操作，延迟调度和事件处理都在独立的线程池中执行。
   *
   * @param event 要发布的事件
   * @param delay 延迟时间
   * @param timeUnit 时间单位
   */
  public void postDelayed(Object event, long delay, TimeUnit timeUnit) {
    checkNotNull(event, "event");
    checkNotNull(timeUnit, "timeUnit");

    getDelayedEventScheduler().scheduleDelayedEvent(this, event, delay, timeUnit);
  }

  /**
   * 获取延迟事件调度器（懒加载）
   */
  private DelayedEventScheduler getDelayedEventScheduler() {
    if (delayedEventScheduler == null) {
      synchronized (this) {
        if (delayedEventScheduler == null) {
          delayedEventScheduler = new DelayedEventScheduler(delayedEventConfig);
        }
      }
    }
    return delayedEventScheduler;
  }

  /**
   * Creates a new AsyncEventBus that will use {@code executor} to dispatch events.
   *
   * @param executor Executor to use to dispatch events. It is the caller's responsibility to shut
   *     down the executor after the last event has been posted to this event bus.
   * @param subscriberExceptionHandler Handler used to handle exceptions thrown from subscribers.
   *     See {@link SubscriberExceptionHandler} for more information.
   * @since 16.0
   */
  public AsyncEventBus(Executor executor, SubscriberExceptionHandler subscriberExceptionHandler) {
    super("default", executor, Dispatcher.legacyAsync(), subscriberExceptionHandler);
    this.delayedEventConfig = DelayedEventConfig.defaultConfig();
  }

  /**
   * Creates a new AsyncEventBus that will use {@code executor} to dispatch events.
   *
   * @param executor Executor to use to dispatch events. It is the caller's responsibility to shut
   *     down the executor after the last event has been posted to this event bus.
   */
  public AsyncEventBus(Executor executor) {
    super("default", executor, Dispatcher.legacyAsync(), LoggingHandler.INSTANCE);
    this.delayedEventConfig = DelayedEventConfig.defaultConfig();
  }
}
