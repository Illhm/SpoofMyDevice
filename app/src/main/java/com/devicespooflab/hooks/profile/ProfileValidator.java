package com.devicespooflab.hooks.profile;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ProfileValidator {
    private static final long MIN_RAM_BYTES = 2L * 1024 * 1024 * 1024;
    private static final long MAX_RAM_BYTES = 24L * 1024 * 1024 * 1024;

    private ProfileValidator() {
    }

    @NonNull
    public static List<String> validate(@NonNull ActiveProfile profile) {
        List<String> anomalies = new ArrayList<>();
        String brand = profile.build.brand.toLowerCase(Locale.US);
        String board = profile.build.board.toLowerCase(Locale.US);

        if (brand.contains("samsung") && board.startsWith("gs")) {
            anomalies.add("Brand Samsung tetapi board terlihat seperti Google/Pixel.");
        }
        if (brand.contains("google") && board.startsWith("exynos")) {
            anomalies.add("Brand Google tetapi board terlihat seperti Samsung Exynos.");
        }

        long ramBytes = profile.hardware.totalRamBytes;
        if (ramBytes < MIN_RAM_BYTES || ramBytes > MAX_RAM_BYTES) {
            anomalies.add("RAM tidak realistis. Harus berada di rentang 2-24GB.");
        }

        if (profile.display.width <= 0 || profile.display.height <= 0 || profile.display.densityDpi <= 0) {
            anomalies.add("Display metrics tidak valid (width/height/density harus > 0).");
        }
        return anomalies;
    }
}
