package com.devicespooflab.hooks.profile.hardware;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class HardwareProfileResolver {
    public enum DeviceTier { ENTRY, MID, FLAGSHIP }

    public static final class HardwarePreset {
        public final long totalRamBytes;
        public final long totalRamKb;
        public final int cpuCores;
        public final int heapClassMb;
        @NonNull public final String primaryAbi;

        public HardwarePreset(long totalRamBytes, int cpuCores, int heapClassMb, @NonNull String primaryAbi) {
            this.totalRamBytes = totalRamBytes;
            this.totalRamKb = totalRamBytes / 1024L;
            this.cpuCores = cpuCores;
            this.heapClassMb = heapClassMb;
            this.primaryAbi = primaryAbi == null ? "arm64-v8a" : primaryAbi;
        }
    }

    private static final class Rule {
        final Pattern pattern;
        final DeviceTier tier;

        Rule(String regex, DeviceTier tier) {
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            this.tier = tier;
        }
    }

    private static final List<Rule> RULES = new ArrayList<>();

    static {
        RULES.add(new Rule("^SM-A.*", DeviceTier.MID));
        RULES.add(new Rule("^SM-M.*", DeviceTier.ENTRY));
        RULES.add(new Rule("^SM-S.*", DeviceTier.FLAGSHIP));
        RULES.add(new Rule("^Pixel [8-9].*", DeviceTier.FLAGSHIP));
        RULES.add(new Rule("^Pixel [5-7].*", DeviceTier.MID));
        RULES.add(new Rule(".*Lite.*", DeviceTier.ENTRY));
    }

    private HardwareProfileResolver() {
    }

    @NonNull
    public static HardwarePreset resolve(String model, String preferredAbi) {
        DeviceTier tier = resolveTier(model);
        String abi = (preferredAbi == null || preferredAbi.trim().isEmpty()) ? "arm64-v8a" : preferredAbi.trim();
        switch (tier) {
            case ENTRY:
                return new HardwarePreset(4L * 1024 * 1024 * 1024, 8, 256, abi);
            case MID:
                return new HardwarePreset(8L * 1024 * 1024 * 1024, 8, 384, abi);
            case FLAGSHIP:
            default:
                return new HardwarePreset(12L * 1024 * 1024 * 1024, 8, 512, abi);
        }
    }

    @NonNull
    public static DeviceTier resolveTier(String model) {
        String value = model == null ? "" : model.trim();
        for (Rule rule : RULES) {
            if (rule.pattern.matcher(value).matches()) {
                return rule.tier;
            }
        }
        return DeviceTier.MID;
    }

    public static long syncedAvailMem(long originalAvail, long originalTotal, long spoofedTotal) {
        if (originalTotal <= 0L || spoofedTotal <= 0L) {
            return Math.max(0L, spoofedTotal / 2L);
        }
        double ratio = (double) originalAvail / (double) originalTotal;
        ratio = Math.max(0.0d, Math.min(1.0d, ratio));
        return Math.round(spoofedTotal * ratio);
    }
}
