package com.devicespooflab.hooks.security;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public final class RemoteSchemaValidator {
    private static final Pattern SAFE = Pattern.compile("^[\\w .:/+-]{0,200}$");

    private RemoteSchemaValidator() {}

    @NonNull
    public static List<String> validate(@NonNull JSONObject json) {
        List<String> errors = new ArrayList<>();
        if (!json.has("sdk_int") || !(json.opt("sdk_int") instanceof Number)) {
            errors.add("sdk_int wajib ada dan bertipe integer");
        }
        sanitizeStrings(json, errors);
        return errors;
    }

    private static void sanitizeStrings(JSONObject json, List<String> errors) {
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = json.opt(key);
            if (value instanceof String && !SAFE.matcher((String) value).matches()) {
                errors.add("Nilai berbahaya terdeteksi pada key: " + key);
            }
        }
    }
}
