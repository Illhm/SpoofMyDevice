package com.devicespooflab.hooks.hooks

import android.media.MediaDrm
import android.webkit.WebView
import com.devicespooflab.hooks.utils.PackageFilter
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

class DrmWebViewHook : HookModule {
    override val targetPackages: List<String> = listOf("TARGET_APPS")
    override val priority: Int = 70

    override fun hook(classLoader: ClassLoader, params: Map<String, String>): Boolean {
        // DRM
        params["drm"]?.let { drmProp ->
            try {
                XposedHelpers.findAndHookMethod(
                    MediaDrm::class.java.name,
                    classLoader,
                    "getPropertyString",
                    String::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val key = param.args[0] as String
                            if (key == "deviceUniqueId") {
                                param.result = drmProp
                            }
                        }
                    }
                )

                XposedHelpers.findAndHookMethod(
                    MediaDrm::class.java.name,
                    classLoader,
                    "getPropertyByteArray",
                    String::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val key = param.args[0] as String
                            if (key == "deviceUniqueId") {
                                param.result = drmProp.toByteArray()
                            }
                        }
                    }
                )
            } catch (ignored: Throwable) {}
        }

        // WebView
        params["webview_visible"]?.let { visibility ->
            try {
                XposedHelpers.findAndHookMethod(
                    WebView::class.java.name,
                    classLoader,
                    "setVisibility",
                    Int::class.javaPrimitiveType,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            // Can be used to block invisible webviews used for tracking
                            if (visibility == "1") {
                                // Enforce visible
                                param.args[0] = android.view.View.VISIBLE
                            }
                        }
                    }
                )
            } catch (ignored: Throwable) {}
        }

        return true
    }
}
