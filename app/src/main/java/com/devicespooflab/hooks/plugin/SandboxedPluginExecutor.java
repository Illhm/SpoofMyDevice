package com.devicespooflab.hooks.plugin;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Ensures plugin invocations adhere to CPU constraints and API boundaries.
 * In a real Rust/Wasmtime implementation, CPU fuel/limits would be handled natively,
 * but this Java wrapper acts as an additional defense-in-depth layer.
 */
public class SandboxedPluginExecutor {

    private final ExecutorService executorService;
    private final long maxExecutionTimeMs;

    public SandboxedPluginExecutor(long maxExecutionTimeMs) {
        // Use a dedicated thread to prevent blocking the main Xposed routing thread
        this.executorService = Executors.newSingleThreadExecutor();
        this.maxExecutionTimeMs = maxExecutionTimeMs;
    }

    /**
     * Executes a plugin task within the predefined time limit.
     */
    public byte[] executeWithLimits(Callable<byte[]> pluginTask) throws SandboxedExecutionException {
        Future<byte[]> future = executorService.submit(pluginTask);

        try {
            // CPU Limit Enforcement: Task must complete within maxExecutionTimeMs
            return future.get(maxExecutionTimeMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true); // Attempt to interrupt the rogue thread
            throw new SandboxedExecutionException("Plugin exceeded CPU execution limit of " + maxExecutionTimeMs + "ms");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SandboxedExecutionException("Plugin execution was interrupted");
        } catch (ExecutionException e) {
            throw new SandboxedExecutionException("Plugin crashed during execution", e.getCause());
        }
    }

    public void shutdown() {
        executorService.shutdownNow();
    }
}
