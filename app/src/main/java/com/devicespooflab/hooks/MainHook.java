package com.devicespooflab.hooks;

import android.os.Build;

import com.devicespooflab.hooks.hooks.AdvertisingIdHooks;
import com.devicespooflab.hooks.hooks.AppSetIdHooks;
import com.devicespooflab.hooks.hooks.ContextHooks;
import com.devicespooflab.hooks.hooks.BuildHooks;
import com.devicespooflab.hooks.hooks.DisplayHooks;
import com.devicespooflab.hooks.hooks.EmulatorDetectionHooks;
import com.devicespooflab.hooks.hooks.GetPropHooks;
import com.devicespooflab.hooks.hooks.HardwareHooks;
import com.devicespooflab.hooks.hooks.JavaSystemPropertyHooks;
import com.devicespooflab.hooks.hooks.MediaDrmHooks;
import com.devicespooflab.hooks.hooks.PackageManagerHooks;
import com.devicespooflab.hooks.hooks.SettingsHooks;
import com.devicespooflab.hooks.hooks.SystemPropertiesHooks;
import com.devicespooflab.hooks.hooks.TelephonyHooks;
import com.devicespooflab.hooks.hooks.VendorSystemPropertiesHooks;
import com.devicespooflab.hooks.hooks.WebViewHooks;
import com.devicespooflab.hooks.utils.ConfigManager;
import com.devicespooflab.hooks.profile.PerAppSettings;
import com.devicespooflab.hooks.profile.ProfileManager;
import com.devicespooflab.hooks.diagnostics.HookDiagnostics;
import com.devicespooflab.hooks.diagnostics.AndroidVersionCompat;

import com.devicespooflab.hooks.hooks.impl.LocationHooks;
import com.devicespooflab.hooks.hooks.impl.SubscriptionHooks;
import com.devicespooflab.hooks.hooks.impl.WifiInfoHooks;
import com.devicespooflab.hooks.hooks.impl.MacAddressHooks;
import com.devicespooflab.hooks.hooks.impl.SensorHooks;
import com.devicespooflab.hooks.hooks.impl.PackageVisibilityHooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Main entry point for DeviceSpoofLabs-Hooks LSPosed module.
 *
 * STANDALONE MODULE - No Magisk dependency.
 *
 * This module hooks Android APIs to spoof device identifiers and properties
 * that some apps read directly from Java APIs or SystemProperties.
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "SpoofMyDevice";
    private static final String MODULE_PACKAGE = "com.spoofmydevice";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log(TAG + ": Loading hooks for " + lpparam.packageName);

        if (MODULE_PACKAGE.equals(lpparam.packageName)) {
            hookActivationCheck(lpparam);
        }

        try {
            ConfigManager.init();
            HookDiagnostics.logHookSuccess("ConfigManager.init", lpparam.packageName, "Config initialized successfully");
        } catch (Exception exception) {
            HookDiagnostics.logHookFailed("ConfigManager.init", lpparam.packageName, exception.getMessage());
            exception.printStackTrace();
            return;
        }

        if (!PerAppSettings.isSpoofEnabledForPackage(lpparam.packageName)) {
            HookDiagnostics.logHookSkipped("AllHooks", lpparam.packageName, "Package not enabled for spoofing or is bypassed");
            return;
        }

        boolean isValid = ProfileManager.isProfileValid();
        HookDiagnostics.logDiagnosticState(lpparam.packageName, true, isValid, isValid ? "" : ProfileManager.getInvalidReason());

        if (!isValid) {
            HookDiagnostics.logHookSkipped("AllHooks", lpparam.packageName, "Profile validation failed: " + ProfileManager.getInvalidReason());
            return;
        }

        try {
            SystemPropertiesHooks.hook(lpparam);
            HookDiagnostics.logHookSuccess("SystemPropertiesHooks", lpparam.packageName, "Loaded successfully");
        } catch (Exception exception) {
            HookDiagnostics.logHookFailed("SystemPropertiesHooks", lpparam.packageName, exception.getMessage());
        }

        try {
            VendorSystemPropertiesHooks.hook(lpparam);
            HookDiagnostics.logHookSuccess("VendorSystemPropertiesHooks", lpparam.packageName, "Loaded successfully");
        } catch (Exception exception) {
            HookDiagnostics.logHookFailed("VendorSystemPropertiesHooks", lpparam.packageName, exception.getMessage());
        }

        try {
            JavaSystemPropertyHooks.hook(lpparam);
            HookDiagnostics.logHookSuccess("JavaSystemPropertyHooks", lpparam.packageName, "Loaded successfully");
        } catch (Exception exception) {
            HookDiagnostics.logHookFailed("JavaSystemPropertyHooks", lpparam.packageName, exception.getMessage());
        }

        try {
            GetPropHooks.hook(lpparam);
            HookDiagnostics.logHookSuccess("GetPropHooks", lpparam.packageName, "Loaded successfully");
        } catch (Exception exception) {
            HookDiagnostics.logHookFailed("GetPropHooks", lpparam.packageName, exception.getMessage());
        }

        try {
            BuildHooks.hook(lpparam);
            HookDiagnostics.logHookSuccess("BuildHooks", lpparam.packageName, "Loaded successfully");
        } catch (Exception exception) {
            HookDiagnostics.logHookFailed("BuildHooks", lpparam.packageName, exception.getMessage());
        }

        hookApplicationAttachForReload(lpparam);
        hookActivityLifecycleForReapply(lpparam);

        try {
            HardwareHooks.hook(lpparam);
            HookDiagnostics.logHookSuccess("HardwareHooks", lpparam.packageName, "Loaded successfully");
        } catch (Exception exception) {
            HookDiagnostics.logHookFailed("HardwareHooks", lpparam.packageName, exception.getMessage());
        }

        try {
            DisplayHooks.hook(lpparam);
            HookDiagnostics.logHookSuccess("DisplayHooks", lpparam.packageName, "Loaded successfully");
        } catch (Exception exception) {
            HookDiagnostics.logHookFailed("DisplayHooks", lpparam.packageName, exception.getMessage());
        }

        try {
            EmulatorDetectionHooks.hook(lpparam);
            HookDiagnostics.logHookSuccess("EmulatorDetectionHooks", lpparam.packageName, "Loaded successfully");
        } catch (Exception exception) {
            HookDiagnostics.logHookFailed("EmulatorDetectionHooks", lpparam.packageName, exception.getMessage());
        }

        try {
            TelephonyHooks.hook(lpparam);
            HookDiagnostics.logHookSuccess("TelephonyHooks", lpparam.packageName, "Loaded successfully");
        } catch (Exception exception) {
            HookDiagnostics.logHookFailed("TelephonyHooks", lpparam.packageName, exception.getMessage());
        }

        try {
            SettingsHooks.hook(lpparam);
            HookDiagnostics.logHookSuccess("SettingsHooks", lpparam.packageName, "Loaded successfully");
        } catch (Exception exception) {
            HookDiagnostics.logHookFailed("SettingsHooks", lpparam.packageName, exception.getMessage());
        }

        try {
            AdvertisingIdHooks.hook(lpparam);
            HookDiagnostics.logHookSuccess("AdvertisingIdHooks", lpparam.packageName, "Loaded successfully");
        } catch (Exception exception) {
            HookDiagnostics.logHookFailed("AdvertisingIdHooks", lpparam.packageName, exception.getMessage());
        }

        if (android.os.Build.VERSION.SDK_INT >= 30) {
            try {
                AppSetIdHooks.hook(lpparam);
                HookDiagnostics.logHookSuccess("AppSetIdHooks", lpparam.packageName, "Loaded successfully");
            } catch (Exception exception) {
                HookDiagnostics.logHookFailed("AppSetIdHooks", lpparam.packageName, exception.getMessage());
            }
        } else {
            HookDiagnostics.logHookSkipped("AppSetIdHooks", lpparam.packageName, "SDK < 30");
        }

        try {
            MediaDrmHooks.hook(lpparam);
            HookDiagnostics.logHookSuccess("MediaDrmHooks", lpparam.packageName, "Loaded successfully");
        } catch (Exception exception) {
            HookDiagnostics.logHookFailed("MediaDrmHooks", lpparam.packageName, exception.getMessage());
        }

        try {
            WebViewHooks.hook(lpparam);
            HookDiagnostics.logHookSuccess("WebViewHooks", lpparam.packageName, "Loaded successfully");
        } catch (Exception exception) {
            HookDiagnostics.logHookFailed("WebViewHooks", lpparam.packageName, exception.getMessage());
        }

        try {
            PackageManagerHooks.hook(lpparam);
            HookDiagnostics.logHookSuccess("PackageManagerHooks", lpparam.packageName, "Loaded successfully");
        } catch (Exception exception) {
            HookDiagnostics.logHookFailed("PackageManagerHooks", lpparam.packageName, exception.getMessage());
        }

        try {
            ContextHooks.hook(lpparam);
            HookDiagnostics.logHookSuccess("ContextHooks", lpparam.packageName, "Loaded successfully");
        } catch (Exception exception) {
            HookDiagnostics.logHookFailed("ContextHooks", lpparam.packageName, exception.getMessage());
        }

        // New Implementation Hooks / Audit Placeholders
        LocationHooks.hook(lpparam);
        SubscriptionHooks.hook(lpparam);
        WifiInfoHooks.hook(lpparam);
        MacAddressHooks.hook(lpparam);
        SensorHooks.hook(lpparam);
        PackageVisibilityHooks.hook(lpparam);

        XposedBridge.log(TAG + ": All hooks initialized for " + lpparam.packageName + " Limitations: " + AndroidVersionCompat.getKnownLimitations());
    }

    private void hookApplicationAttachForReload(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.Application",
                null,
                "attach",
                android.content.Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            android.content.Context context = (android.content.Context) param.args[0];
                            ConfigManager.forceReload(context);
                            BuildHooks.reapply(((android.content.Context) param.args[0]).getClassLoader(), lpparam.packageName);
                            XposedBridge.log(TAG + ": Config reloaded after Application.attach for " + lpparam.packageName);
                        } catch (Throwable throwable) {
                            XposedBridge.log(TAG + ": Failed to reload config after attach for " + lpparam.packageName + ": " + throwable.getMessage());
                        }
                    }
                }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook Application.attach reload path: " + throwable.getMessage());
        }
    }

    private void hookActivityLifecycleForReapply(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.Activity",
                null,
                "onCreate",
                android.os.Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            if (!ConfigManager.isUsingEmbeddedDefaults()) {
                                return;
                            }
                            android.app.Activity activity = (android.app.Activity) param.thisObject;
                            ConfigManager.forceReload(activity);
                            BuildHooks.reapply(activity.getClassLoader(), lpparam.packageName);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            );
        } catch (Throwable ignored) {
        }
    }

    private void hookActivationCheck(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                MainActivity.class.getName(),
                lpparam.classLoader,
                "isModuleActivated",
                XC_MethodReplacement.returnConstant(true)
            );
            XposedBridge.log(TAG + ": Activation check hooked for companion app");
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook activation check: " + throwable.getMessage());
        }
    }
}
