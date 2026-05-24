package com.devicespooflab.hooks.hooks;

import com.devicespooflab.hooks.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks Build.SERIAL and Build.getSerial() to spoof device serial number.
 *
 * NOTE: Only hooks SERIAL, NOT other Build.* fields like FINGERPRINT, MODEL, etc.
 * Those are handled by Magisk via resetprop and should NOT be hooked here.
 */
public class BuildSerialHooks {

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> buildClass = XposedHelpers.findClassIfExists(
                "android.os.Build",
                lpparam.classLoader
        );

        if (buildClass == null) {
            return;
        }

        HookValueResolver resolver = HookValueResolver.forPackage(lpparam.packageName);
        try {
            XposedHelpers.findAndHookMethod(buildClass, "getSerial",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object original = param.getResult();
                            if (!(original instanceof String)) {
                                return;
                            }
                            String spoofedValue = resolver.resolveBuildField("SERIAL", ConfigManager.FIELD_BUILD_ID, (String) original);
                            if (spoofedValue != null) {
                                param.setResult(spoofedValue);
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            String spoofedValue = resolver.resolveBuildField("SERIAL", ConfigManager.FIELD_BUILD_ID, (String) XposedHelpers.getStaticObjectField(buildClass, "SERIAL"));
            if (spoofedValue != null) {
                XposedHelpers.setStaticObjectField(buildClass, "SERIAL", spoofedValue);
            }
        } catch (NoSuchFieldError ignored) {
        }
    }
}
