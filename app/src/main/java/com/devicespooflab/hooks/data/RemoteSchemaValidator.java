package com.devicespooflab.hooks.data;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class RemoteSchemaValidator {
    private RemoteSchemaValidator() {}

    public static ValidationResult validate(JSONObject presetJson) {
        List<String> errors = new ArrayList<>();
        if (presetJson == null) {
            errors.add("Preset JSON kosong");
            return new ValidationResult(false, errors);
        }
        requireString(presetJson, "id", errors);
        requireString(presetJson, "brandLabel", errors);
        requireString(presetJson, "modelLabel", errors);

        JSONObject profile = presetJson.optJSONObject("profile");
        if (profile == null) {
            errors.add("profile object wajib ada");
            return new ValidationResult(false, errors);
        }

        requireString(profile, "brand", errors);
        requireString(profile, "manufacturer", errors);
        requireString(profile, "model", errors);
        requireString(profile, "fingerprint", errors);
        requireNumber(profile, "sdk", errors);
        requireString(profile, "android_release", errors);
        requireNumber(profile, "screen_width", errors);
        requireNumber(profile, "screen_height", errors);
        requireNumber(profile, "screen_density", errors);

        int sdk = profile.optInt("sdk", 0);
        String release = profile.optString("android_release", "");
        if (sdk >= 36 && !"16".equals(release)) {
            errors.add("sdk >= 36 harus android_release = 16");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    private static void requireString(JSONObject json, String key, List<String> errors) {
        if (!json.has(key) || json.optString(key, "").trim().isEmpty()) {
            errors.add("Key string wajib: " + key);
        }
    }

    private static void requireNumber(JSONObject json, String key, List<String> errors) {
        if (!json.has(key) || !(json.opt(key) instanceof Number)) {
            errors.add("Key number wajib: " + key);
        }
    }

    public static class ValidationResult {
        public final boolean valid;
        public final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }
    }
}
