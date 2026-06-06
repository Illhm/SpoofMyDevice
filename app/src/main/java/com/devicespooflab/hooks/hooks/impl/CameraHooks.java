package com.devicespooflab.hooks.hooks.impl;

import android.hardware.camera2.CameraManager;

import com.devicespooflab.hooks.utils.ConfigManager;
import com.devicespooflab.hooks.diagnostics.HookDiagnostics;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class CameraHooks {
    private static final String TAG = "DeviceSpoofLab-Camera";

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(CameraManager.class, "getCameraIdList", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String spoofedIds = ConfigManager.getOptionalConfigValue(ConfigManager.FIELD_CAMERA_IDS);
                    if (spoofedIds != null && !spoofedIds.isEmpty()) {
                        String[] ids = spoofedIds.split(",");
                        for (int i = 0; i < ids.length; i++) {
                            ids[i] = ids[i].trim();
                        }
                        param.setResult(ids);
                    }
                }
            });
            HookDiagnostics.logHookSuccess("CameraHooks", lpparam.packageName, "Successfully hooked CameraManager");
        } catch (Throwable e) {
            HookDiagnostics.logHookFailed("CameraHooks", lpparam.packageName, e.getMessage());
            XposedBridge.log(TAG + ": Failed to hook CameraManager: " + e.getMessage());
        }
    }
}
