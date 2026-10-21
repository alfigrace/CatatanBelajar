package com.example.catatanbelajar.data

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/** Dokumen profil di koleksi `users`, id dokumen = uid FirebaseAuth. */
data class UserProfile(
    val name: String = "",
    val email: String = "",
    val photoBase64: String = ""
)

class UserRepository {

    private val users = FirebaseFirestore.getInstance().collection("users")

    /** Dipanggil sekali setelah register berhasil. */
    suspend fun createProfile(uid: String, name: String, email: String) {
        users.document(uid).set(
            mapOf(
                "name" to name,
                "email" to email,
                "photoBase64" to "",
                "createdAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun getProfile(uid: String): UserProfile? =
        users.document(uid).get().await().toObject(UserProfile::class.java)

    fun updateName(uid: String, name: String): Task<Void> =
        users.document(uid).set(mapOf("name" to name), SetOptions.merge())

    fun updatePhoto(uid: String, photoBase64: String): Task<Void> =
        users.document(uid).set(mapOf("photoBase64" to photoBase64), SetOptions.merge())
}
