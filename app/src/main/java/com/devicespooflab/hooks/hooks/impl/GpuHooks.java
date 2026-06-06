package com.devicespooflab.hooks.hooks.impl;

import com.devicespooflab.hooks.utils.ConfigManager;
import com.devicespooflab.hooks.diagnostics.HookDiagnostics;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class GpuHooks {
    private static final String TAG = "DeviceSpoofLab-GPU";

    // OpenGL constants
    private static final int GL_VENDOR = 0x1F00;
    private static final int GL_RENDERER = 0x1F01;

    // EGL constants
    private static final int EGL_VENDOR = 0x3053;

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            hookOpenGL("android.opengl.GLES10", lpparam);
            hookOpenGL("android.opengl.GLES20", lpparam);
            hookOpenGL("android.opengl.GLES30", lpparam);
            hookOpenGL("android.opengl.GLES31", lpparam);
            hookEGL("android.opengl.EGL14", lpparam);
            HookDiagnostics.logHookSuccess("GpuHooks", lpparam.packageName, "Successfully hooked GPU/OpenGL specifications");
        } catch (Throwable e) {
            HookDiagnostics.logHookFailed("GpuHooks", lpparam.packageName, e.getMessage());
            XposedBridge.log(TAG + ": Failed to hook GPU specs: " + e.getMessage());
        }
    }

    private static void hookOpenGL(String className, XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> glesClass = XposedHelpers.findClassIfExists(className, lpparam.classLoader);
            if (glesClass != null) {
                XposedHelpers.findAndHookMethod(glesClass, "glGetString", int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        int name = (int) param.args[0];

                        if (name == GL_RENDERER) {
                            String spoofedRenderer = ConfigManager.getOptionalConfigValue(ConfigManager.FIELD_GPU_RENDERER);
                            if (spoofedRenderer != null && !spoofedRenderer.isEmpty()) {
                                param.setResult(spoofedRenderer);
                            }
                        } else if (name == GL_VENDOR) {
                            String spoofedVendor = ConfigManager.getOptionalConfigValue(ConfigManager.FIELD_GPU_VENDOR);
                            if (spoofedVendor != null && !spoofedVendor.isEmpty()) {
                                param.setResult(spoofedVendor);
                            }
                        }
                    }
                });
            }
        } catch (Throwable e) {
            // Ignore
        }
    }

    private static void hookEGL(String className, XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> eglClass = XposedHelpers.findClassIfExists(className, lpparam.classLoader);
            if (eglClass != null) {
                // eglQueryString typically takes (EGLDisplay dpy, int name)
                Class<?> eglDisplayClass = XposedHelpers.findClassIfExists("android.opengl.EGLDisplay", lpparam.classLoader);
                if (eglDisplayClass != null) {
                    XposedHelpers.findAndHookMethod(eglClass, "eglQueryString", eglDisplayClass, int.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            int name = (int) param.args[1];

                            if (name == EGL_VENDOR) {
                                String spoofedVendor = ConfigManager.getOptionalConfigValue(ConfigManager.FIELD_GPU_VENDOR);
                                if (spoofedVendor != null && !spoofedVendor.isEmpty()) {
                                    param.setResult(spoofedVendor);
                                }
                            }
                        }
                    });
                }
            }
        } catch (Throwable e) {
            // Ignore
        }
    }
}
