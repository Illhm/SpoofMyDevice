package com.devicespooflab.hooks.data;

import com.devicespooflab.hooks.utils.RandomGenerator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class IntelligentRandomizer {
    private final Random random = new Random();

    public DeviceProfile randomize(List<DevicePreset> presets) {
        DevicePreset basePreset = chooseBasePreset(presets);
        DeviceProfile profile = basePreset.getProfile();
        profile.applyFallbacks();

        String buildId = buildIdForPreset(profile);
        String incremental = RandomGenerator.generateIncremental();
        String securityPatch = LocalDate.now().minusDays(random.nextInt(76)).format(DateTimeFormatter.ISO_DATE);
        String imei = RandomGenerator.generateIMEI();
        String mac = RandomGenerator.generateMacAddress();

        profile.setBuildId(buildId);
        profile.setBuildDisplayId(buildId);
        profile.setBuildIncremental(incremental);
        profile.setSecurityPatch(securityPatch);
        profile.setBuildRelease(profile.getBuildSdk() >= 36 ? "16" : profile.getBuildRelease());

        String fingerprint = String.format(
            Locale.US,
            "%s/%s/%s:%s/%s/%s:user/release-keys",
            profile.getBrand().toLowerCase(Locale.US),
            profile.getProductName(),
            profile.getDeviceCode(),
            profile.getBuildRelease(),
            buildId,
            incremental
        );
        profile.setBuildFingerprint(fingerprint);
        profile.applyFallbacks();

        return profile;
    }

    public String getGeneratedImei() {
        return RandomGenerator.generateIMEI();
    }

    public String getGeneratedMacAddress() {
        return RandomGenerator.generateMacAddress();
    }

    private DevicePreset chooseBasePreset(List<DevicePreset> presets) {
        if (presets == null || presets.isEmpty()) {
            DeviceProfile fallback = new DeviceProfile();
            fallback.applyFallbacks();
            return new DevicePreset("fallback", fallback.getBrand(), fallback.getModel(), "Fallback", fallback);
        }
        int index = random.nextInt(presets.size());
        return presets.get(index);
    }

    private String buildIdForPreset(DeviceProfile profile) {
        String prefix = profile.getBuildSdk() >= 36 ? "BP1A" : "AP4A";
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd", Locale.US));
        int seq = random.nextInt(999) + 1;
        return String.format(Locale.US, "%s.%s.%03d", prefix, date, seq);
    }
}
