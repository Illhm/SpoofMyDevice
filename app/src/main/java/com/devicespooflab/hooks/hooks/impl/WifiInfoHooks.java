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

        try {
            XposedHelpers.findAndHookMethod(wifiInfoClass, "getSSID", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String spoofedWifiChip = ConfigManager.getOptionalConfigValue(ConfigManager.FIELD_WIFI_CHIP);
                    if (spoofedWifiChip != null && !spoofedWifiChip.isEmpty()) {
                        param.setResult("\"" + spoofedWifiChip + "_Network\"");
                    }
                }
            });
            HookDiagnostics.logHookSuccess("WifiInfoHooks.getSSID", lpparam.packageName, "Hooked");
        } catch (Throwable t) {
            HookDiagnostics.logHookFailed("WifiInfoHooks.getSSID", lpparam.packageName, t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(wifiInfoClass, "getBSSID", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String mac = ConfigManager.getWifiMacAddress();
                    if (mac != null && !mac.isEmpty()) {
                        // slightly alter MAC for BSSID
                        param.setResult(mac.substring(0, 16) + "1");
                    }
                }
            });
            HookDiagnostics.logHookSuccess("WifiInfoHooks.getBSSID", lpparam.packageName, "Hooked");
        } catch (Throwable t) {
            HookDiagnostics.logHookFailed("WifiInfoHooks.getBSSID", lpparam.packageName, t.getMessage());
        }
    }
}
