package com.devicespooflab.hooks

import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge

object ParamStore {
    private const val PREF_NAME = "rsh_params"
    private var params: Map<String, String> = emptyMap()

    // Use XSharedPreferences so the module's own prefs are readable
    // from inside every hooked process.
    fun load() {
        try {
            val prefs = XSharedPreferences("com.spoofmydevice", PREF_NAME)
            prefs.makeWorldReadable()
            val loaded = mutableMapOf<String, String>()
            for ((k, v) in prefs.all) if (v is String) loaded[k] = v
            params = loaded
        } catch (e: Exception) {
            XposedBridge.log("ParamStore: ${e.message}")
        }
    }

    fun getParams(): Map<String, String> {
        return params
    }
}
