package com.devicespooflab.hooks.diagnostics;

import android.os.Build;

/**
 * Android version compatibility layer for Android 10-16.
 * Inspired by Tiger Architecture.
 */
public class AndroidVersionCompat {

    /**
     * Checks if current SDK is compatible with reflection-based mutation.
     */
    public static boolean isReflectionAllowed() {
        return Build.VERSION.SDK_INT <= 35; // Android 15 and lower.
        // Android 16 (API 36) has stricter restrictions on hidden APIs and reflection.
    }

    /**
     * Checks if SystemProperties mutation is supported.
     */
    public static boolean isSystemPropertiesHookSupported() {
        return true;
    }

    /**
     * Returns known limitations for the current Android version.
     */
    public static String getKnownLimitations() {
        if (Build.VERSION.SDK_INT >= 35) { // Android 15+
            return "Android 15+ strict reflection and hidden API policies may block some Build field mutations. Using proxy/JNI fallback where possible.";
        }
        if (Build.VERSION.SDK_INT >= 30) {
            return "Android 11+ limits MAC address access and package visibility. Telephony APIs require specific permissions.";
        }
        return "Android 10: Standard compatibility.";
    }
}
