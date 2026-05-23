package com.devicespooflab.hooks.hooks;

import com.devicespooflab.hooks.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class WebViewHooks {
    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> webViewClass = XposedHelpers.findClassIfExists("android.webkit.WebView", lpparam.classLoader);
        if (webViewClass == null) return;
        HookValueResolver resolver = HookValueResolver.forPackage(lpparam.packageName);
        try {
            XposedHelpers.findAndHookMethod(webViewClass, "getSettings", new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam param) {
                    Object settings = param.getResult();
                    if (settings == null) return;
                    String ua = resolver.resolveSystemProperty("webview.user_agent", ConfigManager.getWebViewUserAgent());
                    if (ua == null) return;
                    try {
                        String current = (String) XposedHelpers.callMethod(settings, "getUserAgentString");
                        if (!ua.equals(current)) XposedHelpers.callMethod(settings, "setUserAgentString", ua);
                    } catch (Throwable ignored) {}
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("DeviceSpoofLab-WebView: " + t.getMessage());
        }
    }
}
