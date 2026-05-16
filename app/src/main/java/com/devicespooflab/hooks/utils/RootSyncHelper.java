package com.devicespooflab.hooks.utils;

import com.devicespooflab.hooks.data.DeviceProfile;

import java.io.DataOutputStream;
import java.io.IOException;

public class RootSyncHelper {

    public static boolean syncProfile(DeviceProfile profile) {
        if (profile == null) {
            return false;
        }

        StringBuilder propContent = new StringBuilder();
        propContent.append("FINGERPRINT=").append(profile.getBuildFingerprint() != null ? profile.getBuildFingerprint() : "").append("\n");
        propContent.append("MANUFACTURER=").append(profile.getManufacturer() != null ? profile.getManufacturer() : "").append("\n");
        propContent.append("BRAND=").append(profile.getBrand() != null ? profile.getBrand() : "").append("\n");
        propContent.append("MODEL=").append(profile.getModel() != null ? profile.getModel() : "").append("\n");
        propContent.append("PRODUCT=").append(profile.getProductName() != null ? profile.getProductName() : "").append("\n");
        propContent.append("DEVICE=").append(profile.getDeviceCode() != null ? profile.getDeviceCode() : "").append("\n");
        propContent.append("RELEASE=").append(profile.getBuildRelease() != null ? profile.getBuildRelease() : "").append("\n");
        propContent.append("ID=").append(profile.getBuildId() != null ? profile.getBuildId() : "").append("\n");
        propContent.append("INCREMENTAL=").append(profile.getBuildIncremental() != null ? profile.getBuildIncremental() : "").append("\n");
        propContent.append("SECURITY_PATCH=").append(profile.getSecurityPatch() != null ? profile.getSecurityPatch() : "").append("\n");

        // Include the necessary boolean spoof flags
        propContent.append("spoofBuild=true\n");
        propContent.append("spoofProps=true\n");
        propContent.append("spoofProvider=true\n");
        propContent.append("spoofSignature=true\n");
        propContent.append("spoofVendingBuild=true\n");
        propContent.append("spoofVendingSdk=false\n");
        propContent.append("DEBUG=false\n");

        Process process = null;
        DataOutputStream os = null;
        boolean writeSuccess = false;

        try {
            // Uses Runtime.getRuntime().exec("su") to write this multi-line string directly to /data/adb/pif.prop.
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());

            os.writeBytes("cat << 'EOF' > /data/adb/pif.prop\n");
            os.writeBytes(propContent.toString());
            os.writeBytes("EOF\n");

            // Uses su to change the file permissions to 644 (chmod 644 /data/adb/pif.prop) to ensure the system can read it.
            os.writeBytes("chmod 644 /data/adb/pif.prop\n");

            os.writeBytes("exit\n");
            os.flush();

            int exitCode = process.waitFor();
            writeSuccess = (exitCode == 0);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (process != null) {
                process.destroy();
            }
        }

        if (!writeSuccess) {
            return false;
        }

        // Executes su -c "am force-stop com.google.android.gms" to kill Play Services so the new profile takes effect immediately.
        Process killProcess = null;
        try {
            killProcess = Runtime.getRuntime().exec(new String[]{"su", "-c", "am force-stop com.google.android.gms"});
            int killExitCode = killProcess.waitFor();
            return killExitCode == 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (killProcess != null) {
                killProcess.destroy();
            }
        }
    }
}