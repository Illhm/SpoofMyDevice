package com.devicespooflab.hooks.hooks

interface HookModule {
    /** Apply hooks with spoof parameters. Returns true if successful. */
    fun hook(classLoader: ClassLoader, params: Map<String, String>): Boolean

    /** Lower = applied first. Default 50. */
    val priority: Int get() = 50

    /** Packages where this hook should be active. */
    val targetPackages: List<String>
}
