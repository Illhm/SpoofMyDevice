package com.devicespooflab.hooks.data;

import java.util.Map;
import java.util.HashMap;

public class ActiveProfileManager {
    private static ActiveProfileManager instance;
    private DeviceProfile activeProfile;
    private Map<String, String> extraProperties;

    private ActiveProfileManager() {
        extraProperties = new HashMap<>();
    }

    public static synchronized ActiveProfileManager getInstance() {
        if (instance == null) {
            instance = new ActiveProfileManager();
        }
        return instance;
    }

    public synchronized void updateProfile(DeviceProfile profile, Map<String, String> properties) {
        if (profile != null) {
            this.activeProfile = profile.copy();
        }
        if (properties != null) {
            this.extraProperties = new HashMap<>(properties);
        }
    }

    public DeviceProfile getActiveProfile() {
        return activeProfile;
    }

    public String getExtraProperty(String key) {
        return extraProperties.get(key);
    }
}