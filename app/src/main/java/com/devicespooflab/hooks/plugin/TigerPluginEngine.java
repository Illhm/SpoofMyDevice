package com.devicespooflab.hooks.plugin;

/**
 * Placeholder interface for future Rust/Wasmtime plugin support.
 * As documented in the Tiger plugin dev guide, this provides an integration path
 * for native code and WebAssembly plugins inside the Java environment.
 *
 * SECURITY RESTRICTIONS (WASM SANDBOX GUARANTEES):
 * 1. NO Filesystem access: Plugins receive inputs strictly via memory buffers.
 * 2. NO Network requests: Plugins cannot open sockets.
 * 3. NO Cross-plugin memory: Wasmtime enforces linear memory isolation.
 * 4. NO JNI calls: Sandboxed execution prevents direct OS syscalls.
 * 5. CPU/Memory limits: Enforced by the engine wrapper (e.g. execution timeouts).
 */
public interface TigerPluginEngine {

    /**
     * Initializes the plugin engine with strict sandbox limits.
     * @param maxMemoryBytes Maximum linear memory allowed per plugin.
     * @param maxExecutionMs Maximum CPU time allowed per invocation.
     */
    void initializeSandbox(long maxMemoryBytes, long maxExecutionMs);

    /**
     * Loads a WASM plugin into an isolated linear memory instance.
     * @param pluginId Unique identifier for the plugin.
     * @param wasmBinary Raw byte array of the plugin to prevent filesystem access.
     * @return true if successfully loaded and verified, false otherwise.
     */
    boolean loadIsolatedPlugin(String pluginId, byte[] wasmBinary);

    /**
     * Invokes a function inside a loaded plugin with a strict timeout.
     * Inputs and outputs must be primitives or simple byte arrays to prevent JNI escape.
     *
     * @param pluginId Unique identifier of the isolated plugin.
     * @param functionName Function to execute.
     * @param inputBuffer Read-only input data.
     * @return Processed output data, or throws SandboxedExecutionException on timeout/violation.
     */
    byte[] invokePluginFunction(String pluginId, String functionName, byte[] inputBuffer) throws SandboxedExecutionException;
}
