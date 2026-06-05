package com.devicespooflab.hooks.profile;

import com.devicespooflab.hooks.utils.ConfigManager;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages per-package settings and enablement.
 * Architecture layer inspired by Tiger.
 */
public class PerAppSettings {

    // For now we check if package is activated via ConfigManager
    public static boolean isSpoofEnabledForPackage(String packageName) {
        // If it's a critical system package or specifically bypassed
        if (ConfigManager.shouldBypassVersionSpoof(packageName)) {
            return false;
        }

        // Return true as default if not bypassed.
        // Real implementation would check specific app settings from ConfigManager/ContentProvider.
        return true;
    }
}
