package com.devicespooflab.hooks.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import android.os.UserManager;
import android.os.UserHandle;
import android.content.Context;

public class RootAccessManager {

    private static final long ROOT_TIMEOUT_SECONDS = 45L;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String[] TARGET_PACKAGES = {
        "com.shopee.id",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill"
    };

    public RootCheckResult checkRootAccess() {
        CommandResult result = runSuCommand("echo ROOT_OK", 10L);
        return new RootCheckResult(result.exitCode == 0 && result.output.contains("ROOT_OK"), result.output);
    }

    public MagiskResetResult runMagiskDeviceReset(Context context) {
        String newAndroidId = randomHex(16);
        String randomName = "Device_" + randomAlphaNumeric(6);
        String newGaid = UUID.randomUUID().toString();

        List<Integer> activeUsers = getActiveUserIds(context);

        StringBuilder command = new StringBuilder();

        for (int userId : activeUsers) {
            command.append("echo === Resetting User ").append(userId).append(" ===\n");
            command.append("OLD_ANDROID_ID_").append(userId).append("=$(settings get --user ").append(userId).append(" secure android_id 2>/dev/null || true)\n");
            command.append("echo OLD_ANDROID_ID=$OLD_ANDROID_ID_").append(userId).append("\n");
            command.append("settings put --user ").append(userId).append(" secure android_id ").append(shellQuote(newAndroidId)).append("\n");
            command.append("settings put --user ").append(userId).append(" global bluetooth_name ").append(shellQuote(randomName)).append("\n");
            command.append("settings put --user ").append(userId).append(" global advertising_id ").append(shellQuote(newGaid)).append("\n");

            for (String packageName : TARGET_PACKAGES) {
                command.append("if pm clear --user ").append(userId).append(" ").append(shellQuote(packageName)).append(" >/dev/null 2>&1; then ");
                command.append("echo CLEARED:").append(packageName).append("; else echo CLEAR_FAILED:").append(packageName).append("; fi\n");
            }
        }

        // System wide props
        command.append("setprop net.hostname ").append(shellQuote(randomName)).append(" 2>/dev/null || true\n");

        CommandResult result = runSuCommand(command.toString(), ROOT_TIMEOUT_SECONDS);
        return new MagiskResetResult(
            result.exitCode == 0,
            parseOldAndroidId(result.output),
            newAndroidId,
            randomName,
            newGaid,
            parseClearedPackages(result.output),
            parseFailedPackages(result.output),
            result.output
        );
    }

    private List<Integer> getActiveUserIds(Context context) {
        List<Integer> users = new ArrayList<>();
        users.add(0); // Always include User 0
        try {
            if (context != null) {
                UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
                if (um != null) {
                    List<UserHandle> userProfiles = um.getUserProfiles();
                    for (UserHandle profile : userProfiles) {
                        int userId = profile.hashCode(); // In modern Android, UserHandle.hashCode() usually equals userId.
                        // Or technically profile.getIdentifier() via reflection, but hashCode is safe enough for most cases.
                        // Let's use reflection to get exact ID just in case.
                        try {
                            java.lang.reflect.Method getIdentifierMethod = UserHandle.class.getMethod("getIdentifier");
                            userId = (int) getIdentifierMethod.invoke(profile);
                        } catch (Exception ignored) {
                        }
                        if (userId != 0 && !users.contains(userId)) {
                            users.add(userId);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return users;
    }

    private CommandResult runSuCommand(String command, long timeoutSeconds) {
        Process process = null;
        StringBuilder output = new StringBuilder();
        try {
            process = new ProcessBuilder("su", "-c", command).redirectErrorStream(true).start();
            Process runningProcess = process;
            Thread outputReader = new Thread(() -> readProcessOutput(runningProcess, output), "RootAccessManager-output");
            outputReader.start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                outputReader.join(TimeUnit.SECONDS.toMillis(1L));
                return new CommandResult(-1, output.append("Root command timed out.\n").toString());
            }
            outputReader.join(TimeUnit.SECONDS.toMillis(1L));
            return new CommandResult(process.exitValue(), output.toString());
        } catch (IOException exception) {
            return new CommandResult(-1, exception.getMessage() == null ? "su command failed" : exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new CommandResult(-1, "Root command interrupted.");
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }


    private static void readProcessOutput(Process process, StringBuilder output) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        } catch (IOException exception) {
            output.append(exception.getMessage() == null ? "Unable to read root output." : exception.getMessage()).append('\n');
        }
    }

    private static String parseOldAndroidId(String output) {
        for (String line : output.split("\\n")) {
            if (line.startsWith("OLD_ANDROID_ID=")) {
                return line.substring("OLD_ANDROID_ID=".length()).trim();
            }
        }
        return "";
    }

    private static List<String> parseClearedPackages(String output) {
        return parsePackages(output, "CLEARED:");
    }

    private static List<String> parseFailedPackages(String output) {
        return parsePackages(output, "CLEAR_FAILED:");
    }

    private static List<String> parsePackages(String output, String prefix) {
        List<String> packages = new ArrayList<>();
        for (String line : output.split("\\n")) {
            if (line.startsWith(prefix)) {
                packages.add(line.substring(prefix.length()).trim());
            }
        }
        return packages;
    }

    private static String randomHex(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(Integer.toHexString(SECURE_RANDOM.nextInt(16)));
        }
        return builder.toString();
    }

    private static String randomAlphaNumeric(int length) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(alphabet.charAt(SECURE_RANDOM.nextInt(alphabet.length())));
        }
        return builder.toString();
    }

    private static String shellQuote(String value) {
        if (value == null) {
            return "''";
        }
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static class CommandResult {
        final int exitCode;
        final String output;

        CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output == null ? "" : output;
        }
    }

    public static class RootCheckResult {
        private final boolean granted;
        private final String rawOutput;

        RootCheckResult(boolean granted, String rawOutput) {
            this.granted = granted;
            this.rawOutput = rawOutput == null ? "" : rawOutput;
        }

        public boolean isGranted() {
            return granted;
        }

        public String getRawOutput() {
            return rawOutput;
        }
    }

    public static class MagiskResetResult {
        private final boolean successful;
        private final String oldAndroidId;
        private final String newAndroidId;
        private final String randomName;
        private final String newGaid;
        private final List<String> clearedPackages;
        private final List<String> failedPackages;
        private final String rawOutput;

        MagiskResetResult(
            boolean successful,
            String oldAndroidId,
            String newAndroidId,
            String randomName,
            String newGaid,
            List<String> clearedPackages,
            List<String> failedPackages,
            String rawOutput
        ) {
            this.successful = successful;
            this.oldAndroidId = oldAndroidId == null ? "" : oldAndroidId;
            this.newAndroidId = newAndroidId;
            this.randomName = randomName;
            this.newGaid = newGaid;
            this.clearedPackages = clearedPackages;
            this.failedPackages = failedPackages;
            this.rawOutput = rawOutput == null ? "" : rawOutput;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public String getOldAndroidId() {
            return oldAndroidId;
        }

        public String getNewAndroidId() {
            return newAndroidId;
        }

        public String getRandomName() {
            return randomName;
        }

        public String getNewGaid() {
            return newGaid;
        }

        public List<String> getClearedPackages() {
            return clearedPackages;
        }

        public List<String> getFailedPackages() {
            return failedPackages;
        }

        public String getRawOutput() {
            return rawOutput;
        }

        public String toDisplayText() {
            return String.format(
                Locale.US,
                "Android ID: %s -> %s\nBluetooth/hostname: %s\nGAID: %s\nCleared: %d, Failed: %d",
                oldAndroidId.isEmpty() ? "unknown" : oldAndroidId,
                newAndroidId,
                randomName,
                newGaid,
                clearedPackages.size(),
                failedPackages.size()
            );
        }
    }
}
