# SpoofMyDevice Modernization Plan (Android 16 / API 36)

Dokumen ini merinci rancangan implementasi prioritas 1–5 untuk memperkuat keamanan IPC, konsistensi data spoofing, hardware hooks dinamis, self-test dashboard, dan validator/randomizer cerdas.

## 1) IPC Security Hardening (ConfigProvider + ConfigBridgeReceiver)

### Arsitektur singkat

```text
Companion App (trusted, same signature)
   |
   | ContentResolver + signature permission
   v
ConfigProvider (exported=true, read/write permission signature)
   |-- UID/signature verifier
   |-- token verifier (optional defense-in-depth)
   |-- encrypted local store (AES-GCM, Android Keystore)
   |-- redacted logger
   v
Encrypted config file in app-private storage
```

### Manifest (production-ready)

```xml
<!-- AndroidManifest.xml -->
<permission
    android:name="com.devicespooflab.permission.CONFIG_BRIDGE"
    android:protectionLevel="signature" />

<provider
    android:name=".hooks.ConfigProvider"
    android:authorities="com.devicespooflab.config"
    android:exported="true"
    android:readPermission="com.devicespooflab.permission.CONFIG_BRIDGE"
    android:writePermission="com.devicespooflab.permission.CONFIG_BRIDGE"
    android:grantUriPermissions="false" />

<receiver
    android:name=".hooks.ConfigBridgeReceiver"
    android:exported="true"
    android:permission="com.devicespooflab.permission.CONFIG_BRIDGE" />
```

### Permission + UID verification di Provider

```kotlin
package com.devicespooflab.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Process
import androidx.core.content.ContextCompat

object CallerVerifier {
    private const val BRIDGE_PERMISSION = "com.devicespooflab.permission.CONFIG_BRIDGE"

    fun enforceTrustedCaller(context: Context) {
        val callingUid = Binder.getCallingUid()
        if (callingUid == Process.myUid()) return

        val pm = context.packageManager
        val packages = pm.getPackagesForUid(callingUid).orEmpty()
        if (packages.isEmpty()) {
            throw SecurityException("No package for UID=$callingUid")
        }

        val hasPerm = packages.any { pkg ->
            pm.checkPermission(BRIDGE_PERMISSION, pkg) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasPerm) {
            throw SecurityException("Caller UID=$callingUid lacks signature permission")
        }

        val selfPkg = context.packageName
        val trustedBySignature = packages.any { pkg ->
            pm.checkSignatures(selfPkg, pkg) == PackageManager.SIGNATURE_MATCH
        }

        if (!trustedBySignature) {
            throw SecurityException("Caller UID=$callingUid signature mismatch")
        }
    }
}
```

```kotlin
// ConfigProvider.kt (cuplikan)
override fun query(...): Cursor? {
    CallerVerifier.enforceTrustedCaller(context ?: throw IllegalStateException("No context"))
    ...
}
```

### Jangan ekspos identifier sensitif ke mirror publik

- Default: **nonaktifkan total** mirror `/sdcard/device_profile.conf`.
- Jika mode debug/manual diaktifkan, simpan hanya subset non-sensitif:
  - aman: model, brand, sdk, fingerprint (opsional hash)
  - blok: IMEI, IMSI, ICCID, GAID, GSF, MediaDrm ID.

```kotlin
data class PublicMirror(
    val model: String,
    val brand: String,
    val sdkInt: Int,
    val fingerprintSha256: String
)
```

### Redacted logger

```kotlin
object RedactedLogger {
    private val imeiRegex = Regex("\\b\\d{14,16}\\b")
    private val imsiRegex = Regex("\\b\\d{15}\\b")
    private val gaidRegex = Regex("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b")

    fun sanitize(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return input
            .replace(imeiRegex) { m -> m.value.take(5) + "**********" }
            .replace(imsiRegex, "<redacted-imsi>")
            .replace(gaidRegex, "<redacted-gaid>")
    }
}
```

### Enkripsi config lokal (AES-GCM + Keystore)

```kotlin
class EncryptedConfigStore(
    private val context: Context,
    private val fileName: String = "profile.enc"
) {
    private val keyAlias = "spoof_profile_aes"

    fun write(plainJson: String) {
        val key = AndroidKeyStoreAes.getOrCreateAesKey(keyAlias)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))

        context.openFileOutput(fileName, Context.MODE_PRIVATE).use { out ->
            out.write(iv.size)
            out.write(iv)
            out.write(encrypted)
        }
    }

    fun read(): String {
        val key = AndroidKeyStoreAes.getOrCreateAesKey(keyAlias)
        val bytes = context.openFileInput(fileName).use { it.readBytes() }
        require(bytes.isNotEmpty()) { "Encrypted config empty" }

        val ivSize = bytes[0].toInt()
        require(ivSize in 12..16) { "Invalid IV length" }
        val iv = bytes.copyOfRange(1, 1 + ivSize)
        val ciphertext = bytes.copyOfRange(1 + ivSize, bytes.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }
}
```

### Trade-off

- Security ↑: signature permission + signature match + encryption.
- Perf cost: decrypt sekali saat load profile (kecil, acceptable).
- Maintainability: lebih kompleks, tapi boundary IPC jadi eksplisit.

### Testing

- Unit: test redaction regex & false positive.
- Instrumentation: akses provider dari app unsigned harus `SecurityException`.
- Regression: verify mirror publik tidak berisi field sensitif.

---

## 2) ActiveProfileManager sebagai Single Source of Truth

### Arsitektur

```text
EncryptedConfigStore -> ActiveProfileManager (AtomicReference<ActiveProfile>)
                                     |
                          all hooks read snapshot only
```

### Model immutable + manager

```kotlin
@Immutable
data class ActiveProfile(
    val build: BuildProfile,
    val telephony: TelephonyProfile,
    val display: DisplayProfile,
    val hardware: HardwareProfile,
    val metadata: ProfileMetadata
)

object ActiveProfileManager {
    private val ref = java.util.concurrent.atomic.AtomicReference<ActiveProfile?>(null)

    fun initialize(profile: ActiveProfile) {
        require(ProfileValidator.validate(profile).isValid) { "Invalid profile" }
        ref.set(profile)
    }

    fun snapshot(): ActiveProfile = ref.get()
        ?: throw IllegalStateException("ActiveProfile not initialized")

    fun update(newProfile: ActiveProfile) {
        val result = ProfileValidator.validate(newProfile)
        require(result.isValid) { "Profile invalid: ${result.errors.joinToString()}" }
        ref.set(newProfile)
    }
}
```

### Validator + anomaly check

```kotlin
object ProfileValidator {
    data class Result(val isValid: Boolean, val errors: List<String>)

    fun validate(p: ActiveProfile): Result {
        val errors = mutableListOf<String>()
        if (p.build.brand.equals("samsung", true) && p.build.board.contains("pixel", true)) {
            errors += "Brand/board mismatch"
        }
        if (p.build.sdkInt >= 35 && p.hardware.abis.none { it.contains("arm64") }) {
            errors += "SDK 35+ requires arm64 in profile"
        }
        if (p.hardware.totalRamMb !in 2048..24576) {
            errors += "Unrealistic RAM"
        }
        return Result(errors.isEmpty(), errors)
    }
}
```

### Trade-off

- Konsistensi kuat, race condition turun drastis.
- Hot reload profile perlu invalidation strategy; trade-off dengan kesederhanaan snapshot model.

### Testing

- Concurrent tests: 50 thread membaca snapshot + update simultan.
- Startup benchmark: validator selesai < 20 ms untuk 1 preset.

---

## 3) HardwareHooks Dinamis + Memory Math Fix

### Arsitektur

```text
Build.MODEL -> DeviceClassResolver -> HardwarePreset
                               -> MemoryMathSync(original total/avail)
                               -> Hook outputs (ActivityManager, /proc/*)
```

### Dynamic map + memory fix

```kotlin
enum class DeviceTier { ENTRY, MID, FLAGSHIP }

data class HardwarePreset(
    val cpuCores: Int,
    val ramOptionsMb: IntArray,
    val heapClassMb: Int,
    val abi: Array<String>,
    val socFamily: String
)

object HardwareProfileResolver {
    private val rules = listOf(
        Regex("^SM-A.*") to DeviceTier.MID,
        Regex("^SM-S.*") to DeviceTier.FLAGSHIP,
        Regex("^Pixel [89].*") to DeviceTier.FLAGSHIP
    )

    fun resolve(model: String): HardwarePreset {
        val tier = rules.firstOrNull { it.first.matches(model) }?.second ?: DeviceTier.MID
        return when (tier) {
            DeviceTier.ENTRY -> HardwarePreset(8, intArrayOf(4096), 256, arrayOf("arm64-v8a"), "entry-soc")
            DeviceTier.MID -> HardwarePreset(8, intArrayOf(6144, 8192), 384, arrayOf("arm64-v8a"), "mid-soc")
            DeviceTier.FLAGSHIP -> HardwarePreset(8, intArrayOf(8192, 12288), 512, arrayOf("arm64-v8a"), "flagship-soc")
        }
    }
}

fun syncedAvailMem(spoofedTotal: Long, originalTotal: Long, originalAvail: Long): Long {
    if (originalTotal <= 0L || spoofedTotal <= 0L) return spoofedTotal / 2L
    val usedRatio = ((originalTotal - originalAvail).coerceAtLeast(0L)).toDouble() / originalTotal.toDouble()
    val newUsed = (spoofedTotal * usedRatio).toLong()
    return (spoofedTotal - newUsed).coerceIn(0L, spoofedTotal)
}
```

### Hook /proc framework (future native-ready)

```cpp
// pseudo C++ interface for future LSPosed native bridge
struct ProcInterceptor {
    virtual std::string interceptCpuInfo(const std::string& original, const ActiveProfileNative& p) = 0;
    virtual std::string interceptMemInfo(const std::string& original, const ActiveProfileNative& p) = 0;
    virtual ~ProcInterceptor() = default;
};
```

### Trade-off

- Realisme output ↑, deteksi anomali anti-fraud ↓.
- Complexity ↑ karena ruleset growing; mitigasi dengan table-driven config.

### Testing

- Property-based test: availMem selalu `0 <= avail <= total`.
- Snapshot test `/proc/meminfo` format valid.

---

## 4) ConsistencyCheckFragment + Back Dispatcher (API 36)

### Struktur status

```kotlin
enum class CheckStatus { MATCH, HIDDEN, LEAKED }

data class CheckItem(
    val label: String,
    val expected: String,
    val actual: String,
    val status: CheckStatus
)
```

### Fragment (sequential API probe)

```kotlin
class ConsistencyCheckFragment : Fragment() {
    private val vm: ConsistencyCheckViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            vm.runChecks(requireContext())
        }
    }
}

class ConsistencyCheckViewModel : ViewModel() {
    val state = MutableStateFlow<List<CheckItem>>(emptyList())

    suspend fun runChecks(context: Context) = withContext(Dispatchers.IO) {
        val p = ActiveProfileManager.snapshot()
        val tm = context.getSystemService(TelephonyManager::class.java)
        val model = Build.MODEL.orEmpty()
        val fp = android.os.SystemProperties.get("ro.build.fingerprint", "")
        val imei = runCatching { tm?.imei.orEmpty() }.getOrDefault("")
        val aid = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        val dm = context.resources.displayMetrics

        val items = listOf(
            compare("Build.MODEL", p.build.model, model),
            compare("Fingerprint", p.build.fingerprint, fp),
            compare("IMEI", p.telephony.imei ?: "", imei),
            compare("ANDROID_ID", p.metadata.androidId ?: "", aid),
            compare("DensityDpi", p.display.densityDpi.toString(), dm.densityDpi.toString())
        )
        state.value = items
    }

    private fun compare(label: String, expected: String, actual: String): CheckItem {
        val status = when {
            actual.isBlank() -> CheckStatus.HIDDEN
            actual == expected -> CheckStatus.MATCH
            else -> CheckStatus.LEAKED
        }
        return CheckItem(label, expected, actual, status)
    }
}
```

### MainActivity back callback (Android 15/16)

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!navController.popBackStack()) finish()
        }
    })
}
```

### Testing

- UI test: tiap item render badge warna sesuai status.
- Instrumentation: jalankan saat LSPosed aktif/nonaktif.

---

## 5) IntelligentRandomizer + RemoteSchemaValidator

### Intelligent randomizer

```kotlin
object IntelligentRandomizer {
    fun generate(base: ActiveProfile, now: LocalDate = LocalDate.now()): ActiveProfile {
        val buildDate = now.minusDays((10..120).random().toLong())
        val patchDate = buildDate.minusDays((1..45).random().toLong())

        val newImei = ImeiGenerator.tacForModel(base.build.model) + Random.nextLong(1000000000L, 9999999999L)
        val luhn = Luhn.computeCheckDigit(newImei)
        val imei15 = "$newImei$luhn"

        return base.copy(
            build = base.build.copy(
                buildId = BuildIdGenerator.forFingerprint(base.build.fingerprint, buildDate),
                securityPatch = patchDate.toString()
            ),
            telephony = base.telephony.copy(
                imei = imei15,
                macAddress = MacGenerator.locallyAdministeredUnicast()
            )
        )
    }
}
```

### Remote schema + sanitize + compatibility

```kotlin
object RemoteSchemaValidator {
    data class ValidationResult(val ok: Boolean, val errors: List<String>)

    private val safeString = Regex("^[a-zA-Z0-9._:/\\- ]{1,200}$")

    fun validate(json: JSONObject, moduleVersion: SemVer): ValidationResult {
        val errors = mutableListOf<String>()
        if (!json.has("sdk_int") || json.opt("sdk_int") !is Int) errors += "sdk_int must be integer"
        if (!json.has("model") || json.optString("model").isBlank()) errors += "model required"

        val minModule = SemVer.parse(json.optString("min_module_version", "0.0.0"))
        if (moduleVersion < minModule) errors += "Module version too old"

        json.keys().forEach { key ->
            val v = json.optString(key, "")
            if (v.isNotBlank() && !safeString.matches(v)) errors += "Unsafe chars in $key"
        }

        return ValidationResult(errors.isEmpty(), errors)
    }
}
```

### Testing

- Unit: schema reject wrong types (`sdk_int: "36"` string).
- Security test: payload dengan `;rm -rf` harus ditolak.
- Compatibility test: preset `min_module_version=1.5.0` ditolak di modul `1.4.x`.

---

## Catatan kompatibilitas Android 15–16 (API 35–36)

- Hindari API deprecated `onBackPressed()`, gunakan `OnBackPressedDispatcher`.
- Telephony APIs makin ketat; selalu wrap permission/SecurityException handling.
- System properties reflection bisa dibatasi OEM; fallback chain wajib.
- Perhatikan scoped storage: jangan default tulis ke external public path untuk data sensitif.
