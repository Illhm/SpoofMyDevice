package com.devicespooflab.hooks

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class ShopeeReceiverHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.shopee.id") {
            return
        }

        XposedBridge.log("ShopeeReceiverHook: Hooking com.shopee.id")

        try {
            val contextImplClass = XposedHelpers.findClass("android.app.ContextImpl", lpparam.classLoader)

            XposedBridge.hookAllMethods(contextImplClass, "registerReceiverInternal", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val args = param.args
                    if (args == null || args.isEmpty()) {
                        return
                    }

                    // Find the flags parameter, which is typically the last int parameter or close to the end
                    for (i in args.indices.reversed()) {
                        val arg = args[i]
                        if (arg is Int) {
                            val flags = arg
                            val receiverExported = 0x2
                            val receiverNotExported = 0x4

                            // If neither RECEIVER_EXPORTED nor RECEIVER_NOT_EXPORTED is specified
                            if ((flags and receiverExported) == 0 && (flags and receiverNotExported) == 0) {
                                // Apply RECEIVER_EXPORTED flag
                                param.args[i] = flags or receiverExported
                                XposedBridge.log("ShopeeReceiverHook: Modified flags for registerReceiverInternal to include RECEIVER_EXPORTED. Original: $flags, New: ${param.args[i]}")
                            }
                            break // Only process the last integer argument assuming it represents the flags
                        }
                    }
                }
            })
            XposedBridge.log("ShopeeReceiverHook: Successfully hooked registerReceiverInternal")
        } catch (t: Throwable) {
            XposedBridge.log("ShopeeReceiverHook: Failed to hook registerReceiverInternal: " + t.message)
            XposedBridge.log(t)
        }
    }
}
