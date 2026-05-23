package com.devicespooflab.hooks.hooks;

import android.os.Build;

import de.robv.android.xposed.XposedBridge;

final class HookSelfCheck {
    static void logBasicConsistency(String packageName) {
        try {
            HookValueResolver r = HookValueResolver.forPackage(packageName);
            String fpProp = r.resolveSystemProperty("ro.build.fingerprint", null);
            if (fpProp != null && !fpProp.equals(Build.FINGERPRINT)) {
                XposedBridge.log("DeviceSpoofLab-SelfCheck: fingerprint mismatch build/systemprop");
            }
            String rel = r.resolveSystemProperty("ro.build.version.release", null);
            if (rel != null && !rel.equals(Build.VERSION.RELEASE)) {
                XposedBridge.log("DeviceSpoofLab-SelfCheck: release mismatch build/systemprop");
            }
        } catch (Throwable ignored) {}
    }
}
