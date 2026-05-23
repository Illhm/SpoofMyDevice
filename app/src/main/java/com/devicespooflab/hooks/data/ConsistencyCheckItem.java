package com.devicespooflab.hooks.data;

public class ConsistencyCheckItem {
    private final String key;
    private final String expected;
    private final String actual;
    private final ConsistencyStatus status;

    public ConsistencyCheckItem(String key, String expected, String actual, ConsistencyStatus status) {
        this.key = key;
        this.expected = expected;
        this.actual = actual;
        this.status = status;
    }

    public String getKey() { return key; }
    public String getExpected() { return expected; }
    public String getActual() { return actual; }
    public ConsistencyStatus getStatus() { return status; }
}
