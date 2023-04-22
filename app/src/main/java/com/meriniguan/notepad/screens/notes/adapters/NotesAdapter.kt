package com.meriniguan.notepad.screens.notes.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.meriniguan.notepad.R
import com.meriniguan.notepad.databinding.ItemNoteBinding
import com.meriniguan.notepad.databinding.ItemReducedNoteBinding
import com.meriniguan.notepad.model.note.entities.NoteListItem
import com.meriniguan.notepad.utils.NoteInfo

private const val NOTE_VIEW_TYPE_REDUCED = 0
private const val NOTE_VIEW_TYPE_EXPANDED = 1

class NotesAdapter(
    private val listener: OnItemClickListener
) : PagingDataAdapter<NoteListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    interface OnItemClickListener {
        fun onItemClick(noteListItem: NoteListItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            NOTE_VIEW_TYPE_EXPANDED -> ExpandedNoteViewHolder.from(parent, this)
            NOTE_VIEW_TYPE_REDUCED -> ReducedNoteViewHolder.from(parent, this)
            else -> throw ClassCastException("Unknown note's viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = getItem(position) ?: return
        when (holder) {
            is ExpandedNoteViewHolder -> holder.bind(currentItem)
            is ReducedNoteViewHolder -> holder.bind(currentItem)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position)?.isReducedView == true)
            NOTE_VIEW_TYPE_REDUCED
        else NOTE_VIEW_TYPE_EXPANDED
    }

    class ExpandedNoteViewHolder(private val binding: ItemNoteBinding, private val adapter: NotesAdapter) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val noteListItem = getCurrentNoteListItem() ?: return@setOnClickListener
                adapter.listener.onItemClick(noteListItem)
            }
        }

        fun bind(noteListItem: NoteListItem) {
            binding.apply {
                titleTextView.text = noteListItem.note.title
                textTextView.text = noteListItem.note.text
                if (noteListItem.noteInfo == NoteInfo.DATE_CREATED) {
                    infoTextView.text = root.context.getString(R.string.created, noteListItem.note.dateCreatedFormatted)
                } else if (noteListItem.noteInfo == NoteInfo.DATE_UPDATED) {
                    infoTextView.text = root.context.getString(R.string.updated, noteListItem.note.dateUpdatedFormatted)
                }
                if (noteListItem.hasImage()) {
                    imageImageView.visibility = View.VISIBLE
                    Glide.with(root.context)
                        .load(noteListItem.note.firstImageUri)
                        .into(imageImageView)
                } else {
                    imageImageView.visibility = View.GONE
                    imageImageView.setImageResource(0)
                }
            }
        }

        private fun getCurrentNoteListItem(): NoteListItem? {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                return adapter.getItem(position)
            }
            return null
        }

        private fun NoteListItem.hasImage(): Boolean =
            this.note.firstImageUri != "empty" && this.note.firstImageUri.isNotBlank()

        companion object {
            fun from(parent: ViewGroup, adapter: NotesAdapter): ExpandedNoteViewHolder {
                val binding = ItemNoteBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
                return ExpandedNoteViewHolder(binding, adapter)
            }
        }
    }

    class ReducedNoteViewHolder(private val binding: ItemReducedNoteBinding, private val adapter: NotesAdapter) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val task = getCurrentTask() ?: return@setOnClickListener
                adapter.listener.onItemClick(task)
            }
        }

        fun bind(noteListItem: NoteListItem) {
            binding.apply {
                titleTextView.text = noteListItem.note.title
                textTextView.text = noteListItem.note.text
                if (noteListItem.noteInfo == NoteInfo.DATE_CREATED) {
                    infoTextView.text = root.context.getString(R.string.created, noteListItem.note.dateCreatedFormatted)
                } else if (noteListItem.noteInfo == NoteInfo.DATE_UPDATED) {
                    infoTextView.text = root.context.getString(R.string.updated, noteListItem.note.dateUpdatedFormatted)
                }
                if (noteListItem.hasImage()) {
                    attachmentImageView.visibility = View.VISIBLE
                } else {
                    attachmentImageView.visibility = View.GONE
                }
            }
        }

        private fun getCurrentTask(): NoteListItem? {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                return adapter.getItem(position)
            }
            return null
        }

        private fun NoteListItem.hasImage(): Boolean =
            this.note.firstImageUri != "empty" && this.note.firstImageUri.isNotBlank()

        companion object {
            fun from(parent: ViewGroup, adapter: NotesAdapter): ReducedNoteViewHolder {
                val binding = ItemReducedNoteBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
                return ReducedNoteViewHolder(binding, adapter)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<NoteListItem>() {
        override fun areItemsTheSame(oldItem: NoteListItem, newItem: NoteListItem) =
            oldItem.note.id == newItem.note.id

        override fun areContentsTheSame(oldItem: NoteListItem, newItem: NoteListItem) =
            oldItem == newItem
    }
}