package com.meriniguan.notepad.screens.notes.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.meriniguan.notepad.databinding.FragmentNotesBinding
import com.meriniguan.notepad.utils.TryAgainAction

class NotesLoadStateAdapter(
    private val retry: TryAgainAction,
    private val adapterItemCount: Int
) : LoadStateAdapter<NotesLoadStateAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): Holder {
        val binding =
            FragmentNotesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding, retry)
    }

    override fun onBindViewHolder(holder: Holder, loadState: LoadState) {
        holder.bind(loadState, adapterItemCount)
    }

    class Holder(
        private val binding: FragmentNotesBinding,
        private val tryAgainAction: TryAgainAction
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.loadStateView.tryAgainButton.setOnClickListener {
                tryAgainAction()
            }
        }

        /**
         * This one is to use in TasksFragment.
         */
        fun bind(loadState: CombinedLoadStates, adapterItemCount: Int) {
            binding.apply {
                loadStateView.progressBar.isVisible = loadState.source.refresh is LoadState.Loading
                notesRecyclerView.isVisible = loadState.source.refresh is LoadState.NotLoading
                loadStateView.tryAgainButton.isVisible = loadState.source.refresh is LoadState.Error
                loadStateView.messageTextView.isVisible = loadState.source.refresh is LoadState.Error

                // no items view
                if (loadState.source.refresh is LoadState.NotLoading &&
                    loadState.append.endOfPaginationReached &&
                    adapterItemCount < 1) {
                    notesRecyclerView.isVisible = false
                    noItemsTextView.isVisible = true
                } else {
                    noItemsTextView.isVisible = false
                }
            }
        }

        /**
         * This one is to use in onBindViewHolder method.
         */
        fun bind(loadState: LoadState, adapterItemCount: Int) {
            binding.apply {
                loadStateView.progressBar.isVisible = loadState is LoadState.Loading
                notesRecyclerView.isVisible = loadState is LoadState.NotLoading
                loadStateView.tryAgainButton.isVisible = loadState is LoadState.Error
                loadStateView.messageTextView.isVisible = loadState is LoadState.Error

                // no items view
                if (loadState is LoadState.NotLoading &&
                    loadState.endOfPaginationReached &&
                    adapterItemCount < 1) {
                    notesRecyclerView.isVisible = false
                    noItemsTextView.isVisible = true
                } else {
                    noItemsTextView.isVisible = false
                }
            }
        }
    }
}