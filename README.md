# CatatanBelajar 📱📝

Aplikasi Android untuk mencatat materi kuliah, potongan kode, dan tugas. Setiap pengguna punya akun sendiri, dan catatannya tersimpan di cloud sehingga bisa diakses kapan saja. Catatan bisa diberi foto dari kamera atau galeri.

Dibuat dengan **Kotlin** dan **Firebase**.

---

## Daftar Isi

1. [Deskripsi Singkat](#1-deskripsi-singkat)
2. [Fitur](#2-fitur)
3. [Arsitektur dan Teknologi](#3-arsitektur-dan-teknologi)
4. [Instalasi dan Menjalankan Proyek](#4-instalasi-dan-menjalankan-proyek)

---

## 1. Deskripsi Singkat

**CatatanBelajar** membantu mahasiswa merapikan catatan belajarnya di satu tempat. Pengguna mendaftar, masuk, lalu membuat catatan yang dikelompokkan ke dalam kategori **Kuliah**, **Kode**, **Tugas**, dan **Lainnya**. Semua data disimpan di Cloud Firestore dan dilindungi Security Rules, sehingga tiap pengguna hanya bisa melihat dan mengubah catatannya sendiri.

## 2. Fitur

### Halaman aplikasi

| # | Halaman | Fungsi |
|---|---|---|
| 1 | Splash | Logo beranimasi, lalu otomatis menuju Login atau Home sesuai status login |
| 2 | Login | Masuk dengan email dan password |
| 3 | Register | Membuat akun baru |
| 4 | Home | Daftar catatan, pencarian, filter kategori, tombol tambah |
| 5 | Detail / Form | Melihat, menambah, mengubah, dan menghapus catatan |
| 6 | Profil | Foto profil, ubah nama, statistik catatan, keluar |

### Daftar fitur

**Autentikasi pengguna (multi user)**
- Register dan login memakai **Firebase Authentication** (email dan password).
- Sesi login tersimpan, jadi pengguna tidak perlu masuk ulang setiap membuka aplikasi.
- Validasi input (format email, panjang password, konfirmasi password) dan menampilkan pesan error jika tidak sesuai.
- Logout dari halaman Profil.

**Catatan (CRUD dengan Cloud Firestore)**
- **Create:** tambah catatan lewat tombol **+** (judul, kategori, isi, foto).
- **Read:** daftar catatan tampil **real-time**, otomatis berubah saat data berubah. Halaman detail menampilkan satu catatan lengkap.
- **Update:** ubah judul, kategori, isi, dan foto catatan.
- **Delete:** hapus lewat menu di halaman detail (dengan konfirmasi) atau **geser kartu** di Home, lengkap dengan tombol **Urungkan**.

**Pencarian dan filter**
- Cari catatan berdasarkan judul atau isi.
- Filter berdasarkan kategori: Semua, Kuliah, Kode, Tugas.

**Fitur hardware: kamera dan galeri**
- Ambil foto langsung dengan **kamera** (izin `CAMERA` diminta saat dibutuhkan) atau pilih dari **galeri** (Photo Picker bawaan Android, tanpa izin penyimpanan).
- Dipakai untuk **foto profil** dan **foto lampiran catatan**.
- Foto dikecilkan dan diperbaiki rotasinya sebelum disimpan.

**Profil**
- Ganti foto profil dari kamera atau galeri.
- Ubah nama tampilan.
- Statistik jumlah catatan dan jumlah tugas.

**Keamanan dan keandalan**
- **Firestore Security Rules:** setiap pengguna hanya bisa membaca, mengubah, dan menghapus dokumen miliknya sendiri.
- Perubahan saat offline disimpan di perangkat dan disinkronkan otomatis saat koneksi kembali.
- Tampilan kosong (empty state) dan indikator loading di setiap proses yang butuh jaringan.

## 3. Arsitektur dan Teknologi

### Teknologi

| Bagian | Teknologi |
|---|---|
| Bahasa | Kotlin |
| UI | Android Views (XML), **ViewBinding**, **Material Components 3** |
| Daftar | RecyclerView, `ListAdapter` dan `DiffUtil` |
| Asinkron | Kotlin Coroutines (`lifecycleScope`, `Task.await()`) |
| Autentikasi | **Firebase Authentication** (email dan password) |
| Database | **Cloud Firestore** (NoSQL, real-time listener) |
| Manajemen versi Firebase | **Firebase BoM** (Bill of Materials) |
| Kamera dan galeri | Activity Result API (`TakePicture`, `PickVisualMedia`), `FileProvider`, `ExifInterface` |
| Build | Gradle Kotlin DSL dengan **Version Catalog** (`gradle/libs.versions.toml`) |
| SDK | `compileSdk` dan `targetSdk` 36, `minSdk` 26 (Android 8.0) |

Versi tiap library tercatat di `gradle/libs.versions.toml`.

### Arsitektur

Aplikasi memakai pemisahan lapisan yang sederhana:

```
┌─────────────────────────────────────────┐
│  UI  (ui/)                              │
│  Activity + ViewBinding + Adapter       │
│  Menampilkan data, menangani input      │
└───────────────┬─────────────────────────┘
                │ memanggil
┌───────────────▼─────────────────────────┐
│  Data  (data/)                          │
│  NoteRepository, UserRepository, Note   │
│  Semua akses Firestore ada di sini      │
└───────────────┬─────────────────────────┘
                │
┌───────────────▼─────────────────────────┐
│  Firebase                               │
│  Authentication  ·  Cloud Firestore     │
└─────────────────────────────────────────┘

util/  : fungsi bantu (validasi, format tanggal, pesan error,
         pengolah gambar, pemilih foto kamera/galeri)
```

- **Repository pattern:** Activity tidak menulis query Firestore sendiri, semuanya lewat `NoteRepository` dan `UserRepository`.
- **Real-time:** daftar catatan di Home memakai `addSnapshotListener`. Listener dipasang di `onStart()` dan dilepas di `onStop()`.
- **Kepemilikan data:** tiap dokumen catatan menyimpan `uid` pemiliknya. Query selalu difilter berdasarkan `uid` pengguna yang sedang login, dan Security Rules menegakkannya di sisi server.

### Struktur proyek

```
app/src/main/
├── AndroidManifest.xml
├── java/com/example/catatanbelajar/
│   ├── data/
│   │   ├── Note.kt                  model catatan
│   │   ├── NoteRepository.kt        CRUD koleksi notes
│   │   └── UserRepository.kt        profil pengguna (koleksi users)
│   ├── ui/
│   │   ├── SplashActivity.kt
│   │   ├── LoginActivity.kt
│   │   ├── RegisterActivity.kt
│   │   ├── MainActivity.kt          Home
│   │   ├── NoteAdapter.kt
│   │   ├── NoteDetailActivity.kt    lihat, tambah, edit
│   │   └── ProfileActivity.kt
│   └── util/
│       ├── Extensions.kt
│       ├── ImageUtils.kt
│       ├── ImagePickerHelper.kt
│       └── CategoryStyle.kt
└── res/                             layout, drawable, animator, menu, values, xml
```

### Model data Firestore

```
users/{uid}
    name: String
    email: String
    photoBase64: String       foto profil (JPEG Base64, maks. 512 px)
    createdAt: Timestamp

notes/{autoId}
    uid: String               pemilik catatan (uid FirebaseAuth)
    title: String
    content: String
    category: String          Kuliah | Kode | Tugas | Lainnya
    photoBase64: String       foto lampiran, kosong jika tidak ada (maks. 800 px)
    createdAt: Timestamp      diisi server
    updatedAt: Timestamp      diisi server
```

> **Catatan desain:** foto disimpan sebagai teks Base64 langsung di dokumen Firestore, bukan di Firebase Storage, agar proyek berjalan di paket gratis. Foto dikecilkan dulu (sekitar 50–150 KB) sehingga jauh di bawah batas dokumen 1 MiB. Untuk aplikasi produksi, simpan file di Firebase Storage dan simpan URL-nya saja di Firestore.

### Izin aplikasi

| Izin | Kegunaan |
|---|---|
| `INTERNET` | Terhubung ke Firebase Authentication dan Firestore |
| `CAMERA` | Mengambil foto dengan kamera (diminta saat runtime; jika ditolak, galeri tetap bisa dipakai) |

## 4. Instalasi dan Menjalankan Proyek

### Prasyarat

- **Android Studio** versi terbaru (sudah termasuk JDK dan Android SDK)
- Android SDK Platform **36**
- Perangkat Android (Android 8.0 atau lebih baru) atau emulator
- Akun Google untuk membuat proyek **Firebase**

### Langkah 1: Clone repositori

```bash
git clone https://github.com/alfigrace/CatatanBelajar.git
cd CatatanBelajar
```

Lalu buka folder proyek lewat **Android Studio > Open** dan tunggu proses **Gradle Sync** selesai.

### Langkah 2: Buat proyek Firebase

1. Buka [Firebase Console](https://console.firebase.google.com) dan masuk dengan akun Google.
2. Klik **Add project**, beri nama (misalnya `CatatanBelajar`). Google Analytics boleh dimatikan.
3. Di halaman proyek, klik ikon **Android** untuk mendaftarkan aplikasi:
   - **Package name:** `com.example.catatanbelajar` (harus sama persis dengan `applicationId` di `app/build.gradle.kts`)
   - SHA-1 tidak diperlukan untuk login email dan password.
4. Unduh file **`google-services.json`** dan letakkan di folder **`app/`**, sejajar dengan `app/build.gradle.kts`:

   ```
   CatatanBelajar/
   └── app/
       ├── google-services.json   <- di sini
       └── build.gradle.kts
   ```

> `google-services.json` berisi konfigurasi proyek Firebase milikmu, dan tidak ikut di repositori ini. Setiap orang yang menjalankan proyek harus membuat file-nya sendiri.

### Langkah 3: Aktifkan Authentication

1. Di sidebar Firebase Console, buka kategori **Security**, lalu **Authentication**. Jika muncul tombol **Get started**, klik.
2. Buka tab **Sign-in method**.
3. Pilih **Email/Password**, aktifkan (**Enable**), lalu klik **Save**.

### Langkah 4: Buat database Firestore dan pasang Security Rules

1. Di sidebar, buka kategori **Databases & Storage**, klik **Firestore**, lalu **Create database**.
2. Pilih lokasi terdekat (misalnya `asia-southeast2` Jakarta) dan mode **Production**. Jika diminta memilih edisi, pilih **Standard**.
3. Buka tab **Rules**, ganti seluruh isinya dengan aturan berikut, lalu klik **Publish**:

```
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {

    // Profil: hanya pemilik akun yang boleh membaca dan menulis dokumennya sendiri
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }

    // Catatan: hanya pemilik (field uid) yang boleh mengakses
    match /notes/{noteId} {
      allow create: if request.auth != null
                    && request.resource.data.uid == request.auth.uid;

      allow read, delete: if request.auth != null
                          && resource.data.uid == request.auth.uid;

      allow update: if request.auth != null
                    && resource.data.uid == request.auth.uid
                    && request.resource.data.uid == resource.data.uid;
    }
  }
}
```

Koleksi `users` dan `notes` terbentuk otomatis saat data pertama disimpan, dan tidak perlu membuat index secara manual.

### Langkah 5: Jalankan aplikasi

1. Pilih perangkat atau emulator di toolbar Android Studio.
2. Klik **Run** (tombol hijau) atau tekan `Shift + F10`.

### Cara mencoba

1. Splash tampil, lalu masuk ke halaman Login.
2. Ketuk **Daftar** dan buat akun baru.
3. Di Home, ketuk **+**, isi catatan, tambahkan foto (kamera atau galeri), lalu **Simpan**.
4. Ketuk kartu catatan untuk melihat detail, lalu coba **Edit** dan **Hapus**. Geser kartu untuk menghapus dan coba tombol **Urungkan**.
5. Buka **Profil** (ikon di pojok kanan atas Home): ganti foto, ubah nama, lalu **Keluar**.
6. Daftar dengan akun kedua dan pastikan catatan akun pertama tidak terlihat.

Data bisa dilihat langsung di **Firebase Console > Databases & Storage > Firestore > tab Data**.

### Membuat APK (opsional)

**Build > Build Bundle(s) / APK(s) > Build APK(s)**. Hasilnya ada di `app/build/outputs/apk/debug/`.

### Pemecahan masalah

| Gejala | Solusi |
|---|---|
| Error `google-services.json is missing` | Pastikan file ada di folder `app/` (bukan di dalam `src/`) |
| Error `No matching client found for package name` | Package name di Firebase Console harus `com.example.catatanbelajar`. Daftarkan ulang aplikasi lalu unduh ulang `google-services.json` |
| Aplikasi crash saat membuka Login dengan pesan `...requires ... Theme.MaterialComponents` | Tema aplikasi harus turunan `Theme.Material3`. Pastikan `android:theme` di `AndroidManifest.xml` mengarah ke tema Material 3 dan hapus `res/values-night/themes.xml` jika ada |
| Toast "Akses ditolak. Periksa Firestore Security Rules." | Rules belum di-**Publish**, atau belum login |
| Toast "Email atau password salah" padahal benar | Metode **Email/Password** belum diaktifkan di Authentication |
| Kamera tidak terbuka | Izinkan Kamera di Pengaturan aplikasi. Di emulator, aktifkan kamera virtual pada pengaturan AVD |
| Statistik di Profil tetap 0 | Statistik butuh koneksi internet |
| `Unresolved reference` pada class `...Binding` | Pastikan `viewBinding = true` aktif, lalu **Build > Rebuild Project** |
