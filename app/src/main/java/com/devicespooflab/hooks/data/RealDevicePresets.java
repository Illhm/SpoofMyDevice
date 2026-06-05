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

        // 1. Google Pixel 8 Pro (Husky) - Android 14
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

        // 2. Google Pixel 7 (Panther) - Android 13
        DeviceProfile pixel7 = new DeviceProfile();
        pixel7.setBrand("google");
        pixel7.setManufacturer("Google");
        pixel7.setModel("Pixel 7");
        pixel7.setProductName("panther");
        pixel7.setDeviceCode("panther");
        pixel7.setBoard("panther");
        pixel7.setHardware("panther");
        pixel7.setBoardPlatform("gs201");
        pixel7.setSocModel("gs201");
        pixel7.setSocManufacturer("Google");
        pixel7.setCpuAbi("arm64-v8a");
        pixel7.setCpuAbiList("arm64-v8a");
        pixel7.setCpuAbiList64("arm64-v8a");
        pixel7.setCpuAbiList32("");
        pixel7.setBuildFingerprint("google/panther/panther:13/TQ3A.230805.001/10316531:user/release-keys");
        pixel7.setBuildId("TQ3A.230805.001");
        pixel7.setBuildDisplayId("TQ3A.230805.001");
        pixel7.setBuildIncremental("10316531");
        pixel7.setBuildRelease("13");
        pixel7.setBuildSdk(33);
        pixel7.setSecurityPatch("2023-08-05");
        pixel7.setScreenWidth(1080);
        pixel7.setScreenHeight(2400);
        pixel7.setScreenDensity(420);
        presets.add(new DevicePreset("google_pixel_7_13", "Google", "Pixel 7", "Google Tensor G2", pixel7));

        // 3. Samsung Galaxy S24 Ultra (SM-S928B) - Android 14
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

        // 4. Samsung Galaxy S23 Ultra (SM-S918B) - Android 14
        DeviceProfile s23u = new DeviceProfile();
        s23u.setBrand("samsung");
        s23u.setManufacturer("samsung");
        s23u.setModel("SM-S918B");
        s23u.setProductName("dm3qxxe");
        s23u.setDeviceCode("dm3q");
        s23u.setBoard("dm3q");
        s23u.setHardware("qcom");
        s23u.setBoardPlatform("kalama");
        s23u.setSocModel("SM8550");
        s23u.setSocManufacturer("Qualcomm");
        s23u.setCpuAbi("arm64-v8a");
        s23u.setCpuAbiList("arm64-v8a,armeabi-v7a,armeabi");
        s23u.setCpuAbiList64("arm64-v8a");
        s23u.setCpuAbiList32("armeabi-v7a,armeabi");
        s23u.setBuildFingerprint("samsung/dm3qxxe/dm3q:14/UP1A.231005.007/S918BXXS3BWL3:user/release-keys");
        s23u.setBuildId("UP1A.231005.007");
        s23u.setBuildDisplayId("UP1A.231005.007");
        s23u.setBuildIncremental("S918BXXS3BWL3");
        s23u.setBuildRelease("14");
        s23u.setBuildSdk(34);
        s23u.setSecurityPatch("2023-12-01");
        s23u.setScreenWidth(1440);
        s23u.setScreenHeight(3088);
        s23u.setScreenDensity(500);
        presets.add(new DevicePreset("samsung_s23_ultra_14", "Samsung", "Galaxy S23 Ultra", "Qualcomm Snapdragon 8 Gen 2", s23u));

        // 5. Samsung Galaxy Z Fold 5 (SM-F946B) - Android 13
        DeviceProfile zfold5 = new DeviceProfile();
        zfold5.setBrand("samsung");
        zfold5.setManufacturer("samsung");
        zfold5.setModel("SM-F946B");
        zfold5.setProductName("q5qxxe");
        zfold5.setDeviceCode("q5q");
        zfold5.setBoard("q5q");
        zfold5.setHardware("qcom");
        zfold5.setBoardPlatform("kalama");
        zfold5.setSocModel("SM8550");
        zfold5.setSocManufacturer("Qualcomm");
        zfold5.setCpuAbi("arm64-v8a");
        zfold5.setCpuAbiList("arm64-v8a,armeabi-v7a,armeabi");
        zfold5.setCpuAbiList64("arm64-v8a");
        zfold5.setCpuAbiList32("armeabi-v7a,armeabi");
        zfold5.setBuildFingerprint("samsung/q5qxxe/q5q:13/TP1A.220624.014/F946BXXU1AWH3:user/release-keys");
        zfold5.setBuildId("TP1A.220624.014");
        zfold5.setBuildDisplayId("TP1A.220624.014");
        zfold5.setBuildIncremental("F946BXXU1AWH3");
        zfold5.setBuildRelease("13");
        zfold5.setBuildSdk(33);
        zfold5.setSecurityPatch("2023-08-01");
        zfold5.setScreenWidth(1812);
        zfold5.setScreenHeight(2176);
        zfold5.setScreenDensity(374);
        presets.add(new DevicePreset("samsung_z_fold_5_13", "Samsung", "Galaxy Z Fold 5", "Qualcomm Snapdragon 8 Gen 2", zfold5));

        // 6. Xiaomi 14 (Houji) - Android 14
        DeviceProfile xiaomi14 = new DeviceProfile();
        xiaomi14.setBrand("Xiaomi");
        xiaomi14.setManufacturer("Xiaomi");
        xiaomi14.setModel("23127PN0CG");
        xiaomi14.setProductName("houji_global");
        xiaomi14.setDeviceCode("houji");
        xiaomi14.setBoard("houji");
        xiaomi14.setHardware("qcom");
        xiaomi14.setBoardPlatform("pineapple");
        xiaomi14.setSocModel("SM8650");
        xiaomi14.setSocManufacturer("Qualcomm");
        xiaomi14.setCpuAbi("arm64-v8a");
        xiaomi14.setCpuAbiList("arm64-v8a,armeabi-v7a,armeabi");
        xiaomi14.setCpuAbiList64("arm64-v8a");
        xiaomi14.setCpuAbiList32("armeabi-v7a,armeabi");
        xiaomi14.setBuildFingerprint("Xiaomi/houji_global/houji:14/UKQ1.230804.001/V816.0.1.0.UNCMIXM:user/release-keys");
        xiaomi14.setBuildId("UKQ1.230804.001");
        xiaomi14.setBuildDisplayId("UKQ1.230804.001 test-keys");
        xiaomi14.setBuildIncremental("V816.0.1.0.UNCMIXM");
        xiaomi14.setBuildRelease("14");
        xiaomi14.setBuildSdk(34);
        xiaomi14.setSecurityPatch("2024-01-01");
        xiaomi14.setScreenWidth(1200);
        xiaomi14.setScreenHeight(2670);
        xiaomi14.setScreenDensity(460);
        presets.add(new DevicePreset("xiaomi_14_14", "Xiaomi", "14", "Qualcomm Snapdragon 8 Gen 3", xiaomi14));

        // 7. Xiaomi 13 Pro (Nuwa) - Android 13
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

        // 8. POCO F5 (Marble) - Android 13
        DeviceProfile pocof5 = new DeviceProfile();
        pocof5.setBrand("POCO");
        pocof5.setManufacturer("Xiaomi");
        pocof5.setModel("23049PCD8G");
        pocof5.setProductName("marble_global");
        pocof5.setDeviceCode("marble");
        pocof5.setBoard("marble");
        pocof5.setHardware("qcom");
        pocof5.setBoardPlatform("taro");
        pocof5.setSocModel("SM7475");
        pocof5.setSocManufacturer("Qualcomm");
        pocof5.setCpuAbi("arm64-v8a");
        pocof5.setCpuAbiList("arm64-v8a,armeabi-v7a,armeabi");
        pocof5.setCpuAbiList64("arm64-v8a");
        pocof5.setCpuAbiList32("armeabi-v7a,armeabi");
        pocof5.setBuildFingerprint("POCO/marble_global/marble:13/TKQ1.221114.001/V14.0.4.0.TMRMIXM:user/release-keys");
        pocof5.setBuildId("TKQ1.221114.001");
        pocof5.setBuildDisplayId("TKQ1.221114.001 test-keys");
        pocof5.setBuildIncremental("V14.0.4.0.TMRMIXM");
        pocof5.setBuildRelease("13");
        pocof5.setBuildSdk(33);
        pocof5.setSecurityPatch("2023-05-01");
        pocof5.setScreenWidth(1080);
        pocof5.setScreenHeight(2400);
        pocof5.setScreenDensity(440);
        presets.add(new DevicePreset("poco_f5_13", "POCO", "F5", "Qualcomm Snapdragon 7+ Gen 2", pocof5));

        // 9. OnePlus 11 5G (CPH2449) - Android 13
        DeviceProfile op11 = new DeviceProfile();
        op11.setBrand("OnePlus");
        op11.setManufacturer("OnePlus");
        op11.setModel("CPH2449");
        op11.setProductName("CPH2449");
        op11.setDeviceCode("salami");
        op11.setBoard("kalama");
        op11.setHardware("qcom");
        op11.setBoardPlatform("kalama");
        op11.setSocModel("SM8550");
        op11.setSocManufacturer("Qualcomm");
        op11.setCpuAbi("arm64-v8a");
        op11.setCpuAbiList("arm64-v8a,armeabi-v7a,armeabi");
        op11.setCpuAbiList64("arm64-v8a");
        op11.setCpuAbiList32("armeabi-v7a,armeabi");
        op11.setBuildFingerprint("OnePlus/CPH2449/salami:13/TKQ1.221114.001/Q.15dfcb-1:user/release-keys");
        op11.setBuildId("TKQ1.221114.001");
        op11.setBuildDisplayId("CPH2449_11_A.09");
        op11.setBuildIncremental("Q.15dfcb-1");
        op11.setBuildRelease("13");
        op11.setBuildSdk(33);
        op11.setSecurityPatch("2023-02-05");
        op11.setScreenWidth(1440);
        op11.setScreenHeight(3216);
        op11.setScreenDensity(525);
        presets.add(new DevicePreset("oneplus_11_13", "OnePlus", "11 5G", "Qualcomm Snapdragon 8 Gen 2", op11));

        // 10. Vivo X90 Pro (V2219) - Android 13
        DeviceProfile x90pro = new DeviceProfile();
        x90pro.setBrand("vivo");
        x90pro.setManufacturer("vivo");
        x90pro.setModel("V2219");
        x90pro.setProductName("V2219");
        x90pro.setDeviceCode("V2219");
        x90pro.setBoard("k6985v1_64");
        x90pro.setHardware("mt6985");
        x90pro.setBoardPlatform("mt6985");
        x90pro.setSocModel("MT6985");
        x90pro.setSocManufacturer("MediaTek");
        x90pro.setCpuAbi("arm64-v8a");
        x90pro.setCpuAbiList("arm64-v8a,armeabi-v7a,armeabi");
        x90pro.setCpuAbiList64("arm64-v8a");
        x90pro.setCpuAbiList32("armeabi-v7a,armeabi");
        x90pro.setBuildFingerprint("vivo/V2219/V2219:13/TP1A.220624.014/compiler01041049:user/release-keys");
        x90pro.setBuildId("TP1A.220624.014");
        x90pro.setBuildDisplayId("PD2242F_EX_A_13.1.11.2.W30");
        x90pro.setBuildIncremental("compiler01041049");
        x90pro.setBuildRelease("13");
        x90pro.setBuildSdk(33);
        x90pro.setSecurityPatch("2023-01-01");
        x90pro.setScreenWidth(1260);
        x90pro.setScreenHeight(2800);
        x90pro.setScreenDensity(452);
        presets.add(new DevicePreset("vivo_x90_pro_13", "Vivo", "X90 Pro", "MediaTek Dimensity 9200", x90pro));

        // 11. Nothing Phone (2) (A065) - Android 13
        DeviceProfile nothing2 = new DeviceProfile();
        nothing2.setBrand("Nothing");
        nothing2.setManufacturer("Nothing");
        nothing2.setModel("A065");
        nothing2.setProductName("Pong");
        nothing2.setDeviceCode("Pong");
        nothing2.setBoard("taro");
        nothing2.setHardware("qcom");
        nothing2.setBoardPlatform("taro");
        nothing2.setSocModel("SM8475");
        nothing2.setSocManufacturer("Qualcomm");
        nothing2.setCpuAbi("arm64-v8a");
        nothing2.setCpuAbiList("arm64-v8a,armeabi-v7a,armeabi");
        nothing2.setCpuAbiList64("arm64-v8a");
        nothing2.setCpuAbiList32("armeabi-v7a,armeabi");
        nothing2.setBuildFingerprint("Nothing/Pong/Pong:13/TKQ1.221114.001/1690038848:user/release-keys");
        nothing2.setBuildId("TKQ1.221114.001");
        nothing2.setBuildDisplayId("Nothing OS 2.0.1");
        nothing2.setBuildIncremental("1690038848");
        nothing2.setBuildRelease("13");
        nothing2.setBuildSdk(33);
        nothing2.setSecurityPatch("2023-07-01");
        nothing2.setScreenWidth(1080);
        nothing2.setScreenHeight(2412);
        nothing2.setScreenDensity(394);
        presets.add(new DevicePreset("nothing_phone_2_13", "Nothing", "Phone (2)", "Qualcomm Snapdragon 8+ Gen 1", nothing2));

        // 12. Asus ROG Phone 7 (ASUS_AI2205) - Android 13
        DeviceProfile rog7 = new DeviceProfile();
        rog7.setBrand("asus");
        rog7.setManufacturer("asus");
        rog7.setModel("ASUS_AI2205_C");
        rog7.setProductName("WW_AI2205");
        rog7.setDeviceCode("AI2205");
        rog7.setBoard("kalama");
        rog7.setHardware("qcom");
        rog7.setBoardPlatform("kalama");
        rog7.setSocModel("SM8550");
        rog7.setSocManufacturer("Qualcomm");
        rog7.setCpuAbi("arm64-v8a");
        rog7.setCpuAbiList("arm64-v8a,armeabi-v7a,armeabi");
        rog7.setCpuAbiList64("arm64-v8a");
        rog7.setCpuAbiList32("armeabi-v7a,armeabi");
        rog7.setBuildFingerprint("asus/WW_AI2205/AI2205:13/TKQ1.221114.001/33.0820.0810.158-0:user/release-keys");
        rog7.setBuildId("TKQ1.221114.001");
        rog7.setBuildDisplayId("33.0820.0810.158");
        rog7.setBuildIncremental("33.0820.0810.158-0");
        rog7.setBuildRelease("13");
        rog7.setBuildSdk(33);
        rog7.setSecurityPatch("2023-05-05");
        rog7.setScreenWidth(1080);
        rog7.setScreenHeight(2448);
        rog7.setScreenDensity(395);
        presets.add(new DevicePreset("asus_rog_7_13", "Asus", "ROG Phone 7", "Qualcomm Snapdragon 8 Gen 2", rog7));

        return presets;
    }
}
