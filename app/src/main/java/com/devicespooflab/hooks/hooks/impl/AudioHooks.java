package com.devicespooflab.hooks.hooks.impl;

import android.media.AudioManager;
import com.devicespooflab.hooks.utils.ConfigManager;
import com.devicespooflab.hooks.diagnostics.HookDiagnostics;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class AudioHooks {
    private static final String TAG = "DeviceSpoofLab-Audio";

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> audioManagerClass = XposedHelpers.findClassIfExists("android.media.AudioManager", lpparam.classLoader);
            if (audioManagerClass != null) {
                XposedHelpers.findAndHookMethod(audioManagerClass, "getParameters", String.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String keys = (String) param.args[0];
                        if (keys != null && keys.contains("audio_codec")) {
                            String spoofedCodec = ConfigManager.getOptionalConfigValue(ConfigManager.FIELD_AUDIO_CODEC);
                            if (spoofedCodec != null && !spoofedCodec.isEmpty()) {
                                param.setResult("audio_codec=" + spoofedCodec);
                            }
                        }
                    }
                });
                XposedHelpers.findAndHookMethod(audioManagerClass, "getProperty", String.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String key = (String) param.args[0];
                        if (AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER.equals(key)) {
                             // Hardcode something generic but consistent if needed
                             // param.setResult("256");
                        }
                    }
                });
                HookDiagnostics.logHookSuccess("AudioHooks", lpparam.packageName, "Successfully hooked AudioManager");
            }
        } catch (Throwable e) {
            HookDiagnostics.logHookFailed("AudioHooks", lpparam.packageName, e.getMessage());
            XposedBridge.log(TAG + ": Failed to hook AudioManager: " + e.getMessage());
        }
    }
}
