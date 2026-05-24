package com.devicespooflab.hooks.hooks;

import android.app.ActivityManager;
import android.os.Debug;

import com.devicespooflab.hooks.utils.ConfigManager;
import com.devicespooflab.hooks.hooks.HookProfileResolver;

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
    private static int getConfiguredCores() {
        String coresStr = HookProfileResolver.resolveString("hardware.cpu.cores", ConfigManager.getSystemProperty("hardware.cpu.cores", null));
        if (coresStr != null) {
            try { return Integer.parseInt(coresStr); } catch (Exception e) {}
        }
        return -1;
    }

    private static long getConfiguredRamBytes() {
        String ramStr = HookProfileResolver.resolveString("hardware.ram.bytes", ConfigManager.getSystemProperty("hardware.ram.bytes", null));
        if (ramStr != null) {
            try { return Long.parseLong(ramStr); } catch (Exception e) {}
        }
        return -1; // Fallback
    }

    private static int getConfiguredHeapClass() {
        String heapStr = HookProfileResolver.resolveString("hardware.heap.class", ConfigManager.getSystemProperty("hardware.heap.class", null));
        if (heapStr != null) {
            try { return Integer.parseInt(heapStr); } catch (Exception e) {}
        }
        return -1;
    }

    private static int getConfiguredHeapLarge() {
        String heapStr = HookProfileResolver.resolveString("hardware.heap.large", ConfigManager.getSystemProperty("hardware.heap.large", null));
        if (heapStr != null) {
            try { return Integer.parseInt(heapStr); } catch (Exception e) {}
        }
        return -1;
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            hookRuntimeCores();
            hookActivityManagerMemory(lpparam);
            hookDebugMemory();
            hookFileReads(); // Hook /proc/cpuinfo and /proc/meminfo reads
            XposedBridge.log(TAG + ": Successfully hooked hardware specs");
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook hardware: " + e.getMessage());
        }
    }

    /**
     * Hook Runtime.availableProcessors() to return 8 cores
     */

    private static void hookRuntimeCores() {
        try {
            XposedHelpers.findAndHookMethod(Runtime.class, "availableProcessors",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        int configuredCores = getConfiguredCores();
                        if (configuredCores > 0) {
                            param.setResult(configuredCores);
                        }
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
                        long configuredRam = getConfiguredRamBytes();
                        if (memInfo != null && configuredRam > 0) {
                            long originalTotal = memInfo.totalMem;
                            memInfo.totalMem = configuredRam;

                            // Keep available/free memory proportional
                            if (originalTotal > 0) {
                                double usedRatio = 1.0 - ((double) memInfo.availMem / originalTotal);
                                memInfo.availMem = (long) (configuredRam * (1.0 - usedRatio));
                            }
                        }
                    }
                });


            // Hook getMemoryClass() - returns heap size in MB
            XposedHelpers.findAndHookMethod(activityManagerClass, "getMemoryClass",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        int heapClass = getConfiguredHeapClass();
                        if (heapClass > 0) {
                            param.setResult(heapClass);
                        }
                    }
                });

            // Hook getLargeMemoryClass()
            XposedHelpers.findAndHookMethod(activityManagerClass, "getLargeMemoryClass",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        int heapLarge = getConfiguredHeapLarge();
                        if (heapLarge > 0) {
                            param.setResult(heapLarge);
                        }
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
        try {
            ThreadLocal<Boolean> isHooking = new ThreadLocal<Boolean>() {
                @Override
                protected Boolean initialValue() {
                    return false;
                }
            };

            // Map to track if a specific reader/stream is reading from our target paths
            java.util.Map<Object, String> targetReaders = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

            XC_MethodHook constructorHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (isHooking.get()) return;

                    if (param.args.length > 0 && param.args[0] != null) {
                        String path = null;
                        if (param.args[0] instanceof File) {
                            path = ((File) param.args[0]).getAbsolutePath();
                        } else if (param.args[0] instanceof String) {
                            path = (String) param.args[0];
                        }

                        if (path != null && (path.equals("/proc/cpuinfo") || path.equals("/proc/meminfo"))) {
                            // Register this instance as a target reader
                            targetReaders.put(param.thisObject, path);
                        }
                    }
                }
            };

            XposedBridge.hookAllConstructors(FileReader.class, constructorHook);
            XposedBridge.hookAllConstructors(RandomAccessFile.class, constructorHook);

            // Track when FileReader is wrapped in BufferedReader
            XposedBridge.hookAllConstructors(BufferedReader.class, new XC_MethodHook() {
                 @Override
                 protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                     if (isHooking.get()) return;
                     if (param.args.length > 0 && param.args[0] != null) {
                         String path = targetReaders.get(param.args[0]);
                         if (path != null) {
                             targetReaders.put(param.thisObject, path);
                         }
                     }
                 }
            });


            XposedHelpers.findAndHookMethod(RandomAccessFile.class, "readLine", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (isHooking.get()) return;
                    String path = targetReaders.get(param.thisObject);
                    if (path == null) return;

                    isHooking.set(true);
                    try {
                        String line = (String) param.getResult();
                        if (line != null) {
                            if (path.equals("/proc/meminfo") && line.contains("MemTotal:")) {
                                long configuredRam = getConfiguredRamBytes();
                                if (configuredRam > 0) {
                                    param.setResult("MemTotal:       " + (configuredRam / 1024) + " kB");
                                }
                            } else if (path.equals("/proc/cpuinfo") && line.contains("processor")) {
                                int configuredCores = getConfiguredCores();
                                // Just a simple logic, a full spoof might rewrite the entire file
                                if (configuredCores > 0) {
                                     // For simplicity in this patch, we don't truncate rows but we could adjust
                                     // core counts if we are reading the processor index line.
                                }
                            }
                        }
                    } finally {
                        isHooking.set(false);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(BufferedReader.class, "readLine", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (isHooking.get()) return;
                    String path = targetReaders.get(param.thisObject);
                    if (path == null) return;

                    isHooking.set(true);
                    try {
                        String line = (String) param.getResult();
                        if (line != null) {
                            if (path.equals("/proc/meminfo") && line.contains("MemTotal:")) {
                                long configuredRam = getConfiguredRamBytes();
                                if (configuredRam > 0) {
                                    param.setResult("MemTotal:       " + (configuredRam / 1024) + " kB");
                                }
                            }
                        }
                    } finally {
                        isHooking.set(false);
                    }
                }
            });


        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook file reads: " + e.getMessage());
        }
    }






}
