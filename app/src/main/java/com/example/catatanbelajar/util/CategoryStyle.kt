package com.example.catatanbelajar.util

import android.content.res.ColorStateList
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.catatanbelajar.R

/** Memberi warna label kategori sesuai jenisnya. */
object CategoryStyle {

    fun apply(textView: TextView, category: String) {
        val (bg, fg) = when (category) {
            "Kuliah" -> R.color.cat_kuliah_bg to R.color.cat_kuliah_fg
            "Kode" -> R.color.cat_kode_bg to R.color.cat_kode_fg
            "Tugas" -> R.color.cat_tugas_bg to R.color.cat_tugas_fg
            else -> R.color.cat_lain_bg to R.color.cat_lain_fg
        }
        val context = textView.context
        textView.text = category
        textView.setTextColor(ContextCompat.getColor(context, fg))
        textView.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, bg))
    }
}
