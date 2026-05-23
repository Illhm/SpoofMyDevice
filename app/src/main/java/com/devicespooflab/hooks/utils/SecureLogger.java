package com.devicespooflab.hooks.utils;

import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SecureLogger {
    private static final String TAG = "SpoofMyDevice";

    // Patterns for sensitive identifiers
    private static final Pattern IMEI_PATTERN = Pattern.compile("(?i)(imei|meid|esn)\\s*[:=]\\s*([0-9a-f]{14,15})");
    private static final Pattern IMSI_PATTERN = Pattern.compile("(?i)(imsi)\\s*[:=]\\s*([0-9]{15})");
    private static final Pattern ICCID_PATTERN = Pattern.compile("(?i)(iccid)\\s*[:=]\\s*([0-9a-f]{19,20})");
    private static final Pattern MAC_PATTERN = Pattern.compile("([0-9a-fA-F]{2}[:-]){5}([0-9a-fA-F]{2})");
    private static final Pattern GAID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    public static void i(String message) {
        Log.i(TAG, redact(message));
    }

    public static void d(String message) {
        Log.d(TAG, redact(message));
    }

    public static void w(String message) {
        Log.w(TAG, redact(message));
    }

    public static void w(String message, Throwable t) {
        Log.w(TAG, redact(message), t);
    }

    public static void e(String message) {
        Log.e(TAG, redact(message));
    }

    public static void e(String message, Throwable t) {
        Log.e(TAG, redact(message), t);
    }

    private static String redact(String message) {
        if (message == null) {
            return null;
        }

        String redacted = message;

        // Redact IMEI/MEID (keep first 4, mask rest)
        Matcher imeiMatcher = IMEI_PATTERN.matcher(redacted);
        StringBuffer sb = new StringBuffer();
        while (imeiMatcher.find()) {
            String key = imeiMatcher.group(1);
            String val = imeiMatcher.group(2);
            String masked = val.substring(0, Math.min(4, val.length())) + "**********";
            imeiMatcher.appendReplacement(sb, key + "=" + masked);
        }
        imeiMatcher.appendTail(sb);
        redacted = sb.toString();

        // Redact IMSI (fully)
        Matcher imsiMatcher = IMSI_PATTERN.matcher(redacted);
        sb = new StringBuffer();
        while (imsiMatcher.find()) {
            String key = imsiMatcher.group(1);
            imsiMatcher.appendReplacement(sb, key + "=[REDACTED]");
        }
        imsiMatcher.appendTail(sb);
        redacted = sb.toString();

        // Redact ICCID (fully)
        Matcher iccidMatcher = ICCID_PATTERN.matcher(redacted);
        sb = new StringBuffer();
        while (iccidMatcher.find()) {
            String key = iccidMatcher.group(1);
            iccidMatcher.appendReplacement(sb, key + "=[REDACTED]");
        }
        iccidMatcher.appendTail(sb);
        redacted = sb.toString();

        // Redact MAC Address
        Matcher macMatcher = MAC_PATTERN.matcher(redacted);
        redacted = macMatcher.replaceAll("XX:XX:XX:XX:XX:XX");

        // Redact GAID
        Matcher gaidMatcher = GAID_PATTERN.matcher(redacted);
        redacted = gaidMatcher.replaceAll("********-****-****-****-************");

        return redacted;
    }
}