package com.devicespooflab.hooks.hooks;

import android.app.ActivityManager;

import com.devicespooflab.hooks.utils.ConfigManager;

import java.io.RandomAccessFile;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HardwareHooks {
    private static final String TAG = "DeviceSpoofLab-Hardware";

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        hookRuntimeCores();
        hookActivityManagerMemory(lpparam);
        hookProcMeminfo();
    }

    private static void hookRuntimeCores() {
        XposedHelpers.findAndHookMethod(Runtime.class, "availableProcessors", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                param.setResult(resolveCoreCount());
            }
        });
    }

    private static void hookActivityManagerMemory(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityManagerClass = XposedHelpers.findClassIfExists("android.app.ActivityManager", lpparam.classLoader);
            if (activityManagerClass == null) return;

            XposedHelpers.findAndHookMethod(activityManagerClass, "getMemoryInfo", ActivityManager.MemoryInfo.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    ActivityManager.MemoryInfo memInfo = (ActivityManager.MemoryInfo) param.args[0];
                    if (memInfo == null) return;

                    long oldTotal = memInfo.totalMem;
                    long oldAvail = memInfo.availMem;
                    long spoofTotal = resolveTotalMemoryBytes();
                    if (spoofTotal <= 0L) return;

                    double availRatio = oldTotal > 0L ? ((double) oldAvail / (double) oldTotal) : 0.35d;
                    if (availRatio < 0d) availRatio = 0d;
                    if (availRatio > 1d) availRatio = 1d;

                    memInfo.totalMem = spoofTotal;
                    memInfo.availMem = (long) (spoofTotal * availRatio);
                }
            });

            XposedHelpers.findAndHookMethod(activityManagerClass, "getMemoryClass", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    param.setResult(resolveMemoryClassMb(false));
                }
            });

            XposedHelpers.findAndHookMethod(activityManagerClass, "getLargeMemoryClass", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    param.setResult(resolveMemoryClassMb(true));
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": memory hook failed " + t.getMessage());
        }
    }

    private static void hookProcMeminfo() {
        try {
            XposedHelpers.findAndHookMethod(RandomAccessFile.class, "readLine", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    String line = (String) param.getResult();
                    if (line == null || !line.startsWith("MemTotal:")) return;
                    long totalKb = resolveTotalMemoryBytes() / 1024L;
                    param.setResult(String.format(Locale.US, "MemTotal:       %d kB", totalKb));
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static int resolveCoreCount() {
        String abi = ConfigManager.getCpuAbi();
        return abi != null && abi.contains("64") ? 8 : 4;
    }

    private static long resolveTotalMemoryBytes() {
        int density = ConfigManager.getScreenDensityDpi();
        int width = ConfigManager.getScreenWidth();
        int height = ConfigManager.getScreenHeight();
        long pixels = (long) width * (long) height;
        if (pixels >= 3500000L || density >= 520) return 12L * 1024L * 1024L * 1024L;
        if (pixels >= 2500000L || density >= 440) return 8L * 1024L * 1024L * 1024L;
        return 6L * 1024L * 1024L * 1024L;
    }

    private static int resolveMemoryClassMb(boolean large) {
        long ramGb = resolveTotalMemoryBytes() / (1024L * 1024L * 1024L);
        if (ramGb >= 12) return large ? 1024 : 512;
        if (ramGb >= 8) return large ? 768 : 384;
        return large ? 512 : 256;
    }
}
