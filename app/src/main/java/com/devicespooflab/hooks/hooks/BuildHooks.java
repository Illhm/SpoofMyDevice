package com.devicespooflab.hooks.hooks;

import com.devicespooflab.hooks.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class BuildHooks {
    private static final String TAG = "DeviceSpoofLab-Build";

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        reapply(lpparam.classLoader, lpparam.packageName);
    }

    public static void reapply(ClassLoader cl, String pkg) {
        try {
            Class<?> build = XposedHelpers.findClassIfExists("android.os.Build", cl);
            Class<?> version = XposedHelpers.findClassIfExists("android.os.Build$VERSION", cl);
            if (build == null) return;

            HookValueResolver r = HookValueResolver.forPackage(pkg);
            set(build, "BRAND", r.resolveBuildField("BRAND", ConfigManager.FIELD_BRAND, str(build, "BRAND")));
            set(build, "MANUFACTURER", r.resolveBuildField("MANUFACTURER", ConfigManager.FIELD_MANUFACTURER, str(build, "MANUFACTURER")));
            set(build, "MODEL", r.resolveBuildField("MODEL", ConfigManager.FIELD_MODEL, str(build, "MODEL")));
            set(build, "DEVICE", r.resolveBuildField("DEVICE", ConfigManager.FIELD_DEVICE, str(build, "DEVICE")));
            set(build, "PRODUCT", r.resolveBuildField("PRODUCT", ConfigManager.FIELD_PRODUCT, str(build, "PRODUCT")));
            set(build, "BOARD", r.resolveBuildField("BOARD", ConfigManager.FIELD_BOARD, str(build, "BOARD")));
            set(build, "HARDWARE", r.resolveBuildField("HARDWARE", ConfigManager.FIELD_HARDWARE, str(build, "HARDWARE")));
            set(build, "FINGERPRINT", r.resolveBuildField("FINGERPRINT", ConfigManager.FIELD_FINGERPRINT, str(build, "FINGERPRINT")));
            set(build, "ID", r.resolveBuildField("ID", ConfigManager.FIELD_BUILD_ID, str(build, "ID")));
            set(build, "DISPLAY", r.resolveBuildField("DISPLAY", ConfigManager.FIELD_BUILD_ID, str(build, "DISPLAY")));
            set(build, "TAGS", r.resolveBuildField("TAGS", ConfigManager.FIELD_BUILD_ID, str(build, "TAGS")));
            set(build, "TYPE", r.resolveBuildField("TYPE", ConfigManager.FIELD_BUILD_ID, str(build, "TYPE")));
            set(build, "BOOTLOADER", r.resolveBuildField("BOOTLOADER", ConfigManager.FIELD_BUILD_ID, str(build, "BOOTLOADER")));
            set(build, "SERIAL", r.resolveBuildField("SERIAL", ConfigManager.FIELD_BUILD_ID, str(build, "SERIAL")));

            set(build, "SUPPORTED_ABIS", r.resolveSupportedAbis(strArr(build, "SUPPORTED_ABIS")));
            set(build, "CPU_ABI", r.resolveCpuAbi(str(build, "CPU_ABI")));
            set(build, "CPU_ABI2", r.resolveCpuAbi2(str(build, "CPU_ABI2")));

            if (version != null) {
                set(version, "RELEASE", r.resolveVersionField("RELEASE", str(version, "RELEASE")));
                set(version, "CODENAME", r.resolveVersionField("CODENAME", str(version, "CODENAME")));
                set(version, "INCREMENTAL", r.resolveVersionField("INCREMENTAL", str(version, "INCREMENTAL")));
                set(version, "SECURITY_PATCH", r.resolveVersionField("SECURITY_PATCH", str(version, "SECURITY_PATCH")));
                setInt(version, "SDK_INT", r.resolveSdkInt(intVal(version, "SDK_INT", 0)));
                if (XposedHelpers.findFieldIfExists(version, "SDK_INT_FULL") != null) {
                    setInt(version, "SDK_INT_FULL", r.resolveSdkInt(intVal(version, "SDK_INT_FULL", intVal(version, "SDK_INT", 0))));
                }
            }

            XposedHelpers.findAndHookMethod(build, "getSerial", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) { param.setResult(r.resolveBuildField("SERIAL", ConfigManager.FIELD_BUILD_ID, (String) param.getResult())); }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": " + t.getMessage());
        }
    }

    private static String str(Class<?> c, String f) { try { return (String) XposedHelpers.getStaticObjectField(c, f);} catch (Throwable t) { return null; } }
    private static String[] strArr(Class<?> c, String f) { try { return (String[]) XposedHelpers.getStaticObjectField(c, f);} catch (Throwable t) { return null; } }
    private static int intVal(Class<?> c, String f, int d) { try { return XposedHelpers.getStaticIntField(c, f);} catch (Throwable t) { return d; } }
    private static void set(Class<?> c, String f, Object v) { if (v == null || XposedHelpers.findFieldIfExists(c, f) == null) return; try { XposedHelpers.setStaticObjectField(c, f, v);} catch (Throwable ignored) {} }
    private static void setInt(Class<?> c, String f, int v) { if (XposedHelpers.findFieldIfExists(c, f) == null) return; try { XposedHelpers.setStaticIntField(c, f, v);} catch (Throwable ignored) {} }
}
