package com.devicespooflab.hooks.hooks;

import android.os.Build;
import android.text.TextUtils;

import com.devicespooflab.hooks.utils.ConfigManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class CpuInfoHooks {
    private static final String TAG = "DeviceSpoofLab-CpuInfo";
    private static final ThreadLocal<Boolean> isGeneratingSpoof = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam.appInfo == null || TextUtils.isEmpty(lpparam.appInfo.dataDir)) {
            XposedBridge.log(TAG + ": Cannot hook cpuinfo, missing appInfo or dataDir.");
            return;
        }

        String spoofedCpuInfoPath = lpparam.appInfo.dataDir + "/cache/spoofed_cpuinfo";
        File spoofedFile = new File(spoofedCpuInfoPath);

        hookIoBridge(spoofedCpuInfoPath, spoofedFile);
        hookFileConstructors(spoofedCpuInfoPath, spoofedFile);
        hookFileInputStream(spoofedCpuInfoPath, spoofedFile);

        XposedBridge.log(TAG + ": Successfully hooked CpuInfo reads for " + lpparam.packageName);
    }

    private static void generateSpoofedCpuInfo(File fakeFile) {
        if (isGeneratingSpoof.get()) {
            return;
        }
        isGeneratingSpoof.set(true);
        try {
            if (fakeFile.exists() && fakeFile.length() > 0) {
                // If it already exists and has content, we might not need to regenerate,
                // but for safety in case config changed, we regenerate or just return.
                // Let's regenerate it to be safe.
            }
            File cacheDir = fakeFile.getParentFile();
            if (cacheDir != null && !cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            String hardware = ConfigManager.getBuildHardware();
            String socModel = ConfigManager.getSocModel();

            try (BufferedReader reader = new BufferedReader(new FileReader("/proc/cpuinfo"));
                 Writer writer = new OutputStreamWriter(new FileOutputStream(fakeFile), StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.toLowerCase().startsWith("hardware")) {
                        if (hardware != null && !hardware.isEmpty()) {
                            writer.write("Hardware\t: " + hardware + "\n");
                        } else {
                            writer.write(line + "\n");
                        }
                    } else if (line.toLowerCase().startsWith("processor") || line.toLowerCase().startsWith("model name")) {
                        if (socModel != null && !socModel.isEmpty()) {
                            writer.write("Processor\t: " + socModel + "\n");
                        } else {
                            writer.write(line + "\n");
                        }
                    } else {
                        writer.write(line + "\n");
                    }
                }
            }
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to generate spoofed cpuinfo: " + e.getMessage());
        } finally {
            isGeneratingSpoof.set(false);
        }
    }

    private static void hookIoBridge(String spoofedPath, File spoofedFile) {
        try {
            Class<?> ioBridgeClass = XposedHelpers.findClass("libcore.io.IoBridge", CpuInfoHooks.class.getClassLoader());
            XposedHelpers.findAndHookMethod(ioBridgeClass, "open", String.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (isGeneratingSpoof.get()) return;
                    String path = (String) param.args[0];
                    if (path != null && path.contains("/proc/cpuinfo")) {
                        generateSpoofedCpuInfo(spoofedFile);
                        param.args[0] = spoofedPath;
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking IoBridge.open: " + e.getMessage());
        }
    }

    private static void hookFileConstructors(String spoofedPath, File spoofedFile) {
        try {
            XposedHelpers.findAndHookConstructor(File.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (isGeneratingSpoof.get()) return;
                    String path = (String) param.args[0];
                    if (path != null && path.contains("/proc/cpuinfo")) {
                        generateSpoofedCpuInfo(spoofedFile);
                        param.args[0] = spoofedPath;
                    }
                }
            });

            XposedHelpers.findAndHookConstructor(File.class, String.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (isGeneratingSpoof.get()) return;
                    String parent = (String) param.args[0];
                    String child = (String) param.args[1];
                    String fullPath = "";
                    if (parent != null) {
                        fullPath += parent;
                    }
                    if (child != null) {
                        fullPath += "/" + child;
                    }
                    if (fullPath.contains("/proc/cpuinfo")) {
                        generateSpoofedCpuInfo(spoofedFile);
                        param.args[0] = spoofedFile.getParent();
                        param.args[1] = spoofedFile.getName();
                    }
                }
            });

            XposedHelpers.findAndHookConstructor(File.class, File.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (isGeneratingSpoof.get()) return;
                    File parent = (File) param.args[0];
                    String child = (String) param.args[1];
                    String fullPath = "";
                    if (parent != null) {
                        fullPath += parent.getAbsolutePath();
                    }
                    if (child != null) {
                        fullPath += "/" + child;
                    }
                    if (fullPath.contains("/proc/cpuinfo")) {
                        generateSpoofedCpuInfo(spoofedFile);
                        param.args[0] = spoofedFile.getParentFile();
                        param.args[1] = spoofedFile.getName();
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking File constructors: " + e.getMessage());
        }
    }

    private static void hookFileInputStream(String spoofedPath, File spoofedFile) {
        try {
            XposedHelpers.findAndHookConstructor(FileInputStream.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (isGeneratingSpoof.get()) return;
                    String path = (String) param.args[0];
                    if (path != null && path.contains("/proc/cpuinfo")) {
                        generateSpoofedCpuInfo(spoofedFile);
                        param.args[0] = spoofedPath;
                    }
                }
            });

            XposedHelpers.findAndHookConstructor(FileInputStream.class, File.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (isGeneratingSpoof.get()) return;
                    File file = (File) param.args[0];
                    if (file != null && file.getAbsolutePath().contains("/proc/cpuinfo")) {
                        generateSpoofedCpuInfo(spoofedFile);
                        param.args[0] = spoofedFile;
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Error hooking FileInputStream constructors: " + e.getMessage());
        }
    }
}
