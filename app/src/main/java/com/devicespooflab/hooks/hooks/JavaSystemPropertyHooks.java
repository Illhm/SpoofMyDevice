package com.devicespooflab.hooks.hooks;

import com.devicespooflab.hooks.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class JavaSystemPropertyHooks {
    private JavaSystemPropertyHooks() {}

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        HookValueResolver resolver = HookValueResolver.forPackage(lpparam.packageName);
        XC_MethodHook h = new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) {
                String key = (String) param.args[0];
                if ("os.arch".equals(key)) {
                    String abi = resolver.resolveSystemProperty("ro.product.cpu.abi", ConfigManager.getCpuAbi());
                    if (abi != null) param.setResult(abi.contains("arm64") ? "aarch64" : abi);
                } else if ("http.agent".equals(key)) {
                    String ua = resolver.resolveSystemProperty("webview.user_agent", ConfigManager.getWebViewUserAgent());
                    if (ua != null) param.setResult(ua);
                } else if ("os.version".equals(key)) {
                    String v = resolver.resolveSystemProperty("ro.build.version.release", null);
                    if (v != null) param.setResult(v);
                }
            }
        };
        try { XposedHelpers.findAndHookMethod(System.class, "getProperty", String.class, h);} catch (Throwable ignored) {}
        try { XposedHelpers.findAndHookMethod(System.class, "getProperty", String.class, String.class, h);} catch (Throwable ignored) {}
    }
}
