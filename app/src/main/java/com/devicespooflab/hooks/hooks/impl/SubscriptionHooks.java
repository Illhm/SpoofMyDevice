package com.devicespooflab.hooks.hooks.impl;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.devicespooflab.hooks.diagnostics.HookDiagnostics;

/**
 * Audit placeholder for SubscriptionManager hooks.
 */
public class SubscriptionHooks {

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        // TelephonyManager is already hooked. SubscriptionManager is planned.
        HookDiagnostics.logHookSkipped("SubscriptionHooks", lpparam.packageName, "Planned: Requires mocking SubscriptionInfo objects (API 22+)");
    }
}
