package com.devicespooflab.hooks

import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge

object ParamStore {
    private const val PREFS_FILE = "rsh_params"
    private const val PACKAGE_NAME = "com.spoofmydevice"

    private var prefs: XSharedPreferences? = null
    private var cachedParams: Map<String, String>? = null

    fun load(): Map<String, String> {
        if (cachedParams != null) {
            return cachedParams!!
        }

        try {
            prefs = XSharedPreferences(PACKAGE_NAME, PREFS_FILE)
            prefs?.makeWorldReadable()
            prefs?.reload()

            val params = mutableMapOf<String, String>()
            prefs?.all?.forEach { (key, value) ->
                params[key] = value.toString()
            }

            if (params.isEmpty()) {
                XposedBridge.log("SpoofMyDevice: ParamStore is empty! Have you pressed 'Random All' in the app?")
            } else {
                XposedBridge.log("SpoofMyDevice: Loaded ${params.size} parameters from SharedPreferences.")
            }

            cachedParams = params
            return params
        } catch (e: Exception) {
            XposedBridge.log("SpoofMyDevice: Failed to load SharedPreferences. Error: ${e.message}")
            return emptyMap()
        }
    }

    fun get(key: String, default: String = ""): String {
        return load()[key] ?: default
    }
}
