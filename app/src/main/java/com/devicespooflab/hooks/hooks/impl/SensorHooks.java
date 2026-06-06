package com.devicespooflab.hooks.hooks.impl;

import android.hardware.Sensor;
import android.hardware.SensorManager;

import com.devicespooflab.hooks.utils.ConfigManager;
import com.devicespooflab.hooks.diagnostics.HookDiagnostics;

import java.lang.reflect.Field;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SensorHooks {
    private static final String TAG = "DeviceSpoofLab-Sensor";

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> sensorManagerClass = XposedHelpers.findClassIfExists("android.hardware.SystemSensorManager", lpparam.classLoader);
            if (sensorManagerClass == null) {
                sensorManagerClass = SensorManager.class;
            }

            XposedHelpers.findAndHookMethod(sensorManagerClass, "getSensorList", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    @SuppressWarnings("unchecked")
                    List<Sensor> sensors = (List<Sensor>) param.getResult();
                    if (sensors == null || sensors.isEmpty()) {
                        return;
                    }

                    String spoofedVendor = ConfigManager.getOptionalConfigValue(ConfigManager.FIELD_SOC_NAME);
                    if (spoofedVendor == null) {
                        spoofedVendor = "Qualcomm";
                    }

                    for (Sensor sensor : sensors) {
                        try {
                            Field vendorField = Sensor.class.getDeclaredField("mVendor");
                            vendorField.setAccessible(true);
                            vendorField.set(sensor, spoofedVendor);
                        } catch (Exception e) {
                            // Ignored
                        }
                    }
                }
            });

            HookDiagnostics.logHookSuccess("SensorHooks", lpparam.packageName, "Successfully hooked SensorManager.getSensorList");
        } catch (Throwable e) {
            HookDiagnostics.logHookFailed("SensorHooks", lpparam.packageName, e.getMessage());
            XposedBridge.log(TAG + ": Failed to hook SensorManager: " + e.getMessage());
        }
    }
}
