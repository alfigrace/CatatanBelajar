package com.example.catatanbelajar.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.catatanbelajar.R
import com.example.catatanbelajar.data.Note
import com.example.catatanbelajar.data.NoteRepository
import com.example.catatanbelajar.databinding.ActivityNoteDetailBinding
import com.example.catatanbelajar.util.CategoryStyle
import com.example.catatanbelajar.util.ImagePickerHelper
import com.example.catatanbelajar.util.ImageUtils
import com.example.catatanbelajar.util.awaitWithTimeout
import com.example.catatanbelajar.util.formatLong
import com.example.catatanbelajar.util.friendlyMessage
import com.example.catatanbelajar.util.showBase64
import com.example.catatanbelajar.util.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Halaman 5: Detail catatan. Satu layar dengan dua mode:
 *  - mode lihat  : membaca catatan (READ), tombol Edit dan Hapus di toolbar
 *  - mode edit   : form tambah (CREATE) atau ubah (UPDATE) catatan, termasuk foto dari kamera/galeri
 *
 * Tanpa EXTRA_NOTE_ID layar langsung masuk mode tambah catatan baru.
 */
class NoteDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteDetailBinding
    private val repo = NoteRepository()

    private var noteId: String? = null
    private var current: Note? = null
    private var photoBase64 = ""

    private lateinit var categories: Array<String>

    // Harus dibuat sebagai properti agar terdaftar sebelum Activity STARTED
    private val picker = ImagePickerHelper(this) { uri -> handlePickedPhoto(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        noteId = intent.getStringExtra(EXTRA_NOTE_ID)
        categories = resources.getStringArray(R.array.note_categories)

        setupToolbar()
        setupForm()

        if (noteId == null) {
            // Mode tambah catatan baru
            binding.acCategory.setText(categories.first(), false)
            setEditing(true)
        } else {
            setEditing(false)
            loadNote(noteId!!)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.inflateMenu(R.menu.menu_note_detail)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    setEditing(true)
                    true
                }
                R.id.action_delete -> {
                    confirmDelete()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupForm() {
        binding.acCategory.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, categories)
        )
        binding.btnPhoto.setOnClickListener { picker.show() }
        binding.btnRemovePhoto.setOnClickListener {
            photoBase64 = ""
            refreshPhoto()
        }
        binding.btnSave.setOnClickListener { save() }
        binding.btnCancel.setOnClickListener {
            if (noteId == null) {
                finish()
            } else {
                // Batalkan perubahan: kembalikan foto asli dan kembali ke mode lihat
                photoBase64 = current?.photoBase64.orEmpty()
                refreshPhoto()
                setEditing(false)
            }
        }
    }

    /** READ: ambil satu catatan dari Firestore. */
    private fun loadNote(id: String) {
        binding.progress.isVisible = true
        lifecycleScope.launch {
            try {
                val note = repo.get(id)
                if (note == null) {
                    toast("Catatan tidak ditemukan.")
                    finish()
                    return@launch
                }
                current = note
                photoBase64 = note.photoBase64
                showNote(note)
            } catch (e: Exception) {
                toast(e.friendlyMessage(), long = true)
                finish()
            } finally {
                binding.progress.isVisible = false
            }
        }
    }

    private fun showNote(note: Note) {
        binding.tvTitle.text = note.title
        binding.tvContent.text = note.content
        binding.tvDate.text = note.createdAt.formatLong()
        CategoryStyle.apply(binding.tvCategory, note.category)
        refreshPhoto()
    }

    private fun setEditing(editing: Boolean) {
        val isNew = noteId == null

        binding.groupView.isVisible = !editing && !isNew
        binding.groupEdit.isVisible = editing

        val menu = binding.toolbar.menu
        menu.findItem(R.id.action_edit)?.isVisible = !editing && !isNew
        menu.findItem(R.id.action_delete)?.isVisible = !editing && !isNew

        binding.toolbar.title = when {
            isNew -> "Catatan baru"
            editing -> "Edit catatan"
            else -> "Detail catatan"
        }

        if (editing) {
            current?.let {
                binding.etTitle.setText(it.title)
                binding.etContent.setText(it.content)
                binding.acCategory.setText(it.category, false)
            }
        }
        refreshPhoto()
    }

    private fun refreshPhoto() {
        val hasPhoto = photoBase64.isNotBlank()
        binding.ivPhoto.isVisible = hasPhoto
        if (hasPhoto) binding.ivPhoto.showBase64(photoBase64)

        binding.btnRemovePhoto.isVisible = hasPhoto && binding.groupEdit.isVisible
        binding.btnPhoto.text = if (hasPhoto) "Ganti foto" else "Tambah foto (kamera atau galeri)"
    }

    /** Foto dari kamera/galeri dikecilkan di background thread, lalu disimpan sebagai Base64. */
    private fun handlePickedPhoto(uri: Uri) {
        binding.progress.isVisible = true
        lifecycleScope.launch {
            val base64 = withContext(Dispatchers.Default) {
                ImageUtils.uriToBase64(this@NoteDetailActivity, uri, maxSize = 800)
            }
            binding.progress.isVisible = false
            if (base64 == null) {
                toast("Gagal membaca foto.")
            } else {
                photoBase64 = base64
                refreshPhoto()
            }
        }
    }

    /** CREATE atau UPDATE, tergantung apakah noteId ada. */
    private fun save() {
        val title = binding.etTitle.text?.toString().orEmpty().trim()
        val content = binding.etContent.text?.toString().orEmpty().trim()
        val category = binding.acCategory.text?.toString().orEmpty().ifBlank { categories.first() }

        binding.tilTitle.error = null
        if (title.isEmpty()) {
            binding.tilTitle.error = "Judul wajib diisi"
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            toast("Sesi berakhir. Silakan masuk lagi.")
            return
        }

        setSaving(true)
        lifecycleScope.launch {
            try {
                val id = noteId
                val synced = if (id == null) {
                    repo.create(
                        Note(
                            uid = uid,
                            title = title,
                            content = content,
                            category = category,
                            photoBase64 = photoBase64
                        )
                    ).awaitWithTimeout()
                } else {
                    repo.update(id, title, content, category, photoBase64).awaitWithTimeout()
                }
                toast(
                    if (synced) "Catatan disimpan"
                    else "Disimpan di perangkat, akan disinkronkan saat online"
                )
                finish()
            } catch (e: Exception) {
                toast(e.friendlyMessage(), long = true)
                setSaving(false)
            }
        }
    }

    /** DELETE dengan dialog konfirmasi. */
    private fun confirmDelete() {
        val id = noteId ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus catatan?")
            .setMessage("Catatan ini akan dihapus permanen.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                repo.delete(id).addOnFailureListener { e ->
                    applicationContext.toast(e.friendlyMessage(), long = true)
                }
                toast("Catatan dihapus")
                finish()
            }
            .show()
    }

    private fun setSaving(saving: Boolean) {
        binding.progress.isVisible = saving
        binding.btnSave.isEnabled = !saving
        binding.btnCancel.isEnabled = !saving
    }

    companion object {
        private const val EXTRA_NOTE_ID = "note_id"

        /** [noteId] null = tambah catatan baru. */
        fun intent(context: Context, noteId: String?): Intent =
            Intent(context, NoteDetailActivity::class.java).apply {
                noteId?.let { putExtra(EXTRA_NOTE_ID, it) }
            }
    }
}
