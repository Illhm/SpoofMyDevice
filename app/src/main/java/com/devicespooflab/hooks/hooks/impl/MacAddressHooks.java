package com.devicespooflab.hooks.hooks.impl;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.devicespooflab.hooks.utils.ConfigManager;
import com.devicespooflab.hooks.diagnostics.HookDiagnostics;

import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Hooks NetworkInterface to spoof Hardware (MAC) addresses.
 */
public class MacAddressHooks {

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(NetworkInterface.class, "getHardwareAddress", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    NetworkInterface ni = (NetworkInterface) param.thisObject;
                    if (ni != null && ni.getName() != null && ni.getName().startsWith("wlan")) {
                        String macStr = ConfigManager.getWifiMacAddress();
                        if (macStr != null && !macStr.isEmpty()) {
                            byte[] macBytes = parseMacAddress(macStr);
                            if (macBytes != null) {
                                param.setResult(macBytes);
                            }
                        }
                    }
                }
            });
            HookDiagnostics.logHookSuccess("MacAddressHooks.getHardwareAddress", lpparam.packageName, "Hooked NetworkInterface");
        } catch (Throwable t) {
            HookDiagnostics.logHookFailed("MacAddressHooks.getHardwareAddress", lpparam.packageName, t.getMessage());
        }
    }

    private static byte[] parseMacAddress(String macAddress) {
        String[] parts = macAddress.split(":");
        if (parts.length != 6) {
            return null;
        }
        byte[] bytes = new byte[6];
        try {
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) Integer.parseInt(parts[i], 16);
            }
            return bytes;
        } catch (Exception e) {
            return null;
        }
    }
}
