package com.devicespooflab.hooks.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.devicespooflab.hooks.data.DeviceProfile;
import com.devicespooflab.hooks.profile.hardware.HardwareProfileResolver;

import java.util.concurrent.atomic.AtomicReference;

public final class ActiveProfileManager {
    private static final AtomicReference<ActiveProfile> ACTIVE = new AtomicReference<>();

    private ActiveProfileManager() {
    }

    public static void setActiveProfile(@NonNull ActiveProfile profile) {
        ACTIVE.set(profile);
    }

    @Nullable
    public static ActiveProfile getActiveProfile() {
        return ACTIVE.get();
    }

    @NonNull
    public static ActiveProfile requireActiveProfile() {
        ActiveProfile profile = ACTIVE.get();
        if (profile == null) {
            throw new IllegalStateException("ActiveProfile has not been initialized");
        }
        return profile;
    }

    public static void clear() {
        ACTIVE.set(null);
    }

    @NonNull
    public static ActiveProfile fromDeviceProfile(@NonNull DeviceProfile input,
                                                  @NonNull String source,
                                                  @NonNull String version) {
        DeviceProfile profile = input.copy();
        profile.applyFallbacks();

        HardwareProfileResolver.HardwarePreset preset =
            HardwareProfileResolver.resolve(profile.getModel(), profile.getCpuAbi());

        return new ActiveProfile(
            new ActiveProfile.BuildInfo(profile.getBrand(), profile.getModel(), profile.getBoard(), profile.getBuildFingerprint(), profile.getBuildSdk()),
            new ActiveProfile.TelephonyInfo(profile.getOperatorNumeric(), profile.getSimOperatorNumeric()),
            new ActiveProfile.DisplayInfo(profile.getScreenWidth(), profile.getScreenHeight(), profile.getScreenDensity()),
            new ActiveProfile.HardwareInfo(preset.totalRamBytes, preset.cpuCores, preset.heapClassMb, preset.primaryAbi),
            new ActiveProfile.Metadata(source, version, null)
        );
    }
}
