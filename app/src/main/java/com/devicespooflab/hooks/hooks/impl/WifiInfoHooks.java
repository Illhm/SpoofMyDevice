package com.devicespooflab.hooks.hooks.impl;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.devicespooflab.hooks.utils.ConfigManager;
import com.devicespooflab.hooks.diagnostics.HookDiagnostics;

/**
 * Hooks WifiInfo to spoof MAC address, SSID, and BSSID.
 */
public class WifiInfoHooks {

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> wifiInfoClass = XposedHelpers.findClassIfExists("android.net.wifi.WifiInfo", lpparam.classLoader);
        if (wifiInfoClass == null) {
            HookDiagnostics.logHookSkipped("WifiInfoHooks", lpparam.packageName, "android.net.wifi.WifiInfo class not found");
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(wifiInfoClass, "getMacAddress", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String mac = ConfigManager.getWifiMacAddress();
                    if (mac != null && !mac.isEmpty()) {
                        param.setResult(mac);
                    }
                }
            });
            HookDiagnostics.logHookSuccess("WifiInfoHooks.getMacAddress", lpparam.packageName, "Hooked");
        } catch (Throwable t) {
            HookDiagnostics.logHookFailed("WifiInfoHooks.getMacAddress", lpparam.packageName, t.getMessage());
        }

        // SSID and BSSID planned
        HookDiagnostics.logHookSkipped("WifiInfoHooks.getSSID", lpparam.packageName, "Planned: Requires SSID configuration mapping");
    }
}
