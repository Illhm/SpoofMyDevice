package com.devicespooflab.hooks.utils;

import android.util.Log;

import com.devicespooflab.hooks.data.DeviceProfile;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class to synchronize device profiles across testing environments
 * by persisting configuration to a protected system file and restarting
 * Google Play Services via root access.
 */
public class RootSyncHelper {

    private static final String TAG = "RootSyncHelper";
    private static final String TARGET_FILE = "/data/adb/pif.prop";

    /**
     * Programmatically writes device profile data to /data/adb/pif.prop,
     * applies correct file permissions, and restarts Google Play Services
     * to activate the changes.
     *
     * @param profile The device profile containing properties to sync.
     * @return true if all root commands execute successfully, false otherwise.
     */
    public static boolean syncProfile(DeviceProfile profile) {
        if (profile == null) {
            Log.e(TAG, "DeviceProfile is null");
            return false;
        }

        // 1. Convert the DeviceProfile object into standard .prop file format.
        String propContent = buildPropContent(profile);

        // 2. Execute Root Pipeline sequentially and safely.
        // Command 1: Write file
        if (!runRootCommandWithInput("cat > " + TARGET_FILE, propContent)) {
            Log.e(TAG, "Failed to write properties to " + TARGET_FILE);
            return false;
        }

        // Command 2: Apply permissions
        if (!runRootCommand("chmod 644 " + TARGET_FILE)) {
            Log.e(TAG, "Failed to set permissions on " + TARGET_FILE);
            return false;
        }

        // Command 3: Restart Google Play Services
        if (!runRootCommand("am force-stop com.google.android.gms")) {
            Log.e(TAG, "Failed to force-stop com.google.android.gms");
            return false;
        }

        Log.d(TAG, "Successfully synced profile to " + TARGET_FILE + " and restarted GMS");
        return true;
    }

    private static boolean runRootCommand(String command) {
        return runRootCommandWithInput(command, null);
    }

    private static boolean runRootCommandWithInput(String command, String input) {
        Process process = null;
        DataOutputStream os = null;
        InputStream is = null;
        InputStream es = null;
        try {
            // Launch root shell specifically to run this one command securely
            process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});

            if (input != null) {
                os = new DataOutputStream(process.getOutputStream());
                os.writeBytes(input);
                os.flush();
                os.close();
                os = null; // Mark as closed
            }

            // Consume streams in separate threads to prevent deadlocks
            is = process.getInputStream();
            es = process.getErrorStream();

            StreamConsumer outputConsumer = new StreamConsumer(is, "OUTPUT");
            StreamConsumer errorConsumer = new StreamConsumer(es, "ERROR");

            outputConsumer.start();
            errorConsumer.start();

            // Wait for the process to finish
            int exitCode = process.waitFor();

            // Wait for consumers to finish reading
            outputConsumer.join();
            errorConsumer.join();

            if (exitCode != 0) {
                Log.e(TAG, "Command '" + command + "' failed with exit code " + exitCode);
                return false;
            }

            return true;
        } catch (InterruptedIOException e) {
            Log.e(TAG, "Root execution interrupted", e);
            Thread.currentThread().interrupt(); // Restore interrupted status
            return false;
        } catch (IOException e) {
            Log.e(TAG, "IO Error during root execution", e);
            return false;
        } catch (InterruptedException e) {
            Log.e(TAG, "Root execution interrupted", e);
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during root execution", e);
            return false;
        } finally {
            // 3. Ensure proper stream management
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ignored) {}
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {}
            }
            if (es != null) {
                try {
                    es.close();
                } catch (IOException ignored) {}
            }
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * Helper method to build the .prop file content from a DeviceProfile.
     */
    private static String buildPropContent(DeviceProfile profile) {
        // Use LinkedHashMap to preserve insertion order (optional, but looks cleaner)
        Map<String, String> props = new LinkedHashMap<>();

        // Extract relevant fields
        putIfNotEmpty(props, "MANUFACTURER", profile.getManufacturer());
        putIfNotEmpty(props, "BRAND", profile.getBrand());
        putIfNotEmpty(props, "MODEL", profile.getModel());
        putIfNotEmpty(props, "PRODUCT", profile.getProductName());
        putIfNotEmpty(props, "DEVICE", profile.getDeviceCode());
        putIfNotEmpty(props, "BOARD", profile.getBoard());
        putIfNotEmpty(props, "HARDWARE", profile.getHardware());
        putIfNotEmpty(props, "BOARD_PLATFORM", profile.getBoardPlatform());
        putIfNotEmpty(props, "FINGERPRINT", profile.getBuildFingerprint());
        putIfNotEmpty(props, "ID", profile.getBuildId());
        putIfNotEmpty(props, "DISPLAY_ID", profile.getBuildDisplayId());
        putIfNotEmpty(props, "INCREMENTAL", profile.getBuildIncremental());
        putIfNotEmpty(props, "RELEASE", profile.getBuildRelease());
        if (profile.getBuildSdk() > 0) {
            putIfNotEmpty(props, "SDK", String.valueOf(profile.getBuildSdk()));
        }
        putIfNotEmpty(props, "SECURITY_PATCH", profile.getSecurityPatch());

        // Add standard spoof flags as requested
        props.put("spoofBuild", "true");
        props.put("spoofProps", "true");

        // Format into KEY=value lines
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : props.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    private static void putIfNotEmpty(Map<String, String> map, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            map.put(key, value.trim());
        }
    }

    /**
     * Consumes input/error streams in a background thread to prevent deadlocks.
     */
    private static class StreamConsumer extends Thread {
        private final InputStream is;
        private final String type;

        public StreamConsumer(InputStream is, String type) {
            this.is = is;
            this.type = type;
        }

        @Override
        public void run() {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = br.readLine()) != null) {
                    if ("ERROR".equals(type)) {
                        Log.w(TAG, "su error: " + line);
                    } else {
                        Log.d(TAG, "su output: " + line);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading process stream", e);
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException ignored) {}
                }
            }
        }
    }
}
