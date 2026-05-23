package com.devicespooflab.hooks.security;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RedactedLogger {

    private static final Pattern IMEI_PATTERN = Pattern.compile("\\b\\d{14,16}\\b");
    private static final Pattern IMSI_PATTERN = Pattern.compile("\\b\\d{15}\\b");
    private static final Pattern GAID_PATTERN = Pattern.compile("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b");

    private RedactedLogger() {
    }

    @NonNull
    public static String sanitize(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String sanitized = replaceImei(input);
        sanitized = IMSI_PATTERN.matcher(sanitized).replaceAll("<redacted-imsi>");
        sanitized = GAID_PATTERN.matcher(sanitized).replaceAll("<redacted-gaid>");
        return sanitized;
    }

    public static void d(@NonNull String tag, @Nullable String message) {
        Log.d(tag, sanitize(message));
    }

    public static void w(@NonNull String tag, @Nullable String message, @Nullable Throwable throwable) {
        Log.w(tag, sanitize(message), throwable);
    }

    @NonNull
    private static String replaceImei(@NonNull String input) {
        Matcher matcher = IMEI_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String value = matcher.group();
            String masked;
            if (value.length() > 10) {
                masked = value.substring(0, value.length() - 10) + "**********";
            } else {
                masked = "**********";
            }
            matcher.appendReplacement(sb, masked);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
