package com.devicespooflab.hooks.hooks

import android.telephony.TelephonyManager
import com.devicespooflab.hooks.utils.PackageFilter
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

class TelephonyHook : HookModule {
    override val targetPackages: List<String> = listOf("TARGET_APPS", PackageFilter.PHONE)
    override val priority: Int = 20

    override fun hook(classLoader: ClassLoader, params: Map<String, String>): Boolean {
        hookTelephonyMethod(classLoader, "getSimOperator", params["sim_operator"])
        hookTelephonyMethod(classLoader, "getSimOperatorName", params["sim_operator_name"])
        hookTelephonyMethod(classLoader, "getNetworkOperator", params["sim_operator"])
        hookTelephonyMethod(classLoader, "getNetworkOperatorName", params["sim_operator_name"])
        hookTelephonyMethod(classLoader, "getSimCountryIso", params["sim_country_iso"])
        hookTelephonyMethod(classLoader, "getNetworkCountryIso", params["sim_country_iso"])
        hookTelephonyMethod(classLoader, "getSimSerialNumber", params["sim_serial_number"])
        hookTelephonyMethod(classLoader, "getLine1Number", params["line_number"])
        hookTelephonyMethod(classLoader, "getSubscriberId", params["subscriber_id"])

        // IMEI & DeviceID hooks with SlotIndex logic
        try {
            XposedHelpers.findAndHookMethod(
                TelephonyManager::class.java.name,
                classLoader,
                "getDeviceId",
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val slot = param.args[0] as Int
                        param.result = if (slot == 0) params["imei_1"] else params["imei_2"]
                    }
                }
            )

            XposedHelpers.findAndHookMethod(
                TelephonyManager::class.java.name,
                classLoader,
                "getImei",
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val slot = param.args[0] as Int
                        param.result = if (slot == 0) params["imei_1"] else params["imei_2"]
                    }
                }
            )

            // Parameterless ones default to slot 0 (IMEI 1)
            XposedHelpers.findAndHookMethod(
                TelephonyManager::class.java.name,
                classLoader,
                "getDeviceId",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = params["imei_1"]
                    }
                }
            )

            XposedHelpers.findAndHookMethod(
                TelephonyManager::class.java.name,
                classLoader,
                "getImei",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = params["imei_1"]
                    }
                }
            )
        } catch (ignored: Throwable) {}

        return true
    }

    private fun hookTelephonyMethod(classLoader: ClassLoader, methodName: String, result: String?) {
        if (result == null) return
        try {
            XposedHelpers.findAndHookMethod(
                TelephonyManager::class.java.name,
                classLoader,
                methodName,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = result
                    }
                }
            )
        } catch (ignored: Throwable) {}

        try {
             XposedHelpers.findAndHookMethod(
                TelephonyManager::class.java.name,
                classLoader,
                methodName,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = result
                    }
                }
            )
        } catch (ignored: Throwable) {}
    }
}
