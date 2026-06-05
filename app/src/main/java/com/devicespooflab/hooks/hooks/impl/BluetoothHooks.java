package com.devicespooflab.hooks.hooks.impl;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.devicespooflab.hooks.utils.ConfigManager;
import com.devicespooflab.hooks.diagnostics.HookDiagnostics;

/**
 * Hooks BluetoothAdapter to spoof Bluetooth MAC addresses.
 */
public class BluetoothHooks {

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> bluetoothAdapterClass = XposedHelpers.findClassIfExists("android.bluetooth.BluetoothAdapter", lpparam.classLoader);

        if (bluetoothAdapterClass == null) {
            HookDiagnostics.logHookSkipped("BluetoothHooks", lpparam.packageName, "android.bluetooth.BluetoothAdapter class not found");
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(bluetoothAdapterClass, "getAddress", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String mac = ConfigManager.getBluetoothMacAddress();
                    // Fallback to wifi mac if bluetooth mac isn't specifically set but wifi is
                    if (mac == null || mac.isEmpty()) {
                        mac = ConfigManager.getWifiMacAddress();
                    }

                    if (mac != null && !mac.isEmpty()) {
                        param.setResult(mac);
                    }
                }
            });
            HookDiagnostics.logHookSuccess("BluetoothHooks.getAddress", lpparam.packageName, "Hooked");
        } catch (Throwable t) {
            HookDiagnostics.logHookFailed("BluetoothHooks.getAddress", lpparam.packageName, t.getMessage());
        }
    }
}
