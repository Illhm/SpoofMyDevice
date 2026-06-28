package com.devicespooflab.hooks.hooks

import android.telephony.TelephonyManager
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

class TelephonyHook : HookModule {
    override val priority = 10
    override val targetPackages = listOf("all")

    override fun hook(classLoader: ClassLoader, params: Map<String, String>): Boolean {
        // Telephony (8 param) — Prioritas P0
        val hookMethod = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val methodName = param.method.name
                val slot = if (param.args.isNotEmpty() && param.args[0] is Int) param.args[0] as Int else 0

                when (methodName) {
                    "getSimOperator" -> params["sim_operator"]?.let { param.result = it }
                    "getSimOperatorName" -> params["sim_operator_name"]?.let { param.result = it }
                    "getSimCountryIso" -> params["sim_country_iso"]?.let { param.result = it }
                    "getSimSerialNumber" -> params["sim_serial_number"]?.let { param.result = it }
                    "getLine1Number" -> params["line_number"]?.let { param.result = it }
                    "getSubscriberId" -> params["subscriber_id"]?.let { param.result = it }
                    "getImei", "getDeviceId" -> {
                        val imei = if (slot == 0) params["imei_1"] else params["imei_2"]
                        if (imei != null) param.result = imei
                    }
                }
            }
        }

        try {
            val telephonyClass = TelephonyManager::class.java

            // getSimOperator
            XposedHelpers.findAndHookMethod(telephonyClass, "getSimOperator", hookMethod)
            XposedHelpers.findAndHookMethod(telephonyClass, "getSimOperator", Int::class.javaPrimitiveType, hookMethod)

            // getSimOperatorName
            XposedHelpers.findAndHookMethod(telephonyClass, "getSimOperatorName", hookMethod)
            XposedHelpers.findAndHookMethod(telephonyClass, "getSimOperatorName", Int::class.javaPrimitiveType, hookMethod)

            // getSimCountryIso
            XposedHelpers.findAndHookMethod(telephonyClass, "getSimCountryIso", hookMethod)
            XposedHelpers.findAndHookMethod(telephonyClass, "getSimCountryIso", Int::class.javaPrimitiveType, hookMethod)

            // getSimSerialNumber
            XposedHelpers.findAndHookMethod(telephonyClass, "getSimSerialNumber", hookMethod)
            XposedHelpers.findAndHookMethod(telephonyClass, "getSimSerialNumber", Int::class.javaPrimitiveType, hookMethod)

            // getLine1Number
            XposedHelpers.findAndHookMethod(telephonyClass, "getLine1Number", hookMethod)
            XposedHelpers.findAndHookMethod(telephonyClass, "getLine1Number", Int::class.javaPrimitiveType, hookMethod)

            // getSubscriberId
            XposedHelpers.findAndHookMethod(telephonyClass, "getSubscriberId", hookMethod)
            XposedHelpers.findAndHookMethod(telephonyClass, "getSubscriberId", Int::class.javaPrimitiveType, hookMethod)

            // getImei & getDeviceId
            XposedHelpers.findAndHookMethod(telephonyClass, "getImei", hookMethod)
            XposedHelpers.findAndHookMethod(telephonyClass, "getImei", Int::class.javaPrimitiveType, hookMethod)
            XposedHelpers.findAndHookMethod(telephonyClass, "getDeviceId", hookMethod)
            XposedHelpers.findAndHookMethod(telephonyClass, "getDeviceId", Int::class.javaPrimitiveType, hookMethod)

        } catch (e: Exception) {
            // Ignore missing methods on older/newer Android versions
        }
        return true
    }
}
