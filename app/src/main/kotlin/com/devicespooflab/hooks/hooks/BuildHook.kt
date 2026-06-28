package com.devicespooflab.hooks.hooks

import android.os.Build
import com.devicespooflab.hooks.utils.ReflectionHelper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

class BuildHook : HookModule {
    override val priority = 10
    override val targetPackages = listOf("all")

    override fun hook(classLoader: ClassLoader, params: Map<String, String>): Boolean {
        // Build Info (20 param) — Prioritas P0
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

        ReflectionHelper.setStaticObjectFieldSafe(Build.VERSION::class.java, "RELEASE", params["phone_version_release"])
        ReflectionHelper.setStaticObjectFieldSafe(Build.VERSION::class.java, "INCREMENTAL", params["phone_incremental"])

        // Build.TIME (Long)
        params["phone_build_date_utc"]?.toLongOrNull()?.let {
            ReflectionHelper.setStaticObjectFieldSafe(Build::class.java, "TIME", it)
        }

        // SystemProperties Hooking (baseband, patch)
        try {
            val systemPropertiesClass = XposedHelpers.findClass("android.os.SystemProperties", classLoader)
            XposedHelpers.findAndHookMethod(
                systemPropertiesClass, "get", String::class.java, String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as String
                        if (key == "gsm.version.baseband") {
                            params["phone_baseband"]?.let { param.result = it }
                        } else if (key == "ro.build.version.security_patch") {
                            params["phone_patch"]?.let { param.result = it }
                        }
                    }
                }
            )
            XposedHelpers.findAndHookMethod(
                systemPropertiesClass, "get", String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as String
                        if (key == "gsm.version.baseband") {
                            params["phone_baseband"]?.let { param.result = it }
                        } else if (key == "ro.build.version.security_patch") {
                            params["phone_patch"]?.let { param.result = it }
                        }
                    }
                }
            )
        } catch (e: Exception) {
            // Ignore if class not found or other errors
        }

        return true
    }
}
