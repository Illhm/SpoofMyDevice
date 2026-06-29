package com.devicespooflab.hooks.hooks

import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

class WifiHook : HookModule {
    override val priority = 20
    override val targetPackages = listOf("all")

    override fun hook(classLoader: ClassLoader, params: Map<String, String>): Boolean {
        // WiFi (4 param) — Prioritas P1

        try {
            val wifiInfoClass = WifiInfo::class.java

            // 1. MAC Address
            XposedHelpers.findAndHookMethod(
                wifiInfoClass, "getMacAddress",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        params["wifi_mac"]?.let { param.result = it }
                    }
                }
            )

            // 2. SSID
            XposedHelpers.findAndHookMethod(
                wifiInfoClass, "getSSID",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        params["ssid"]?.let { param.result = "\"$it\"" }
                    }
                }
            )

            // 3. BSSID
            XposedHelpers.findAndHookMethod(
                wifiInfoClass, "getBSSID",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        params["bssid"]?.let { param.result = it }
                    }
                }
            )

        } catch (e: Exception) {}

        try {
            val wifiManagerClass = WifiManager::class.java

            // 4. WiFi State
            XposedHelpers.findAndHookMethod(
                wifiManagerClass, "getWifiState",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        params["wifi_state"]?.toIntOrNull()?.let { param.result = it }
                    }
                }
            )
        } catch (e: Exception) {}

        return true
    }
}
