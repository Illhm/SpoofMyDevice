package com.devicespooflab.hooks.hooks

import android.os.Build
import com.devicespooflab.hooks.utils.PackageFilter
import com.devicespooflab.hooks.utils.ReflectionHelper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

class BuildHook : HookModule {
    override val targetPackages: List<String> = listOf("SYSTEM_SERVER", "TARGET_APPS", PackageFilter.SYSTEM_SERVER)
    override val priority: Int = 10

    override fun hook(classLoader: ClassLoader, params: Map<String, String>): Boolean {
        // Build Fields
        ReflectionHelper.setStaticObjectFieldSafe(Build::class.java, "BRAND", params["phone_brand"])
        ReflectionHelper.setStaticObjectFieldSafe(Build::class.java, "MODEL", params["phone_model"])
        ReflectionHelper.setStaticObjectFieldSafe(Build::class.java, "MANUFACTURER", params["phone_manufacturer"])
        ReflectionHelper.setStaticObjectFieldSafe(Build::class.java, "DEVICE", params["phone_device"])
        ReflectionHelper.setStaticObjectFieldSafe(Build::class.java, "BOARD", params["phone_board"])
        ReflectionHelper.setStaticObjectFieldSafe(Build::class.java, "HARDWARE", params["phone_hardware"])
        ReflectionHelper.setStaticObjectFieldSafe(Build::class.java, "PRODUCT", params["phone_name"])
        ReflectionHelper.setStaticObjectFieldSafe(Build::class.java, "DISPLAY", params["phone_display"])
        ReflectionHelper.setStaticObjectFieldSafe(Build::class.java, "ID", params["phone_id"])
        ReflectionHelper.setStaticObjectFieldSafe(Build::class.java, "TAGS", params["phone_tags"])
        ReflectionHelper.setStaticObjectFieldSafe(Build::class.java, "HOST", params["phone_host"])
        ReflectionHelper.setStaticObjectFieldSafe(Build::class.java, "USER", params["phone_user"])
        ReflectionHelper.setStaticObjectFieldSafe(Build::class.java, "TYPE", params["phone_type"])
        ReflectionHelper.setStaticObjectFieldSafe(Build::class.java, "FINGERPRINT", params["phone_fingerprint"])

        // Time
        params["phone_build_date"]?.toLongOrNull()?.let { timeMs ->
            try {
                val timeField = XposedHelpers.findFieldIfExists(Build::class.java, "TIME")
                if (timeField != null) {
                    timeField.isAccessible = true
                    timeField.setLong(null, timeMs)
                }
            } catch (ignored: Throwable) {}
        }

        // Build.VERSION Fields
        ReflectionHelper.setStaticObjectFieldSafe(Build.VERSION::class.java, "RELEASE", params["phone_version_release"])
        ReflectionHelper.setStaticObjectFieldSafe(Build.VERSION::class.java, "INCREMENTAL", params["phone_incremental"])

        // SystemProperties Hook (for baseband and patch, occasionally build fields)
        try {
            XposedHelpers.findAndHookMethod(
                "android.os.SystemProperties",
                classLoader,
                "get",
                String::class.java,
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as String
                        when (key) {
                            "gsm.version.baseband" -> param.result = params["phone_baseband"]
                            "ro.build.version.security_patch" -> param.result = params["phone_patch"]
                        }
                    }
                }
            )

            XposedHelpers.findAndHookMethod(
                "android.os.SystemProperties",
                classLoader,
                "get",
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as String
                        when (key) {
                            "gsm.version.baseband" -> param.result = params["phone_baseband"]
                            "ro.build.version.security_patch" -> param.result = params["phone_patch"]
                        }
                    }
                }
            )
        } catch (ignored: Throwable) {}

        return true
    }
}
