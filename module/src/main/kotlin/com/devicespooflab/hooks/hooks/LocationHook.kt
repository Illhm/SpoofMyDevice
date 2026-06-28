package com.devicespooflab.hooks.hooks

import android.location.Location
import com.devicespooflab.hooks.utils.PackageFilter
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

class LocationHook : HookModule {
    override val targetPackages: List<String> = listOf("TARGET_APPS")
    override val priority: Int = 50

    override fun hook(classLoader: ClassLoader, params: Map<String, String>): Boolean {
        hookLocationMethod(classLoader, "getLatitude", params["geo_latitude"]?.toDoubleOrNull())
        hookLocationMethod(classLoader, "getLongitude", params["geo_longitude"]?.toDoubleOrNull())
        hookLocationMethod(classLoader, "getAltitude", params["geo_altitude"]?.toDoubleOrNull())
        hookLocationMethod(classLoader, "getAccuracy", params["geo_accuracy"]?.toFloatOrNull())
        hookLocationMethod(classLoader, "getSpeed", params["geo_speed"]?.toFloatOrNull())
        hookLocationMethod(classLoader, "getBearing", params["geo_bearing"]?.toFloatOrNull())

        return true
    }

    private fun hookLocationMethod(classLoader: ClassLoader, methodName: String, result: Any?) {
        if (result == null) return
        try {
            XposedHelpers.findAndHookMethod(
                Location::class.java.name,
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
