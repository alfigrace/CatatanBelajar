package com.example.catatanbelajar.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.catatanbelajar.R
import com.example.catatanbelajar.data.NoteRepository
import com.example.catatanbelajar.data.UserRepository
import com.example.catatanbelajar.databinding.ActivityProfileBinding
import com.example.catatanbelajar.util.ImagePickerHelper
import com.example.catatanbelajar.util.ImageUtils
import com.example.catatanbelajar.util.awaitWithTimeout
import com.example.catatanbelajar.util.friendlyMessage
import com.example.catatanbelajar.util.showBase64
import com.example.catatanbelajar.util.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Halaman 6: Profil. Menampilkan data user, foto profil dari kamera atau galeri,
 * ubah nama, statistik catatan, dan logout.
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val userRepo = UserRepository()
    private val noteRepo = NoteRepository()

    // Harus dibuat sebagai properti agar terdaftar sebelum Activity STARTED
    private val picker = ImagePickerHelper(this) { uri -> savePhoto(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (auth.currentUser == null) {
            logout()
            return
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnChangePhoto.setOnClickListener { picker.show() }
        binding.rowChangePhoto.setOnClickListener { picker.show() }
        binding.rowEditName.setOnClickListener { showEditNameDialog() }
        binding.btnLogout.setOnClickListener { confirmLogout() }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) loadProfile()
    }

    private fun loadProfile() {
        val user = auth.currentUser ?: return
        binding.tvEmail.text = user.email
        binding.tvName.text = user.displayName ?: "Pengguna"

        lifecycleScope.launch {
            // Data profil dari Firestore (users/{uid})
            runCatching { userRepo.getProfile(user.uid) }.getOrNull()?.let { profile ->
                if (profile.name.isNotBlank()) binding.tvName.text = profile.name
                binding.ivAvatar.showBase64(profile.photoBase64, R.drawable.ic_avatar_placeholder)
            }
            // Statistik (butuh koneksi internet; jika gagal, biarkan 0)
            runCatching { noteRepo.count(user.uid) }.getOrNull()?.let {
                binding.tvCountNotes.text = it.toString()
            }
            runCatching { noteRepo.count(user.uid, "Tugas") }.getOrNull()?.let {
                binding.tvCountTasks.text = it.toString()
            }
        }
    }

    /** Foto profil: dikecilkan ke 512 px, lalu disimpan sebagai Base64 di users/{uid}. */
    private fun savePhoto(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        binding.progress.isVisible = true
        lifecycleScope.launch {
            try {
                val base64 = withContext(Dispatchers.Default) {
                    ImageUtils.uriToBase64(this@ProfileActivity, uri, maxSize = 512, quality = 75)
                }
                if (base64 == null) {
                    toast("Gagal membaca foto.")
                    return@launch
                }
                binding.ivAvatar.showBase64(base64)   // tampilkan langsung
                val synced = userRepo.updatePhoto(uid, base64).awaitWithTimeout()
                toast(if (synced) "Foto profil diperbarui" else "Disimpan di perangkat, akan disinkronkan saat online")
            } catch (e: Exception) {
                toast(e.friendlyMessage(), long = true)
                loadProfile()   // kembalikan ke foto sebelumnya
            } finally {
                binding.progress.isVisible = false
            }
        }
    }

    private fun showEditNameDialog() {
        val input = EditText(this).apply {
            setText(binding.tvName.text)
            setSelection(text.length)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            hint = "Nama lengkap"
        }
        val padding = (24 * resources.displayMetrics.density).toInt()
        val container = FrameLayout(this).apply {
            setPadding(padding, padding / 3, padding, 0)
            addView(input)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Ubah nama")
            .setView(container)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    toast("Nama tidak boleh kosong.")
                } else {
                    saveName(name)
                }
            }
            .show()
    }

    private fun saveName(name: String) {
        val user = auth.currentUser ?: return
        binding.progress.isVisible = true
        lifecycleScope.launch {
            try {
                user.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(name).build()
                ).await()
                userRepo.updateName(user.uid, name).awaitWithTimeout()
                binding.tvName.text = name
                toast("Nama diperbarui")
            } catch (e: Exception) {
                toast(e.friendlyMessage(), long = true)
            } finally {
                binding.progress.isVisible = false
            }
        }
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Keluar dari akun?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Keluar") { _, _ -> logout() }
            .show()
    }

    private fun logout() {
        auth.signOut()
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }
}
