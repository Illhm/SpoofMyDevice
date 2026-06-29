package com.devicespooflab.hooks.hooks

import android.content.ContentResolver
import android.os.Build
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

class IdentityHook : HookModule {
    override val priority = 10
    override val targetPackages = listOf("all")

    override fun hook(classLoader: ClassLoader, params: Map<String, String>): Boolean {
        // Identity (5 param) — Prioritas P0

        // 1. Android ID (Settings.Secure)
        try {
            val secureClass = XposedHelpers.findClass("android.provider.Settings\$Secure", classLoader)
            XposedHelpers.findAndHookMethod(
                secureClass, "getString", ContentResolver::class.java, String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val key = param.args[1] as String
                        if (key == "android_id") {
                            params["android_id"]?.let { param.result = it }
                        }
                    }
                }
            )
        } catch (e: Exception) {}

        // 2. Build.getSerial()
        try {
            XposedHelpers.findAndHookMethod(
                Build::class.java, "getSerial",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        params["phone_serial"]?.let { param.result = it }
                    }
                }
            )
        } catch (e: Exception) {}

        // 3. GSF ID (GoogleServicesFramework)
        // Hook is applied in the target app requesting it (via content provider or specific API).
        // Since it's usually requested via ContentResolver, we hook Cursor or the generic query if needed.
        // For simplicity as requested in plan, we add a placeholder or direct hook if applicable class exists.
        try {
            val partnerClass = XposedHelpers.findClassIfExists("com.google.android.gsf.GoogleSettingsContract\$Partner", classLoader)
            if (partnerClass != null) {
                XposedHelpers.findAndHookMethod(
                    partnerClass, "getString", ContentResolver::class.java, String::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val key = param.args[1] as String
                            if (key == "android_id" || key.contains("gsf")) {
                                params["gsf"]?.let { param.result = it }
                            }
                        }
                    }
                )
            }
        } catch (e: Exception) {}

        // 4. Advertising ID
        try {
            val adInfoClass = XposedHelpers.findClassIfExists("com.google.android.gms.ads.identifier.AdvertisingIdClient\$Info", classLoader)
            if (adInfoClass != null) {
                XposedHelpers.findAndHookMethod(
                    adInfoClass, "getId",
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            params["ads_id"]?.let { param.result = it }
                        }
                    }
                )
            }
        } catch (e: Exception) {}

        return true
    }
}
