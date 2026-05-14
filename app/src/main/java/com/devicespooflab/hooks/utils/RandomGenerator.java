package com.devicespooflab.hooks.utils;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import com.devicespooflab.hooks.utils.ConfigManager;

/**
 * Generates valid random device identifiers with proper checksums and formats.
 */
public class RandomGenerator {

    private static final SecureRandom random = new SecureRandom();

    public static String generateIMEI() {
        String tac = "35847631"; // Generic Google TAC

        StringBuilder serial = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            serial.append(random.nextInt(10));
        }

        String imeiWithoutCheck = tac + serial.toString();
        int checkDigit = calculateLuhnCheckDigit(imeiWithoutCheck);

        return imeiWithoutCheck + checkDigit;
    }

    public static String generateMEID() {
        StringBuilder meid = new StringBuilder();
        String hexChars = "0123456789ABCDEF";
        for (int i = 0; i < 14; i++) {
            meid.append(hexChars.charAt(random.nextInt(16)));
        }
        return meid.toString();
    }

    public static String generateIMSI() {
        String mcc = "310";
        String mnc = "260";

        StringBuilder msin = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            msin.append(random.nextInt(10));
        }

        return mcc + mnc + msin.toString();
    }

    public static String generateICCID() {
        String prefix = "8901";
        String issuer = "260";

        StringBuilder account = new StringBuilder();
        for (int i = 0; i < 11; i++) {
            account.append(random.nextInt(10));
        }

        String iccidWithoutCheck = prefix + issuer + account.toString();
        int checkDigit = calculateLuhnCheckDigit(iccidWithoutCheck);

        return iccidWithoutCheck + checkDigit;
    }

    public static String generatePhoneNumber() {
        int areaCode = 200 + random.nextInt(800);
        int exchange = 200 + random.nextInt(800);
        int subscriber = random.nextInt(10000);

        return String.format("+1%03d%03d%04d", areaCode, exchange, subscriber);
    }

    public static String generateSerial() {
        String chars = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ"; // No I or O to avoid confusion
        StringBuilder serial = new StringBuilder();
        for (int i = 0; i < 11; i++) {
            serial.append(chars.charAt(random.nextInt(chars.length())));
        }
        return serial.toString();
    }

    public static String generateGAID() {
        return UUID.randomUUID().toString();
    }

    public static String generateMacAddress() {
        StringBuilder mac = new StringBuilder();
        String hexChars = "0123456789ABCDEF";
        for (int i = 0; i < 6; i++) {
            if (i == 0) {
                // first byte must be locally administered and unicast (e.g. 02, 06, 0A, 0E)
                mac.append("02");
            } else {
                mac.append(hexChars.charAt(random.nextInt(16)));
                mac.append(hexChars.charAt(random.nextInt(16)));
            }
            if (i < 5) {
                mac.append(":");
            }
        }
        return mac.toString();
    }

    public static byte[] generateMediaDrmId() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return bytes;
    }

    public static String generateGSFId() {
        StringBuilder gsf = new StringBuilder();
        String hexChars = "0123456789abcdef";
        for (int i = 0; i < 16; i++) {
            gsf.append(hexChars.charAt(random.nextInt(16)));
        }
        return gsf.toString();
    }

    public static String generateAndroidId() {
        StringBuilder androidId = new StringBuilder();
        String hexChars = "0123456789abcdef";
        for (int i = 0; i < 16; i++) {
            androidId.append(hexChars.charAt(random.nextInt(16)));
        }
        return androidId.toString();
    }

    /**
     * Generate Build.FINGERPRINT in format:
     * brand/product/device:version/build_id/incremental:type/keys
     */
    public static String generateFingerprint() {
        String buildId = generateBuildId();
        String incremental = generateIncremental();
        return String.format(
            "google/cheetah/cheetah:15/%s/%s:user/release-keys",
            buildId,
            incremental
        );
    }

    /**
     * Generate Build ID (e.g., AP4A.241205.013)
     * Format: [A-Z]{2}[0-9]{1,2}[A-Z]\.YYMMDD\.XXX
     */
    public static String generateBuildId() {
        String prefix = "AP4A"; // Android 15 prefix
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd", Locale.US);
        String date = dateFormat.format(new Date());
        int build = random.nextInt(999) + 1;
        return String.format("%s.%s.%03d", prefix, date, build);
    }

    /**
     * Generate incremental build number (e.g., 12621605)
     * 8-digit number
     */
    public static String generateIncremental() {
        return String.format("%08d", random.nextInt(100000000));
    }

    /**
     * Generate bootloader version (e.g., cheetah-1.2-A1B2C3D4)
     */
    public static String generateBootloader() {
        StringBuilder hex = new StringBuilder();
        String hexChars = "0123456789ABCDEF";
        for (int i = 0; i < 8; i++) {
            hex.append(hexChars.charAt(random.nextInt(16)));
        }
        return "cheetah-1.2-" + hex.toString();
    }

    /**
     * Generate security patch date (YYYY-MM-DD)
     * Returns a recent date within last 90 days
     */
    public static String generateSecurityPatch() {
        Calendar cal = Calendar.getInstance();
        // Random day within last 90 days
        int daysAgo = random.nextInt(90);
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return dateFormat.format(cal.getTime());
    }

    /**
     * Generate random hex string of specified length
     */
    public static String generateHex(int length) {
        StringBuilder hex = new StringBuilder();
        String hexChars = "0123456789abcdef";
        for (int i = 0; i < length; i++) {
            hex.append(hexChars.charAt(random.nextInt(16)));
        }
        return hex.toString();
    }

    private static int calculateLuhnCheckDigit(String number) {
        int sum = 0;
        boolean alternate = true;

        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return (10 - (sum % 10)) % 10;
    }

    public static class HardwarePreset {
        public final String brand;
        public final String manufacturer;
        public final String model;
        public final String productName;
        public final String deviceCode;
        public final String board;
        public final String hardware;
        public final String boardPlatform;
        public final String screenWidth;
        public final String screenHeight;
        public final String screenDensity;
        public final String buildRelease;
        public final String buildSdk;

        public HardwarePreset(String brand, String manufacturer, String model, String productName, String deviceCode, String board, String hardware, String boardPlatform, String screenWidth, String screenHeight, String screenDensity, String buildRelease, String buildSdk) {
            this.brand = brand;
            this.manufacturer = manufacturer;
            this.model = model;
            this.productName = productName;
            this.deviceCode = deviceCode;
            this.board = board;
            this.hardware = hardware;
            this.boardPlatform = boardPlatform;
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            this.screenDensity = screenDensity;
            this.buildRelease = buildRelease;
            this.buildSdk = buildSdk;
        }
    }

    public static final List<HardwarePreset> AVAILABLE_PRESETS = Arrays.asList(
        new HardwarePreset("google", "Google", "Pixel 7 Pro", "cheetah", "cheetah", "cheetah", "cheetah", "gs201", "1440", "3120", "512", "15", "35"),
            new HardwarePreset("samsung", "Samsung", "SM-S918B", "dm3", "dm3xxx", "kalama", "qcom", "kalama", "1440", "3088", "500", "13", "33"),
            new HardwarePreset("samsung", "Samsung", "SM-S911B", "dm1", "dm1xxx", "kalama", "qcom", "kalama", "1080", "2340", "425", "13", "33"),
            new HardwarePreset("samsung", "Samsung", "SM-S908B", "b0q", "b0qxxx", "exynos2200", "exynos", "exynos2200", "1440", "3088", "500", "12", "31"),
            new HardwarePreset("samsung", "Samsung", "SM-S901B", "r0q", "r0qxxx", "exynos2200", "exynos", "exynos2200", "1080", "2340", "425", "12", "31"),
            new HardwarePreset("samsung", "Samsung", "SM-G998B", "p3s", "p3sxxx", "exynos2100", "exynos", "exynos2100", "1440", "3200", "515", "11", "30"),
            new HardwarePreset("samsung", "Samsung", "SM-G991B", "o1s", "o1sxxx", "exynos2100", "exynos", "exynos2100", "1080", "2400", "421", "11", "30"),
            new HardwarePreset("samsung", "Samsung", "SM-G988B", "z3s", "z3sxxx", "exynos990", "exynos", "exynos990", "1440", "3200", "511", "10", "29"),
            new HardwarePreset("samsung", "Samsung", "SM-G981B", "x1s", "x1sxxx", "exynos990", "exynos", "exynos990", "1440", "3200", "563", "10", "29"),
            new HardwarePreset("samsung", "Samsung", "SM-F936B", "q4q", "q4qxxx", "taro", "qcom", "taro", "1768", "2208", "373", "12", "31"),
            new HardwarePreset("samsung", "Samsung", "SM-F721B", "b4q", "b4qxxx", "taro", "qcom", "taro", "1080", "2640", "426", "12", "31"),
            new HardwarePreset("samsung", "Samsung", "SM-F926B", "q3q", "q3qxxx", "lahaina", "qcom", "lahaina", "1768", "2208", "373", "11", "30"),
            new HardwarePreset("samsung", "Samsung", "SM-F711B", "b3q", "b3qxxx", "lahaina", "qcom", "lahaina", "1080", "2640", "426", "11", "30"),
            new HardwarePreset("samsung", "Samsung", "SM-A546B", "a54x", "a54xxxx", "s5e8835", "exynos", "s5e8835", "1080", "2340", "403", "13", "33"),
            new HardwarePreset("samsung", "Samsung", "SM-A536B", "a53x", "a53xxxx", "s5e8825", "exynos", "s5e8825", "1080", "2400", "405", "12", "31"),
            new HardwarePreset("samsung", "Samsung", "SM-A528B", "a52sq", "a52sqxxx", "lahaina", "qcom", "lahaina", "1080", "2400", "405", "11", "30"),
            new HardwarePreset("samsung", "Samsung", "SM-A346B", "a34x", "a34xxxx", "mt6877v", "mtk", "mt6877v", "1080", "2340", "390", "13", "33"),
            new HardwarePreset("samsung", "Samsung", "SM-A336B", "a33x", "a33xxxx", "s5e8825", "exynos", "s5e8825", "1080", "2400", "411", "12", "31"),
            new HardwarePreset("samsung", "Samsung", "SM-M536B", "m53x", "m53xxxx", "mt6877", "mtk", "mt6877", "1080", "2400", "394", "12", "31"),
            new HardwarePreset("samsung", "Samsung", "SM-M336B", "m33x", "m33xxxx", "s5e8825", "exynos", "s5e8825", "1080", "2408", "400", "12", "31"),
            new HardwarePreset("samsung", "Samsung", "SM-A736B", "a73x", "a73xxxx", "lahaina", "qcom", "lahaina", "1080", "2400", "393", "12", "31"),
            new HardwarePreset("samsung", "Samsung", "SM-A725F", "a72q", "a72qxxx", "atoll", "qcom", "atoll", "1080", "2400", "394", "11", "30"),
            new HardwarePreset("samsung", "Samsung", "SM-A715F", "a71q", "a71qxxx", "sm6150", "qcom", "sm6150", "1080", "2400", "393", "10", "29"),
            new HardwarePreset("samsung", "Samsung", "SM-G781B", "r8q", "r8qxxx", "kona", "qcom", "kona", "1080", "2400", "407", "10", "29"),
            new HardwarePreset("samsung", "Samsung", "SM-G975F", "beyond2", "beyond2xxx", "exynos9820", "exynos", "exynos9820", "1440", "3040", "522", "9", "28"),
            new HardwarePreset("samsung", "Samsung", "SM-G973F", "beyond1", "beyond1xxx", "exynos9820", "exynos", "exynos9820", "1440", "3040", "550", "9", "28"),
            new HardwarePreset("samsung", "Samsung", "SM-N986B", "c2s", "c2sxxx", "exynos990", "exynos", "exynos990", "1440", "3088", "496", "10", "29"),
            new HardwarePreset("samsung", "Samsung", "SM-N975F", "d2s", "d2sxxx", "exynos9825", "exynos", "exynos9825", "1440", "3040", "498", "9", "28"),
            new HardwarePreset("samsung", "Samsung", "SM-A146B", "a14x", "a14xxxx", "s5e8535", "exynos", "s5e8535", "1080", "2408", "400", "13", "33"),
            new HardwarePreset("samsung", "Samsung", "SM-A136B", "a13x", "a13xxxx", "mt6833", "mtk", "mt6833", "720", "1600", "270", "12", "31"),
            new HardwarePreset("samsung", "Samsung", "SM-A236B", "a23x", "a23xxxx", "hollywood", "qcom", "hollywood", "1080", "2408", "400", "12", "31"),
            new HardwarePreset("samsung", "Samsung", "SM-S928B", "e3q", "e3qxxx", "pineapple", "qcom", "pineapple", "1440", "3120", "505", "14", "34"),
            new HardwarePreset("samsung", "Samsung", "SM-S921B", "e1s", "e1sxxx", "s5e9945", "exynos", "s5e9945", "1080", "2340", "416", "14", "34"),
            new HardwarePreset("samsung", "Samsung", "SM-F946B", "q5q", "q5qxxx", "kalama", "qcom", "kalama", "1812", "2176", "373", "13", "33"),
            new HardwarePreset("samsung", "Samsung", "SM-F731B", "b5q", "b5qxxx", "kalama", "qcom", "kalama", "1080", "2640", "425", "13", "33"),
            new HardwarePreset("samsung", "Samsung", "SM-A556B", "a55x", "a55xxxx", "s5e8845", "exynos", "s5e8845", "1080", "2340", "390", "14", "34"),
            new HardwarePreset("samsung", "Samsung", "SM-A356B", "a35x", "a35xxxx", "s5e8835", "exynos", "s5e8835", "1080", "2340", "390", "14", "34"),
            new HardwarePreset("samsung", "Samsung", "SM-M546B", "m54x", "m54xxxx", "s5e8835", "exynos", "s5e8835", "1080", "2400", "393", "13", "33"),
            new HardwarePreset("samsung", "Samsung", "SM-M346B", "m34x", "m34xxxx", "s5e8825", "exynos", "s5e8825", "1080", "2340", "396", "13", "33"),
            new HardwarePreset("samsung", "Samsung", "SM-A245F", "a24", "a24xxx", "mt6789", "mtk", "mt6789", "1080", "2340", "396", "13", "33"),
            new HardwarePreset("samsung", "Samsung", "SM-A156B", "a15x", "a15xxxx", "mt6835", "mtk", "mt6835", "1080", "2340", "396", "14", "34"),
            new HardwarePreset("samsung", "Samsung", "SM-A055F", "a05", "a05xxx", "mt6769", "mtk", "mt6769", "720", "1600", "262", "13", "33"),
            new HardwarePreset("samsung", "Samsung", "SM-F956B", "q6q", "q6qxxx", "pineapple", "qcom", "pineapple", "1856", "2160", "374", "14", "34"),
            new HardwarePreset("samsung", "Samsung", "SM-F741B", "b6q", "b6qxxx", "pineapple", "qcom", "pineapple", "1080", "2640", "426", "14", "34"),
            new HardwarePreset("samsung", "Samsung", "SM-S711B", "r9q", "r9qxxx", "exynos2200", "exynos", "exynos2200", "1080", "2340", "403", "13", "33"),
            new HardwarePreset("samsung", "Samsung", "SM-G990B", "r9s", "r9sxxx", "lahaina", "qcom", "lahaina", "1080", "2400", "401", "12", "31"),
            new HardwarePreset("samsung", "Samsung", "SM-A525F", "a52", "a52xxx", "atoll", "qcom", "atoll", "1080", "2400", "407", "11", "30"),
            new HardwarePreset("samsung", "Samsung", "SM-A325F", "a32", "a32xxx", "mt6769", "mtk", "mt6769", "1080", "2400", "411", "11", "30"),
            new HardwarePreset("samsung", "Samsung", "SM-M146B", "m14x", "m14xxxx", "s5e8535", "exynos", "s5e8535", "1080", "2408", "400", "13", "33"),
            new HardwarePreset("samsung", "Samsung", "SM-M045F", "m04", "m04xxx", "mt6765", "mtk", "mt6765", "720", "1600", "270", "12", "31"),
            new HardwarePreset("samsung", "Samsung", "SM-X910", "gts9u", "gts9uxxx", "kalama", "qcom", "kalama", "1848", "2960", "240", "13", "33")
    );

    public static Map<String, String> generateBatchRandomization() {
        Map<String, String> properties = new HashMap<>();
        HardwarePreset preset = AVAILABLE_PRESETS.get(random.nextInt(AVAILABLE_PRESETS.size()));

        String buildId = generateBuildId();
        String incremental = generateIncremental();
        String bootloader = generateBootloader(preset);
        String securityPatch = generateSecurityPatch();

        // Identifiers
        properties.put(ConfigManager.KEY_SPOOF_IMEI, generateIMEI());
        properties.put(ConfigManager.KEY_SPOOF_MEID, generateMEID());
        properties.put(ConfigManager.KEY_SPOOF_IMSI, generateIMSI());
        properties.put(ConfigManager.KEY_SPOOF_ICCID, generateICCID());
        properties.put(ConfigManager.KEY_SPOOF_PHONE_NUMBER, generatePhoneNumber());
        properties.put(ConfigManager.KEY_SPOOF_GAID, generateGAID());
        properties.put(ConfigManager.KEY_SPOOF_GSF_ID, generateGSFId());

        byte[] drmId = generateMediaDrmId();
        StringBuilder drmIdHex = new StringBuilder();
        for (byte b : drmId) {
            drmIdHex.append(String.format("%02x", b));
        }
        properties.put(ConfigManager.KEY_SPOOF_MEDIA_DRM_ID, drmIdHex.toString());
        properties.put(ConfigManager.KEY_SPOOF_APP_SET_ID, generateGAID());
        properties.put("ANDROID_ID", generateAndroidId());
        properties.put("ro.serialno", generateSerial());
        properties.put("ro.boot.serialno", generateSerial());
        properties.put("device.mac_address", generateMacAddress());
        properties.put("device.wifi_mac_address", generateMacAddress());
        properties.put("device.bluetooth_mac_address", generateMacAddress());

        // Device Profile Properties
        properties.put("ro.product.brand", preset.brand);
        properties.put("ro.product.manufacturer", preset.manufacturer);
        properties.put("ro.product.model", preset.model);
        properties.put("ro.product.name", preset.productName);
        properties.put("ro.product.device", preset.deviceCode);
        properties.put("ro.product.board", preset.board);
        properties.put("ro.hardware", preset.hardware);
        properties.put("ro.boot.hardware", preset.hardware);
        properties.put("ro.board.platform", preset.boardPlatform);

        String[] partitions = {"product", "system", "system_ext", "vendor", "vendor_dlkm", "odm", "bootimage", "system_dlkm"};
        for (String partition : partitions) {
            properties.put("ro.product." + partition + ".brand", preset.brand);
            properties.put("ro.product." + partition + ".manufacturer", preset.manufacturer);
            properties.put("ro.product." + partition + ".model", preset.model);
            properties.put("ro.product." + partition + ".name", preset.productName);
            properties.put("ro.product." + partition + ".device", preset.deviceCode);
        }

        String fingerprint = generateFingerprint(preset, buildId, incremental);
        properties.put("ro.build.fingerprint", fingerprint);
        properties.put("ro.build.id", buildId);
        properties.put("ro.build.display.id", buildId);
        properties.put("ro.build.version.incremental", incremental);
        properties.put("ro.build.version.release", preset.buildRelease);
        properties.put("ro.build.version.release_or_codename", preset.buildRelease);
        properties.put("ro.build.version.release_or_preview_display", preset.buildRelease);
        properties.put("ro.build.version.sdk", preset.buildSdk);
        properties.put("ro.build.version.security_patch", securityPatch);

        properties.put("ro.product.build.fingerprint", fingerprint);
        properties.put("ro.product.build.id", buildId);
        properties.put("ro.product.build.version.incremental", incremental);
        properties.put("ro.product.build.version.release", preset.buildRelease);
        properties.put("ro.product.build.version.release_or_codename", preset.buildRelease);
        properties.put("ro.product.build.version.sdk", preset.buildSdk);

        properties.put("ro.system.build.fingerprint", fingerprint);
        properties.put("ro.system_ext.build.fingerprint", fingerprint);
        properties.put("ro.vendor.build.fingerprint", fingerprint);
        properties.put("ro.odm.build.fingerprint", fingerprint);
        properties.put("ro.bootimage.build.fingerprint", fingerprint);
        properties.put("ro.system_dlkm.build.fingerprint", fingerprint);
        properties.put("ro.vendor_dlkm.build.fingerprint", fingerprint);

        properties.put("ro.vendor.build.version.release", preset.buildRelease);
        properties.put("ro.vendor.build.version.release_or_codename", preset.buildRelease);
        properties.put("ro.vendor_dlkm.build.version.release", preset.buildRelease);
        properties.put("ro.vendor_dlkm.build.version.release_or_codename", preset.buildRelease);
        properties.put("ro.odm.build.version.release", preset.buildRelease);
        properties.put("ro.odm.build.version.release_or_codename", preset.buildRelease);
        properties.put("ro.bootimage.build.version.release", preset.buildRelease);
        properties.put("ro.bootimage.build.version.release_or_codename", preset.buildRelease);
        properties.put("ro.system_dlkm.build.version.release", preset.buildRelease);
        properties.put("ro.system_dlkm.build.version.release_or_codename", preset.buildRelease);

        properties.put("ro.bootloader", bootloader);

        properties.put("screen.width", preset.screenWidth);
        properties.put("screen.height", preset.screenHeight);
        properties.put("screen.density", preset.screenDensity);
        properties.put("ro.sf.lcd_density", preset.screenDensity);

        String description = String.format("%s-user %s %s %s release-keys", preset.deviceCode, preset.buildRelease, buildId, incremental);
        properties.put("ro.build.description", description);

        return properties;
    }

    public static String generateFingerprint(HardwarePreset preset, String buildId, String incremental) {
        return String.format(
            "%s/%s/%s:%s/%s/%s:user/release-keys",
            preset.brand.toLowerCase(Locale.US),
            preset.productName,
            preset.deviceCode,
            preset.buildRelease,
            buildId,
            incremental
        );
    }

    public static String generateBootloader(HardwarePreset preset) {
        StringBuilder hex = new StringBuilder();
        String hexChars = "0123456789ABCDEF";
        for (int i = 0; i < 8; i++) {
            hex.append(hexChars.charAt(random.nextInt(16)));
        }
        return preset.deviceCode + "-1.2-" + hex.toString();
    }
}
