package com.devicespooflab.hooks.profile;

import com.devicespooflab.hooks.utils.ConfigManager;
import com.devicespooflab.hooks.diagnostics.HookDiagnostics;

/**
 * Manages the active spoof profiles and logic around them.
 * Architecture layer inspired by Tiger.
 */
public class ProfileManager {

    private static String invalidReason = null;

    public static boolean isProfileValid() {
        invalidReason = null;

        String brand = ConfigManager.getBuildBrand();
        String model = ConfigManager.getBuildModel();
        String fingerprint = ConfigManager.getBuildFingerprint();

        if (fingerprint == null || fingerprint.trim().isEmpty()) {
            invalidReason = "Fingerprint is missing or empty";
            return false;
        }

        if (brand != null && !brand.isEmpty() && !fingerprint.toLowerCase().contains(brand.toLowerCase().replace(" ", ""))) {
            invalidReason = "Fingerprint does not match brand logically";
            return false;
        }

        String[] parts = fingerprint.split("/");
        if (parts.length < 3) {
            invalidReason = "Fingerprint format is invalid (missing standard dividers)";
            return false;
        }

        int sdk = ConfigManager.getBuildVersionSdk();
        if (sdk > 0 && sdk < 26) {
            invalidReason = "Spoofed SDK version must be >= 26 for compatibility";
            return false;
        }

        return true;
    }

    public static String getInvalidReason() {
        return invalidReason != null ? invalidReason : "Unknown";
    }
}
