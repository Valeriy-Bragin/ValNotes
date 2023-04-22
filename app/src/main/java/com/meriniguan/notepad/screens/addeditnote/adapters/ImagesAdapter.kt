package com.meriniguan.notepad.screens.addeditnote.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.meriniguan.notepad.databinding.ItemImageBinding
import com.meriniguan.notepad.model.image.entities.Image

class ImagesAdapter(
    val context: Context,
    val listener: OnImageClickListener
) : RecyclerView.Adapter<ImagesAdapter.ImagesViewHolder>() {

    interface OnImageClickListener {
        fun onImageLongClick(imageId: Long)
    }

    var images = emptyList<Image>()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImagesAdapter.ImagesViewHolder {
        val binding = ItemImageBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ImagesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImagesAdapter.ImagesViewHolder, position: Int) {
        val image = images[position]
        holder.bind(image)
    }

    override fun getItemCount(): Int {
        return images.size
    }

    inner class ImagesViewHolder(val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val image = getCurrentImageURi() ?: return@setOnClickListener
                listener.onImageLongClick(image.id)
            }
        }

        fun bind(image: Image) {
            binding.apply {
                Glide.with(root.context)
                    .load(image.uri)
                    .into(imageImageView)
            }
        }

        private fun getCurrentImageURi(): Image? {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                return images[position]
            }
            return null
        }
    }
}