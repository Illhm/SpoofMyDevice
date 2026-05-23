package com.devicespooflab.hooks.data;

import android.util.Log;

import java.util.Locale;

public class ProfileValidator {

    private static final String TAG = "SpoofMyDevice-Validator";

    public static boolean validate(DeviceProfile profile) {
        if (profile == null) {
            Log.e(TAG, "Profile is null");
            return false;
        }

        // Check required fields
        if (isEmpty(profile.getManufacturer()) || isEmpty(profile.getModel())) {
            Log.e(TAG, "Missing manufacturer or model");
            return false;
        }

        // Example consistency check: Samsung devices usually use Exynos or specific Qualcomm boards
        String manufacturer = profile.getManufacturer().toLowerCase(Locale.US);
        String board = profile.getBoard() != null ? profile.getBoard().toLowerCase(Locale.US) : "";
        if (manufacturer.contains("samsung")) {
            if (board.contains("pixel") || board.contains("tensor")) {
                Log.w(TAG, "Inconsistent: Samsung manufacturer with Pixel/Tensor board");
                // We'll just warn, not fail the validation, but this shows capability
            }
        }

        // Basic ABI checks
        String abi = profile.getCpuAbi();
        if (isEmpty(abi)) {
            Log.e(TAG, "Missing primary CPU ABI");
            return false;
        }

        return true;
    }

    private static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}