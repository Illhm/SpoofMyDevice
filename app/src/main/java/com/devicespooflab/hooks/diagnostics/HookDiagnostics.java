package com.devicespooflab.hooks.diagnostics;

import de.robv.android.xposed.XposedBridge;

/**
 * Diagnostics and logging system for hooks.
 * Architecture layer inspired by Tiger.
 */
public class HookDiagnostics {

    private static final String TAG = "SpoofMyDevice-Diag";

    public static void logHookSuccess(String hookName, String packageName, String details) {
        XposedBridge.log(String.format("%s: [SUCCESS] %s applied for %s: %s", TAG, hookName, packageName, details));
    }

    public static void logHookSkipped(String hookName, String packageName, String reason) {
        XposedBridge.log(String.format("%s: [SKIPPED] %s skipped for %s. Reason: %s", TAG, hookName, packageName, reason));
    }

    public static void logHookFailed(String hookName, String packageName, String reason) {
        XposedBridge.log(String.format("%s: [FAILED] %s failed for %s. Reason: %s", TAG, hookName, packageName, reason));
    }

    public static void logDiagnosticState(String packageName, boolean configLoaded, boolean profileValid, String profileInvalidReason) {
        XposedBridge.log(TAG + ": --- Diagnostics for " + packageName + " ---");
        XposedBridge.log(TAG + ": Config Loaded: " + configLoaded);
        XposedBridge.log(TAG + ": Profile Valid: " + profileValid + (profileValid ? "" : " (" + profileInvalidReason + ")"));
        XposedBridge.log(TAG + ": Android API Level: " + android.os.Build.VERSION.SDK_INT);
        XposedBridge.log(TAG + ": --------------------------------------");
    }
}
