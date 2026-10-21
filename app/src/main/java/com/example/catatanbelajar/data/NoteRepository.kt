package com.example.catatanbelajar.data

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Semua operasi CRUD catatan ke Cloud Firestore (koleksi "notes").
 *
 * Create  -> create()
 * Read    -> listen() (real-time daftar) dan get() (satu dokumen)
 * Update  -> update()
 * Delete  -> delete()
 */
class NoteRepository {

    private val notes = FirebaseFirestore.getInstance().collection("notes")

    /**
     * READ (real-time): dipanggil setiap data berubah.
     * Hanya mengambil catatan milik [uid]. Pengurutan dilakukan di sisi klien
     * supaya tidak perlu membuat composite index di Firestore.
     */
    fun listen(
        uid: String,
        onChange: (List<Note>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration =
        notes.whereEqualTo("uid", uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(Note::class.java).orEmpty()
                // createdAt masih null = baru ditulis dan belum dikonfirmasi server -> tampil paling atas
                .sortedByDescending { it.createdAt?.time ?: Long.MAX_VALUE }
            onChange(list)
        }

    /** READ (sekali): ambil satu catatan berdasarkan id dokumen. */
    suspend fun get(id: String): Note? =
        notes.document(id).get().await().toObject(Note::class.java)

    /** CREATE: tambah catatan baru. createdAt dan updatedAt diisi server. */
    fun create(note: Note): Task<DocumentReference> = notes.add(note)

    /** UPDATE: ubah isi catatan. createdAt tidak disentuh. */
    fun update(
        id: String,
        title: String,
        content: String,
        category: String,
        photoBase64: String
    ): Task<Void> = notes.document(id).update(
        mapOf(
            "title" to title,
            "content" to content,
            "category" to category,
            "photoBase64" to photoBase64,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    )

    /** DELETE: hapus catatan. */
    fun delete(id: String): Task<Void> = notes.document(id).delete()

    /** Menghitung jumlah catatan milik [uid], opsional per kategori (butuh koneksi internet). */
    suspend fun count(uid: String, category: String? = null): Long {
        var query: Query = notes.whereEqualTo("uid", uid)
        if (category != null) query = query.whereEqualTo("category", category)
        return query.count().get(AggregateSource.SERVER).await().count
    }
}
