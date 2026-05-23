package com.devicespooflab.hooks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

public class ConfigBridgeReceiver extends BroadcastReceiver {

    public static final String ACTION_GET_CONFIG = "com.spoofmydevice.action.GET_CONFIG";
    public static final String EXTRA_CONTENT = "content";

    private boolean isCallerAuthorized(Context context) {
        if (context == null) {
            return false;
        }
        int callerUid = Binder.getCallingUid();
        int myUid = android.os.Process.myUid();
        if (callerUid == myUid) {
            return true;
        }

        int permissionCheck = context.checkCallingPermission("com.spoofmydevice.permission.READ_CONFIG");
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Log.e("SpoofMyDevice", "Caller does not have READ_CONFIG permission. UID: " + callerUid);
            return false;
        }

        PackageManager pm = context.getPackageManager();
        String[] packages = pm.getPackagesForUid(callerUid);
        if (packages != null && packages.length > 0) {
            for (String pkg : packages) {
                if (pm.checkSignatures(context.getPackageName(), pkg) == PackageManager.SIGNATURE_MATCH) {
                    return true;
                }
            }
        }
        Log.e("SpoofMyDevice", "Caller signature does not match. UID: " + callerUid);
        return false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null || !ACTION_GET_CONFIG.equals(intent.getAction())) {
            return;
        }
        if (!isCallerAuthorized(context)) {
            Log.e("SpoofMyDevice", "Unauthorized broadcast access to config");
            return;
        }
        try {
            File configFile = new File(context.getFilesDir(), ConfigProvider.FILE_NAME);
            if (!configFile.exists()) {
                return;
            }
            byte[] bytes = new byte[(int) configFile.length()];
            try (FileInputStream inputStream = new FileInputStream(configFile)) {
                int read = inputStream.read(bytes);
                String content = read <= 0 ? "" : new String(bytes, 0, read, StandardCharsets.UTF_8);
                Bundle extras = getResultExtras(true);
                extras.putString(EXTRA_CONTENT, content);
                setResultExtras(extras);
            }
        } catch (Throwable ignored) {
        }
    }
}
