package com.devicespooflab.hooks.hooks;

import android.os.Build;
import android.content.Context;

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
        if (Build.VERSION.SDK_INT < 33) {
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

                        // The flags parameter is typically the last int parameter or second to last
                        // In registerReceiver/registerReceiverInternal, signature includes int flags
                        for (int i = args.length - 1; i >= 0; i--) {
                            if (args[i] instanceof Integer) {
                                int flags = (Integer) args[i];

                                // Only modify if it's likely a flags argument (not 0, not some other unrelated int like userId)
                                // Actually, flags can be 0. So we just check the first int we find from the right.
                                // In Android source, registerReceiverInternal signature often looks like:
                                // registerReceiverInternal(BroadcastReceiver receiver, int userId, IntentFilter filter, String broadcastPermission, Handler scheduler, Context context, int flags)
                                // We check if neither export flag is present
                                if ((flags & RECEIVER_EXPORTED) == 0 && (flags & RECEIVER_NOT_EXPORTED) == 0) {
                                    param.args[i] = flags | RECEIVER_EXPORTED;
                                }
                                break;
                            }
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
