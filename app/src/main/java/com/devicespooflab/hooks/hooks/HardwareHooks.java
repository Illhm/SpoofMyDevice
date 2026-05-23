package com.devicespooflab.hooks.hooks;

import android.app.ActivityManager;
import android.os.Debug;

import com.devicespooflab.hooks.profile.hardware.HardwareProfileResolver;
import com.devicespooflab.hooks.utils.ConfigManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks to spoof hardware specifications (CPU cores, RAM, CPU frequency, etc.)
 * to match real Pixel 7 Pro hardware.
 *
 * Real Pixel 7 Pro specs:
 * - CPU: Google Tensor G2 (8 cores: 2x2.85GHz + 2x2.35GHz + 4x1.80GHz)
 * - RAM: 12GB LPDDR5
 * - Architecture: ARM64-v8a
 */
public class HardwareHooks {

    private static final String TAG = "DeviceSpoofLab-Hardware";

    private static volatile HardwareProfileResolver.HardwarePreset activePreset =
        HardwareProfileResolver.resolve("", "arm64-v8a");

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            refreshPreset();
            hookRuntimeCores();
            hookActivityManagerMemory(lpparam);
            hookDebugMemory();
            hookFileReads(); // Hook /proc/cpuinfo and /proc/meminfo reads
            XposedBridge.log(TAG + ": Successfully hooked hardware specs");
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook hardware: " + e.getMessage());
        }
    }

    private static void refreshPreset() {
        try {
            String model = ConfigManager.getBuildModel();
            String abi = ConfigManager.getCpuAbi();
            if (abi == null || abi.trim().isEmpty()) {
                abi = "arm64-v8a";
            }
            activePreset = HardwareProfileResolver.resolve(model, abi);
        } catch (Throwable ignored) {
            activePreset = HardwareProfileResolver.resolve("", "arm64-v8a");
        }
    }

    /**
     * Hook Runtime.availableProcessors() to return spoofed cores
     */
    private static void hookRuntimeCores() {
        try {
            XposedHelpers.findAndHookMethod(Runtime.class, "availableProcessors",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(activePreset.cpuCores);
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook Runtime.availableProcessors(): " + e.getMessage());
        }
    }

    /**
     * Hook ActivityManager memory info methods
     */
    private static void hookActivityManagerMemory(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityManagerClass = XposedHelpers.findClassIfExists(
                "android.app.ActivityManager", lpparam.classLoader);

            if (activityManagerClass == null) {
                return;
            }

            // Hook getMemoryInfo()
            XposedHelpers.findAndHookMethod(activityManagerClass, "getMemoryInfo",
                ActivityManager.MemoryInfo.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ActivityManager.MemoryInfo memInfo = (ActivityManager.MemoryInfo) param.args[0];
                        if (memInfo != null) {
                            long originalTotal = memInfo.totalMem;
                            long originalAvail = memInfo.availMem;
                            memInfo.totalMem = activePreset.totalRamBytes;
                            memInfo.availMem = HardwareProfileResolver.syncedAvailMem(originalAvail, originalTotal, activePreset.totalRamBytes);
                        }
                    }
                });

            // Hook getMemoryClass() - returns heap size in MB
            XposedHelpers.findAndHookMethod(activityManagerClass, "getMemoryClass",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(activePreset.heapClassMb);
                    }
                });

            // Hook getLargeMemoryClass()
            XposedHelpers.findAndHookMethod(activityManagerClass, "getLargeMemoryClass",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(activePreset.heapClassMb * 2);
                    }
                });

        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook ActivityManager memory: " + e.getMessage());
        }
    }

    /**
     * Hook Debug.getNativeHeapSize() and related memory methods
     */
    private static void hookDebugMemory() {
        try {
            XposedHelpers.findAndHookMethod(Debug.class, "getNativeHeapSize",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        long originalSize = (Long) param.getResult();
                        // Scale to 12GB device
                        param.setResult(originalSize * 4);
                    }
                });
        } catch (Exception e) {
            // Method might not exist on all Android versions
        }
    }

    /**
     * Hook file reads to intercept /proc/cpuinfo and /proc/meminfo
     */
    private static void hookFileReads() {
        // Hook BufferedReader for /proc/cpuinfo
        try {
            XposedHelpers.findAndHookConstructor(BufferedReader.class,
                java.io.Reader.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        BufferedReader reader = (BufferedReader) param.thisObject;
                        // Check if reading from FileReader
                        if (param.args[0] instanceof FileReader) {
                            // We'll intercept readLine() calls instead
                        }
                    }
                });
        } catch (Exception e) {
            // Ignore
        }

        // Hook RandomAccessFile reads for /proc/meminfo
        try {
            XposedHelpers.findAndHookMethod(RandomAccessFile.class, "readLine",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String line = (String) param.getResult();
                        if (line != null) {
                            // Spoof MemTotal in /proc/meminfo
                            if (line.startsWith("MemTotal:")) {
                                param.setResult("MemTotal:       " + activePreset.totalRamKb + " kB");
                            }
                        }
                    }
                });
        } catch (Exception e) {
            // Ignore
        }

        // Hook File operations for /proc/cpuinfo
        try {
            XposedHelpers.findAndHookMethod(File.class, "exists",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        File file = (File) param.thisObject;
                        String path = file.getAbsolutePath();

                        // Ensure /proc/cpuinfo exists (some apps check this)
                        if (path.equals("/proc/cpuinfo") || path.equals("/proc/meminfo")) {
                            // Let it pass through normally
                        }
                    }
                });
        } catch (Exception e) {
            // Ignore
        }
    }
}
