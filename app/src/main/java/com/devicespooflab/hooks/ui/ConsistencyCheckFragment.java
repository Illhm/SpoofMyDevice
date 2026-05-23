package com.devicespooflab.hooks.ui;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.devicespooflab.hooks.data.ConsistencyCheckItem;
import com.devicespooflab.hooks.data.ConsistencyStatus;
import com.devicespooflab.hooks.utils.ConfigManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ConsistencyCheckFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        TextView textView = new TextView(requireContext());
        StringBuilder report = new StringBuilder();
        for (ConsistencyCheckItem item : runConsistencyChecks(requireContext())) {
            report.append(item.getStatus()).append(" | ").append(item.getKey())
                .append("\nactual=").append(item.getActual())
                .append("\nexpected=").append(item.getExpected()).append("\n\n");
        }
        textView.setText(report.toString());
        textView.setTextIsSelectable(true);
        return textView;
    }

    public List<ConsistencyCheckItem> runConsistencyChecks(Context context) {
        List<ConsistencyCheckItem> items = new ArrayList<>();

        String buildModel = Build.MODEL;
        items.add(compare("Build.MODEL", ConfigManager.getBuildModel(), buildModel));

        String fingerprint = readSystemProperty("ro.build.fingerprint");
        items.add(compare("SystemProperties.ro.build.fingerprint", ConfigManager.getBuildFingerprint(), fingerprint));

        String imei = readImeiSafely(context);
        items.add(compare("TelephonyManager.getImei", ConfigManager.getIMEI(), imei));

        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        items.add(compare("Settings.Secure.ANDROID_ID", ConfigManager.getAndroidId(), androidId));

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        items.add(compare("DisplayMetrics.widthPixels", String.valueOf(ConfigManager.getScreenWidth()), String.valueOf(metrics.widthPixels)));
        items.add(compare("DisplayMetrics.heightPixels", String.valueOf(ConfigManager.getScreenHeight()), String.valueOf(metrics.heightPixels)));
        items.add(compare("DisplayMetrics.densityDpi", String.valueOf(ConfigManager.getScreenDensityDpi()), String.valueOf(metrics.densityDpi)));
        return items;
    }

    private ConsistencyCheckItem compare(String key, String expected, String actual) {
        ConsistencyStatus status;
        if (actual == null || actual.trim().isEmpty()) {
            status = ConsistencyStatus.HIDDEN;
        } else if (expected != null && expected.equals(actual)) {
            status = ConsistencyStatus.MATCH;
        } else {
            status = ConsistencyStatus.LEAKED;
        }
        return new ConsistencyCheckItem(key, expected, actual, status);
    }

    private String readSystemProperty(String key) {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method getMethod = clazz.getMethod("get", String.class);
            Object value = getMethod.invoke(null, key);
            return value == null ? "" : String.valueOf(value);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private String readImeiSafely(Context context) {
        try {
            int granted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE);
            if (granted != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return "";
            }
            TelephonyManager tm = context.getSystemService(TelephonyManager.class);
            if (tm == null) {
                return "";
            }
            String imei = tm.getImei();
            return imei == null ? "" : imei;
        } catch (Throwable ignored) {
            return "";
        }
    }
}
