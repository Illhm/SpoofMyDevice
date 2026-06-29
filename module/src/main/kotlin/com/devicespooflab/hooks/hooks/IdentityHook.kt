package com.devicespooflab.hooks.hooks

import android.content.ContentResolver
import com.devicespooflab.hooks.utils.PackageFilter
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

class IdentityHook : HookModule {
    override val targetPackages: List<String> = listOf("SYSTEM_SERVER", "TARGET_APPS", PackageFilter.SYSTEM_SERVER)
    override val priority: Int = 30

    override fun hook(classLoader: ClassLoader, params: Map<String, String>): Boolean {
        // ANDROID_ID
        params["android_id"]?.let { androidId ->
            try {
                XposedHelpers.findAndHookMethod(
                    "android.provider.Settings\$Secure",
                    classLoader,
                    "getString",
                    ContentResolver::class.java,
                    String::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val key = param.args[1] as String
                            if (key == "android_id") {
                                param.result = androidId
                            }
                        }
                    }
                )

                XposedHelpers.findAndHookMethod(
                    "android.provider.Settings\$Secure",
                    classLoader,
                    "getStringForUser",
                    ContentResolver::class.java,
                    String::class.java,
                    Int::class.javaPrimitiveType,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val key = param.args[1] as String
                            if (key == "android_id") {
                                param.result = androidId
                            }
                        }
                    }
                )
            } catch (ignored: Throwable) {}
        }

        // Serial Number
        params["phone_serial"]?.let { serial ->
            try {
                XposedHelpers.findAndHookMethod(
                    "android.os.Build",
                    classLoader,
                    "getSerial",
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            param.result = serial
                        }
                    }
                )
            } catch (ignored: Throwable) {}
        }

        // GSF ID
        params["gsf"]?.let { gsf ->
            try {
                XposedHelpers.findAndHookMethod(
                    "com.google.android.gsf.GoogleSettingsContract\$Partner",
                    classLoader,
                    "getString",
                    ContentResolver::class.java,
                    String::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val key = param.args[1] as String
                            if (key == "android_id") {
                                param.result = gsf
                            }
                        }
                    }
                )
            } catch (ignored: Throwable) {}
        }

        // Advertising ID
        params["ads_id"]?.let { adsId ->
            try {
                val adInfoClass = XposedHelpers.findClassIfExists("com.google.android.gms.ads.identifier.AdvertisingIdClient\$Info", classLoader)
                if (adInfoClass != null) {
                    XposedHelpers.findAndHookMethod(
                        adInfoClass,
                        "getId",
                        object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                param.result = adsId
                            }
                        }
                    )
                }
            } catch (ignored: Throwable) {}
        }

        return true
    }
}
