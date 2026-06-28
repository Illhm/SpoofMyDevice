package com.devicespooflab.hooks.hooks

import android.bluetooth.BluetoothAdapter
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

class BluetoothHook : HookModule {
    override val priority = 30
    override val targetPackages = listOf("all")

    override fun hook(classLoader: ClassLoader, params: Map<String, String>): Boolean {
        // Bluetooth (1 param) — Prioritas P2

        try {
            val bluetoothAdapterClass = BluetoothAdapter::class.java

            XposedHelpers.findAndHookMethod(
                bluetoothAdapterClass, "getAddress",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        params["bluetooth_mac"]?.let { param.result = it }
                    }
                }
            )
        } catch (e: Exception) {}

        return true
    }
}
