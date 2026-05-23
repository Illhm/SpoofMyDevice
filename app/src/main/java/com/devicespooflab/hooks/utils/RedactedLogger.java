package com.devicespooflab.hooks.utils;

import android.util.Log;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class RedactedLogger {
    private static final String TAG = "SpoofDiag";

    private RedactedLogger() {}

    public static void d(String message, Map<String, String> payload) {
        Map<String, String> safePayload = new LinkedHashMap<>();
        if (payload != null) {
            for (Map.Entry<String, String> entry : payload.entrySet()) {
                safePayload.put(entry.getKey(), redactByKey(entry.getKey(), entry.getValue()));
            }
        }
        Log.d(TAG, message + " | " + safePayload);
    }

    public static String redactByKey(String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String lower = key == null ? "" : key.toLowerCase(Locale.US);
        if (lower.contains("imei") || lower.contains("imsi") || lower.contains("iccid")
            || lower.contains("gaid") || lower.contains("gsf") || lower.contains("media_drm")) {
            return maskKeepPrefix(value, 4);
        }
        if (lower.contains("mac")) {
            return maskMac(value);
        }
        return value;
    }

    private static String maskKeepPrefix(String input, int keep) {
        if (input.length() <= keep) {
            return "****";
        }
        StringBuilder sb = new StringBuilder(input.substring(0, keep));
        for (int i = keep; i < input.length(); i++) {
            sb.append('*');
        }
        return sb.toString();
    }

    private static String maskMac(String mac) {
        String[] chunks = mac.split(":");
        if (chunks.length < 6) {
            return "**:**:**:**:**:**";
        }
        return chunks[0] + ":" + chunks[1] + ":" + chunks[2] + ":**:**:**";
    }
}
