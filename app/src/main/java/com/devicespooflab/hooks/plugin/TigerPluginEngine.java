package com.devicespooflab.hooks.plugin;

/**
 * Placeholder interface for future Rust/Wasmtime plugin support.
 * As documented in the Tiger plugin dev guide, this provides an integration path
 * for native code and WebAssembly plugins inside the Java environment.
 */
public interface TigerPluginEngine {

    /**
     * Initializes the plugin engine.
     */
    void initialize();

    /**
     * Loads a WASM plugin from the given path.
     * @param pluginPath Absolute path to the plugin.
     * @return true if successfully loaded, false otherwise.
     */
    boolean loadPlugin(String pluginPath);

    /**
     * Invokes a function inside a loaded plugin.
     * @param pluginName Name of the plugin.
     * @param functionName Function to execute.
     * @param args Arguments to pass.
     * @return Result of the function, or null.
     */
    Object invokePluginFunction(String pluginName, String functionName, Object[] args);
}
