package com.devicespooflab.hooks.hooks;

import android.util.Log;
import com.devicespooflab.hooks.utils.ConfigManager;

public class HookSelfCheck {
    private static final String TAG = "DeviceSpoofLab-SelfCheck";

    public static void runSelfCheck() {
        Log.d(TAG, "Running hook self check...");

        // 1. Cross-field validation (Build vs SystemProperties)
        String buildDevice = HookProfileResolver.resolveString(ConfigManager.FIELD_DEVICE, ConfigManager.getBuildDevice());
        String propDevice = HookProfileResolver.resolveString(ConfigManager.FIELD_DEVICE, ConfigManager.getSystemProperty("ro.product.device", null));
        if (buildDevice != null && propDevice != null && !buildDevice.equals(propDevice)) {
            Log.w(TAG, "Mismatch: Build.DEVICE (" + buildDevice + ") != ro.product.device (" + propDevice + ")");
        }

        // 2. Validate ABIs
        String osArch = System.getProperty("os.arch");
        String buildAbi = HookProfileResolver.resolveString("cpu_abi", ConfigManager.getCpuAbi());
        if (osArch != null && buildAbi != null && !osArch.contains(buildAbi) && !buildAbi.contains(osArch)) {
            Log.w(TAG, "Mismatch: os.arch (" + osArch + ") != Build.CPU_ABI (" + buildAbi + ")");
        }

        // 3. User-Agent
        String webViewUA = HookProfileResolver.resolveString("webview_user_agent", ConfigManager.getWebViewUserAgent());
        String httpAgent = System.getProperty("http.agent");
        if (webViewUA != null && httpAgent != null && !webViewUA.equals(httpAgent)) {
            Log.w(TAG, "Mismatch: WebView UA != http.agent");
        }


        // 4. Telephony
        String phoneCountStr = HookProfileResolver.resolveString("telephony_phone_count", ConfigManager.getSystemProperty("telephony_phone_count", null));
        if (phoneCountStr != null) {
             try {
                 int phoneCount = Integer.parseInt(phoneCountStr);
                 if (phoneCount > 2) {
                     Log.w(TAG, "Telephony phoneCount is unusually high: " + phoneCount);
                 }
             } catch (Exception e) {}
        }

        // 5. Hardware Info
        String ramStr = HookProfileResolver.resolveString("hardware.ram.bytes", ConfigManager.getSystemProperty("hardware.ram.bytes", null));
        String coresStr = HookProfileResolver.resolveString("hardware.cpu.cores", ConfigManager.getSystemProperty("hardware.cpu.cores", null));

        if (ramStr != null) {
            try {
                long ram = Long.parseLong(ramStr);
                if (ram < 1024 * 1024 * 1024L) {
                    Log.w(TAG, "Configured RAM is suspiciously low: " + ram);
                }
            } catch (Exception e) {}
        }

        if (coresStr != null) {
            try {
                int cores = Integer.parseInt(coresStr);
                if (cores < 1 || cores > 128) {
                    Log.w(TAG, "Configured CPU cores is out of bounds: " + cores);
                }
            } catch (Exception e) {}
        }

        Log.d(TAG, "Self check completed.");
}
}