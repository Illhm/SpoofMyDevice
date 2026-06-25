# Dokumentasi Arsitektur SpoofMyDevice

## Tujuan Kode
SpoofMyDevice adalah aplikasi pendamping (companion app) berbasis Android sekaligus modul Xposed/LSPosed yang bertujuan untuk melindungi privasi pengguna dan mencegah pelacakan perangkat dengan cara memalsukan (*spoofing*) spesifikasi dan identitas sistem Android secara *real-time*. Tujuan utamanya adalah untuk memastikan aplikasi target yang memindai perangkat memperoleh informasi perangkat keras (hardware) dan perangkat lunak yang konsisten dari profil perangkat nyata (seperti profil Pixel, Galaxy, dll), tanpa perlu menggunakan akses root (kecuali untuk utilitas tambahan seperti membersihkan cache via su), dan tanpa perlu merusak struktur integritas sistem yang asli (beroperasi seutuhnya pada ruang lingkup proses hooking memori). Proyek ini juga disiapkan sebagai *framework* yang fleksibel, mendukung mekanisme keamanan *sandboxing*, *plugin engine*, dan manajemen profil secara menyeluruh.

## Inti Kode
Sistem ini dibangun atas berbagai lapisan arsitektur yang terbagi dalam komponen-komponen kunci berikut:

1. **Entry Point & Sistem Hooking (Lapisan Xposed)**
   - `MainHook`: Kelas *entry point* (implementasi `IXposedHookLoadPackage`) yang menyuntikkan dan menginisialisasi semua layanan *hook* ke dalam aplikasi target.
   - **Manajemen Modul Hook**: Terdapat puluhan kelas *hook* spesifik yang ditugaskan untuk mencegat fungsi sistem:
     - `SystemPropertiesHooks`, `BuildHooks`, `GetPropHooks`: Mengubah *system properties* (seperti `ro.build.fingerprint`) dan variabel statis di kelas `Build` menggunakan Java Reflection untuk mencegah pelacakan profil asli.
     - `HardwareHooks`, `DisplayHooks`: Memanipulasi bacaan spesifikasi fisik (jumlah *core* CPU, kapasitas RAM, dimensi layar, kepadatan piksel).
     - `EmulatorDetectionHooks`: Lapis pertahanan yang mengamankan aplikasi dari deteksi emulator/Xposed dengan menyembunyikan berkas rahasia seperti `/dev/qemu_pipe`, jalur su (`/sbin/su`, `/data/adb/magisk`), dan memanipulasi *stack traces*.
     - `TelephonyHooks`, `AdvertisingIdHooks`, `MacAddressHooks`: Menyembunyikan identitas seluler (IMEI, IMSI, ICCID), ID iklan, dan alamat fisik perangkat (MAC, Bluetooth).
     - `ContextHooks`, `WebViewHooks`, `AppSetIdHooks`, `MediaDrmHooks`: Mengamankan jalur identifikasi unik lainnya pada level aplikasi (mis. agen peramban (*User-Agent*), AppSetId, DRM).

2. **Manajemen Konfigurasi, Sinkronisasi, dan Penyimpanan (Data & Utils)**
   - `ConfigManager` & `ConfigFileManager`: Kelas krusial yang mengontrol akses ke *state* spoofing. Bertanggung jawab memuat *key-value* dari fail `device_profile.conf` dan menyinkronkan data dari lapisan penyedia konfigurasi ke seluruh lapisan memori dan aplikasi ter-hook.
   - `ConfigProvider` & `ConfigBridgeReceiver`: Karena modifikasi Xposed bekerja di luar kotak pasir reguler (*process boundaries*), Content Provider ini (diamankan dengan *signature permission*) bertugas menyiarkan nilai *config* ke dalam *environment* Xposed secara *cross-process*.
   - `DevicePresetCatalog`, `RealDevicePresets`, `SamsungPresets`: Repositori internal yang menampung *dump* lengkap (*blueprints*) dari spesifikasi perangkat asli untuk digunakan sebagai profil, guna lulus dari pengujian korelasi (*fraud-detection*) yang ketat.
   - `RootAccessManager`: Komponen opsional pada companion app untuk memberikan izin kontrol *root-level* jika perangkat di-*root* (untuk fungsi *tools* semacam manajemen cache aplikasi dan pemaksaan *stop*).

3. **Validasi & Integritas Profil (Profile & Diagnostics)**
   - `ProfileManager`: Pengecek rasionalitas konfigurasi. Jika profil modifikasi dinilai tidak logis (mis. *fingerprint* dan *brand* tidak sesuai, atau versi Android < API 26), proses sistem modifikasi tidak akan disuntikkan demi menghalangi deteksi *red flag*.
   - `HookDiagnostics`: Sistem telemetri untuk pencatatan jejak (audit) modul Xposed yang gagal/terlewati (*skipped*)/berhasil diterapkan.
   - `AndroidVersionCompat`: Utilitas komparator batasan *framework* API SDK, memastikan fungsionalitas disesuaikan secara logis dengan versi perangkat dari Android 8 hingga API 36 (Android 16).

4. **Sistem Plugin Keamanan Tinggi (Plugin Engine Layer)**
   - `TigerPluginEngine` & `SandboxedPluginExecutor`: Sistem mesin plugin prototipe berlapis baja (*Tiger-inspired layer*) yang memberlakukan *strict sandbox constraints*. Mengamankan eksekusi dari jaringan pihak ketiga atau API JNI ilegal dengan membatasi eksekusi menggunakan basis `Future` timeouts; melemparkan `SandboxedExecutionException` apabila terjadi pelanggaran privilese (*sandbox evasion*).

5. **Antarmuka Pengguna & Navigasi (UI Layer)**
   - `MainActivity`, `RealInfoActivity`, `SafeModeAppsActivity`: *Activity* utama di mana pengaturan modifikasi diberlakukan.
   - `HomeFragment`, `DeviceSettingsFragment`, `AppSettingsFragment`, `RealInfoFragment`: Komponen berbasis fragmen tempat pengguna dapat mengatur preferensi spesifikasi profil per-aplikasi (`PerAppSettings`), melihat status layanan asli, dan menerapkan konfigurasi modifikasi yang dipilih.

## Alur Logic
Proses sistem memodifikasi perilaku *runtime* Android mengalir sebagai berikut:

1. **Pemilihan Profil (Fase UI & Penyimpanan Data)**
   - Pengguna membuka SpoofMyDevice (via `MainActivity` -> `DeviceSettingsFragment`).
   - Sistem membaca profil nyata dari `DevicePresetCatalog` dan menampilkannya kepada pengguna.
   - Setelah profil *Custom* maupun *Preset* disimpan, `ConfigFileManager` menulisnya ke berkas lokal.
   - Modifikasi preferensi paket aplikasi (*bypass* / *enable*) disimpan dalam `AppSettingsStore`.
   - Modifikasi memicu *broadcast intent* yang ditangkap oleh `ConfigBridgeReceiver`, menginstruksikan `ConfigProvider` untuk siap sedia menyajikan profil ini kepada klien (Target App).

2. **Injeksi Sistem dan Penyiapan Hook (Fase Zygote / Runtime)**
   - Ketika paket/aplikasi target dimuat (*launch*), proses Xposed `IXposedHookLoadPackage` terpicu yang menjalankan `MainHook`.
   - Sistem menarik konfigurasi memori lewat sinkronisasi `ConfigManager`.
   - Di sini `PerAppSettings` dicek untuk menentukan apakah aplikasi dalam mode diam (di-bypass) atau di-*spoof*.
   - Jika modul beroperasi, `ProfileManager` menjalankan validasi korelasi. Jika lulus validasi konsistensi, status diteruskan ke tahap aktivasi seluruh kumpulan *hook*.

3. **Pencegatan Level Eksekusi dan Adaptasi (Fase Intercept)**
   - Modul `*Hooks.java` yang relevan aktif menempel pada metode *native* OS dan Java (menggunakan `XposedHelpers.findAndHookMethod`).
   - Contoh: Ketika aplikasi klien meminta properti IMEI ke TelephonyManager, fungsi `TelephonyHooks` mencegat balasan (*intercept*) dan memberikan variabel dari profil *Custom* (lewat `ConfigManager.getIMEI()`).
   - Contoh lain: Jika aplikasi melacak status emulator menggunakan pembacaan berkas (seperti mencari `su` atau membaca ukuran RAM), kelas pembaca sistem (`java.io.File`, `Runtime.exec`) diarahkan agar memberikan nilai murni dari `RealDevicePresets` dan mengaburkan jejak utilitas eksternal (`EmulatorDetectionHooks`).
   - Fungsi sandboxing pada tingkat eksekusi `SandboxedPluginExecutor` mengekang skrip dinamis agar tetap berjalan dalam batas *timeout* CPU memori dan ruang lingkup terbatas yang diinisialisasi oleh TigerPluginEngine.

4. **Monitoring dan Resiliensi Jangka Panjang**
   - Agar aplikasi dapat me-*refresh* perubahan data saat dipakai, sebuah penyadap `Activity Lifecycle` dan `Application.attach()` disuntikkan di aplikasi tersebut. Konfigurasi memori dapat diubah (*reload*) seketika tanpa harus melakukan *force reboot* berkat struktur pemantauan Xposed ini.
   - Seluruh langkah modifikasi yang dieksekusi atau tidak terdukung disuplai ke log komprehensif menggunakan `HookDiagnostics`.

Dengan arsitektur yang sangat kompartemen seperti ini, sistem memastikan penyamaran perangkat bekerja sangat transparan (*stealth*) tanpa membahayakan konsistensi fungsi inti dalam sistem operasi asli pengguna.