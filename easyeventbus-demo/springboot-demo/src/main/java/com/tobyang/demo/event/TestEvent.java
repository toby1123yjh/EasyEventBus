package com.tobyang.demo.event;

/**
 * 简单测试事件
 */
public class TestEvent {
    private final String id;
    private final String message;
    private final boolean shouldFail;

    public TestEvent(String id, String message) {
        this(id, message, false);
    }

    public TestEvent(String id, String message, boolean shouldFail) {
        this.id = id;
        this.message = message;
        this.shouldFail = shouldFail;
    }

    public String getId() { return id; }
    public String getMessage() { return message; }
    public boolean shouldFail() { return shouldFail; }

    @Override
    public String toString() {
        return "TestEvent{id='" + id + "', message='" + message + "', shouldFail=" + shouldFail + '}';
    }
}
