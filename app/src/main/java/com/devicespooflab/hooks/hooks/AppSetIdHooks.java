package com.devicespooflab.hooks.hooks;

import android.os.Build;

import com.devicespooflab.hooks.utils.ConfigManager;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks Google App Set ID (Android 11+).
 * App Set ID is Google's replacement for Advertising ID for aggregate measurement.
 *
 * Only enabled on Android 11+ (SDK 30+)
 */
public class AppSetIdHooks {

    private static final String TAG = "DeviceSpoofLab-AppSetId";
    private static final int MIN_SDK = 30; // Android 11

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        // Only hook on Android 11+ (SDK 30+)
        if (Build.VERSION.SDK_INT < MIN_SDK) {
            return;
        }

        try {
            hookAppSetIdInfo(lpparam);
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook App Set ID: " + e.getMessage());
        }
    }

    private static void hookAppSetIdInfo(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> appSetIdInfoClass = XposedHelpers.findClassIfExists(
            "com.google.android.gms.appset.AppSetIdInfo", lpparam.classLoader);

        if (appSetIdInfoClass == null) {
            return;
        }

        try {
            // Hook AppSetIdInfo.getId()
            XposedHelpers.findAndHookMethod(appSetIdInfoClass, "getId",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String configuredValue = ConfigManager.getAppSetId();
                        if (configuredValue != null) {
                            // If user explicitly configured an AppSetId, use it.
                            param.setResult(configuredValue);
                        } else {
                            // Otherwise, generate a deterministic UUID based on the package name.
                            // The AppSetId must be deterministic and persistent per package name.
                            // Random UUIDs on every call break session consistency and cause network drops.
                            try {
                                String packageName = lpparam.packageName;
                                if (packageName != null && !packageName.isEmpty()) {
                                    String deterministicUuid = UUID.nameUUIDFromBytes(packageName.getBytes(StandardCharsets.UTF_8)).toString();
                                    param.setResult(deterministicUuid);
                                }
                            } catch (Exception e) {
                                XposedBridge.log(TAG + ": Error generating deterministic AppSetId: " + e.getMessage());
                            }
                        }
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook getId(): " + e.getMessage());
        }

        try {
            // Hook AppSetIdInfo.getScope()
            // Scope: 1 = APP, 2 = DEVELOPER
            // Return APP scope (1) for per-app isolation
            XposedHelpers.findAndHookMethod(appSetIdInfoClass, "getScope",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(1); // APP scope
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook getScope(): " + e.getMessage());
        }
    }
}
