package com.devicespooflab.hooks.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DeviceProfileValidator {
    private DeviceProfileValidator() {}

    public static List<String> validate(DeviceProfile profile) {
        List<String> issues = new ArrayList<>();
        if (profile == null) {
            issues.add("Profile kosong");
            return issues;
        }
        DeviceProfile p = profile.copy();
        p.applyFallbacks();

        String brand = safe(p.getBrand()).toLowerCase(Locale.US);
        String fp = safe(p.getBuildFingerprint()).toLowerCase(Locale.US);
        if (!brand.isEmpty() && !fp.contains(brand)) {
            issues.add("Brand tidak sinkron dengan fingerprint");
        }
        if (p.getBuildSdk() >= 36 && !"16".equals(p.getBuildRelease())) {
            issues.add("SDK 36 harus memakai Android release 16");
        }
        if (p.getScreenWidth() <= 0 || p.getScreenHeight() <= 0 || p.getScreenDensity() <= 0) {
            issues.add("Screen metrics belum valid");
        }
        if (safe(p.getCpuAbi()).isEmpty() || safe(p.getCpuAbiList()).isEmpty()) {
            issues.add("ABI belum sinkron");
        }
        if (safe(p.getBoard()).isEmpty() || safe(p.getHardware()).isEmpty()) {
            issues.add("Board/hardware kosong");
        }
        return issues;
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
