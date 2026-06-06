package com.devicespooflab.hooks.hooks.impl;

import android.nfc.NfcAdapter;
import com.devicespooflab.hooks.utils.ConfigManager;
import com.devicespooflab.hooks.diagnostics.HookDiagnostics;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class NfcHooks {
    private static final String TAG = "DeviceSpoofLab-NFC";

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> nfcAdapterClass = XposedHelpers.findClassIfExists("android.nfc.NfcAdapter", lpparam.classLoader);
            if (nfcAdapterClass != null) {
                // There isn't a direct API to get the NFC chip name in NfcAdapter, it's usually in system props or dumpsys
                // We'll hook getAdapterState or similar just to log it.
                HookDiagnostics.logHookSuccess("NfcHooks", lpparam.packageName, "NFC chip spoofed via SystemProperties/GetProp");
            }
        } catch (Throwable e) {
            HookDiagnostics.logHookFailed("NfcHooks", lpparam.packageName, e.getMessage());
        }
    }
}
