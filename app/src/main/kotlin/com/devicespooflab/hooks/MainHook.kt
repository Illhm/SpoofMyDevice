package com.devicespooflab.hooks

import android.content.Context
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.devicespooflab.hooks.hooks.*
import com.devicespooflab.hooks.utils.PackageFilter

class MainHook : IXposedHookLoadPackage {

    private val hookModules: List<HookModule> = listOf(
        BuildHook(),
        TelephonyHook(),
        IdentityHook(),
        WifiHook(),
        LocationHook(),
        BluetoothHook(),
        DrmWebViewHook()
    ).sortedBy { it.priority }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val packageName = lpparam.packageName

        // In Xposed/Zygisk, we might need a context to load SharedPreferences.
        // Usually hooked on Application.onCreate or attachBaseContext
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.Application",
                lpparam.classLoader,
                "attach",
                Context::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val context = param.args[0] as Context

                        // Load params from SharedPreferences
                        ParamStore.load(context)
                        val params = ParamStore.getParams()

                        // Apply relevant hooks for this package
                        if (params.isNotEmpty()) {
                            for (module in hookModules) {
                                val isTarget = module.targetPackages.isEmpty() ||
                                               module.targetPackages.contains(packageName) ||
                                               module.targetPackages.contains("all") ||
                                               (module.targetPackages.contains("system_server") && PackageFilter.isSystemProcess(packageName)) ||
                                               (module.targetPackages.contains("phone") && PackageFilter.isPhoneProcess(packageName))

                                if (isTarget) {
                                    try {
                                        val success = module.hook(lpparam.classLoader, params)
                                        if (success) {
                                            XposedBridge.log("MainHook: Successfully applied ${module.javaClass.simpleName} to $packageName")
                                        }
                                    } catch (e: Exception) {
                                        XposedBridge.log("MainHook: Failed to apply ${module.javaClass.simpleName} to $packageName: ${e.message}")
                                    }
                                }
                            }
                        }
                    }
                }
            )
        } catch (e: Exception) {
            XposedBridge.log("MainHook: Failed to hook Application.attach for $packageName: ${e.message}")
        }
    }
}
