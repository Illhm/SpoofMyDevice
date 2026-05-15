package com.devicespooflab.hooks.hooks;

import android.content.Context;
import android.os.Build;

import com.devicespooflab.hooks.utils.ConfigManager;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final Set<String> hookedSettingsClasses = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            hookWebSettings(lpparam);
            hookWebViewConstructor(lpparam);
            hookDefaultUserAgent(lpparam);
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
                            Class<?> settingsClass = settings.getClass();
                            String className = settingsClass.getName();

                            // Ensure we only hook each settings implementation class once
                            if (!hookedSettingsClasses.contains(className)) {
                                hookedSettingsClasses.add(className);

                                try {
                                    // Hook getUserAgentString() on the actual implementation class
                                    XposedHelpers.findAndHookMethod(settingsClass, "getUserAgentString",
                                        new XC_MethodHook() {
                                            @Override
                                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                                String originalUa = (String) param.getResult();
                                                String spoofedUa = applySpoofedUserAgent(originalUa);
                                                if (spoofedUa != null) {
                                                    param.setResult(spoofedUa);
                                                }
                                            }
                                        });

                                    // Hook setUserAgentString() to allow apps to set their own UA, but still spoof the base if needed
                                    XposedHelpers.findAndHookMethod(settingsClass, "setUserAgentString", String.class,
                                        new XC_MethodHook() {
                                            @Override
                                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                                String newUa = (String) param.args[0];
                                                if (newUa != null) {
                                                    String spoofedUa = applySpoofedUserAgent(newUa);
                                                    if (spoofedUa != null) {
                                                        param.args[0] = spoofedUa;
                                                    }
                                                }
                                            }
                                        });

                                } catch (Exception e) {
                                    XposedBridge.log(TAG + ": Failed to hook settings methods on " + className + ": " + e.getMessage());
                                }
                            }

                            // Set spoofed UA immediately to ensure the settings object is updated
                            try {
                                String originalUa = (String) XposedHelpers.callMethod(settings, "getUserAgentString");
                                String spoofedUA = applySpoofedUserAgent(originalUa);
                                if (spoofedUA != null) {
                                    XposedHelpers.callMethod(settings, "setUserAgentString", spoofedUA);
                                }
                            } catch (Exception e) {
                                // Failed to set, that's okay
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

        XC_MethodHook constructorHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    Object webView = param.thisObject;
                    Object settings = XposedHelpers.callMethod(webView, "getSettings");

                    if (settings != null) {
                        String originalUa = (String) XposedHelpers.callMethod(settings, "getUserAgentString");
                        String spoofedUA = applySpoofedUserAgent(originalUa);
                        if (spoofedUA != null) {
                            XposedHelpers.callMethod(settings, "setUserAgentString", spoofedUA);
                        }
                    }
                } catch (Exception e) {
                    // Failed to set UA in constructor, that's okay
                }
            }
        };

        try {
            // Hook WebView(Context) constructor
            XposedHelpers.findAndHookConstructor(webViewClass, Context.class, constructorHook);
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook WebView constructor: " + e.getMessage());
        }

        try {
            // Hook WebView(Context, AttributeSet) constructor
            XposedHelpers.findAndHookConstructor(webViewClass, Context.class, android.util.AttributeSet.class, constructorHook);
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook WebView(Context, AttributeSet) constructor: " + e.getMessage());
        }

        try {
            // Hook WebView(Context, AttributeSet, int) constructor
            XposedHelpers.findAndHookConstructor(webViewClass, Context.class, android.util.AttributeSet.class, int.class, constructorHook);
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook WebView(Context, AttributeSet, int) constructor: " + e.getMessage());
        }
    }

    private static void hookDefaultUserAgent(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> webSettingsClass = XposedHelpers.findClassIfExists(
            "android.webkit.WebSettings", lpparam.classLoader);

        if (webSettingsClass == null) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(webSettingsClass, "getDefaultUserAgent", Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String originalUa = (String) param.getResult();
                        String spoofedUa = applySpoofedUserAgent(originalUa);
                        if (spoofedUa != null) {
                            param.setResult(spoofedUa);
                        }
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook WebSettings.getDefaultUserAgent(): " + e.getMessage());
        }
    }

    /**
     * Applies the spoofed User-Agent while preserving any custom suffix or identifiers
     * appended by the host app. This is crucial so that internal API routing logic
     * or session management that relies on the custom suffix does not break.
     */
    private static String applySpoofedUserAgent(String originalUa) {
        String baseSpoofedUa = ConfigManager.getWebViewUserAgent();
        if (baseSpoofedUa == null || baseSpoofedUa.isEmpty()) {
            return null; // Return null if no spoofing is configured
        }

        if (originalUa == null || originalUa.isEmpty()) {
            return baseSpoofedUa;
        }

        // Try to identify the standard "Safari/XXX.XX" end of a standard Android User-Agent.
        // Apps usually append their custom string *after* this part.
        int safariIndex = originalUa.lastIndexOf("Safari/");
        if (safariIndex != -1) {
            int spaceAfterSafari = originalUa.indexOf(' ', safariIndex);
            if (spaceAfterSafari != -1 && spaceAfterSafari + 1 < originalUa.length()) {
                // There is custom app-specific suffix after "Safari/..."
                String customSuffix = originalUa.substring(spaceAfterSafari + 1).trim();

                // Now we need to append this custom suffix to our spoofed base UA.
                // Assuming the baseSpoofedUa also ends with "Safari/..." or similar standard ending.
                // We will append it with a space.
                if (!customSuffix.isEmpty()) {
                     return baseSpoofedUa + " " + customSuffix;
                }
            }
        }

        return baseSpoofedUa;
    }
}
