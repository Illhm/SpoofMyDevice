
package com.devicespooflab.hooks.ui;

import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.devicespooflab.hooks.data.ActiveProfileManager;
import com.devicespooflab.hooks.data.DeviceProfile;
import com.devicespooflab.hooks.databinding.FragmentConsistencyCheckBinding;

public class ConsistencyCheckFragment extends Fragment {

    public enum Status {
        MATCH, HIDDEN, LEAKED
    }

    private FragmentConsistencyCheckBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentConsistencyCheckBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.runCheckButton.setOnClickListener(v -> runChecks());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @SuppressWarnings("MissingPermission")
    private void runChecks() {
        binding.resultsContainer.removeAllViews();
        DeviceProfile activeProfile = ActiveProfileManager.getInstance().getActiveProfile();

        if (activeProfile == null) {
            addResult("No active profile set", Status.LEAKED);
            return;
        }

        // 1. Build.MODEL check
        String actualModel = Build.MODEL;
        String expectedModel = activeProfile.getModel();
        Status modelStatus = actualModel.equals(expectedModel) ? Status.MATCH : Status.LEAKED;
        addResult("Build.MODEL: " + actualModel + " (Expected: " + expectedModel + ")", modelStatus);

        // 2. SystemProperties check (via reflection)
        try {
            Class<?> sysPropClass = Class.forName("android.os.SystemProperties");
            String actualFingerprint = (String) sysPropClass.getMethod("get", String.class).invoke(null, "ro.build.fingerprint");
            String expectedFingerprint = activeProfile.getBuildFingerprint();
            Status fingerprintStatus = (actualFingerprint != null && actualFingerprint.equals(expectedFingerprint)) ? Status.MATCH : Status.LEAKED;
            addResult("SystemProp Fingerprint: " + actualFingerprint, fingerprintStatus);
        } catch (Exception e) {
            addResult("SystemProp Check Failed", Status.LEAKED);
        }

        // 3. Telephony check (Simulated IMEI via permission bypass for self-test)
        try {
            TelephonyManager tm = (TelephonyManager) requireContext().getSystemService(android.content.Context.TELEPHONY_SERVICE);
            // This might fail if the app lacks permission, but the hook should ideally catch it before the permission check in some contexts,
            // or we evaluate based on security context.
            String actualImei = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                    actualImei = tm.getImei();
                } else {

                    actualImei = tm.getDeviceId();
                }
            } catch (SecurityException se) {
                actualImei = "PERMISSION_DENIED";
            }

            // For now, if we get permission denied, it's hidden. If we got the original, it's leaked.
            Status imeiStatus = "PERMISSION_DENIED".equals(actualImei) ? Status.HIDDEN : Status.LEAKED;
            addResult("Telephony IMEI: " + actualImei, imeiStatus);
        } catch (Exception e) {
             addResult("Telephony IMEI check failed", Status.HIDDEN);
        }

        // 4. Settings.Secure check
        String actualAndroidId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        String expectedAndroidId = activeProfile.getAndroidId();
        Status androidIdStatus = (actualAndroidId != null && actualAndroidId.equals(expectedAndroidId)) ? Status.MATCH : Status.LEAKED;
        addResult("Settings ANDROID_ID: " + actualAndroidId, androidIdStatus);

        // 5. DisplayMetrics check
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        Status displayStatus = (metrics.widthPixels == activeProfile.getScreenWidth() &&
                                metrics.heightPixels == activeProfile.getScreenHeight()) ? Status.MATCH : Status.LEAKED;
        addResult("Display Metrics: " + metrics.widthPixels + "x" + metrics.heightPixels, displayStatus);
    }

    private void addResult(String text, Status status) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setPadding(0, 8, 0, 8);
        tv.setTextSize(16f);

        switch (status) {
            case MATCH:
                tv.setTextColor(Color.parseColor("#4CAF50")); // Green
                tv.setText("🟢 " + text);
                break;
            case HIDDEN:
                tv.setTextColor(Color.parseColor("#FFC107")); // Yellow
                tv.setText("🟡 " + text);
                break;
            case LEAKED:
                tv.setTextColor(Color.parseColor("#F44336")); // Red
                tv.setText("🔴 " + text);
                break;
        }

        binding.resultsContainer.addView(tv);
    }
}