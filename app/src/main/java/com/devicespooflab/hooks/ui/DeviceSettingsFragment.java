package com.devicespooflab.hooks.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaDrm;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.devicespooflab.hooks.MainActivity;
import com.devicespooflab.hooks.R;
import com.devicespooflab.hooks.data.ConfigFileManager;
import com.devicespooflab.hooks.data.DeviceProfile;
import com.devicespooflab.hooks.databinding.FragmentDeviceSettingsBinding;
import com.devicespooflab.hooks.utils.ConfigManager;
import com.devicespooflab.hooks.utils.RandomGenerator;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
public class DeviceSettingsFragment extends Fragment {

    private static final UUID WIDEVINE_UUID = new UUID(
        0xedef8ba979d64aceL,
        0xa3c827dcd51d21edL
    );

    private FragmentDeviceSettingsBinding binding;
    private final ActivityResultLauncher<String[]> phonePermissionsLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            if (binding == null) {
                return;
            }
            populateAdvancedDefaultsIfNeeded();
        });
    private String selectedPresetId;
    private boolean customMode;
    private boolean initialized;
    private boolean applying;
    private boolean advancedExpanded;
    private boolean bindingToggleState;
    private DeviceProfile workingProfile;
    private final Map<String, ToggleBinding> toggleBindings = new LinkedHashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDeviceSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        installSpoofToggles();
        setupListeners();
        updateAdvancedSectionState();
    }

    @Override
    public void onResume() {
        super.onResume();
        advancedExpanded = false;
        updateAdvancedSectionState();
        refreshFromHost(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        toggleBindings.clear();
        binding = null;
    }

    public void refreshFromHost(boolean force) {
        if (binding == null || !(requireActivity() instanceof MainActivity)) {
            return;
        }
        advancedExpanded = false;
        updateAdvancedSectionState();
        if (initialized && !force) {
            return;
        }

        MainActivity activity = (MainActivity) requireActivity();
        ConfigFileManager.LoadedConfig loadedConfig = activity.getLoadedConfigState();
        workingProfile = loadedConfig.getProfile();

        bindProfile(workingProfile);
        bindAdvancedProperties(loadedConfig.getExtraProperties());
        bindToggleStates(loadedConfig.getExtraProperties());
        populateAdvancedDefaultsIfNeeded();
        updateFieldEnablement();
        initialized = true;
    }

    @Nullable
    public Draft buildDraft() {
        if (binding == null) {
            return null;
        }

        DeviceProfile draft = workingProfile == null ? new DeviceProfile() : workingProfile.copy();
        draft.setOperatorAlpha(text(binding.inputOperatorAlpha));
        draft.setOperatorNumeric(text(binding.inputOperatorNumeric));
        draft.setSimOperatorAlpha(text(binding.inputOperatorAlpha));
        draft.setSimOperatorNumeric(text(binding.inputOperatorNumeric));
        draft.setSimCountryIso(text(binding.inputSimCountry));
        draft.setTimezone(text(binding.inputTimezone));
        return new Draft(draft, buildExtraProperties());
    }

    private void setupListeners() {
        binding.advancedToggleHeader.setOnClickListener(v -> {
            advancedExpanded = !advancedExpanded;
            updateAdvancedSectionState();
            if (advancedExpanded) {
                maybeRequestPhonePermissions();
                populateAdvancedDefaultsIfNeeded();
            }
        });

        binding.buttonAdvancedRandomAll.setOnClickListener(v -> randomAdvancedFields());
        binding.buttonAdvancedClearAll.setOnClickListener(v -> clearAdvancedFields());

        binding.layoutAdvancedImei.setEndIconOnClickListener(v ->
            setText(binding.inputAdvancedImei, RandomGenerator.generateIMEI())
        );
        binding.layoutAdvancedMeid.setEndIconOnClickListener(v ->
            setText(binding.inputAdvancedMeid, RandomGenerator.generateMEID())
        );
        binding.layoutAdvancedImsi.setEndIconOnClickListener(v ->
            setText(binding.inputAdvancedImsi, RandomGenerator.generateIMSI())
        );
        binding.layoutAdvancedIccid.setEndIconOnClickListener(v ->
            setText(binding.inputAdvancedIccid, RandomGenerator.generateICCID())
        );
        binding.layoutAdvancedPhoneNumber.setEndIconOnClickListener(v ->
            setText(binding.inputAdvancedPhoneNumber, RandomGenerator.generatePhoneNumber())
        );
        binding.layoutAdvancedGaid.setEndIconOnClickListener(v ->
            setText(binding.inputAdvancedGaid, RandomGenerator.generateGAID())
        );
        binding.layoutAdvancedGsfId.setEndIconOnClickListener(v ->
            setText(binding.inputAdvancedGsfId, RandomGenerator.generateGSFId())
        );
        binding.layoutAdvancedMediaDrmId.setEndIconOnClickListener(v ->
            setText(binding.inputAdvancedMediaDrmId, toHex(RandomGenerator.generateMediaDrmId()))
        );
        binding.layoutAdvancedAppSetId.setEndIconOnClickListener(v ->
            setText(binding.inputAdvancedAppSetId, RandomGenerator.generateGAID())
        );
    }

    private void bindProfile(DeviceProfile profile) {
        if (profile == null || binding == null) {
            return;
        }
        setText(binding.inputOperatorAlpha, profile.getOperatorAlpha());
        setText(binding.inputOperatorNumeric, profile.getOperatorNumeric());
        setText(binding.inputSimCountry, profile.getSimCountryIso());
        setText(binding.inputTimezone, profile.getTimezone());
    }

    private void bindAdvancedProperties(Map<String, String> extraProperties) {
        setText(binding.inputAdvancedImei, extraProperties.get(ConfigManager.KEY_SPOOF_IMEI));
        setText(binding.inputAdvancedMeid, extraProperties.get(ConfigManager.KEY_SPOOF_MEID));
        setText(binding.inputAdvancedImsi, extraProperties.get(ConfigManager.KEY_SPOOF_IMSI));
        setText(binding.inputAdvancedIccid, extraProperties.get(ConfigManager.KEY_SPOOF_ICCID));
        setText(binding.inputAdvancedPhoneNumber, extraProperties.get(ConfigManager.KEY_SPOOF_PHONE_NUMBER));
        setText(binding.inputAdvancedGaid, extraProperties.get(ConfigManager.KEY_SPOOF_GAID));
        setText(binding.inputAdvancedGsfId, extraProperties.get(ConfigManager.KEY_SPOOF_GSF_ID));
        setText(binding.inputAdvancedMediaDrmId, extraProperties.get(ConfigManager.KEY_SPOOF_MEDIA_DRM_ID));
        setText(binding.inputAdvancedAppSetId, extraProperties.get(ConfigManager.KEY_SPOOF_APP_SET_ID));
    }

    private void bindToggleStates(Map<String, String> extraProperties) {
        bindingToggleState = true;
        for (ToggleBinding toggleBinding : toggleBindings.values()) {
            String explicitValue = extraProperties.get(ConfigManager.getTogglePropertyKey(toggleBinding.fieldId));
            boolean enabled = explicitValue == null
                || explicitValue.equals("1")
                || explicitValue.equalsIgnoreCase("true");
            toggleBinding.toggle.setChecked(enabled);
        }
        bindingToggleState = false;
        updateFieldEnablement();
    }

    private void populateAdvancedDefaultsIfNeeded() {
        setIfBlank(binding.inputAdvancedImei, resolveCurrentImei());
        setIfBlank(binding.inputAdvancedMeid, resolveCurrentMeid());
        setIfBlank(binding.inputAdvancedImsi, resolveCurrentImsi());
        setIfBlank(binding.inputAdvancedIccid, resolveCurrentIccid());
        setIfBlank(binding.inputAdvancedPhoneNumber, resolveCurrentPhoneNumber());
        setIfBlank(binding.inputAdvancedGsfId, resolveCurrentGsfId());
        setIfBlank(binding.inputAdvancedMediaDrmId, resolveCurrentMediaDrmId());
        loadGoogleIdsIfNeeded();
    }

    private void maybeRequestPhonePermissions() {
        List<String> missingPermissions = new ArrayList<>();
        Context context = requireContext();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.READ_PHONE_NUMBERS);
        }

        if (!missingPermissions.isEmpty()) {
            phonePermissionsLauncher.launch(missingPermissions.toArray(new String[0]));
        }
    }

    private void updateAdvancedSectionState() {
        if (binding == null) {
            return;
        }
        binding.advancedContentCard.setVisibility(advancedExpanded ? View.VISIBLE : View.GONE);
        binding.advancedToggleIcon.setRotation(advancedExpanded ? 180f : 0f);
    }

    private Map<String, String> buildExtraProperties() {
        Map<String, String> extraProperties = new LinkedHashMap<>();
        if (requireActivity() instanceof MainActivity) {
            extraProperties.putAll(((MainActivity) requireActivity()).getLoadedConfigState().getExtraProperties());
        }

        putOptional(extraProperties, ConfigManager.KEY_SPOOF_IMEI, text(binding.inputAdvancedImei));
        putOptional(extraProperties, ConfigManager.KEY_SPOOF_MEID, text(binding.inputAdvancedMeid));
        putOptional(extraProperties, ConfigManager.KEY_SPOOF_IMSI, text(binding.inputAdvancedImsi));
        putOptional(extraProperties, ConfigManager.KEY_SPOOF_ICCID, text(binding.inputAdvancedIccid));
        putOptional(extraProperties, ConfigManager.KEY_SPOOF_PHONE_NUMBER, text(binding.inputAdvancedPhoneNumber));
        putOptional(extraProperties, ConfigManager.KEY_SPOOF_GAID, text(binding.inputAdvancedGaid));
        putOptional(extraProperties, ConfigManager.KEY_SPOOF_GSF_ID, text(binding.inputAdvancedGsfId));
        putOptional(extraProperties, ConfigManager.KEY_SPOOF_MEDIA_DRM_ID, text(binding.inputAdvancedMediaDrmId));
        putOptional(extraProperties, ConfigManager.KEY_SPOOF_APP_SET_ID, text(binding.inputAdvancedAppSetId));
        for (ToggleBinding toggleBinding : toggleBindings.values()) {
            String key = ConfigManager.getTogglePropertyKey(toggleBinding.fieldId);
            if (toggleBinding.toggle.isChecked()) {
                extraProperties.remove(key);
            } else {
                extraProperties.put(key, "false");
            }
        }
        return extraProperties;
    }

    private void randomAdvancedFields() {
        setText(binding.inputAdvancedImei, RandomGenerator.generateIMEI());
        setText(binding.inputAdvancedMeid, RandomGenerator.generateMEID());
        setText(binding.inputAdvancedImsi, RandomGenerator.generateIMSI());
        setText(binding.inputAdvancedIccid, RandomGenerator.generateICCID());
        setText(binding.inputAdvancedPhoneNumber, RandomGenerator.generatePhoneNumber());
        setText(binding.inputAdvancedGaid, RandomGenerator.generateGAID());
        setText(binding.inputAdvancedGsfId, RandomGenerator.generateGSFId());
        setText(binding.inputAdvancedMediaDrmId, toHex(RandomGenerator.generateMediaDrmId()));
        setText(binding.inputAdvancedAppSetId, RandomGenerator.generateGAID());
    }

    private void clearAdvancedFields() {
        setText(binding.inputAdvancedImei, "");
        setText(binding.inputAdvancedMeid, "");
        setText(binding.inputAdvancedImsi, "");
        setText(binding.inputAdvancedIccid, "");
        setText(binding.inputAdvancedPhoneNumber, "");
        setText(binding.inputAdvancedGaid, "");
        setText(binding.inputAdvancedGsfId, "");
        setText(binding.inputAdvancedMediaDrmId, "");
        setText(binding.inputAdvancedAppSetId, "");
    }

    private void installSpoofToggles() {
        if (binding == null || !toggleBindings.isEmpty()) {
            return;
        }

        registerToggle(ConfigManager.FIELD_OPERATOR_ALPHA, layoutOf(binding.inputOperatorAlpha), true);
        registerToggle(ConfigManager.FIELD_OPERATOR_NUMERIC, layoutOf(binding.inputOperatorNumeric), true);
        registerToggle(ConfigManager.FIELD_SIM_COUNTRY, layoutOf(binding.inputSimCountry), true);
        registerToggle(ConfigManager.FIELD_TIMEZONE, layoutOf(binding.inputTimezone), true);

        registerToggle(ConfigManager.FIELD_IMEI, binding.layoutAdvancedImei, false);
        registerToggle(ConfigManager.FIELD_MEID, binding.layoutAdvancedMeid, false);
        registerToggle(ConfigManager.FIELD_IMSI, binding.layoutAdvancedImsi, false);
        registerToggle(ConfigManager.FIELD_ICCID, binding.layoutAdvancedIccid, false);
        registerToggle(ConfigManager.FIELD_PHONE_NUMBER, binding.layoutAdvancedPhoneNumber, false);
        registerToggle(ConfigManager.FIELD_GAID, binding.layoutAdvancedGaid, false);
        registerToggle(ConfigManager.FIELD_GSF_ID, binding.layoutAdvancedGsfId, false);
        registerToggle(ConfigManager.FIELD_MEDIA_DRM_ID, binding.layoutAdvancedMediaDrmId, false);
        registerToggle(ConfigManager.FIELD_APP_SET_ID, binding.layoutAdvancedAppSetId, false);

        updateFieldEnablement();
    }

    private void registerToggle(String fieldId, TextInputLayout inputLayout, boolean dependsOnCustomMode) {
        if (inputLayout == null) {
            return;
        }
        ViewGroup parent = (ViewGroup) inputLayout.getParent();
        if (!(parent instanceof LinearLayout)) {
            return;
        }

        LinearLayout.LayoutParams existingParams = asLinearLayoutParams(inputLayout.getLayoutParams());
        LinearLayout.LayoutParams wrapperParams = new LinearLayout.LayoutParams(existingParams);
        LinearLayout wrapper = new LinearLayout(requireContext());
        wrapper.setOrientation(LinearLayout.HORIZONTAL);
        wrapper.setGravity(android.view.Gravity.CENTER_VERTICAL);
        wrapper.setLayoutParams(wrapperParams);

        MaterialCheckBox toggle = new MaterialCheckBox(requireContext());
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        toggleParams.setMarginEnd(dp(12));
        toggle.setLayoutParams(toggleParams);
        toggle.setUseMaterialThemeColors(true);
        toggle.setChecked(true);
        toggle.setMinWidth(0);
        toggle.setMinimumWidth(0);
        toggle.setPadding(0, 0, 0, 0);
        CharSequence hint = inputLayout.getHint();
        if (hint != null) {
            toggle.setContentDescription(hint);
        }

        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        );
        inputLayout.setLayoutParams(fieldParams);

        int index = parent.indexOfChild(inputLayout);
        parent.removeView(inputLayout);
        wrapper.addView(toggle);
        wrapper.addView(inputLayout);
        parent.addView(wrapper, index);

        ToggleBinding toggleBinding = new ToggleBinding(fieldId, toggle, inputLayout, dependsOnCustomMode);
        toggleBindings.put(fieldId, toggleBinding);
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingToggleState) {
                return;
            }
            if (!dependsOnCustomMode && isChecked && advancedExpanded) {
                maybeRequestPhonePermissions();
                populateAdvancedDefaultsIfNeeded();
            }
            updateFieldEnablement();
        });
    }

    private TextInputLayout layoutOf(TextInputEditText editText) {
        if (editText == null) {
            return null;
        }
        ViewParent parent = editText.getParent();
        while (parent != null) {
            if (parent instanceof TextInputLayout) {
                return (TextInputLayout) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private LinearLayout.LayoutParams asLinearLayoutParams(ViewGroup.LayoutParams params) {
        if (params instanceof LinearLayout.LayoutParams) {
            return new LinearLayout.LayoutParams((LinearLayout.LayoutParams) params);
        }
        if (params instanceof ViewGroup.MarginLayoutParams) {
            return new LinearLayout.LayoutParams((ViewGroup.MarginLayoutParams) params);
        }
        return new LinearLayout.LayoutParams(params);
    }

    private void updateFieldEnablement() {
        for (ToggleBinding toggleBinding : toggleBindings.values()) {
            boolean enabled = toggleBinding.toggle.isChecked();
            toggleBinding.inputLayout.setEnabled(enabled);
            toggleBinding.inputLayout.setAlpha(toggleBinding.toggle.isChecked() ? 1f : 0.55f);
            if (toggleBinding.inputLayout.getEditText() != null) {
                toggleBinding.inputLayout.getEditText().setEnabled(enabled);
            }
        }

        applyEditTextState(binding.inputOperatorAlpha, ConfigManager.FIELD_OPERATOR_ALPHA, true);
        applyEditTextState(binding.inputOperatorNumeric, ConfigManager.FIELD_OPERATOR_NUMERIC, true);
        applyEditTextState(binding.inputSimCountry, ConfigManager.FIELD_SIM_COUNTRY, true);
        applyEditTextState(binding.inputTimezone, ConfigManager.FIELD_TIMEZONE, true);

        applyEditTextState(binding.inputAdvancedImei, ConfigManager.FIELD_IMEI, false);
        applyEditTextState(binding.inputAdvancedMeid, ConfigManager.FIELD_MEID, false);
        applyEditTextState(binding.inputAdvancedImsi, ConfigManager.FIELD_IMSI, false);
        applyEditTextState(binding.inputAdvancedIccid, ConfigManager.FIELD_ICCID, false);
        applyEditTextState(binding.inputAdvancedPhoneNumber, ConfigManager.FIELD_PHONE_NUMBER, false);
        applyEditTextState(binding.inputAdvancedGaid, ConfigManager.FIELD_GAID, false);
        applyEditTextState(binding.inputAdvancedGsfId, ConfigManager.FIELD_GSF_ID, false);
        applyEditTextState(binding.inputAdvancedMediaDrmId, ConfigManager.FIELD_MEDIA_DRM_ID, false);
        applyEditTextState(binding.inputAdvancedAppSetId, ConfigManager.FIELD_APP_SET_ID, false);
    }

    private void applyEditTextState(TextInputEditText editText, String fieldId, boolean dependsOnCustomMode) {
        if (editText == null) {
            return;
        }
        ToggleBinding toggleBinding = toggleBindings.get(fieldId);
        boolean checked = toggleBinding == null || toggleBinding.toggle.isChecked();
        boolean enabled = checked;
        editText.setEnabled(enabled);
        editText.setAlpha(checked ? 1f : 0.55f);
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            requireContext().getResources().getDisplayMetrics()
        ));
    }

    private void putOptional(Map<String, String> target, String key, String value) {
        if (value == null || value.isEmpty()) {
            target.remove(key);
            return;
        }
        target.put(key, value);
    }

    private void setIfBlank(TextInputEditText editText, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (text(editText).isEmpty()) {
            setText(editText, value);
        }
    }

    private String resolveCurrentImei() {
        try {
            TelephonyManager telephonyManager = requireContext().getSystemService(TelephonyManager.class);
            if (telephonyManager == null) {
                return null;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return telephonyManager.getImei();
            }
            return telephonyManager.getDeviceId();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String resolveCurrentMeid() {
        try {
            TelephonyManager telephonyManager = requireContext().getSystemService(TelephonyManager.class);
            if (telephonyManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                return null;
            }
            return telephonyManager.getMeid();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String resolveCurrentImsi() {
        try {
            TelephonyManager telephonyManager = requireContext().getSystemService(TelephonyManager.class);
            if (telephonyManager == null) {
                return null;
            }
            return telephonyManager.getSubscriberId();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String resolveCurrentIccid() {
        try {
            SubscriptionManager subscriptionManager = requireContext().getSystemService(SubscriptionManager.class);
            if (subscriptionManager == null) {
                return null;
            }
            List<SubscriptionInfo> subscriptions = subscriptionManager.getActiveSubscriptionInfoList();
            if (subscriptions == null || subscriptions.isEmpty()) {
                return null;
            }
            String iccId = subscriptions.get(0).getIccId();
            return (iccId == null || iccId.trim().isEmpty()) ? null : iccId;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String resolveCurrentPhoneNumber() {
        try {
            TelephonyManager telephonyManager = requireContext().getSystemService(TelephonyManager.class);
            if (telephonyManager == null) {
                return null;
            }
            return telephonyManager.getLine1Number();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String resolveCurrentGsfId() {
        Cursor cursor = null;
        try {
            cursor = requireContext().getContentResolver().query(
                Uri.parse("content://com.google.android.gsf.gservices"),
                null,
                null,
                new String[]{"android_id"},
                null
            );
            if (cursor != null && cursor.moveToFirst() && cursor.getColumnCount() >= 2) {
                String gsfId = cursor.getString(1);
                return (gsfId == null || gsfId.trim().isEmpty()) ? null : gsfId;
            }
        } catch (Throwable ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private String resolveCurrentMediaDrmId() {
        MediaDrm mediaDrm = null;
        try {
            mediaDrm = new MediaDrm(WIDEVINE_UUID);
            byte[] value = mediaDrm.getPropertyByteArray("deviceUniqueId");
            if (value == null || value.length == 0) {
                return null;
            }
            StringBuilder builder = new StringBuilder(value.length * 2);
            for (byte b : value) {
                builder.append(String.format("%02x", b & 0xff));
            }
            return builder.toString();
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (mediaDrm != null) {
                try {
                    mediaDrm.release();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private void loadGoogleIdsIfNeeded() {
        Context appContext = requireContext().getApplicationContext();
        new Thread(() -> {
            String gaid = resolveCurrentGaid(appContext);
            String appSetId = resolveCurrentAppSetId(appContext);
            if (binding == null) {
                return;
            }
            binding.getRoot().post(() -> {
                if (binding == null) {
                    return;
                }
                setIfBlank(binding.inputAdvancedGaid, gaid);
                setIfBlank(binding.inputAdvancedAppSetId, appSetId);
            });
        }, "spoofmydevice-google-id-loader").start();
    }

    private String resolveCurrentGaid(Context context) {
        try {
            Class<?> clientClass = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
            Object info = clientClass.getMethod("getAdvertisingIdInfo", Context.class).invoke(null, context);
            if (info == null) {
                return null;
            }
            Object result = info.getClass().getMethod("getId").invoke(info);
            return result instanceof String ? (String) result : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String resolveCurrentAppSetId(Context context) {
        try {
            Class<?> appSetClass = Class.forName("com.google.android.gms.appset.AppSet");
            Object client = appSetClass.getMethod("getClient", Context.class).invoke(null, context);
            if (client == null) {
                return null;
            }

            Object task = client.getClass().getMethod("getAppSetIdInfo").invoke(client);
            if (task == null) {
                return null;
            }

            Class<?> taskClass = Class.forName("com.google.android.gms.tasks.Task");
            Class<?> tasksClass = Class.forName("com.google.android.gms.tasks.Tasks");
            Object info = tasksClass.getMethod("await", taskClass).invoke(null, task);
            if (info == null) {
                return null;
            }

            Object result = info.getClass().getMethod("getId").invoke(info);
            return result instanceof String ? (String) result : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String toHex(byte[] value) {
        if (value == null || value.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length * 2);
        for (byte b : value) {
            builder.append(String.format("%02x", b & 0xff));
        }
        return builder.toString();
    }

    private void setText(TextInputEditText editText, String value) {
        editText.setText(value == null ? "" : value);
    }

    private String text(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private int intValue(TextInputEditText editText, int fallback) {
        try {
            return Integer.parseInt(text(editText));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static class Draft {
        public final DeviceProfile profile;
        public final Map<String, String> extraProperties;

        public Draft(DeviceProfile profile, Map<String, String> extraProperties) {
            this.profile = profile;
            this.extraProperties = new LinkedHashMap<>(extraProperties);
        }
    }

    private static class ToggleBinding {
        final String fieldId;
        final MaterialCheckBox toggle;
        final TextInputLayout inputLayout;
        final boolean dependsOnCustomMode;

        ToggleBinding(String fieldId, MaterialCheckBox toggle, TextInputLayout inputLayout, boolean dependsOnCustomMode) {
            this.fieldId = fieldId;
            this.toggle = toggle;
            this.inputLayout = inputLayout;
            this.dependsOnCustomMode = dependsOnCustomMode;
        }
    }
}
