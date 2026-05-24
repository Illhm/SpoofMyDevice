package com.devicespooflab.hooks;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.devicespooflab.hooks.utils.ConfigManager;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;

public class AndroidIdSettingsActivity extends AppCompatActivity {
    private static final String GLOBAL = "GLOBAL";
    private final SecureRandom random = new SecureRandom();

    private AutoCompleteTextView targetView;
    private EditText androidIdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_android_id_settings);

        targetView = findViewById(R.id.targetPackageInput);
        androidIdView = findViewById(R.id.androidIdInput);
        Button randomButton = findViewById(R.id.generateRandomButton);
        Button saveButton = findViewById(R.id.saveButton);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
            new String[]{GLOBAL, "com.example.app"});
        targetView.setAdapter(adapter);
        targetView.setText(GLOBAL, false);

        androidIdView.setText(ConfigManager.getAndroidId());
        randomButton.setOnClickListener(v -> androidIdView.setText(generateRandomAndroidId()));
        saveButton.setOnClickListener(v -> saveValue());
    }

    private void saveValue() {
        String target = targetView.getText() == null ? GLOBAL : targetView.getText().toString().trim();
        String value = androidIdView.getText() == null ? "" : androidIdView.getText().toString().trim().toLowerCase();
        if (!isValid(value)) {
            Toast.makeText(this, "ANDROID_ID must be 16 hex chars", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, String> updates = new LinkedHashMap<>();
        if (TextUtils.isEmpty(target) || GLOBAL.equalsIgnoreCase(target)) {
            updates.put("ANDROID_ID", value);
        } else {
            updates.put("android_id." + target, value);
        }
        ConfigManager.updateConfig(this, updates);
        ConfigManager.forceReload(this);
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
    }

    private String generateRandomAndroidId() {
        byte[] bytes = new byte[8];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(16);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private boolean isValid(String value) {
        if (value == null || value.length() != 16) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return false;
        }
        return true;
    }
}
