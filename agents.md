# Dokumentasi Arsitektur SpoofMyDevice

## Tujuan Kode
SpoofMyDevice adalah sebuah aplikasi dan modul LSPosed yang dirancang untuk memalsukan (spoofing) profil dan identitas perangkat keras Android. Tujuan utamanya adalah untuk melindungi privasi pengguna atau menghindari deteksi (seperti pemeriksaan korelasi hardware dan sistem anti-fraud) oleh aplikasi target. Proyek ini memungkinkan modifikasi nilai-nilai pada sistem Android (seperti properti Build, System Properties, dan spesifikasi hardware) saat *runtime*, sehingga aplikasi yang memindai perangkat akan membaca informasi spesifikasi dari perangkat nyata (seperti profil Pixel atau Galaxy yang sepenuhnya valid), tanpa mengharuskan modul berjalan dengan akses root (beroperasi di ruang lingkup LSPosed/Xposed).

## Inti Kode
Kode ini terbagi dalam beberapa komponen utama yang bekerja saling terintegrasi:

1. **Xposed/LSPosed Hooks (`MainHook` dan paket `com.devicespooflab.hooks.hooks`)**
   - `MainHook`: *Entry point* utama bagi modul Xposed. Bertanggung jawab menginisialisasi semua kelas hook (Build, System Properties, Display, Emulator Detection, Hardware, Telephony, dll.) ketika aplikasi target dimuat oleh sistem Android.
   - **Kelas-Kelas Hook (mis. `SystemPropertiesHooks`, `BuildHooks`, `HardwareHooks`)**: Logika inti untuk mencegat (intercept) pemanggilan API dari aplikasi target. Contoh: memalsukan nilai yang dibaca lewat API `android.os.SystemProperties`, memodifikasi hasil bacaan sistem level rendah (`/proc/cpuinfo`, memori, CPU cores), atau API resolusi tampilan.
   - `EmulatorDetectionHooks`: Modul pertahanan untuk menyembunyikan jejak-jejak emulator, Xposed, Magisk, atau *root* dengan mencegat fungsi pengecekan *file existence* dan eksekusi command line (mis. sembunyikan `/su/bin/su` atau `/dev/qemu_pipe`).

2. **Manajemen Profil & Validasi (`ConfigManager`, `ConfigFileManager`, dan `ProfileManager`)**
   - `ConfigManager` & `ConfigFileManager`: Bertanggung jawab membaca, memori, dan mengelola konfigurasi (`device_profile.conf`) yang berisi *key-value* parameter *spoof*. Komponen ini menjembatani sinkronisasi *state* dari antarmuka pengguna (UI) ke memori proses masing-masing aplikasi yang di-*hook*.
   - `ProfileManager`: Lapisan validasi integritas profil. Bertugas memeriksa konsistensi logika konfigurasi (misalnya: memastikan format MAC address benar, IMEI memiliki panjang digit yang valid, *fingerprint* sesuai *brand*, serta relasi versi Android OS dengan *API Level*). Ini krusial agar profil palsu tidak terdeteksi oleh sistem deteksi anomali.

3. **Katalog Profil (Data Presets)**
   - `RealDevicePresets` & `DevicePresetCatalog`: Menyediakan *dump* parameter spesifikasi perangkat nyata yang divalidasi (seperti data murni Google Pixel 8 Pro). Hal ini menghindari terdeteksinya "profil sintetik", yang acapkali gagal dalam verifikasi korelasi hardware secara logika.

4. **Lapisan Diagnostik dan Antarmuka (UI/IPC)**
   - `HookDiagnostics`: Sistem *logging* tunggal (tersentralisasi) untuk mencatat aktivitas modul, keberhasilan, alasan *hook* dilewati (skipped), atau kendala eksekusi.
   - **UI & IPC Security**: Pengaturan dikendalikan melalui aplikasi kompanion (Android *Activities*). File konfigurasi akan disebar melintasi batas-batas proses (ke modul Xposed) secara aman via `ConfigProvider`, yang diproteksi menggunakan `Signature Permission` dan tingkat level akses untuk menjaga keamanan data identitas pengguna.

## Alur Logic
Alur dari sistem SpoofMyDevice beroperasi melalui siklus *end-to-end* sebagai berikut:

1. **Pengaturan Profil (Sisi Aplikasi Companion):**
   - Pengguna membuka aplikasi kompanion. Aplikasi mengambil dan menampilkan status modul dan profil perangkat yang sedang aktif.
   - Pengguna memilih *preset* perangkat (misal Pixel 8 Pro dari `RealDevicePresets`) atau mendefinisikan profil *custom* lalu menyimpannya.
   - Aplikasi menggunakan `ConfigFileManager` untuk menulis *key-value* tersebut ke dalam berkas `device_profile.conf`. Untuk mengatasi permasalahan akses data antar aplikasi, konfigurasi dikendalikan ketersediaannya (mirror/provider).

2. **Pemuatan Modul (Injeksi Saat Runtime):**
   - Saat sebuah aplikasi di-*launch* di perangkat, sistem Android memanggil *framework* Xposed (lewat *Zygote*/`handleLoadPackage`).
   - Eksekusi diteruskan ke `MainHook`. Awalnya, modul membaca konfigurasi dari `ConfigManager` dan mengecek `PerAppSettings` untuk memastikan apakah paket target diizinkan untuk di-*spoof*. Jika tidak aktif, modul akan keluar awal.

3. **Validasi Pra-Syarat (Pre-flight Validation):**
   - `MainHook` memanggil `ProfileManager.isProfileValid()` untuk mengecek apakah konfigurasi tidak bertentangan.
   - Jika konfigurasi gagal (contoh: OS direntang sebagai Android 14 tapi SDK diatur pada API 30), seluruh proses *hook* digagalkan dan di-log. Ini untuk menghindari akun pengguna di-*banned* karena menyerahkan data perangkat keras tidak logis ke aplikasi pendeteksi bot.

4. **Eksekusi Hooks (Intersepsi Dinamis):**
   - Setelah tervalidasi, serangkaian rutinitas `try-catch` memanggil berbagai modul Hook untuk mengubah fungsionalitas Android bagi aplikasi yang berjalan tersebut:
     - Pada `BuildHooks`: nilai *fields* statis dalam `android.os.Build` akan ditiban secara *reflection*.
     - Pada `SystemPropertiesHooks`: Fungsi `get` untuk memanggil *property* Android dicegat sehingga merespons dengan data fiktif profil.
     - Pada `HardwareHooks` dan lainnya: Fungsi utilitas tingkat rendah (seperti `Runtime.availableProcessors`) serta bacaan IO pada berkas sistem disamarkan.

5. **Penerapan dan Resiliensi Eksekusi:**
   - Selain itu, komponen khusus juga meng-injeksi deteksi `Activity Lifecycle` dan `Application.attach()` di aplikasi target untuk memaksa *reload* konfigurasi secara dinamis tanpa perlu me-*restart* *Zygote* secara keseluruhan.
   - Pada akhirnya, aplikasi target berjalan normal tetapi dengan perspektif ilusi terhadap platform di mana aplikasi tersebut berada; semua interaksi untuk mendeteksi ID unik, identitas *hardware*, atau *root* akan mendapatkan balasan sesuai dengan *blueprints* murni yang ada pada SpoofMyDevice.