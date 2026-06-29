package com.devicespooflab.hooks.hooks

import android.bluetooth.BluetoothAdapter
import com.devicespooflab.hooks.utils.PackageFilter
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

class BluetoothHook : HookModule {
    override val targetPackages: List<String> = listOf("TARGET_APPS")
    override val priority: Int = 60

    override fun hook(classLoader: ClassLoader, params: Map<String, String>): Boolean {
        params["bluetooth_mac"]?.let { mac ->
            try {
                XposedHelpers.findAndHookMethod(
                    BluetoothAdapter::class.java.name,
                    classLoader,
                    "getAddress",
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            param.result = mac
                        }
                    }
                )
            } catch (ignored: Throwable) {}
        }
        return true
    }
}
