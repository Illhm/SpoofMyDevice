package com.devicespooflab.hooks.hooks;

import com.devicespooflab.hooks.utils.ConfigManager;

import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class VendorSystemPropertiesHooks {
    private static final String[] CLASSES = {"android.os.SemSystemProperties", "com.samsung.android.os.SemSystemProperties"};

    private VendorSystemPropertiesHooks() {}

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        String m = ConfigManager.getBuildManufacturer();
        if (m == null || !m.toLowerCase(Locale.US).contains("samsung")) return;
        HookValueResolver resolver = HookValueResolver.forPackage(lpparam.packageName);
        for (String n : CLASSES) {
            hookClass(XposedHelpers.findClassIfExists(n, lpparam.classLoader), resolver);
            hookClass(XposedHelpers.findClassIfExists(n, null), resolver);
        }
    }

    private static void hookClass(Class<?> c, HookValueResolver resolver) {
        if (c == null) return;
        tryHook(c, "get", new Object[]{String.class}, resolver);
        tryHook(c, "get", new Object[]{String.class, String.class}, resolver);
        try {
            XposedHelpers.findAndHookMethod(c, "getInt", String.class, int.class, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam p){ String v=resolver.resolveSystemProperty((String)p.args[0], null); if(v!=null) try{p.setResult(Integer.parseInt(v));}catch(Throwable ignored){}}});
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(c, "getLong", String.class, long.class, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam p){ String v=resolver.resolveSystemProperty((String)p.args[0], null); if(v!=null) try{p.setResult(Long.parseLong(v));}catch(Throwable ignored){}}});
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.findAndHookMethod(c, "getBoolean", String.class, boolean.class, new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam p){ String v=resolver.resolveSystemProperty((String)p.args[0], null); if(v!=null) p.setResult("1".equals(v)||"true".equalsIgnoreCase(v));}});
        } catch (Throwable ignored) {}
    }

    private static void tryHook(Class<?> c, String name, Object[] params, HookValueResolver resolver) {
        try {
            Object[] args = new Object[params.length + 1];
            System.arraycopy(params, 0, args, 0, params.length);
            args[params.length] = new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam p) { p.setResult(resolver.resolveSystemProperty((String)p.args[0], (String)p.getResult())); }
            };
            XposedHelpers.findAndHookMethod(c, name, args);
        } catch (Throwable ignored) {}
    }
}
