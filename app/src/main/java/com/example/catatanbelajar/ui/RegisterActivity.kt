package com.example.catatanbelajar.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.catatanbelajar.data.UserRepository
import com.example.catatanbelajar.databinding.ActivityRegisterBinding
import com.example.catatanbelajar.util.friendlyMessage
import com.example.catatanbelajar.util.isValidEmail
import com.example.catatanbelajar.util.toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Halaman 3: Register. Membuat akun di Firebase Auth lalu profil di Firestore (users/{uid}). */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth = FirebaseAuth.getInstance()
    private val userRepo = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.tvGoLogin.setOnClickListener { finish() }
        binding.btnRegister.setOnClickListener { attemptRegister() }
    }

    private fun attemptRegister() {
        val name = binding.etName.text?.toString().orEmpty().trim()
        val email = binding.etEmail.text?.toString().orEmpty().trim()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirm = binding.etConfirm.text?.toString().orEmpty()

        binding.tilName.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirm.error = null

        var valid = true
        if (name.isEmpty()) {
            binding.tilName.error = "Nama wajib diisi"
            valid = false
        }
        if (!email.isValidEmail()) {
            binding.tilEmail.error = "Format email tidak valid"
            valid = false
        }
        if (password.length < 6) {
            binding.tilPassword.error = "Password minimal 6 karakter"
            valid = false
        }
        if (confirm != password) {
            binding.tilConfirm.error = "Konfirmasi password tidak sama"
            valid = false
        }
        if (!valid) return

        setLoading(true)
        lifecycleScope.launch {
            try {
                // 1) Buat akun di Firebase Authentication
                val user = auth.createUserWithEmailAndPassword(email, password).await().user
                    ?: throw IllegalStateException("Gagal membuat akun.")

                // 2) Simpan nama tampilan di akun Auth
                user.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(name).build()
                ).await()

                // 3) Simpan profil di Firestore
                userRepo.createProfile(user.uid, name, email)

                toast("Akun berhasil dibuat. Selamat datang, $name!")
                startActivity(
                    Intent(this@RegisterActivity, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            } catch (e: Exception) {
                toast(e.friendlyMessage(), long = true)
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.isVisible = loading
        binding.btnRegister.isEnabled = !loading
    }
}
