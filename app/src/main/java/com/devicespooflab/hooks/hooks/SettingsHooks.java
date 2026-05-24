package com.devicespooflab.hooks.hooks;

import android.content.ContentResolver;

import com.devicespooflab.hooks.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SettingsHooks {

    private static final String ANDROID_ID = "android_id";
    private static final String GSF_ID = "gsf_id";

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> settingsSecure = XposedHelpers.findClassIfExists("android.provider.Settings$Secure", lpparam.classLoader);
        if (settingsSecure == null) return;

        HookValueResolver resolver = HookValueResolver.forPackage(lpparam.packageName);
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                String name = (String) param.args[1];
                if (name == null) return;
                if (ANDROID_ID.equals(name)) {
                    if (!resolver.isSpoofEnabled(ConfigManager.FIELD_GSF_ID)) return;
                    String spoofedValue = ConfigManager.getAndroidId();
                    if (spoofedValue != null) param.setResult(spoofedValue);
                    return;
                }
                if (name.contains("gsf") || GSF_ID.equals(name)) {
                    if (!resolver.isSpoofEnabled(ConfigManager.FIELD_GSF_ID)) return;
                    String spoofedValue = ConfigManager.getGSFId();
                    if (spoofedValue != null) param.setResult(spoofedValue);
                }
            }
        };

        try { XposedHelpers.findAndHookMethod(settingsSecure, "getString", ContentResolver.class, String.class, hook); } catch (NoSuchMethodError ignored) {}
        try { XposedHelpers.findAndHookMethod(settingsSecure, "getString", ContentResolver.class, String.class, String.class, hook); } catch (NoSuchMethodError ignored) {}
    }
}
