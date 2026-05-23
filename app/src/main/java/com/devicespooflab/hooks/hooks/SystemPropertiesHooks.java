package com.devicespooflab.hooks.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SystemPropertiesHooks {
    private static final String TAG = "DeviceSpoofLab-SystemProps";

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        hookSystemProperties(lpparam.classLoader, lpparam.packageName);
        hookSystemProperties(ClassLoader.getSystemClassLoader(), lpparam.packageName);
    }

    private static void hookSystemProperties(ClassLoader cl, String pkg) {
        Class<?> c = XposedHelpers.findClassIfExists("android.os.SystemProperties", cl);
        if (c == null) return;
        HookValueResolver resolver = HookValueResolver.forPackage(pkg);
        try {
            XposedHelpers.findAndHookMethod(c, "get", String.class, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) { param.setResult(resolver.resolveSystemProperty((String) param.args[0], (String) param.getResult())); }
            });
            XposedHelpers.findAndHookMethod(c, "get", String.class, String.class, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) { param.setResult(resolver.resolveSystemProperty((String) param.args[0], (String) param.getResult())); }
            });
            XposedHelpers.findAndHookMethod(c, "getInt", String.class, int.class, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) {
                    String v = resolver.resolveSystemProperty((String) param.args[0], null);
                    if (v != null) try { param.setResult(Integer.parseInt(v)); } catch (NumberFormatException ignored) {}
                }
            });
            XposedHelpers.findAndHookMethod(c, "getLong", String.class, long.class, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) {
                    String v = resolver.resolveSystemProperty((String) param.args[0], null);
                    if (v != null) try { param.setResult(Long.parseLong(v)); } catch (NumberFormatException ignored) {}
                }
            });
            XposedHelpers.findAndHookMethod(c, "getBoolean", String.class, boolean.class, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) {
                    String v = resolver.resolveSystemProperty((String) param.args[0], null);
                    if (v != null) param.setResult("1".equals(v) || "true".equalsIgnoreCase(v));
                }
            });
        } catch (Throwable t) { XposedBridge.log(TAG + ": " + t.getMessage()); }
    }
}
