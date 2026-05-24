package com.devicespooflab.hooks.hooks;

import android.os.Build;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class ContextHooks {

    private static final String TAG = "DeviceSpoofLab-ContextHooks";

    // RECEIVER_EXPORTED was introduced in Android 13 (API 33) but enforced in Android 14 (API 34)
    private static final int RECEIVER_EXPORTED = 0x2;
    private static final int RECEIVER_NOT_EXPORTED = 0x4;

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        // The issue we're fixing is only relevant on Android 13+ (enforced in 14+)
        // On older Android versions, the registerReceiverInternal signature does not have a flags parameter
        // at the end, but rather a userId parameter, which we do not want to modify.
        int sdkInt = Build.VERSION.SDK_INT;
        try {
            String spoofedSdk = com.devicespooflab.hooks.hooks.HookProfileResolver.resolveString("sdk", com.devicespooflab.hooks.utils.ConfigManager.getSystemProperty("ro.build.version.sdk", null));
            if (spoofedSdk != null) sdkInt = Integer.parseInt(spoofedSdk);
        } catch (Exception e) {}
        if (sdkInt < 33) {
            return;
        }

        try {
            Class<?> contextImplClass = XposedHelpers.findClassIfExists(
                    "android.app.ContextImpl",
                    lpparam.classLoader
            );

            if (contextImplClass != null) {
                // Android 14+ strict receiver enforcement workaround
                XC_MethodHook receiverHook = new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Object[] args = param.args;
                        if (args == null || args.length == 0) {
                            return;
                        }

                        if (!(param.method instanceof Method)) {
                            return;
                        }

                        Method method = (Method) param.method;
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length == 0) {
                            return;
                        }

                        int lastArgIndex = parameterTypes.length - 1;
                        if (parameterTypes[lastArgIndex] != Integer.TYPE || !(args[lastArgIndex] instanceof Integer)) {
                            return;
                        }

                        boolean hasIntentFilter = false;
                        for (Class<?> parameterType : parameterTypes) {
                            if ("android.content.IntentFilter".equals(parameterType.getName())) {
                                hasIntentFilter = true;
                                break;
                            }
                        }
                        if (!hasIntentFilter) {
                            return;
                        }

                        int flags = (Integer) args[lastArgIndex];
                        if ((flags & RECEIVER_EXPORTED) == 0 && (flags & RECEIVER_NOT_EXPORTED) == 0) {
                            param.args[lastArgIndex] = flags | RECEIVER_EXPORTED;
                        }
                    }
                };

                XposedBridge.hookAllMethods(contextImplClass, "registerReceiver", receiverHook);
                XposedBridge.hookAllMethods(contextImplClass, "registerReceiverInternal", receiverHook);
            }
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook ContextImpl: " + throwable.getMessage());
        }
    }
}
