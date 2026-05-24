package com.devicespooflab.hooks.hooks;

import com.devicespooflab.hooks.utils.ConfigManager;
import com.devicespooflab.hooks.hooks.HookProfileResolver;
import android.util.Base64;
import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks MediaDrm to spoof Widevine device unique ID.
 * Many apps use this for device fingerprinting.
 */
public class MediaDrmHooks {

    private static final String DEVICE_UNIQUE_ID = "deviceUniqueId";

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> mediaDrmClass = XposedHelpers.findClassIfExists(
                "android.media.MediaDrm",
                lpparam.classLoader
        );

        if (mediaDrmClass == null) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(mediaDrmClass, "getPropertyByteArray",
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String propertyName = (String) param.args[0];

                            if (DEVICE_UNIQUE_ID.equals(propertyName)) {
                                byte[] rawSpoofedValue = ConfigManager.getMediaDrmId();
                                if (rawSpoofedValue != null) {
                                    String strValue = Base64.encodeToString(rawSpoofedValue, Base64.DEFAULT);
                                    String resolvedStrValue = HookProfileResolver.resolveString(ConfigManager.KEY_SPOOF_MEDIA_DRM_ID, strValue);

                                    if (resolvedStrValue != null) {
                                        try {
                                            byte[] resolvedSpoofedValue = Base64.decode(resolvedStrValue, Base64.DEFAULT);
                                            param.setResult(resolvedSpoofedValue);
                                        } catch (Exception e) {
                                            // Handle base64 failure safely
                                        }
                                    }
                                }
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }
    }
}
