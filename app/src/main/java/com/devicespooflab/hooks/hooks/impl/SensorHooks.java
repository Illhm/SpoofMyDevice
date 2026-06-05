package com.devicespooflab.hooks.hooks.impl;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import com.devicespooflab.hooks.diagnostics.HookDiagnostics;

/**
 * Audit placeholder for Sensor spoofing.
 */
public class SensorHooks {

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        HookDiagnostics.logHookSkipped("SensorHooks", lpparam.packageName, "Planned: Requires hooking SensorEventListener / SensorManager");
    }
}
