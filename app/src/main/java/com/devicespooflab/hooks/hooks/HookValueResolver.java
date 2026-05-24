package com.devicespooflab.hooks.hooks;

import android.text.TextUtils;

import com.devicespooflab.hooks.utils.ConfigManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XposedBridge;

final class HookValueResolver {
    private static final String TAG = "DeviceSpoofLab-Resolver";
    private static final Set<String> SENSITIVE = Set.of("imei", "imsi", "iccid", "gaid", "media_drm", "drm", "android_id", "app_set_id");

    private final String packageName;

    private HookValueResolver(String packageName) { this.packageName = packageName; }

    static HookValueResolver forPackage(String packageName) {
        return new HookValueResolver(packageName);
    }

    boolean isSpoofEnabled(String fieldId) {
        return !ConfigManager.isUsingEmbeddedDefaults() && ConfigManager.isSpoofEnabled(fieldId);
    }


    String resolveAndroidId(String original) {
        if (ConfigManager.isUsingEmbeddedDefaults()) return original;
        String perApp = null;
        if (!TextUtils.isEmpty(packageName)) {
            perApp = ConfigManager.getSystemProperty("android_id." + packageName, null);
        }
        String candidate = !TextUtils.isEmpty(perApp) ? perApp : ConfigManager.getAndroidId();
        if (!isValidAndroidId(candidate)) return original;
        logModified("android_id", candidate, original);
        return candidate;
    }
    String resolveSystemProperty(String key, String original) {
        if (TextUtils.isEmpty(key) || ConfigManager.isUsingEmbeddedDefaults()) {
            return original;
        }
        String resolved = ConfigManager.getSystemProperty(key, original);
        logModified(key, resolved, original);
        return resolved;
    }

    Map<String, String> spoofedSystemProperties() {
        if (ConfigManager.isUsingEmbeddedDefaults()) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(ConfigManager.getEffectiveSystemProperties());
    }

    String resolveBuildField(String fieldName, String toggleField, String original) {
        if (!isSpoofEnabled(toggleField)) return original;
        String v;
        switch (fieldName) {
            case "BRAND": v = ConfigManager.getBuildBrand(); break;
            case "MANUFACTURER": v = ConfigManager.getBuildManufacturer(); break;
            case "MODEL": v = ConfigManager.getBuildModel(); break;
            case "DEVICE": v = ConfigManager.getBuildDevice(); break;
            case "PRODUCT": v = ConfigManager.getBuildProduct(); break;
            case "BOARD": v = ConfigManager.getBuildBoard(); break;
            case "HARDWARE": v = ConfigManager.getBuildHardware(); break;
            case "FINGERPRINT": v = validatedFingerprint(original); break;
            case "ID": v = ConfigManager.getBuildId(); break;
            case "DISPLAY": v = ConfigManager.getBuildDisplay(); break;
            case "TAGS": v = ConfigManager.getBuildTags(); break;
            case "TYPE": v = ConfigManager.getBuildType(); break;
            case "BOOTLOADER": v = ConfigManager.getBuildBootloader(); break;
            case "SERIAL": v = ConfigManager.getSerial(); break;
            default: v = original;
        }
        return TextUtils.isEmpty(v) ? original : v;
    }

    int resolveSdkInt(int original) {
        if (!isSpoofEnabled(ConfigManager.FIELD_SDK)) return original;
        int sdk = ConfigManager.getBuildVersionSdk();
        return sdk > 0 ? sdk : original;
    }

    String resolveVersionField(String field, String original) {
        switch (field) {
            case "RELEASE":
                return isSpoofEnabled(ConfigManager.FIELD_ANDROID_RELEASE)
                    ? fallback(ConfigManager.getBuildVersionRelease(), original) : original;
            case "CODENAME":
                return isSpoofEnabled(ConfigManager.FIELD_ANDROID_RELEASE)
                    ? fallback(ConfigManager.getBuildVersionCodename(), original) : original;
            case "INCREMENTAL":
                return isSpoofEnabled(ConfigManager.FIELD_BUILD_INCREMENTAL)
                    ? fallback(ConfigManager.getBuildVersionIncremental(), original) : original;
            case "SECURITY_PATCH":
                return isSpoofEnabled(ConfigManager.FIELD_SECURITY_PATCH)
                    ? validateSecurityPatch(ConfigManager.getBuildVersionSecurityPatch(), original) : original;
            default:
                return original;
        }
    }

    String[] resolveSupportedAbis(String[] original) {
        String[] configured = splitCsv(ConfigManager.getCpuAbiList());
        return configured.length == 0 ? original : configured;
    }

    String resolveCpuAbi(String original) {
        return fallback(ConfigManager.getCpuAbi(), original);
    }

    String resolveCpuAbi2(String original) {
        String[] abi32 = splitCsv(ConfigManager.getCpuAbiList32());
        if (abi32.length >= 2) return abi32[1];
        if (abi32.length == 1) return abi32[0];
        return original;
    }

    private String validatedFingerprint(String original) {
        String configured = ConfigManager.getBuildFingerprint();
        if (!TextUtils.isEmpty(configured) && configured.split(":").length >= 2) return configured.trim();
        String brand = fallback(ConfigManager.getBuildBrand(), null);
        String product = fallback(ConfigManager.getBuildProduct(), null);
        String device = fallback(ConfigManager.getBuildDevice(), null);
        String release = fallback(ConfigManager.getBuildVersionRelease(), null);
        String id = fallback(ConfigManager.getBuildId(), null);
        String incremental = fallback(ConfigManager.getBuildVersionIncremental(), null);
        String type = fallback(ConfigManager.getBuildType(), "user");
        String tags = fallback(ConfigManager.getBuildTags(), "release-keys");
        if (brand == null || product == null || device == null || release == null || id == null || incremental == null) {
            return original;
        }
        return brand + "/" + product + "/" + device + ":" + release + "/" + id + "/" + incremental + ":" + type + "/" + tags;
    }


    private static boolean isValidAndroidId(String value) {
        if (TextUtils.isEmpty(value) || value.length() != 16) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = Character.toLowerCase(value.charAt(i));
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return false;
        }
        return true;
    }

    private static String[] splitCsv(String input) {
        if (TextUtils.isEmpty(input)) return new String[0];
        String[] p = input.split(",");
        int n = 0;
        for (int i = 0; i < p.length; i++) {
            String t = p[i] == null ? "" : p[i].trim();
            if (!t.isEmpty()) p[n++] = t;
        }
        if (n == 0) return new String[0];
        String[] out = new String[n];
        System.arraycopy(p, 0, out, 0, n);
        return out;
    }

    private static String fallback(String v, String d) { return TextUtils.isEmpty(v) ? d : v; }

    private static String validateSecurityPatch(String candidate, String original) {
        if (TextUtils.isEmpty(candidate)) return original;
        try {
            new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(candidate);
            return candidate;
        } catch (ParseException e) {
            return original;
        }
    }

    private static void logModified(String key, String value, String original) {
        if (TextUtils.equals(value, original)) return;
        String lower = key.toLowerCase(Locale.US);
        for (String token : SENSITIVE) {
            if (lower.contains(token)) {
                XposedBridge.log(TAG + ": modified " + key + "=[REDACTED]");
                return;
            }
        }
        XposedBridge.log(TAG + ": modified " + key + "=" + value);
    }
}
