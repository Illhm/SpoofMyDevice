package com.devicespooflab.hooks.hooks;

import android.app.ActivityManager;
import android.os.Debug;

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
 * Hooks to spoof hardware specifications (CPU cores, RAM, etc.)
 * matching the currently active device profile.
 */
public class HardwareHooks {

    private static final String TAG = "DeviceSpoofLab-Hardware";

    // Defaults in case config is missing
    private static final int DEFAULT_CORES = 8;
    private static final long DEFAULT_RAM_BYTES = 12L * 1024 * 1024 * 1024; // 12GB
    private static final long DEFAULT_RAM_KB = 12L * 1024 * 1024; // 12GB in KB

    private static int getSpoofedCores() {
        // Advanced spoofing might read actual profile cores. For now we use 8 which is standard.
        return 8;
    }

    private static long getSpoofedRamBytes() {
        return DEFAULT_RAM_BYTES;
    }

    private static long getSpoofedRamKb() {
        return DEFAULT_RAM_KB;
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            hookRuntimeCores();
            hookActivityManagerMemory(lpparam);
            hookDebugMemory();
            // File reads for /proc/cpuinfo and /proc/meminfo are now handled by FileSystemHooks
            XposedBridge.log(TAG + ": Successfully hooked hardware specs");
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook hardware: " + e.getMessage());
        }
    }

    /**
     * Hook Runtime.availableProcessors()
     */
    private static void hookRuntimeCores() {
        try {
            XposedHelpers.findAndHookMethod(Runtime.class, "availableProcessors",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(getSpoofedCores());
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
                            long spoofedTotal = getSpoofedRamBytes();
                            memInfo.totalMem = spoofedTotal;
                            long originalTotal = memInfo.totalMem;
                            if (originalTotal > 0) {
                                double usedRatio = 1.0 - ((double) memInfo.availMem / originalTotal);
                                memInfo.availMem = (long) (spoofedTotal * (1.0 - usedRatio));
                            }
                        }
                    }
                });

            // Hook getMemoryClass() - returns heap size in MB
            XposedHelpers.findAndHookMethod(activityManagerClass, "getMemoryClass",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        // Pixel 7 Pro typically has 512MB heap per app
                        param.setResult(512);
                    }
                });

            // Hook getLargeMemoryClass()
            XposedHelpers.findAndHookMethod(activityManagerClass, "getLargeMemoryClass",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        // Large heap on Pixel 7 Pro
                        param.setResult(1024);
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

    // File reads logic moved to FileSystemHooks
}
