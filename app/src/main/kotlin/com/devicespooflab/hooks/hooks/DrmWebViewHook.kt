package com.devicespooflab.hooks.hooks

import android.media.MediaDrm
import android.webkit.WebView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

class DrmWebViewHook : HookModule {
    override val priority = 30
    override val targetPackages = listOf("all")

    override fun hook(classLoader: ClassLoader, params: Map<String, String>): Boolean {
        // DRM & WebView (2 param) — Prioritas P2

        // 1. MediaDrm
        try {
            val mediaDrmClass = MediaDrm::class.java
            XposedHelpers.findAndHookMethod(
                mediaDrmClass, "getPropertyString", String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        // Assuming the "drm" parameter contains JSON or a specific string mapping
                        // In reality, this requires more complex logic to parse `params["drm"]`
                        // and map it to properties like "deviceUniqueId", "vendor", etc.
                        // Here we just blindly return it for demonstration or specific keys if known.
                        val key = param.args[0] as String
                        if (key == MediaDrm.PROPERTY_DEVICE_UNIQUE_ID) {
                           params["drm"]?.let { param.result = it }
                        }
                    }
                }
            )
        } catch (e: Exception) {}

        // 2. WebView
        try {
            val webViewClass = WebView::class.java
            // webview_visible might control whether WebView behaves like a real device or headless
            // Simple placeholder hook:
            XposedHelpers.findAndHookMethod(
                webViewClass, "setVisibility", Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        params["webview_visible"]?.toIntOrNull()?.let { param.args[0] = it }
                    }
                }
            )
        } catch (e: Exception) {}

        return true
    }
}
