package com.devicespooflab.hooks.profile;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Immutable
public final class ActiveProfile {
    @NonNull public final BuildInfo build;
    @NonNull public final TelephonyInfo telephony;
    @NonNull public final DisplayInfo display;
    @NonNull public final HardwareInfo hardware;
    @NonNull public final Metadata metadata;

    public ActiveProfile(@NonNull BuildInfo build,
                         @NonNull TelephonyInfo telephony,
                         @NonNull DisplayInfo display,
                         @NonNull HardwareInfo hardware,
                         @NonNull Metadata metadata) {
        this.build = build;
        this.telephony = telephony;
        this.display = display;
        this.hardware = hardware;
        this.metadata = metadata;
    }

    @Immutable
    public static final class BuildInfo {
        @NonNull public final String brand;
        @NonNull public final String model;
        @NonNull public final String board;
        @NonNull public final String fingerprint;
        public final int sdkInt;

        public BuildInfo(@NonNull String brand, @NonNull String model, @NonNull String board,
                         @NonNull String fingerprint, int sdkInt) {
            this.brand = safe(brand);
            this.model = safe(model);
            this.board = safe(board);
            this.fingerprint = safe(fingerprint);
            this.sdkInt = sdkInt;
        }
    }

    @Immutable
    public static final class TelephonyInfo {
        @NonNull public final String operatorNumeric;
        @NonNull public final String simOperatorNumeric;

        public TelephonyInfo(@NonNull String operatorNumeric, @NonNull String simOperatorNumeric) {
            this.operatorNumeric = safe(operatorNumeric);
            this.simOperatorNumeric = safe(simOperatorNumeric);
        }
    }

    @Immutable
    public static final class DisplayInfo {
        public final int width;
        public final int height;
        public final int densityDpi;

        public DisplayInfo(int width, int height, int densityDpi) {
            this.width = width;
            this.height = height;
            this.densityDpi = densityDpi;
        }
    }

    @Immutable
    public static final class HardwareInfo {
        public final long totalRamBytes;
        public final int cpuCores;
        public final int heapClassMb;
        @NonNull public final String abi;

        public HardwareInfo(long totalRamBytes, int cpuCores, int heapClassMb, @NonNull String abi) {
            this.totalRamBytes = totalRamBytes;
            this.cpuCores = cpuCores;
            this.heapClassMb = heapClassMb;
            this.abi = safe(abi);
        }
    }

    @Immutable
    public static final class Metadata {
        @NonNull public final String source;
        @NonNull public final String version;
        @NonNull public final Map<String, String> flags;

        public Metadata(@NonNull String source, @NonNull String version, Map<String, String> flags) {
            this.source = safe(source);
            this.version = safe(version);
            this.flags = Collections.unmodifiableMap(flags == null ? new HashMap<>() : new HashMap<>(flags));
        }
    }

    @NonNull
    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
