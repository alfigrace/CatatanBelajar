package com.example.catatanbelajar.util

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.catatanbelajar.util.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

/**
 * Fitur hardware: memilih foto dari KAMERA atau GALERI.
 *
 * Cara pakai (harus dibuat sebagai properti Activity, bukan di dalam onCreate):
 *
 *     private val picker = ImagePickerHelper(this) { uri -> /* pakai uri */ }
 *     ...
 *     picker.show()
 *
 * - Kamera: butuh izin CAMERA (diminta saat runtime) dan FileProvider untuk menyimpan hasil foto.
 * - Galeri: memakai Photo Picker bawaan Android, tanpa izin apa pun.
 */
class ImagePickerHelper(
    private val activity: ComponentActivity,
    private val onPicked: (Uri) -> Unit
) {

    private var cameraUri: Uri? = null

    private val galleryLauncher = activity.registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(onPicked) }

    private val cameraLauncher = activity.registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) cameraUri?.let(onPicked) }

    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else activity.toast("Izin kamera ditolak. Kamu masih bisa memilih dari galeri.", long = true)
    }

    /** Menampilkan dialog pilihan Kamera / Galeri. */
    fun show() {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Pilih sumber foto")
            .setItems(arrayOf("Kamera", "Galeri")) { _, which ->
                if (which == 0) openCamera() else openGallery()
            }
            .show()
    }

    private fun openGallery() {
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun openCamera() {
        val granted = ContextCompat.checkSelfPermission(
            activity, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) launchCamera() else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        val dir = File(activity.cacheDir, "images").apply { mkdirs() }
        val file = File.createTempFile("IMG_", ".jpg", dir)
        val uri = FileProvider.getUriForFile(
            activity, "${activity.packageName}.fileprovider", file
        )
        cameraUri = uri
        cameraLauncher.launch(uri)
    }
}
