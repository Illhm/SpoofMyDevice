package com.devicespooflab.hooks.hooks

import android.net.wifi.WifiInfo
import com.devicespooflab.hooks.utils.PackageFilter
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

class WifiHook : HookModule {
    override val targetPackages: List<String> = listOf("TARGET_APPS")
    override val priority: Int = 40

    override fun hook(classLoader: ClassLoader, params: Map<String, String>): Boolean {
        params["ssid"]?.let { ssid ->
            hookWifiMethod(classLoader, "getSSID", "\"$ssid\"")
        }
        params["bssid"]?.let { bssid ->
            hookWifiMethod(classLoader, "getBSSID", bssid)
        }
        params["wifi_mac"]?.let { mac ->
            hookWifiMethod(classLoader, "getMacAddress", mac)
        }
        params["wifi_state"]?.let { state ->
            try {
                XposedHelpers.findAndHookMethod(
                    "android.net.wifi.WifiManager",
                    classLoader,
                    "getWifiState",
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            param.result = state.toIntOrNull() ?: 3
                        }
                    }
                )
            } catch (ignored: Throwable) {}
        }

        return true
    }

    private fun hookWifiMethod(classLoader: ClassLoader, methodName: String, result: String) {
        try {
            XposedHelpers.findAndHookMethod(
                WifiInfo::class.java.name,
                classLoader,
                methodName,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = result
                    }
                }
            )
        } catch (ignored: Throwable) {}
    }
}
