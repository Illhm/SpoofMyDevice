package com.devicespooflab.hooks.hooks;

import android.content.Context;

import com.devicespooflab.hooks.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks WebView User-Agent to match Pixel 7 Pro device profile.
 *
 * Non-aggressive implementation - only spoofs User-Agent string, no Canvas/WebGL.
 * User requested: "spoof the headers such as user agent, device model, dimensions"
 */
public class WebViewHooks {

    private static final String TAG = "DeviceSpoofLab-WebView";
    private static final String DEVCHECK_PACKAGE = "flar2.devcheck";

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        // DevCheck uses native anti-tamper library (pairipcore). Broad constructor hooks can
        // increase startup instability on some ROM/bridge combinations; keep this package stable.
        if (DEVCHECK_PACKAGE.equals(lpparam.packageName)) {
            XposedBridge.log(TAG + ": Skipping WebView hooks for " + lpparam.packageName + " (stability guard)");
            return;
        }

        try {
            hookWebSettings(lpparam);
            hookWebViewConstructor(lpparam);
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook WebView: " + e.getMessage());
        }
    }

    private static void hookWebSettings(XC_LoadPackage.LoadPackageParam lpparam) {
        // Don't hook WebSettings directly - it's abstract
        // Instead, hook the implementation class used by WebView
        // We hook it indirectly through WebView.getSettings()

        Class<?> webViewClass = XposedHelpers.findClassIfExists(
            "android.webkit.WebView", lpparam.classLoader);

        if (webViewClass == null) {
            return;
        }

        try {
            // Hook WebView.getSettings() to intercept and modify the returned WebSettings object
            XposedHelpers.findAndHookMethod(webViewClass, "getSettings",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object settings = param.getResult();
                        if (settings != null) {
                            // Set spoofed UA immediately
                            String spoofedUA = ConfigManager.getWebViewUserAgent();
                            if (spoofedUA != null) {
                                try {
                                    XposedHelpers.callMethod(settings, "setUserAgentString", spoofedUA);
                                } catch (Exception e) {
                                    // Failed to set, that's okay
                                }
                            }
                        }
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook WebView.getSettings(): " + e.getMessage());
        }
    }

    private static void hookWebViewConstructor(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> webViewClass = XposedHelpers.findClassIfExists(
            "android.webkit.WebView", lpparam.classLoader);

        if (webViewClass == null) {
            return;
        }

        try {
            // Hook WebView(Context) constructor
            XposedHelpers.findAndHookConstructor(webViewClass,
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            Object webView = param.thisObject;
                            Object settings = XposedHelpers.callMethod(webView, "getSettings");

                            String spoofedUA = ConfigManager.getWebViewUserAgent();
                            if (spoofedUA != null) {
                                XposedHelpers.callMethod(settings, "setUserAgentString", spoofedUA);
                            }
                        } catch (Exception e) {
                            // Failed to set UA in constructor, that's okay
                        }
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook WebView(Context) constructor: " + e.getMessage());
        }

        try {
            // Hook WebView(Context, AttributeSet) constructor
            XposedHelpers.findAndHookConstructor(webViewClass,
                Context.class, android.util.AttributeSet.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            Object webView = param.thisObject;
                            Object settings = XposedHelpers.callMethod(webView, "getSettings");

                            String spoofedUA = ConfigManager.getWebViewUserAgent();
                            if (spoofedUA != null) {
                                XposedHelpers.callMethod(settings, "setUserAgentString", spoofedUA);
                            }
                        } catch (Exception e) {
                            // Failed to set UA in constructor, that's okay
                        }
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook WebView(Context, AttributeSet) constructor: " + e.getMessage());
        }

        try {
            // Hook WebView(Context, AttributeSet, int) constructor
            XposedHelpers.findAndHookConstructor(webViewClass,
                Context.class, android.util.AttributeSet.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            Object webView = param.thisObject;
                            Object settings = XposedHelpers.callMethod(webView, "getSettings");

                            String spoofedUA = ConfigManager.getWebViewUserAgent();
                            if (spoofedUA != null) {
                                XposedHelpers.callMethod(settings, "setUserAgentString", spoofedUA);
                            }
                        } catch (Exception e) {
                            // Failed to set UA in constructor, that's okay
                        }
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook WebView(Context, AttributeSet, int) constructor: " + e.getMessage());
        }
    }
}
