package com.devicespooflab.hooks.hooks;

import android.util.Log;
import com.devicespooflab.hooks.utils.ConfigManager;

/**
 * Acts as the single source of truth for all hook values.
 * Loads and validates active profile configuration from ConfigManager.
 * Enforces field-level enable/disable toggles.
 * Provides a consistent, redacted debug log.
 */
public class HookProfileResolver {
    private static final String TAG = "DeviceSpoofLab-Resolver";

    public static String resolveString(String fieldId, String configValue) {
        if (!ConfigManager.isSpoofEnabled(fieldId)) {
            return null;
        }
        if (configValue == null || configValue.isEmpty()) {
            return null;
        }
        logModification(fieldId, configValue);
        return configValue;
    }

    public static boolean isFeatureEnabled(String featureKey) {
        return "true".equals(ConfigManager.getSystemProperty(featureKey, "false"));
    }

    public static void logModification(String fieldId, String value) {
        if (value == null) return;

        // Redact sensitive values
        if (fieldId.equals(ConfigManager.KEY_SPOOF_IMEI) ||
            fieldId.equals(ConfigManager.KEY_SPOOF_IMSI) ||
            fieldId.equals(ConfigManager.KEY_SPOOF_ICCID) ||
            fieldId.equals(ConfigManager.KEY_SPOOF_GAID) ||
            fieldId.equals(ConfigManager.KEY_SPOOF_MEDIA_DRM_ID)) {
            Log.d(TAG, "Spoofing " + fieldId + ": [REDACTED]");
        } else {
            Log.d(TAG, "Spoofing " + fieldId + ": " + value);
        }
    }
}
