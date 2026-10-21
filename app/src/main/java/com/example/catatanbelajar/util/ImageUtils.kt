package com.example.catatanbelajar.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

/**
 * Foto disimpan sebagai teks Base64 langsung di dokumen Firestore, jadi tidak perlu
 * Firebase Storage (yang butuh paket Blaze). Foto dikecilkan dulu agar dokumen
 * jauh di bawah batas 1 MiB.
 */
object ImageUtils {

    /**
     * Membaca gambar dari [uri] (kamera atau galeri), memperbaiki rotasi EXIF,
     * memperkecil sisi terpanjang menjadi maksimal [maxSize] px, lalu mengubahnya
     * menjadi JPEG Base64. Mengembalikan null jika gagal.
     */
    fun uriToBase64(context: Context, uri: Uri, maxSize: Int, quality: Int = 70): String? {
        return try {
            val resolver = context.contentResolver

            // 1) Baca ukuran asli tanpa memuat bitmap, lalu tentukan inSampleSize
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxSize &&
                bounds.outHeight / (sample * 2) >= maxSize
            ) {
                sample *= 2
            }

            // 2) Decode bitmap yang sudah diperkecil kasar
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            // 3) Baca orientasi EXIF (foto kamera sering tersimpan miring)
            val rotation = resolver.openInputStream(uri)?.use {
                ExifInterface(it).rotationDegrees
            } ?: 0

            // 4) Skala halus + putar
            val scale = minOf(1f, maxSize.toFloat() / maxOf(decoded.width, decoded.height))
            val matrix = Matrix().apply {
                postScale(scale, scale)
                postRotate(rotation.toFloat())
            }
            val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

            // 5) JPEG -> Base64
            val out = ByteArrayOutputStream()
            result.compress(Bitmap.CompressFormat.JPEG, quality, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    fun base64ToBitmap(base64: String): Bitmap? = try {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}
