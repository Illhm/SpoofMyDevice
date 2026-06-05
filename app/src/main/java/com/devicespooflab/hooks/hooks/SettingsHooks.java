package com.devicespooflab.hooks.hooks;

import android.content.ContentResolver;

import com.devicespooflab.hooks.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks Settings.Secure.getString() to spoof Android ID and GSF ID.
 *
 * IMPORTANT: Uses narrow hooks on Settings.Secure.getString() rather than
 * broad ContentResolver.query() hooks which would break many apps.
 */
public class SettingsHooks {

    private static final String ANDROID_ID = "android_id";
    private static final String GSF_ID = "gsf_id";
    private static final String DEVICE_NAME = "device_name";
    private static final String BLUETOOTH_NAME = "bluetooth_name";
    private static final String ENABLED_INPUT_METHODS = "enabled_input_methods";

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        hookSettingsSecure(lpparam);
        hookSettingsGlobal(lpparam);
        hookSettingsSystem(lpparam);
    }

    private static void hookSettingsSecure(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> settingsSecure = XposedHelpers.findClassIfExists(
                "android.provider.Settings$Secure",
                lpparam.classLoader
        );

        if (settingsSecure == null) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(settingsSecure, "getString",
                    ContentResolver.class, String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String name = (String) param.args[1];

                            if (name == null) {
                                return;
                            }

                            if (ANDROID_ID.equals(name)) {
                                String spoofedValue = ConfigManager.getAndroidId();
                                if (spoofedValue != null) {
                                    param.setResult(spoofedValue);
                                }
                                return;
                            }

                            if (name.contains("gsf") || GSF_ID.equals(name)) {
                                String spoofedValue = ConfigManager.getGSFId();
                                if (spoofedValue != null) {
                                    param.setResult(spoofedValue);
                                }
                                return;
                            }

                            // Reorganizing enabled_input_methods if already partially supported (just a placeholder/pass-through if no specific config)
                            if (ENABLED_INPUT_METHODS.equals(name)) {
                                // Specific logic can go here if ConfigManager adds it later.
                                return;
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(settingsSecure, "getString",
                    ContentResolver.class, String.class, String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String name = (String) param.args[1];

                            if (name == null) {
                                return;
                            }

                            if (ANDROID_ID.equals(name)) {
                                String spoofedValue = ConfigManager.getAndroidId();
                                if (spoofedValue != null) {
                                    param.setResult(spoofedValue);
                                }
                                return;
                            }

                            if (name.contains("gsf") || GSF_ID.equals(name)) {
                                String spoofedValue = ConfigManager.getGSFId();
                                if (spoofedValue != null) {
                                    param.setResult(spoofedValue);
                                }
                                return;
                            }

                            if (ENABLED_INPUT_METHODS.equals(name)) {
                                return;
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }
    }

    private static void hookSettingsGlobal(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> settingsGlobal = XposedHelpers.findClassIfExists(
                "android.provider.Settings$Global",
                lpparam.classLoader
        );

        if (settingsGlobal == null) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(settingsGlobal, "getString",
                    ContentResolver.class, String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String name = (String) param.args[1];
                            if (DEVICE_NAME.equals(name)) {
                                String spoofedValue = ConfigManager.getBuildDevice();
                                if (spoofedValue != null) {
                                    param.setResult(spoofedValue);
                                }
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }
    }

    private static void hookSettingsSystem(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> settingsSystem = XposedHelpers.findClassIfExists(
                "android.provider.Settings$System",
                lpparam.classLoader
        );

        if (settingsSystem == null) {
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(settingsSystem, "getString",
                    ContentResolver.class, String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String name = (String) param.args[1];
                            if (BLUETOOTH_NAME.equals(name)) {
                                // Default bluetooth name to the device name
                                String spoofedValue = ConfigManager.getBuildDevice();
                                if (spoofedValue != null) {
                                    param.setResult(spoofedValue);
                                }
                            }
                        }
                    });
        } catch (NoSuchMethodError ignored) {
        }
    }
}
