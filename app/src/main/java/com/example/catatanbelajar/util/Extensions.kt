package com.example.catatanbelajar.util

import android.content.Context
import android.util.Patterns
import android.widget.ImageView
import android.widget.Toast
import com.example.catatanbelajar.util.ImageUtils
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LOCALE_ID: Locale = Locale.forLanguageTag("id-ID")

fun Context.toast(message: String, long: Boolean = false) {
    Toast.makeText(this, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

fun String.isValidEmail(): Boolean = Patterns.EMAIL_ADDRESS.matcher(this).matches()

/** Tanggal pendek untuk daftar, mis. "16 Okt 2026". Null = timestamp server belum terisi. */
fun Date?.formatShort(): String =
    this?.let { SimpleDateFormat("d MMM yyyy", LOCALE_ID).format(it) } ?: "Menyimpan..."

/** Tanggal lengkap untuk halaman detail, mis. "16 Oktober 2026, 19.42". */
fun Date?.formatLong(): String =
    this?.let { SimpleDateFormat("d MMMM yyyy, HH.mm", LOCALE_ID).format(it) } ?: "Menyimpan..."

/** Menampilkan gambar Base64 ke ImageView; jika kosong atau gagal, pakai [fallbackRes]. */
fun ImageView.showBase64(base64: String, fallbackRes: Int? = null) {
    val bitmap = if (base64.isBlank()) null else ImageUtils.base64ToBitmap(base64)
    when {
        bitmap != null -> setImageBitmap(bitmap)
        fallbackRes != null -> setImageResource(fallbackRes)
    }
}

/**
 * Menunggu Task selesai maksimal [ms] milidetik.
 *
 * @return true  = sudah dikonfirmasi server
 *         false = timeout (biasanya offline); Firestore tetap menyimpan di perangkat
 *                 dan mengirimnya otomatis saat koneksi kembali.
 * Jika Task gagal (misalnya ditolak Security Rules), exception dilempar seperti biasa.
 */
suspend fun <T> Task<T>.awaitWithTimeout(ms: Long = 8000L): Boolean =
    withTimeoutOrNull(ms) {
        await()
        true
    } ?: false

/** Mengubah exception Firebase menjadi pesan yang mudah dipahami pengguna. */
fun Throwable.friendlyMessage(): String = when (this) {
    is FirebaseNetworkException -> "Tidak ada koneksi internet."
    is FirebaseAuthUserCollisionException -> "Email sudah terdaftar. Silakan masuk."
    // WeakPassword adalah turunan InvalidCredentials, jadi harus diperiksa lebih dulu
    is FirebaseAuthWeakPasswordException -> "Password terlalu lemah (minimal 6 karakter)."
    is FirebaseAuthInvalidUserException -> "Akun tidak ditemukan."
    is FirebaseAuthInvalidCredentialsException -> "Email atau password salah."
    is FirebaseTooManyRequestsException -> "Terlalu banyak percobaan. Coba lagi nanti."
    is FirebaseFirestoreException ->
        if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED)
            "Akses ditolak. Periksa Firestore Security Rules."
        else
            "Terjadi kesalahan database (${code.name})."
    else -> message ?: "Terjadi kesalahan."
}
