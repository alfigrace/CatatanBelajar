package com.example.catatanbelajar.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catatanbelajar.R
import com.example.catatanbelajar.data.Note
import com.example.catatanbelajar.data.NoteRepository
import com.example.catatanbelajar.data.UserRepository
import com.example.catatanbelajar.databinding.ActivityMainBinding
import com.example.catatanbelajar.util.friendlyMessage
import com.example.catatanbelajar.util.showBase64
import com.example.catatanbelajar.util.toast
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

/**
 * Halaman 4: Home / Dashboard.
 * Menampilkan daftar catatan milik user secara real-time (READ), dengan pencarian,
 * filter kategori, tombol tambah (CREATE), dan geser untuk hapus (DELETE) + urungkan.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NoteAdapter

    private val noteRepo = NoteRepository()
    private val userRepo = UserRepository()

    private var uid: String = ""
    private var allNotes: List<Note> = emptyList()
    private var searchQuery = ""
    private var categoryFilter: String? = null   // null = semua kategori
    private var registration: ListenerRegistration? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            goToLogin()
            return
        }
        uid = user.uid

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvGreeting.text = "Halo, ${user.displayName ?: "Pengguna"}"

        setupList()
        setupSearchAndFilter()

        binding.fabAdd.setOnClickListener {
            startActivity(NoteDetailActivity.intent(this, null))
        }
        binding.ivAvatar.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        if (uid.isEmpty()) return
        startListening()
        loadProfile()
    }

    override fun onStop() {
        super.onStop()
        // Hentikan listener Firestore agar tidak boros baterai dan kuota
        registration?.remove()
        registration = null
    }

    private fun setupList() {
        adapter = NoteAdapter { note ->
            startActivity(NoteDetailActivity.intent(this, note.id))
        }
        binding.rvNotes.layoutManager = LinearLayoutManager(this)
        binding.rvNotes.adapter = adapter

        // Geser kartu ke kiri atau kanan untuk menghapus
        val swipe = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return
                deleteWithUndo(adapter.noteAt(position))
            }
        }
        ItemTouchHelper(swipe).attachToRecyclerView(binding.rvNotes)
    }

    private fun setupSearchAndFilter() {
        binding.etSearch.doAfterTextChanged {
            searchQuery = it?.toString().orEmpty().trim()
            applyFilter()
        }

        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            categoryFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_kuliah -> "Kuliah"
                R.id.chip_kode -> "Kode"
                R.id.chip_tugas -> "Tugas"
                else -> null
            }
            applyFilter()
        }
    }

    /** READ real-time: setiap ada perubahan di Firestore, daftar otomatis diperbarui. */
    private fun startListening() {
        binding.progress.isVisible = true
        registration = noteRepo.listen(
            uid,
            onChange = { notes ->
                allNotes = notes
                binding.progress.isVisible = false
                applyFilter()
            },
            onError = { e ->
                binding.progress.isVisible = false
                toast(e.friendlyMessage(), long = true)
            }
        )
    }

    private fun applyFilter() {
        val filtered = allNotes.filter { note ->
            val matchCategory = categoryFilter == null || note.category == categoryFilter
            val matchQuery = searchQuery.isEmpty() ||
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.content.contains(searchQuery, ignoreCase = true)
            matchCategory && matchQuery
        }
        adapter.submitList(filtered)

        binding.layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.tvEmpty.text = if (allNotes.isEmpty()) {
            "Belum ada catatan.\nKetuk + untuk mulai."
        } else {
            "Tidak ada catatan yang cocok."
        }
    }

    /** DELETE: hapus catatan, lalu tawarkan "Urungkan" (membuat ulang dokumen yang sama). */
    private fun deleteWithUndo(note: Note) {
        noteRepo.delete(note.id).addOnFailureListener { e ->
            applicationContext.toast(e.friendlyMessage(), long = true)
        }
        Snackbar.make(binding.root, "Catatan dihapus", Snackbar.LENGTH_LONG)
            .setAction("Urungkan") {
                // createdAt lama ikut dikirim sehingga urutan daftar tidak berubah
                noteRepo.create(note.copy(id = ""))
            }
            .show()
    }

    /** Memuat foto profil kecil di pojok kanan atas. */
    @SuppressLint("SetTextI18n")
    private fun loadProfile() {
        lifecycleScope.launch {
            val profile = runCatching { userRepo.getProfile(uid) }.getOrNull() ?: return@launch
            binding.ivAvatar.showBase64(profile.photoBase64, R.drawable.ic_avatar_placeholder)
            if (profile.name.isNotBlank()) binding.tvGreeting.text = "Halo, ${profile.name}"
        }
    }

    private fun goToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }
}
