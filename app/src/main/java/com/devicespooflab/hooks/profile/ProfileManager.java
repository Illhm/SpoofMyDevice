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
        String manufacturer = ConfigManager.getBuildManufacturer();
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

        if (manufacturer != null && !manufacturer.isEmpty() && !fingerprint.toLowerCase().contains(manufacturer.toLowerCase().replace(" ", ""))) {
            invalidReason = "Fingerprint does not match manufacturer logically";
            return false;
        }

        String[] parts = fingerprint.split("/");
        if (parts.length < 3) {
            invalidReason = "Fingerprint format is invalid (missing standard dividers)";
            return false;
        }

        // Product/device/board/hardware consistency check
        String product = ConfigManager.getBuildProduct();
        String device = ConfigManager.getBuildDevice();
        String board = ConfigManager.getBuildBoard();
        String hardware = ConfigManager.getBuildHardware();

        if (product != null && device != null && !product.equals(device)) {
            HookDiagnostics.logHookSkipped("ProfileManager", "System", "Warning: product != device (" + product + " != " + device + ")");
        }

        if (board != null && hardware != null && board.isEmpty() && !hardware.isEmpty()) {
             invalidReason = "Hardware is defined but board is empty";
             return false;
        }

        int sdk = ConfigManager.getBuildVersionSdk();
        if (sdk > 0 && sdk < 26) {
            invalidReason = "Spoofed SDK version must be >= 26 for compatibility";
            return false;
        }

        String release = ConfigManager.getBuildVersionRelease();
        if (release != null && sdk > 0) {
            try {
                int releaseInt = Integer.parseInt(release.split("\\.")[0]);
                if (releaseInt == 10 && sdk != 29) { invalidReason = "Android 10 must be SDK 29"; return false; }
                if (releaseInt == 11 && sdk != 30) { invalidReason = "Android 11 must be SDK 30"; return false; }
                if (releaseInt == 12 && sdk != 31 && sdk != 32) { invalidReason = "Android 12 must be SDK 31/32"; return false; }
                if (releaseInt == 13 && sdk != 33) { invalidReason = "Android 13 must be SDK 33"; return false; }
                if (releaseInt == 14 && sdk != 34) { invalidReason = "Android 14 must be SDK 34"; return false; }
                if (releaseInt == 15 && sdk != 35) { invalidReason = "Android 15 must be SDK 35"; return false; }
            } catch (NumberFormatException ignored) {}
        }

        // Format checks for Mac, Android ID, IMEI
        String macAddress = ConfigManager.getWifiMacAddress();
        if (macAddress != null && !macAddress.isEmpty()) {
            if (!macAddress.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")) {
                invalidReason = "Invalid MAC Address format";
                return false;
            }
        }

        String androidId = ConfigManager.getAndroidId();
        if (androidId != null && !androidId.isEmpty()) {
            if (!androidId.matches("^[0-9a-fA-F]{16}$")) {
                invalidReason = "Invalid Android ID format (must be 16 hex chars)";
                return false;
            }
        }

        String imei = ConfigManager.getIMEI();
        if (imei != null && !imei.isEmpty()) {
            if (!imei.matches("^[0-9]{15}$")) {
                invalidReason = "Invalid IMEI format (must be 15 digits)";
                return false;
            }
        }

        return true;
    }

    public static String getInvalidReason() {
        return invalidReason != null ? invalidReason : "Unknown";
    }
}
