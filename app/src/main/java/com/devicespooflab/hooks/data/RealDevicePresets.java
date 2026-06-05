package com.devicespooflab.hooks.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 100% Real hardware dumps for spoofing accuracy.
 * Removes synthetically generated profiles that often fail hardware correlation checks.
 */
public class RealDevicePresets {

    public static List<DevicePreset> getRealPresets() {
        List<DevicePreset> presets = new ArrayList<>();

        // Google Pixel 8 Pro (Husky) - Android 14
        DeviceProfile pixel8Pro = new DeviceProfile();
        pixel8Pro.setBrand("google");
        pixel8Pro.setManufacturer("Google");
        pixel8Pro.setModel("Pixel 8 Pro");
        pixel8Pro.setProductName("husky");
        pixel8Pro.setDeviceCode("husky");
        pixel8Pro.setBoard("husky");
        pixel8Pro.setHardware("husky");
        pixel8Pro.setBoardPlatform("zuma");
        pixel8Pro.setSocModel("zuma");
        pixel8Pro.setSocManufacturer("Google");
        pixel8Pro.setCpuAbi("arm64-v8a");
        pixel8Pro.setCpuAbiList("arm64-v8a");
        pixel8Pro.setCpuAbiList64("arm64-v8a");
        pixel8Pro.setCpuAbiList32("");
        pixel8Pro.setBuildFingerprint("google/husky/husky:14/UQ1A.240105.004/11228488:user/release-keys");
        pixel8Pro.setBuildId("UQ1A.240105.004");
        pixel8Pro.setBuildDisplayId("UQ1A.240105.004");
        pixel8Pro.setBuildIncremental("11228488");
        pixel8Pro.setBuildRelease("14");
        pixel8Pro.setBuildSdk(34);
        pixel8Pro.setSecurityPatch("2024-01-05");
        pixel8Pro.setScreenWidth(1344);
        pixel8Pro.setScreenHeight(2992);
        pixel8Pro.setScreenDensity(480);
        presets.add(new DevicePreset("google_pixel_8_pro_14", "Google", "Pixel 8 Pro", "Google Tensor G3", pixel8Pro));

        // Samsung Galaxy S24 Ultra (SM-S928B) - Android 14
        DeviceProfile s24u = new DeviceProfile();
        s24u.setBrand("samsung");
        s24u.setManufacturer("samsung");
        s24u.setModel("SM-S928B");
        s24u.setProductName("e3qxxe");
        s24u.setDeviceCode("e3q");
        s24u.setBoard("e3q");
        s24u.setHardware("qcom");
        s24u.setBoardPlatform("pineapple");
        s24u.setSocModel("SM8650");
        s24u.setSocManufacturer("Qualcomm");
        s24u.setCpuAbi("arm64-v8a");
        s24u.setCpuAbiList("arm64-v8a,armeabi-v7a,armeabi");
        s24u.setCpuAbiList64("arm64-v8a");
        s24u.setCpuAbiList32("armeabi-v7a,armeabi");
        s24u.setBuildFingerprint("samsung/e3qxxe/e3q:14/UP1A.231005.007/S928BXXU1AWL8:user/release-keys");
        s24u.setBuildId("UP1A.231005.007");
        s24u.setBuildDisplayId("UP1A.231005.007");
        s24u.setBuildIncremental("S928BXXU1AWL8");
        s24u.setBuildRelease("14");
        s24u.setBuildSdk(34);
        s24u.setSecurityPatch("2024-01-01");
        s24u.setScreenWidth(1440);
        s24u.setScreenHeight(3120);
        s24u.setScreenDensity(500);
        presets.add(new DevicePreset("samsung_s24_ultra_14", "Samsung", "Galaxy S24 Ultra", "Qualcomm Snapdragon 8 Gen 3", s24u));

        // Xiaomi 13 Pro (Nuwa) - Android 13
        DeviceProfile xiaomi13 = new DeviceProfile();
        xiaomi13.setBrand("Xiaomi");
        xiaomi13.setManufacturer("Xiaomi");
        xiaomi13.setModel("2210132G");
        xiaomi13.setProductName("nuwa_global");
        xiaomi13.setDeviceCode("nuwa");
        xiaomi13.setBoard("nuwa");
        xiaomi13.setHardware("qcom");
        xiaomi13.setBoardPlatform("kalama");
        xiaomi13.setSocModel("SM8550");
        xiaomi13.setSocManufacturer("Qualcomm");
        xiaomi13.setCpuAbi("arm64-v8a");
        xiaomi13.setCpuAbiList("arm64-v8a,armeabi-v7a,armeabi");
        xiaomi13.setCpuAbiList64("arm64-v8a");
        xiaomi13.setCpuAbiList32("armeabi-v7a,armeabi");
        xiaomi13.setBuildFingerprint("Xiaomi/nuwa_global/nuwa:13/TKQ1.221114.001/V14.0.19.0.TMBMIXM:user/release-keys");
        xiaomi13.setBuildId("TKQ1.221114.001");
        xiaomi13.setBuildDisplayId("TKQ1.221114.001 test-keys");
        xiaomi13.setBuildIncremental("V14.0.19.0.TMBMIXM");
        xiaomi13.setBuildRelease("13");
        xiaomi13.setBuildSdk(33);
        xiaomi13.setSecurityPatch("2023-04-01");
        xiaomi13.setScreenWidth(1440);
        xiaomi13.setScreenHeight(3200);
        xiaomi13.setScreenDensity(560);
        presets.add(new DevicePreset("xiaomi_13_pro_13", "Xiaomi", "13 Pro", "Qualcomm Snapdragon 8 Gen 2", xiaomi13));

        return presets;
    }
}
