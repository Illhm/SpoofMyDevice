package com.devicespooflab.hooks.data;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.regex.Pattern;

public class RemoteSchemaValidator {

    // Allow basic alphanumeric and dot/dash/underscore strings, reject shell metacharacters
    private static final Pattern SAFE_STRING_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-\\.\\s:/,]+$");

    public static boolean validatePreset(JSONObject presetJson, int currentModuleVersion) {
        try {
            // Validate compatibility
            if (presetJson.has("min_module_version")) {
                int requiredVersion = presetJson.getInt("min_module_version");
                if (currentModuleVersion < requiredVersion) {
                    return false;
                }
            }

            // Schema checks: Required fields
            if (!presetJson.has("model") || !presetJson.has("manufacturer")) {
                return false;
            }

            // Sanitize all string inputs to prevent command injection when values are used in shell commands
            Iterator<String> keys = presetJson.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = presetJson.get(key);
                if (value instanceof String) {
                    String strValue = (String) value;
                    if (!strValue.isEmpty() && !SAFE_STRING_PATTERN.matcher(strValue).matches()) {
                        // Unsafe character detected
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}