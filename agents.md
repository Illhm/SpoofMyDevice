## 🎯 Fokus

Hanya **LSPosed Module (Core Hook Engine)** — module yang membaca 45 parameter dari SharedPreferences lalu meng-intercept semua panggilan sistem Android agar mengembalikan nilai spoofed. Tidak termasuk Companion App, ContentProvider, server lisensi, atau monetisasi.

**Bahasa:** Kotlin · **Target:** Android 8.0+ (API 26+) · **Framework:** LSPosed API (Zygisk-based) · **Scope:** `system_server`, `phone` process, target apps

---

## 🏗️ Arsitektur Module

```
┌──────────────────────┐
│   SharedPreferences   │  ← 45 parameter ditulis via ContentProvider
│   (rsh_params)        │
└──────────┬───────────┘
           │ read on every hook call
┌──────────▼───────────┐
│   MainHook.kt        │  IXposedHookLoadPackage
│   Entry point         │
└──────────┬───────────┘
           │
    ┌──────┼──────┬─────────┬──────────┬──────────┐
    ▼      ▼      ▼         ▼          ▼          ▼
 Build  Tele   Identity   WiFi    Location   Bluetooth
 Hook    Hook    Hook      Hook      Hook       Hook
```

---

## 📦 Hook Targets — 45 Parameter

### ① Build Info (20 param) — Prioritas P0

| # | Key | Source | Hook Method |
| --- | --- | --- | --- |
| 1 | `phone_brand` | `Build.BRAND` | Static field hook |
| 2 | `phone_model` | `Build.MODEL` | Static field hook |
| 3 | `phone_manufacturer` | `Build.MANUFACTURER` | Static field hook |
| 4 | `phone_device` | `Build.DEVICE` | Static field hook |
| 5 | `phone_board` | `Build.BOARD` | Static field hook |
| 6 | `phone_hardware` | `Build.HARDWARE` | Static field hook |
| 7 | `phone_name` | `Build.PRODUCT` | Static field hook |
| 8 | `phone_display` | `Build.DISPLAY` | Static field hook |
| 9 | `phone_version_release` | `Build.VERSION.RELEASE` | Static field hook |
| 10 | `phone_incremental` | `Build.VERSION.INCREMENTAL` | Static field hook |
| 11 | `phone_id` | `Build.ID` | Static field hook |
| 12 | `phone_tags` | `Build.TAGS` | Static field hook |
| 13 | `phone_host` | `Build.HOST` | Static field hook |
| 14 | `phone_user` | `Build.USER` | Static field hook |
| 15 | `phone_type` | `Build.TYPE` | Static field hook |
| 16 | `phone_baseband` | `SystemProperties` | `get()` hook |
| 17 | `phone_patch` | `SystemProperties` | `get()` hook |
| 18 | `phone_build_date` | `Build.TIME` | Static field hook |
| 19 | `phone_build_date_utc` | `Build.TIME` / 1000L | Derived |
| 20 | `phone_fingerprint` | `Build.FINGERPRINT` | Static field hook |

**Pendekatan:** Hook seluruh static field `android.os.Build` + `android.os.Build$VERSION` saat class loading. Gunakan `XposedHelpers.setStaticObjectField`.

```kotlin
// BuildHook.kt
class BuildHook : HookModule {
    override fun hook(classLoader: ClassLoader, params: Map<String, String>) {
        XposedHelpers.setStaticObjectField(
            Build::class.java, "BRAND", params["phone_brand"]
        )
        XposedHelpers.setStaticObjectField(
            Build::class.java, "MODEL", params["phone_model"]
        )
        // ... 18 lainnya
        XposedHelpers.setStaticObjectField(
            Build::class.java, "FINGERPRINT", params["phone_fingerprint"]
        )
    }
}
```

**Catatan:** Android 14+ mungkin perlu reflection tambahan karena enforced API restrictions. Gunakan `XposedBridge` untuk bypass non-SDK interface restrictions.

---

### ② Telephony (8 param) — Prioritas P0

| # | Key | API yang di-hook |
| --- | --- | --- |
| 21 | `sim_operator` | `TelephonyManager.getSimOperator()` |
| 22 | `sim_operator_name` | `TelephonyManager.getSimOperatorName()` |
| 23 | `sim_country_iso` | `TelephonyManager.getSimCountryIso()` |
| 24 | `sim_serial_number` | `TelephonyManager.getSimSerialNumber()` |
| 25 | `line_number` | `TelephonyManager.getLine1Number()` |
| 26 | `subscriber_id` | `TelephonyManager.getSubscriberId()` |
| 37 | `imei_1` | `TelephonyManager.getImei(0)` / `getDeviceId(0)` |
| 38 | `imei_2` | `TelephonyManager.getImei(1)` / `getDeviceId(1)` |

**Perhatian khusus:**

- **Dual-SIM:** Bedakan hook per `slotIndex`. `getImei(0)` ≠ `getImei(1)`.
- **Android 14+ MIUI/ColorOS:** `getImei()` bisa return empty string — hook `getDeviceId()` sebagai fallback.
- **`getSubscriberId()`** → IMSI: linked ke `sim_operator` MCC/MNC + Luhn check digit.

```kotlin
class TelephonyHook : HookModule {
    override fun hook(classLoader: ClassLoader, params: Map<String, String>) {
        XposedHelpers.findAndHookMethod(
            TelephonyManager::class.java,
            "getDeviceId", Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val slot = param.args[0] as Int
                    param.result = if (slot == 0) params["imei_1"] else params["imei_2"]
                }
            }
        )
        // hook getImei(), getSimOperator(), dll.
    }
}
```

---

### ③ Identity (5 param) — Prioritas P0

| # | Key | API yang di-hook |
| --- | --- | --- |
| 39 | `android_id` | `Settings.Secure.getString(contentResolver, "android_id")` |
| 40 | `phone_serial` | `Build.getSerial()` |
| 41 | `gsf` | `GoogleSettingsContract.Partner.getString()` |
| 43 | `ads_id` | `AdvertisingIdClient.getAdvertisingIdInfo()` |

**Perhatian:**

- `ANDROID_ID` perlu hook **write + read** — persist after reboot.
- `Build.getSerial()`: Android 10+ perlu hook via system_server.
- GSF ID: hook di `com.google.android.gsf` package.

---

### ④ WiFi (4 param) — Prioritas P1

| # | Key | API yang di-hook |
| --- | --- | --- |
| 27 | `ssid` | `WifiInfo.getSSID()` |
| 28 | `bssid` | `WifiInfo.getBSSID()` |
| 29 | `wifi_mac` | `WifiInfo.getMacAddress()` |
| 30 | `wifi_state` | `WifiManager.getWifiState()` |

**Catatan:** Android 10+ `getMacAddress()` return `02:00:00:00:00:00` — override dengan MAC valid (OUI matched to brand).

---

### ⑤ Location (6 param) — Prioritas P1

| # | Key | API yang di-hook |
| --- | --- | --- |
| 31 | `geo_latitude` | `Location.getLatitude()` |
| 32 | `geo_longitude` | `Location.getLongitude()` |
| 33 | `geo_altitude` | `Location.getAltitude()` |
| 34 | `geo_accuracy` | `Location.getAccuracy()` |
| 35 | `geo_speed` | `Location.getSpeed()` |
| 36 | `geo_bearing` | `Location.getBearing()` |

**Pendekatan:** Hook `LocationManager.getLastKnownLocation()` + `requestLocationUpdates()` → return mock Location.

---

### ⑥ Bluetooth (1 param) — Prioritas P2

| # | Key | API yang di-hook |
| --- | --- | --- |
| 42 | `bluetooth_mac` | `BluetoothAdapter.getAddress()` |

### ⑦ DRM & WebView (2 param) — Prioritas P2

| # | Key | API yang di-hook |
| --- | --- | --- |
| 44 | `drm` | `MediaDrm.getPropertyString()` |
| 45 | `webview_visible` | `WebView.setVisibility()` reflection |

---

## 🧵 Module Lifecycle

```
Zygote fork
  │
  ▼
IXposedHookZygoteInit.initZygote()
  │  Register module hooks, load resource hooks
  │
  ▼
IXposedHookLoadPackage.handleLoadPackage()
  │  Cek package name → system_server / phone / target app?
  │
  ▼
ParamStore.load() → baca SharedPreferences
  │
  ▼
Setiap HookModule.apply(params)
  │
  ▼
✅ Module siap — semua system call ter-intercept
```

**Scope per process:**

- `system_server` — Build, ANDROID_ID, getSerial
- `com.android.phone` — Telephony
- Target apps — WiFi, Location, Bluetooth, DRM, WebView, Ad ID

---

## 🔌 Interface: HookModule

```kotlin
interface HookModule {
    /** Apply hooks with spoof parameters. Returns true if successful. */
    fun hook(classLoader: ClassLoader, params: Map<String, String>): Boolean

    /** Lower = applied first. Default 50. */
    val priority: Int get() = 50

    /** Packages where this hook should be active. */
    val targetPackages: List<String>
}
```

Semua hook class mengimplementasikan interface ini — `MainHook.kt` meng-iterate semua implementasi saat `handleLoadPackage()`.

---

## 📁 Struktur Module

```
module/
├── build.gradle.kts
├── proguard-rules.pro
└── src/main/
    ├── AndroidManifest.xml              # Zygisk init
    └── kotlin/
        ├── MainHook.kt                  # IXposedHookLoadPackage
        ├── ZygoteInit.kt                # IXposedHookZygoteInit
        ├── hooks/
        │   ├── HookModule.kt            # interface
        │   ├── BuildHook.kt             # 20 param
        │   ├── TelephonyHook.kt         # 8 param
        │   ├── IdentityHook.kt          # 5 param
        │   ├── WifiHook.kt              # 4 param
        │   ├── LocationHook.kt          # 6 param
        │   ├── BluetoothHook.kt         # 1 param
        │   └── DrmWebViewHook.kt        # 2 param
        ├── ParamStore.kt                # SharedPreferences reader
        └── utils/
            ├── ReflectionHelper.kt      # Bypass helpers
            └── PackageFilter.kt         # Scope matcher
```

---

## 📐 Konsistensi Parameter (Module Side)

Module tidak generate parameter (itu job App companion), tapi module bertanggung jawab:

| Aturan | Implementasi |
| --- | --- |
| `phone_build_date_utc` ↔ `phone_build_date` | Module baca dari ParamStore tanpa offset |
| `imei_1` ≠ `imei_2` | Module return beda per slot index |
| WiFi MAC OUI → phone_brand | Sudah digenerate App companion, module hanya intercept |
| `geo_lat/long` → `sim_country_iso` | Sudah konsisten dari App companion |
| Param tidak hilang after reboot | ParamStore persist via SharedPreferences |

---
