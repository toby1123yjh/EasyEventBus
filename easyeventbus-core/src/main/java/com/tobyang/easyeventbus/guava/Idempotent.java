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
 * Marks a method as an idempotency checker for event processing.
 *
 *
 * <p>The idempotency checker method must:
 * <ul>
 *   <li>Return {@code boolean} type</li>
 *   <li>Accept exactly one parameter of the same event type as the @Subscribe method</li>
 *   <li>Be in the same class as the corresponding @Subscribe method</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * @Idempotent
 * public boolean checkOrderProcessed(OrderCreatedEvent event) {
 *     // Check if this order has already been processed
 *     return !orderRepository.exists(event.getOrderId());
 * }
 *
 * }</pre>
 *
 * @author tobyang
 * @since 1.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Idempotent {
    // No parameters needed - the method itself performs the idempotency check
}
