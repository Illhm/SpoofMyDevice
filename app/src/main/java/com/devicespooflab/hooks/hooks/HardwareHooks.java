package com.devicespooflab.hooks.hooks;

import android.app.ActivityManager;
import android.os.Debug;

import com.devicespooflab.hooks.data.ActiveProfileManager;
import com.devicespooflab.hooks.data.DeviceHardwareProfile;
import com.devicespooflab.hooks.data.DeviceProfile;
import com.devicespooflab.hooks.utils.ConfigManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HardwareHooks {

    private static final String TAG = "DeviceSpoofLab-Hardware";

    private static DeviceHardwareProfile getHardwareProfile() {
        DeviceProfile active = ActiveProfileManager.getInstance().getActiveProfile();
        String model = active != null ? active.getModel() : null;
        return DeviceHardwareProfile.getForModel(model);
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            hookRuntimeCores();
            hookActivityManagerMemory(lpparam);
            hookDebugMemory();
            hookFileReads();
            XposedBridge.log(TAG + ": Successfully hooked hardware specs");
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook hardware: " + e.getMessage());
        }
    }

    private static void hookRuntimeCores() {
        try {
            XposedHelpers.findAndHookMethod(Runtime.class, "availableProcessors",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(getHardwareProfile().cores);
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook Runtime.availableProcessors(): " + e.getMessage());
        }
    }

    private static void hookActivityManagerMemory(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityManagerClass = XposedHelpers.findClassIfExists(
                "android.app.ActivityManager", lpparam.classLoader);

            if (activityManagerClass == null) {
                return;
            }

            XposedHelpers.findAndHookMethod(activityManagerClass, "getMemoryInfo",
                ActivityManager.MemoryInfo.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ActivityManager.MemoryInfo memInfo = (ActivityManager.MemoryInfo) param.args[0];
                        if (memInfo != null) {
                            DeviceHardwareProfile hw = getHardwareProfile();
                            long originalTotal = memInfo.totalMem;
                            long originalAvail = memInfo.availMem;

                            memInfo.totalMem = hw.ramBytes;

                            if (originalTotal > 0) {
                                double availRatio = (double) originalAvail / originalTotal;
                                memInfo.availMem = (long) (hw.ramBytes * availRatio);
                            } else {
                                memInfo.availMem = hw.ramBytes / 2;
                            }
                        }
                    }
                });

            XposedHelpers.findAndHookMethod(activityManagerClass, "getMemoryClass",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(getHardwareProfile().heapClass);
                    }
                });

            XposedHelpers.findAndHookMethod(activityManagerClass, "getLargeMemoryClass",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(getHardwareProfile().largeHeapClass);
                    }
                });

        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook ActivityManager memory: " + e.getMessage());
        }
    }

    private static void hookDebugMemory() {
        try {
            XposedHelpers.findAndHookMethod(Debug.class, "getNativeHeapSize",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        long originalSize = (Long) param.getResult();
                        DeviceHardwareProfile hw = getHardwareProfile();
                        long scale = hw.ramBytes / (4L * 1024 * 1024 * 1024);
                        if (scale < 1) scale = 1;
                        param.setResult(originalSize * scale);
                    }
                });
        } catch (Exception e) {
        }
    }

    private static void hookFileReads() {
        try {
            ThreadLocal<Boolean> isSpoofing = new ThreadLocal<>();
            XposedHelpers.findAndHookMethod(BufferedReader.class, "readLine", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (Boolean.TRUE.equals(isSpoofing.get())) return;
                    isSpoofing.set(true);
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        String line = (String) param.getResult();
                        if (line != null && line.startsWith("Hardware")) {
                            DeviceProfile profile = ActiveProfileManager.getInstance().getActiveProfile();
                            if (profile != null && profile.getHardware() != null) {
                                param.setResult("Hardware\t: " + profile.getHardware());
                            }
                        }
                    } finally {
                        isSpoofing.remove();
                    }
                }
            });
        } catch (Exception e) {
        }

        try {
            XposedHelpers.findAndHookMethod(RandomAccessFile.class, "readLine",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String line = (String) param.getResult();
                        if (line != null) {
                            if (line.startsWith("MemTotal:")) {
                                long ramKb = getHardwareProfile().ramBytes / 1024;
                                param.setResult("MemTotal:       " + ramKb + " kB");
                            }
                        }
                    }
                });
        } catch (Exception e) {
        }

        try {
            XposedHelpers.findAndHookMethod(File.class, "exists",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        File file = (File) param.thisObject;
                        String path = file.getAbsolutePath();

                        if (path.equals("/proc/cpuinfo") || path.equals("/proc/meminfo")) {
                            // Let it pass
                        }
                    }
                });
        } catch (Exception e) {
        }
    }
}
