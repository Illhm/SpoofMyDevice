package com.devicespooflab.hooks

import android.content.Context
import android.content.SharedPreferences
import de.robv.android.xposed.XposedBridge

object ParamStore {
    private const val PREF_NAME = "rsh_params"
    private var params: Map<String, String> = emptyMap()

    fun load(context: Context) {
        try {
            val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val allEntries = prefs.all
            val loadedParams = mutableMapOf<String, String>()

            for ((key, value) in allEntries) {
                if (value is String) {
                    loadedParams[key] = value
                }
            }
            params = loadedParams
            XposedBridge.log("ParamStore: Loaded ${params.size} parameters from SharedPreferences")
        } catch (e: Exception) {
            XposedBridge.log("ParamStore: Failed to load parameters: ${e.message}")
        }
    }

    fun getParams(): Map<String, String> {
        return params
    }
}
