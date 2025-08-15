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

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Test for the three-phase event processing pattern.
 */
public class EventProcessorTest {

    /** Test event class. */
    public static class TestEvent {
        private final String id;
        private final String message;

        public TestEvent(String id, String message) {
            this.id = id;
            this.message = message;
        }

        public String getId() { return id; }
        public String getMessage() { return message; }

        @Override
        public String toString() {
            return "TestEvent{id='" + id + "', message='" + message + "'}";
        }
    }

    /** Test listener with all three phases. */
    public static class TestEventListener {
        private final AtomicBoolean shouldProcess = new AtomicBoolean(true);
        private final AtomicInteger processCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicBoolean shouldFail = new AtomicBoolean(false);

        @Idempotent
        public boolean checkShouldProcess(TestEvent event) {
            return shouldProcess.get();
        }

        @Subscribe
        @FailRetry(retries = 2, interval = 10, timeUnit = java.util.concurrent.TimeUnit.MILLISECONDS)
        public void handleTestEvent(TestEvent event) {
            processCount.incrementAndGet();
            if (shouldFail.get()) {
                throw new RuntimeException("Simulated failure");
            }
        }

        @FailSubscribe
        public void handleTestEventFailure(TestEvent event) {
            failureCount.incrementAndGet();
        }

        // Test control methods
        public void setShouldProcess(boolean shouldProcess) {
            this.shouldProcess.set(shouldProcess);
        }

        public void setShouldFail(boolean shouldFail) {
            this.shouldFail.set(shouldFail);
        }

        public int getProcessCount() { return processCount.get(); }
        public int getFailureCount() { return failureCount.get(); }
        public void reset() {
            processCount.set(0);
            failureCount.set(0);
            shouldProcess.set(true);
            shouldFail.set(false);
        }
    }

    @Test
    public void testNormalProcessing() throws InterruptedException {
        EventBus eventBus = new EventBus("test");
        TestEventListener listener = new TestEventListener();
        
        eventBus.register(listener);
        eventBus.post(new TestEvent("1", "test message"));
        
        // Wait a bit for async processing
        Thread.sleep(100);
        
        assertEquals("Should process once", 1, listener.getProcessCount());
        assertEquals("Should not call failure handler", 0, listener.getFailureCount());
    }

    @Test
    public void testIdempotencySkip() throws InterruptedException {
        EventBus eventBus = new EventBus("test");
        TestEventListener listener = new TestEventListener();
        
        // Set to skip processing
        listener.setShouldProcess(false);
        
        eventBus.register(listener);
        eventBus.post(new TestEvent("1", "test message"));
        
        // Wait a bit for async processing
        Thread.sleep(100);
        
        assertEquals("Should not process due to idempotency", 0, listener.getProcessCount());
        assertEquals("Should not call failure handler", 0, listener.getFailureCount());
    }

    @Test
    public void testRetryAndFailure() throws InterruptedException {
        EventBus eventBus = new EventBus("test");
        TestEventListener listener = new TestEventListener();
        
        // Set to fail processing
        listener.setShouldFail(true);
        
        eventBus.register(listener);
        eventBus.post(new TestEvent("1", "test message"));
        
        // Wait for retries to complete
        Thread.sleep(500);
        
        assertEquals("Should try 3 times (1 + 2 retries)", 3, listener.getProcessCount());
        assertEquals("Should call failure handler once", 1, listener.getFailureCount());
    }
}
