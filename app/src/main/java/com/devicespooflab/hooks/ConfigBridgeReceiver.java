package com.devicespooflab.hooks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.devicespooflab.hooks.security.CallerVerifier;
import com.devicespooflab.hooks.security.RedactedLogger;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

public class ConfigBridgeReceiver extends BroadcastReceiver {

    private static final String TAG = "ConfigBridgeReceiver";
    public static final String ACTION_GET_CONFIG = "com.spoofmydevice.action.GET_CONFIG";
    public static final String EXTRA_CONTENT = "content";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null || !ACTION_GET_CONFIG.equals(intent.getAction())) {
            return;
        }

        try {
            CallerVerifier.enforceTrustedCaller(context);
            File configFile = new File(context.getFilesDir(), ConfigProvider.FILE_NAME);
            if (!configFile.exists()) {
                return;
            }
            byte[] bytes = new byte[(int) configFile.length()];
            try (FileInputStream inputStream = new FileInputStream(configFile)) {
                int read = inputStream.read(bytes);
                String content = read <= 0 ? "" : new String(bytes, 0, read, StandardCharsets.UTF_8);
                Bundle extras = getResultExtras(true);
                if (extras != null) {
                    extras.putString(EXTRA_CONTENT, content);
                    setResultExtras(extras);
                }
            }
        } catch (Throwable error) {
            RedactedLogger.w(TAG, "Bridge receiver rejected/failed request", error);
        }
    }
}
