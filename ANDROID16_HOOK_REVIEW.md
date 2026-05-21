# Android 16 Hook Compatibility Review

Tanggal review: 21 Mei 2026.

## Ringkasan

- 16 hook ditinjau untuk kompatibilitas Android 16 (API 36) dengan cross-check ke Android SDK reference dan behavior change docs.
- Mayoritas hook masih valid karena targetnya method/class yang tetap tersedia, namun terdapat 3 area yang perlu hardening implementasi:
  1. `ContextHooks`: mitigasi penentuan argumen `flags` agar tidak salah menyentuh parameter `int` non-flags pada overload tertentu.
  2. `WebViewHooks`: cakupan constructor diperluas agar kompatibel dengan seluruh variasi konstruktor WebView yang relevan.
  3. `AppSetIdHooks`: penggunaan konstanta scope dibuat dinamis (reflective) agar tahan perubahan upstream.

## Validasi per File

### 1) AdvertisingIdHooks.java
- Hook pada `AdvertisingIdClient$Info.getId()` dan `AdvertisingIdClient.getAdvertisingIdInfo(Context)` tetap relevan untuk Play services path.
- Catatan: ini API Google Play services (bukan Android framework core), sehingga kompatibilitas bergantung versi GMSCore.
- Status: **valid dengan caveat runtime availability**.

### 2) AppSetIdHooks.java
- Hook `AppSetIdInfo.getId()` dan `getScope()` tetap sesuai.
- Perbaikan diterapkan: nilai APP scope tidak lagi hardcoded `1`, tapi resolve dari `SCOPE_APP` dengan fallback.
- Status: **valid + diperbaiki**.

### 3) BuildHooks.java
- Modifikasi field static `android.os.Build` / `Build.VERSION` masih memungkinkan di konteks Xposed.
- Method `Build.getSerial()` masih ada; akses normal app dibatasi permission, namun hook after-method tetap dapat menimpa result.
- Status: **valid**.

### 4) BuildSerialHooks.java
- Hook `Build.getSerial()` dan field `Build.SERIAL` masih kompatibel; `SERIAL` deprecated tetapi tetap ada untuk backward compatibility.
- Status: **valid**.

### 5) ContextHooks.java
- Android 14+ mewajibkan receiver export flag untuk context-registered receivers; masih berlaku di Android 16.
- Perbaikan diterapkan: hanya modifikasi **parameter terakhir** jika benar-benar bertipe `int` dan signature mengandung `IntentFilter`, mencegah salah patch ke argumen lain.
- Status: **valid + diperbaiki**.

### 6) DisplayHooks.java
- Hook terhadap API display metrics/size masih tersedia pada Android modern meski sebagian method legacy dapat deprecated.
- Status: **valid (perlu monitor method legacy)**.

### 7) EmulatorDetectionHooks.java
- Hook property/heuristik deteksi emulator berbasis Build/System masih relevan.
- Status: **valid**.

### 8) GetPropHooks.java
- Intersepsi command/property read pattern (`getprop`) masih relevan untuk bypass deteksi.
- Status: **valid**.

### 9) HardwareHooks.java
- Hook info hardware (ABI/sensor/cpu markers) masih sejalan dengan API Android 16.
- Status: **valid**.

### 10) JavaSystemPropertyHooks.java
- Hook `java.lang.System.getProperty(...)` tetap kompatibel JVM/ART modern.
- Status: **valid**.

### 11) MediaDrmHooks.java
- Hook identifier path MediaDrm tetap sensitif permission/security policy, namun API surface tetap ada.
- Status: **valid dengan caveat policy/runtime**.

### 12) PackageManagerHooks.java
- Hook query feature/package checks tetap relevan; perlu hati-hati perubahan visibility/package queries (Android 11+).
- Status: **valid**.

### 13) SettingsHooks.java
- Hook `Settings.Secure/Global/System` get-path masih tersedia.
- Status: **valid**.

### 14) SystemPropertiesHooks.java
- `android.os.SystemProperties` get/getInt/getLong/getBoolean signatures masih ada (hidden API internal, tapi tetap dipakai framework).
- Status: **valid**.

### 15) TelephonyHooks.java
- API seperti `getImei`, `getMeid`, `getSubscriberId`, `getLine1Number` masih ada namun dibatasi permission ketat sejak API 29+.
- Hook tetap berguna untuk spoof result di app context.
- Status: **valid dengan caveat permission gating**.

### 16) WebViewHooks.java
- Konstruktor WebView bervariasi; beberapa overload deprecated, namun masih ada.
- Perbaikan diterapkan: hook diganti menjadi `hookAllConstructors(WebView.class, ...)` untuk memastikan seluruh variasi constructor ter-cover.
- Status: **valid + diperbaiki**.

## Detail Perbaikan yang Diterapkan

1. `ContextHooks`
   - Sebelumnya mencari `Integer` dari kanan tanpa validasi signature penuh.
   - Kini memvalidasi method reflection + memastikan argumen terakhir `int` + ada `IntentFilter` dalam parameter list sebelum set `RECEIVER_EXPORTED`.

2. `AppSetIdHooks`
   - Sebelumnya `getScope()` mengembalikan literal `1`.
   - Kini resolve konstanta `SCOPE_APP` via refleksi dengan fallback aman.

3. `WebViewHooks`
   - Sebelumnya hanya 3 constructor di-hook.
   - Kini semua constructor WebView di-hook, sehingga lebih robust untuk Android vendor/framework variant.

## Rekomendasi Best Practice Android 16

- Selalu gunakan hook defensif berbasis runtime signature checking untuk method overload yang rawan berubah.
- Untuk API berbasis Play Services, tambahkan fallback karena class dapat berubah antar versi GMSCore.
- Hindari hardcoded enum/int constant bila class menyediakan konstanta publik/static.
- Pertahankan logging granular per hook agar regression Android minor release cepat terdeteksi.
- Jalankan smoke test per target SDK (34, 35, 36) karena perilaku permission/hidden API dapat berbeda antar release.
