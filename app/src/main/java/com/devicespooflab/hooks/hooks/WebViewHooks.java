package com.devicespooflab.hooks.hooks;

import android.content.Context;

import com.devicespooflab.hooks.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hooks WebView User-Agent to match Pixel 7 Pro device profile.
 *
 * Non-aggressive implementation - only spoofs User-Agent string, no Canvas/WebGL.
 * User requested: "spoof the headers such as user agent, device model, dimensions"
 */
public class WebViewHooks {

    private static final String TAG = "DeviceSpoofLab-WebView";
    private static final Set<String> hookedSettingsClasses = Collections.synchronizedSet(new HashSet<>());
    private static final Pattern DEVICE_PATTERN = Pattern.compile("\\(Linux;[^)]+\\)");

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
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
            XposedHelpers.findAndHookMethod(webViewClass, "getSettings",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object settings = param.getResult();
                        if (settings != null) {
                            Class<?> settingsClass = settings.getClass();
                            String className = settingsClass.getName();

                            if (!hookedSettingsClasses.contains(className)) {
                                hookedSettingsClasses.add(className);
                                hookSettingsClass(settingsClass);
                            }

                            // Proactively spoof the UA natively. Retrieve the default UA, patch it, and set it.
                            try {
                                String defaultUA = (String) XposedHelpers.callMethod(settings, "getUserAgentString");
                                String spoofedUA = ConfigManager.getWebViewUserAgent();
                                if (defaultUA != null && spoofedUA != null) {
                                    String patchedUA = replaceDevicePortion(defaultUA, spoofedUA);
                                    // Bypassing our own hook logic isn't strictly necessary since our hook
                                    // preserves custom suffixes (like the default Android one), but we call
                                    // it directly to push it to the native engine immediately.
                                    XposedHelpers.callMethod(settings, "setUserAgentString", patchedUA);
                                }
                            } catch (Exception e) {
                                XposedBridge.log(TAG + ": Failed to initialize UA natively: " + e.getMessage());
                            }
                        }
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook WebView.getSettings(): " + e.getMessage());
        }
    }

    private static void hookSettingsClass(Class<?> settingsClass) {
        try {
            XposedHelpers.findAndHookMethod(settingsClass, "getUserAgentString", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String originalUA = (String) param.getResult();
                    String spoofedUA = ConfigManager.getWebViewUserAgent();
                    if (originalUA != null && spoofedUA != null) {
                        param.setResult(replaceDevicePortion(originalUA, spoofedUA));
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook getUserAgentString: " + e.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(settingsClass, "setUserAgentString", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String customUA = (String) param.args[0];
                    String spoofedUA = ConfigManager.getWebViewUserAgent();
                    if (customUA != null && spoofedUA != null) {
                        param.args[0] = replaceDevicePortion(customUA, spoofedUA);
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook setUserAgentString: " + e.getMessage());
        }
    }

    private static String replaceDevicePortion(String originalUA, String spoofedUA) {
        Matcher originalMatcher = DEVICE_PATTERN.matcher(originalUA);
        Matcher spoofedMatcher = DEVICE_PATTERN.matcher(spoofedUA);

        if (originalMatcher.find() && spoofedMatcher.find()) {
            return originalUA.replace(originalMatcher.group(0), spoofedMatcher.group(0));
        }
        return originalUA;
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
                            XposedHelpers.callMethod(webView, "getSettings");
                        } catch (Exception e) {
                            // Failed to set UA in constructor, that's okay
                        }
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook WebView constructor: " + e.getMessage());
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
                            XposedHelpers.callMethod(webView, "getSettings");
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
                            XposedHelpers.callMethod(webView, "getSettings");
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
