package com.example.catatanbelajar.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.catatanbelajar.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Halaman 1: Splash. Memutar animasi logo, lalu memeriksa status login:
 * sudah login -> Home, belum -> Login.
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Jalankan animasi logo (garis tergambar, lalu kursor berkedip)
        val logo = findViewById<ImageView>(R.id.imgLogo)
        (logo.drawable as? Animatable)?.start()

        lifecycleScope.launch {
            delay(2200)
            val target = if (FirebaseAuth.getInstance().currentUser != null) {
                MainActivity::class.java
            } else {
                LoginActivity::class.java
            }
            startActivity(Intent(this@SplashActivity, target))
            finish()
        }
    }
}
