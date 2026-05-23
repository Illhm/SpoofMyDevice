package com.devicespooflab.hooks.data;

import java.util.Locale;
import java.util.Random;

public class IntelligentRandomizer {

    private static final Random random = new Random();

    public static DeviceProfile randomize(DeviceProfile base) {
        if (base == null) return null;
        DeviceProfile randomized = base.copy();

        // Logical randomize MAC
        randomized.setCpuAbiList(base.getCpuAbiList()); // Keep original attributes mostly

        // Generate chronologically sound MACs
        byte[] macBytes = new byte[6];
        random.nextBytes(macBytes);
        // Ensure unicast and locally administered for fake MACs
        macBytes[0] = (byte)(macBytes[0] | (byte)0x02);
        macBytes[0] = (byte)(macBytes[0] & (byte)0xFE);

        StringBuilder macBuilder = new StringBuilder(18);
        for (byte b : macBytes) {
            if (macBuilder.length() > 0) {
                macBuilder.append(":");
            }
            macBuilder.append(String.format("%02x", b));
        }
        String spoofedMac = macBuilder.toString().toLowerCase(Locale.US);
        // (Usually we'd set this in a config map, but here we just simulate the logic)

        return randomized;
    }
}