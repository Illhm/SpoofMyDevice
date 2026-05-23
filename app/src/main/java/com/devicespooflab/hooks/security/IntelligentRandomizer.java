package com.devicespooflab.hooks.security;

import androidx.annotation.NonNull;

import com.devicespooflab.hooks.data.DeviceProfile;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Random;

public final class IntelligentRandomizer {
    private static final Random RNG = new Random();

    private IntelligentRandomizer() {}

    @NonNull
    public static DeviceProfile randomizeChronological(@NonNull DeviceProfile base) {
        DeviceProfile out = base.copy();
        out.applyFallbacks();

        LocalDate buildDate = LocalDate.now().minusDays(RNG.nextInt(120));
        LocalDate patchDate = buildDate.minusDays(7 + RNG.nextInt(45));
        out.setSecurityPatch(patchDate.toString());
        out.setBuildIncremental(String.format(Locale.US, "%s%03d", buildDate.toString().replace("-", ""), RNG.nextInt(999)));
        out.setBuildId("AP" + (RNG.nextInt(8) + 1) + "A." + buildDate.toString().replace("-", "") + "." + (100 + RNG.nextInt(899)));
        out.setBuildDisplayId(out.getBuildId());
        out.setBuildDescription(out.getDeviceCode() + "-user " + out.getBuildRelease() + " " + out.getBuildId() + " " + out.getBuildIncremental() + " release-keys");
        out.setSerialNumber(randomDigits(12));
        return out;
    }

    @NonNull
    public static String generateImei() {
        String body = randomDigits(14);
        return body + luhnDigit(body);
    }

    private static String randomDigits(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(RNG.nextInt(10));
        return sb.toString();
    }

    private static int luhnDigit(String body) {
        int sum = 0;
        boolean alt = true;
        for (int i = body.length() - 1; i >= 0; i--) {
            int d = body.charAt(i) - '0';
            if (alt) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            alt = !alt;
        }
        return (10 - (sum % 10)) % 10;
    }
}
