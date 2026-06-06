package com.devicespooflab.hooks.hooks.impl;

import android.os.Build;
import com.devicespooflab.hooks.utils.ConfigManager;
import com.devicespooflab.hooks.diagnostics.HookDiagnostics;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class FileSystemHooks {
    private static final String TAG = "DeviceSpoofLab-FileSystem";
    private static final ThreadLocal<Boolean> IN_HOOK = ThreadLocal.withInitial(() -> false);

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            hookIoBridgeOpen();
            hookFileInputStream();
            hookRandomAccessFile();
            hookProcessBuilderAndRuntimeExec();
            hookOsOpen();
            HookDiagnostics.logHookSuccess("FileSystemHooks", lpparam.packageName, "Successfully hooked file system IO");
        } catch (Throwable e) {
            HookDiagnostics.logHookFailed("FileSystemHooks", lpparam.packageName, e.getMessage());
            XposedBridge.log(TAG + ": Failed to hook FileSystem: " + e.getMessage());
        }
    }

    private static void hookIoBridgeOpen() {
        try {
            Class<?> ioBridgeClass = XposedHelpers.findClass("libcore.io.IoBridge", null);
            XposedHelpers.findAndHookMethod(ioBridgeClass, "open", String.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (IN_HOOK.get()) return;
                    String path = (String) param.args[0];
                    if (path == null) return;

                    String virtualContent = getVirtualContentForPath(path);
                    if (virtualContent != null) {
                        // Advanced: redirect to a real file we control or throw and handle in higher layers
                        // For simplicity in Xposed, we'll let it fail or use a memory pipe if possible
                        // But since libcore.io.IoBridge returns a FileDescriptor, we need a real fd.
                        // We will write the virtual content to a temporary file in memory/cache and return its fd.
                        // However, that might be overkill if FileInputStream is already hooked.
                        // We will skip direct IoBridge hooking for now to avoid FD leaks unless absolutely necessary.
                    }
                }
            });
        } catch (Throwable e) {
            // Ignore
        }
    }

    private static void hookFileInputStream() {
        try {
            XposedHelpers.findAndHookConstructor(FileInputStream.class, File.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (IN_HOOK.get()) return;
                    File file = (File) param.args[0];
                    if (file == null) return;

                    String path = file.getAbsolutePath();
                    String virtualContent = getVirtualContentForPath(path);
                    if (virtualContent != null) {
                        IN_HOOK.set(true);
                        try {
                            // Can't easily mock FileInputStream natively here without creating a real temp file
                            // Let's create a temp file in memory to satisfy the FD requirement
                            File tempFile = createVirtualTempFile(path, virtualContent);
                            if (tempFile != null) {
                                param.args[0] = tempFile;
                            }
                        } finally {
                            IN_HOOK.set(false);
                        }
                    }
                }
            });

            XposedHelpers.findAndHookConstructor(FileInputStream.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (IN_HOOK.get()) return;
                    String path = (String) param.args[0];
                    if (path == null) return;

                    String virtualContent = getVirtualContentForPath(path);
                    if (virtualContent != null) {
                        IN_HOOK.set(true);
                        try {
                            File tempFile = createVirtualTempFile(path, virtualContent);
                            if (tempFile != null) {
                                param.args[0] = tempFile.getAbsolutePath();
                            }
                        } finally {
                            IN_HOOK.set(false);
                        }
                    }
                }
            });
        } catch (Throwable e) {
            XposedBridge.log(TAG + ": Failed to hook FileInputStream: " + e.getMessage());
        }
    }

    private static void hookRandomAccessFile() {
        try {
            XposedHelpers.findAndHookConstructor(RandomAccessFile.class, File.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (IN_HOOK.get()) return;
                    File file = (File) param.args[0];
                    if (file == null) return;

                    String path = file.getAbsolutePath();
                    String virtualContent = getVirtualContentForPath(path);
                    if (virtualContent != null) {
                        IN_HOOK.set(true);
                        try {
                            File tempFile = createVirtualTempFile(path, virtualContent);
                            if (tempFile != null) {
                                param.args[0] = tempFile;
                            }
                        } finally {
                            IN_HOOK.set(false);
                        }
                    }
                }
            });

            XposedHelpers.findAndHookConstructor(RandomAccessFile.class, String.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (IN_HOOK.get()) return;
                    String path = (String) param.args[0];
                    if (path == null) return;

                    String virtualContent = getVirtualContentForPath(path);
                    if (virtualContent != null) {
                        IN_HOOK.set(true);
                        try {
                            File tempFile = createVirtualTempFile(path, virtualContent);
                            if (tempFile != null) {
                                param.args[0] = tempFile.getAbsolutePath();
                            }
                        } finally {
                            IN_HOOK.set(false);
                        }
                    }
                }
            });
        } catch (Throwable e) {
            XposedBridge.log(TAG + ": Failed to hook RandomAccessFile: " + e.getMessage());
        }
    }

    private static void hookOsOpen() {
        try {
            Class<?> osClass = XposedHelpers.findClassIfExists("android.system.Os", null);
            if (osClass != null) {
                XposedHelpers.findAndHookMethod(osClass, "open", String.class, int.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (IN_HOOK.get()) return;
                        String path = (String) param.args[0];
                        if (path == null) return;

                        String virtualContent = getVirtualContentForPath(path);
                        if (virtualContent != null) {
                            IN_HOOK.set(true);
                            try {
                                File tempFile = createVirtualTempFile(path, virtualContent);
                                if (tempFile != null) {
                                    param.args[0] = tempFile.getAbsolutePath();
                                }
                            } finally {
                                IN_HOOK.set(false);
                            }
                        }
                    }
                });
            }
        } catch (Throwable e) {
            // Ignore
        }
    }

    private static void hookProcessBuilderAndRuntimeExec() {
        XC_MethodHook execHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (IN_HOOK.get()) return;

                String[] command = null;
                if (param.args[0] instanceof String) {
                    command = ((String) param.args[0]).split("\\s+");
                } else if (param.args[0] instanceof String[]) {
                    command = (String[]) param.args[0];
                }

                if (command != null && command.length > 0) {
                    if ("getprop".equals(command[0]) || "cat".equals(command[0]) || "uname".equals(command[0])) {
                        // For simplicity in Xposed, returning mocked Process is hard due to package-private constructors.
                        // We can modify the command to point to a shell script we control, or block it.
                        // Let's block certain queries that expose real hardware.
                        if ("cat".equals(command[0]) && command.length > 1) {
                            String targetPath = command[1];
                            String content = getVirtualContentForPath(targetPath);
                            if (content != null) {
                                IN_HOOK.set(true);
                                try {
                                    File tempFile = createVirtualTempFile(targetPath, content);
                                    if (tempFile != null) {
                                        if (param.args[0] instanceof String) {
                                            param.args[0] = "cat " + tempFile.getAbsolutePath();
                                        } else if (param.args[0] instanceof String[]) {
                                            String[] newCmd = new String[command.length];
                                            System.arraycopy(command, 0, newCmd, 0, command.length);
                                            newCmd[1] = tempFile.getAbsolutePath();
                                            param.args[0] = newCmd;
                                        }
                                    }
                                } finally {
                                    IN_HOOK.set(false);
                                }
                            }
                        }
                    }
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(Runtime.class, "exec", String.class, execHook);
            XposedHelpers.findAndHookMethod(Runtime.class, "exec", String[].class, execHook);
            XposedHelpers.findAndHookMethod(Runtime.class, "exec", String.class, String[].class, execHook);
            XposedHelpers.findAndHookMethod(Runtime.class, "exec", String[].class, String[].class, execHook);
            XposedHelpers.findAndHookMethod(Runtime.class, "exec", String.class, String[].class, File.class, execHook);
            XposedHelpers.findAndHookMethod(Runtime.class, "exec", String[].class, String[].class, File.class, execHook);
        } catch (Throwable e) {
            // Ignore
        }
    }

    private static File createVirtualTempFile(String originalPath, String content) {
        try {
            // Generate a safe temp file name
            String safeName = originalPath.replace("/", "_");
            File tempDir = new File("/data/local/tmp");
            if (!tempDir.exists() || !tempDir.canWrite()) {
                // Fallback to /sdcard/Android/data or similar if we can't write to tmp
                tempDir = new File("/sdcard/Download");
            }
            if (!tempDir.exists() || !tempDir.canWrite()) {
                return null;
            }

            File tempFile = new File(tempDir, "virt_" + safeName + "_" + System.nanoTime() + ".tmp");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
            fos.write(content.getBytes());
            fos.close();

            // Try to make it readable to everyone
            tempFile.setReadable(true, false);

            return tempFile;
        } catch (Throwable e) {
            return null;
        }
    }

    private static String getVirtualContentForPath(String path) {
        if (path.equals("/proc/cpuinfo")) {
            return generateCpuInfo();
        } else if (path.equals("/proc/version")) {
            return generateKernelVersion();
        } else if (path.equals("/proc/meminfo")) {
            return generateMemInfo();
        } else if (path.startsWith("/sys/devices/system/cpu/")) {
            // Minimal sysfs mock
            return null;
        } else if (path.startsWith("/sys/class/power_supply/")) {
            // Minimal mock
            return null;
        }
        return null;
    }

    private static String generateCpuInfo() {
        String socName = ConfigManager.getOptionalConfigValue(ConfigManager.FIELD_SOC_NAME);
        if (socName == null || socName.isEmpty()) socName = "Snapdragon 8 Gen 2";

        String hardware = ConfigManager.getBuildHardware();
        if (hardware == null || hardware.isEmpty()) hardware = "qcom";

        StringBuilder sb = new StringBuilder();
        sb.append("Processor\t: AArch64 Processor rev 0 (aarch64)\n");
        sb.append("model name\t: ARMv8 Processor rev 0 (v8l)\n");
        sb.append("BogoMIPS\t: 38.40\n");
        sb.append("Features\t: fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp cpuid asimdrdm lrcpc dcpop asimddp\n");
        sb.append("CPU implementer\t: 0x41\n");
        sb.append("CPU architecture: 8\n");
        sb.append("CPU variant\t: 0x2\n");
        sb.append("CPU part\t: 0xd40\n");
        sb.append("CPU revision\t: 0\n\n");

        // Output for 8 cores
        for (int i = 0; i < 8; i++) {
            sb.append("processor\t: ").append(i).append("\n");
            sb.append("BogoMIPS\t: 38.40\n");
            sb.append("Features\t: fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp cpuid asimdrdm lrcpc dcpop asimddp\n");
            sb.append("CPU implementer\t: 0x41\n");
            sb.append("CPU architecture: 8\n");
            sb.append("CPU variant\t: 0x2\n");
            sb.append("CPU part\t: 0xd40\n");
            sb.append("CPU revision\t: 0\n\n");
        }

        sb.append("Hardware\t: ").append(hardware).append("\n");
        sb.append("Revision\t: 0000\n");
        sb.append("Serial\t\t: 0000000000000000\n");

        return sb.toString();
    }

    private static String generateKernelVersion() {
        String kernelRelease = ConfigManager.getOptionalConfigValue(ConfigManager.FIELD_KERNEL_RELEASE);
        if (kernelRelease == null || kernelRelease.isEmpty()) {
            kernelRelease = "5.10.198-android13-4-g123456789abc #1 SMP PREEMPT";
        }
        return "Linux version " + kernelRelease + " (builder@build-server) (Android (10147774, +pgo, +boltdir, +lto, -mlgo, based on r487747c) clang version 17.0.2 (https://android.googlesource.com/toolchain/llvm-project d9f89f4d166111d466bba7adf5b32e0161476b7f), LLD 17.0.2) #1 SMP PREEMPT Wed Jun 12 12:00:00 UTC 2024\n";
    }

    private static String generateMemInfo() {
        // Return 12GB or roughly based on what's expected
        return "MemTotal:       11831828 kB\n" +
               "MemFree:         4385904 kB\n" +
               "MemAvailable:    6319808 kB\n" +
               "Buffers:          163840 kB\n" +
               "Cached:          2085448 kB\n" +
               "SwapCached:        12480 kB\n" +
               "Active:          3571240 kB\n" +
               "Inactive:        1828940 kB\n" +
               "SwapTotal:       4194300 kB\n" +
               "SwapFree:        2894100 kB\n";
    }
}
