# SpoofMyDevice - Architecture & Logic Documentation

## 1. Architecture Overview

**SpoofMyDevice** is an LSPosed companion application and module designed to alter device identifiers and hardware parameters for specifically targeted Android applications.

Its primary purpose is to allow developers, researchers, or users to spoof their device profile (such as turning a Pixel phone's fingerprint into a Galaxy tablet) *without* modifying global system files like `build.prop`, and *without* requiring continuous root access. The spoofing is tightly scoped to the applications selected in the LSPosed manager.

The architecture is divided into three high-level layers:
1. **UI Layer (Companion App):** Allows the user to select, edit, and save device profiles.
2. **Data Layer (Config Management):** Handles saving the profile securely and sharing it across process boundaries.
3. **Hook Layer (Xposed Module):** Injects into target apps at runtime to intercept Android APIs and replace real values with spoofed ones.

---

## 2. File-by-File Analysis

### Hook Layer (`com.devicespooflab.hooks.hooks` & `MainHook.java`)
*   **`MainHook.java`:** The root entry point of the LSPosed module (`IXposedHookLoadPackage`). When a target app starts, this class initializes the configuration and sequentially registers all specific API hooks. It also hooks app lifecycle events (`Application.attach`, `Activity.onCreate`) to support dynamic configuration reloading.
*   **`hooks/SystemPropertiesHooks.java`:** Intercepts calls to `android.os.SystemProperties.get` and `getInt`. This is critical for apps that bypass public APIs and read system properties via reflection.
*   **`hooks/BuildHooks.java`:** Directly modifies the static fields in `android.os.Build` and `android.os.Build$VERSION`. Because many apps read `Build.MODEL` directly from memory, this class uses reflection to overwrite these static variables when the app loads.
*   **`hooks/DisplayHooks.java`:** Intercepts `android.view.Display.getMetrics` and `Resources.getDisplayMetrics` to spoof screen resolution and density (useful for tablet/phone emulation).
*   **`hooks/HardwareHooks.java`, `TelephonyHooks.java`, etc.:** Intercept specific subsystem managers (e.g., TelephonyManager to spoof IMEI/Phone Number).

### Data Layer (`com.devicespooflab.hooks.data` & `utils`)
*   **`utils/ConfigManager.java`:** The central source of truth for the hook layer. It fetches the configuration properties from the companion app and holds them in memory for the hooks to access rapidly.
*   **`data/ConfigFileManager.java`:** Handles creating, formatting, and saving the user's chosen profile into a configuration file (`device_profile.conf`) on the disk.
*   **`ConfigProvider.java`:** A `ContentProvider` exported by the companion app. Since Xposed hooks run inside the sandbox of the *target app*, they cannot read the companion app's private files directly. The hooks use this ContentProvider to securely stream the configuration file across process boundaries.

### UI Layer (`com.devicespooflab.hooks.ui` & `MainActivity.java`)
*   **`MainActivity.java` & `HomeFragment.java`:** The main dashboard that displays module status, current preset, and configuration paths.
*   **`DeviceSettingsFragment.java` / `AppSettingsFragment.java`:** User interfaces for selecting device presets, customizing specific values, and adjusting app behavior.

---

## 3. Execution Flow

The flow of data and execution operates in two distinct phases:

### Phase A: Configuration (User Input)
1.  **Open App:** The user opens the SpoofMyDevice app.
2.  **Edit Profile:** The user selects a preset (e.g., "Samsung Galaxy S23") or edits custom values.
3.  **Save:** The user clicks "Save".
4.  **Write to Disk:** `ConfigFileManager` translates the UI models into a key-value format and writes them to `device_profile.conf` in the app's private storage.

### Phase B: Hook Execution (Target App Launch)
1.  **App Launch:** The user launches a target application (e.g., a game or tracker).
2.  **LSPosed Injection:** During process creation, LSPosed injects the module, triggering `MainHook.handleLoadPackage`.
3.  **Config Load:** `MainHook` calls `ConfigManager.init()`. The hook code queries `content://com.spoofmydevice.configprovider/device_profile.conf` to read the configuration from the companion app.
4.  **Hook Initialization:** `MainHook` loads all hook classes (`BuildHooks`, `SystemPropertiesHooks`, etc.).
5.  **Runtime Interception:** As the target app runs, it inevitably requests device info (e.g., `Build.MANUFACTURER`).
6.  **Value Replacement:** The Xposed framework redirects the request to the hook. The hook reads the requested key from `ConfigManager`, replaces the real value, and returns the spoofed value to the target app.

---

## 4. Complex Logic Breakdown

### A. SystemProperties Hooking (Bypass Logic)
Modern applications often detect spoofing by ignoring the public `android.os.Build` APIs and reading raw `ro.*` properties using `android.os.SystemProperties.get()`.
*   **Traditional Approach:** Tools like Magisk use `resetprop` to change these values system-wide. This often triggers SafetyNet/Play Integrity failures and can break system functionality.
*   **SpoofMyDevice Approach:** `SystemPropertiesHooks` uses Xposed to hook the Java API of `SystemProperties` *exclusively within the target app's process space*. When the app asks for `ro.product.model`, the hook intercepts it and returns the spoofed value from memory. To the system and other apps, the device is completely unmodified.

### B. Sandboxing Bypass via ContentProvider
Android's security model (App Sandboxing) isolates processes by UID. Because Xposed hooks run as part of the target app's process, they inherit the target app's permissions.
*   **Problem:** The hook cannot read `/data/data/com.spoofmydevice/files/device_profile.conf` directly.
*   **Solution:** The companion app exposes `ConfigProvider` (`content://com.spoofmydevice.configprovider/device_profile.conf`). The hook acts as a ContentResolver client to read the configuration over IPC (Inter-Process Communication). Fallbacks are implemented for situations where IPC fails.

### C. Dynamic Reloading
Target apps can be long-running processes. If a user changes a spoof profile, they typically have to Force Stop the target app.
*   **Solution:** `MainHook` hooks lifecycle events like `android.app.Application.attach` and `android.app.Activity.onCreate`. When a target app's activity is resumed or created, the hook forces `ConfigManager` to reload the latest config from the ContentProvider and recursively reapplies the static `Build` fields, allowing near-instant profile switching.

---

## 5. Dependencies & External Libraries

*   **`de.robv.android.xposed:api` (Provided/CompileOnly):**
    *   **Role:** The core framework that enables method hooking. It provides APIs like `XposedHelpers.findAndHookMethod` and `XposedBridge.log`.
    *   **Note:** It is included as `compileOnly` because the actual implementation is injected into the app process by LSPosed at runtime; bundling it in the APK would cause crashes.
*   **AndroidX & Google Material (`androidx.appcompat`, `com.google.android.material`):**
    *   **Role:** Provides modern UI components, fragments, themes, and navigation systems used to build the companion app's dashboard and settings screens.
