package com.devicespooflab.hooks.plugin;

/**
 * Exception thrown when a plugin violates sandbox constraints
 * (e.g., CPU timeout, memory limit exceeded, or illegal operation attempt).
 */
public class SandboxedExecutionException extends Exception {
    public SandboxedExecutionException(String message) {
        super(message);
    }

    public SandboxedExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
