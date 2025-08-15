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
import java.util.concurrent.TimeUnit;

/**
 * Marks a method for automatic retry when event processing fails.
 *
 * <p>This annotation must be used in conjunction with {@link Subscribe} annotation.
 * When an event subscriber method throws an exception, the event will be automatically
 * retried according to the specified retry configuration.
 *
 * <p>The failed events will be placed in a delayed queue and re-executed after the
 * specified interval. If all retry attempts fail, the event may be routed to a
 * {@link FailSubscribe} method if available.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Subscribe
 * @FailRetry(retries = 3, interval = 5, timeUnit = TimeUnit.SECONDS)
 * public void handleEvent(MyEvent event) {
 *     // Event processing logic that might fail
 * }
 * }</pre>
 *
 * @author tobyang
 * @since 1.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FailRetry {

    /**
     * The maximum number of retry attempts.
     * 
     * @return the maximum retry count, must be greater than 0
     */
    int retries() default 3;

    /**
     * The interval between retry attempts.
     * 
     * @return the retry interval value
     */
    long interval() default 1;

    /**
     * The time unit for the retry interval.
     * 
     * @return the time unit for the interval
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
