package com.devicespooflab.hooks.data;

import java.util.Locale;

public class DeviceHardwareProfile {
    public final int cores;
    public final long ramBytes;
    public final int heapClass;
    public final int largeHeapClass;

    public DeviceHardwareProfile(int cores, long ramBytes, int heapClass, int largeHeapClass) {
        this.cores = cores;
        this.ramBytes = ramBytes;
        this.heapClass = heapClass;
        this.largeHeapClass = largeHeapClass;
    }

    public static DeviceHardwareProfile getForModel(String model) {
        if (model == null) {
            return new DeviceHardwareProfile(8, 8L * 1024 * 1024 * 1024, 256, 512); // default
        }

        String lowerModel = model.toLowerCase(Locale.US);

        // Pixel Flagships (Pixel 6, 7, 8 Pro)
        if (lowerModel.contains("pixel") && (lowerModel.contains("pro") || lowerModel.contains("fold"))) {
            return new DeviceHardwareProfile(8, 12L * 1024 * 1024 * 1024, 512, 1024);
        }
        // Pixel Base
        if (lowerModel.contains("pixel")) {
            return new DeviceHardwareProfile(8, 8L * 1024 * 1024 * 1024, 256, 512);
        }

        // Galaxy S Flagships
        if (lowerModel.startsWith("sm-s")) {
            if (lowerModel.contains("u") || lowerModel.contains("b")) { // S2* Ultra
                return new DeviceHardwareProfile(8, 12L * 1024 * 1024 * 1024, 512, 1024);
            }
            return new DeviceHardwareProfile(8, 8L * 1024 * 1024 * 1024, 256, 512);
        }

        // Galaxy A Mid-range
        if (lowerModel.startsWith("sm-a")) {
            return new DeviceHardwareProfile(8, 6L * 1024 * 1024 * 1024, 192, 512);
        }

        // Default modern device
        return new DeviceHardwareProfile(8, 8L * 1024 * 1024 * 1024, 256, 512);
    }
}