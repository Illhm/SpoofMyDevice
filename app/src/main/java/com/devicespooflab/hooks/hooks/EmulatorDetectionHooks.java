package com.devicespooflab.hooks.hooks;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks to hide emulator-specific files and artifacts.
 *
 * This is a DEFENSIVE hook - it only hides emulator files that don't exist
 * on real devices. Safe to use on real rooted devices.
 *
 * Property-based detection (ro.kernel.qemu, ro.boot.qemu) is handled by
 * SystemPropertiesHooks.
 */
public class EmulatorDetectionHooks {

    private static final String TAG = "DeviceSpoofLab-Emulator";

    // Detection files to hide (Emulator, Root, Xposed)
    private static final String[] DETECTION_FILES = {
        "/dev/qemu_pipe",
        "/dev/goldfish_pipe",
        "/sys/qemu_trace",
        "/system/lib/libc_malloc_debug_qemu.so",
        "/system/lib64/libc_malloc_debug_qemu.so",
        "/sys/devices/virtual/misc/goldfish_pipe",
        "/sys/devices/virtual/misc/goldfish_sync",

        // Root / Magisk / su binaries
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su",
        "/data/adb/magisk",
        "/data/adb/ksu",
        "/data/adb/apatch",

        // Xposed / LSPosed artifacts
        "/data/misc/riru",
        "/data/misc/zygisk"
    };

    // Keywords in filenames that indicate emulator or root tools
    private static final String[] DETECTION_KEYWORDS = {
        "goldfish",
        "ranchu",
        "vbox",
        "qemu",
        "magisk",
        "edxposed",
        "lsposed",
        "riru",
        "zygisk"
    };

    // Packages to hide via ClassLoader checks
    private static final String[] XPOSED_PACKAGES = {
        "de.robv.android.xposed.XposedBridge",
        "de.robv.android.xposed.XposedHelpers"
    };

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            hookFileExists();
            hookFileCanRead();
            hookFileListFiles();
            hookRuntimeExec();
            hookClassLoader();
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook emulator detection: " + e.getMessage());
        }
    }

    /**
     * Hook File.exists() to return false for emulator-specific files
     */
    private static void hookFileExists() {
        try {
            XposedHelpers.findAndHookMethod(File.class, "exists",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        File file = (File) param.thisObject;
                        String path = file.getAbsolutePath();

                        // Check if this is a detection-specific file
                        for (String targetFile : DETECTION_FILES) {
                            if (path.equals(targetFile) || path.contains(targetFile)) {
                                param.setResult(false);
                                return;
                            }
                        }

                        // Check for detection keywords in path
                        String lowerPath = path.toLowerCase();
                        for (String keyword : DETECTION_KEYWORDS) {
                            if (lowerPath.contains(keyword)) {
                                param.setResult(false);
                                return;
                            }
                        }
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook File.exists(): " + e.getMessage());
        }
    }

    /**
     * Hook File.canRead() to return false for detection-specific files
     */
    private static void hookFileCanRead() {
        try {
            XposedHelpers.findAndHookMethod(File.class, "canRead",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        File file = (File) param.thisObject;
                        String path = file.getAbsolutePath();

                        for (String targetFile : DETECTION_FILES) {
                            if (path.equals(targetFile) || path.contains(targetFile)) {
                                param.setResult(false);
                                return;
                            }
                        }

                        String lowerPath = path.toLowerCase();
                        for (String keyword : DETECTION_KEYWORDS) {
                            if (lowerPath.contains(keyword)) {
                                param.setResult(false);
                                return;
                            }
                        }
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook File.canRead(): " + e.getMessage());
        }
    }

    /**
     * Hook File.listFiles() to filter out emulator/root files from directory listings
     */
    private static void hookFileListFiles() {
        try {
            // Hook listFiles()
            XposedHelpers.findAndHookMethod(File.class, "listFiles",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        File[] files = (File[]) param.getResult();
                        if (files == null) {
                            return;
                        }

                        List<File> filtered = filterEmulatorFiles(Arrays.asList(files));
                        param.setResult(filtered.toArray(new File[0]));
                    }
                });

            // Hook listFiles(FileFilter)
            XposedHelpers.findAndHookMethod(File.class, "listFiles",
                java.io.FileFilter.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        File[] files = (File[]) param.getResult();
                        if (files == null) {
                            return;
                        }

                        List<File> filtered = filterEmulatorFiles(Arrays.asList(files));
                        param.setResult(filtered.toArray(new File[0]));
                    }
                });

            // Hook listFiles(FilenameFilter)
            XposedHelpers.findAndHookMethod(File.class, "listFiles",
                java.io.FilenameFilter.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        File[] files = (File[]) param.getResult();
                        if (files == null) {
                            return;
                        }

                        List<File> filtered = filterEmulatorFiles(Arrays.asList(files));
                        param.setResult(filtered.toArray(new File[0]));
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook File.listFiles(): " + e.getMessage());
        }
    }

    /**
     * Filter emulator files from a list
     */
    private static List<File> filterEmulatorFiles(List<File> files) {
        List<File> filtered = new ArrayList<>();

        for (File file : files) {
            String name = file.getName().toLowerCase();
            String path = file.getAbsolutePath().toLowerCase();
            boolean isEmulatorFile = false;

            // Check for detection keywords
            for (String keyword : DETECTION_KEYWORDS) {
                if (name.contains(keyword) || path.contains(keyword)) {
                    isEmulatorFile = true;
                    break;
                }
            }

            // Check for exact detection paths
            if (!isEmulatorFile) {
                for (String targetFile : DETECTION_FILES) {
                    if (path.equals(targetFile.toLowerCase()) || path.contains(targetFile.toLowerCase())) {
                        isEmulatorFile = true;
                        break;
                    }
                }
            }

            if (!isEmulatorFile) {
                filtered.add(file);
            }
        }

        return filtered;
    }

    /**
     * Hook Runtime.exec() to intercept 'su', 'magisk', and 'xposed' executions
     */
    private static void hookRuntimeExec() {
        XC_MethodHook execHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object arg = param.args[0];
                String command = null;

                if (arg instanceof String) {
                    command = (String) arg;
                } else if (arg instanceof String[]) {
                    String[] cmdArray = (String[]) arg;
                    if (cmdArray.length > 0) {
                        command = cmdArray[0];
                    }
                }

                if (command != null) {
                    String lowerCmd = command.toLowerCase();
                    if (lowerCmd.equals("su") || lowerCmd.endsWith("/su") ||
                        lowerCmd.contains("magisk") || lowerCmd.contains("xposed")) {
                        // Prevent execution by setting an invalid/harmless command or throwing
                        param.setThrowable(new java.io.IOException("Cannot run program \"" + command + "\": error=2, No such file or directory"));
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
            XposedBridge.log(TAG + ": Failed to hook Runtime.exec(): " + e.getMessage());
        }
    }

    /**
     * Hook ClassLoader to hide Xposed classes from apps attempting dynamic detection
     */
    private static void hookClassLoader() {
        try {
            XposedHelpers.findAndHookMethod(ClassLoader.class, "loadClass", String.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String className = (String) param.args[0];
                    if (className != null) {
                        for (String xposedClass : XPOSED_PACKAGES) {
                            if (className.equals(xposedClass) || className.startsWith("de.robv.android.xposed.")) {
                                param.setThrowable(new ClassNotFoundException(className));
                                return;
                            }
                        }
                    }
                }
            });
        } catch (Throwable e) {
            XposedBridge.log(TAG + ": Failed to hook ClassLoader.loadClass(): " + e.getMessage());
        }
    }
}
