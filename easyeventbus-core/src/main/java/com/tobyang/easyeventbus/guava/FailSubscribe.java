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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a failure event subscriber.
 *
 * <p>This annotation is similar to {@link Subscribe}, but it subscribes to events
 * that have failed processing in normal {@link Subscribe} methods. When a
 * {@link Subscribe} method throws an exception and all retry attempts (if configured
 * with {@link FailRetry}) are exhausted, the event will be routed to methods
 * annotated with {@code @FailSubscribe}.
 *
 * <p>The failure subscriber method should accept the same event type as the
 * original subscriber method that failed. It can also optionally accept additional
 * parameters to get information about the failure context.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Subscribe
 * @FailRetry(retries = 3)
 * public void handleEvent(MyEvent event) {
 *     // Primary event processing logic
 *     if (someCondition) {
 *         throw new RuntimeException("Processing failed");
 *     }
 * }
 *
 * @FailSubscribe
 * public void handleFailedEvent(MyEvent event) {
 *     // Handle the event that failed in the primary subscriber
 *     logger.error("Event processing failed after retries: {}", event);
 * }
 * }</pre>
 *
 * <p>The failure subscriber methods will be invoked serially by each event bus
 * that they are registered with, similar to regular {@link Subscribe} methods.
 *
 * @author tobyang
 * @since 1.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FailSubscribe {
}
