package com.example.catatanbelajar.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.catatanbelajar.data.Note
import com.example.catatanbelajar.databinding.ItemNoteBinding
import com.example.catatanbelajar.util.CategoryStyle
import com.example.catatanbelajar.util.formatShort

private val NOTE_DIFF = object : DiffUtil.ItemCallback<Note>() {
    override fun areItemsTheSame(oldItem: Note, newItem: Note) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Note, newItem: Note) = oldItem == newItem
}

/** Adapter daftar catatan di Home. DiffUtil membuat pembaruan daftar efisien. */
class NoteAdapter(
    private val onClick: (Note) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(NOTE_DIFF) {

    class NoteViewHolder(val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder =
        NoteViewHolder(ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        with(holder.binding) {
            tvTitle.text = note.title
            tvContent.text = note.content
            tvDate.text = note.createdAt.formatShort()
            CategoryStyle.apply(tvCategory, note.category)
            ivHasPhoto.isVisible = note.photoBase64.isNotBlank()
            root.setOnClickListener { onClick(note) }
        }
    }

    fun noteAt(position: Int): Note = getItem(position)
}
