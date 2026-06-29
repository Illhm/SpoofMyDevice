package com.devicespooflab.hooks

import com.devicespooflab.hooks.hooks.*
import com.devicespooflab.hooks.utils.PackageFilter
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {

    private val modules = listOf(
        BuildHook(),
        TelephonyHook(),
        IdentityHook(),
        WifiHook(),
        LocationHook(),
        BluetoothHook(),
        DrmWebViewHook()
    ).sortedBy { it.priority }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "com.spoofmydevice") {
            return
        }

        val params = ParamStore.load()
        if (params.isEmpty()) {
            XposedBridge.log("SpoofMyDevice: No parameters loaded for ${lpparam.packageName}, skipping hooks.")
            return
        }

        for (module in modules) {
            val isTarget = module.targetPackages.isEmpty() ||
                           module.targetPackages.contains(lpparam.packageName) ||
                           (module.targetPackages.contains("TARGET_APPS") && PackageFilter.isTargetApp(lpparam.packageName))

            if (isTarget) {
                try {
                    val success = module.hook(lpparam.classLoader, params)
                    if (!success) {
                        XposedBridge.log("SpoofMyDevice: ${module.javaClass.simpleName} failed in ${lpparam.packageName}")
                    }
                } catch (t: Throwable) {
                    XposedBridge.log("SpoofMyDevice: Error applying ${module.javaClass.simpleName} in ${lpparam.packageName}: ${t.message}")
                }
            }
        }
    }
}
