package com.example.catatanbelajar.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Satu dokumen di koleksi `notes`.
 *
 * Semua properti punya nilai default supaya Firestore bisa membuat objek
 * lewat konstruktor tanpa argumen saat memanggil toObject().
 */
data class Note(
    @DocumentId val id: String = "",
    val uid: String = "",                 // pemilik catatan (FirebaseAuth uid)
    val title: String = "",
    val content: String = "",
    val category: String = "Kuliah",
    val photoBase64: String = "",         // foto lampiran, kosong jika tidak ada
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val updatedAt: Date? = null
)
