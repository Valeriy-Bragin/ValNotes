package com.meriniguan.notepad.screens.notes

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.meriniguan.notepad.screens.notes.adapters.NotesAdapter
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import com.meriniguan.notepad.R
import com.meriniguan.notepad.databinding.FragmentNotesBinding
import com.meriniguan.notepad.screens.addeditnote.EMPTY_NOTE_DISCARDED_REQUEST_KEY
import com.meriniguan.notepad.screens.addeditnote.NOTE_UPDATED_REQUEST_KEY
import com.meriniguan.notepad.screens.notes.adapters.FooterHeaderLoadStateAdapter
import com.meriniguan.notepad.screens.notes.adapters.NotesLoadStateAdapter
import com.meriniguan.notepad.utils.DrawerItemClickListener
import com.meriniguan.notepad.utils.OnFabItemClickedListener
import com.meriniguan.notepad.utils.TryAgainAction
import com.meriniguan.notepad.views.Fab
import com.meriniguan.notepad.utils.onQueryTextSubmitted
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotesFragment : Fragment(R.layout.fragment_notes), DrawerItemClickListener {

    private val viewModel: NotesViewModel by viewModels()

    private lateinit var binding: FragmentNotesBinding
    private lateinit var notesAdapter: NotesAdapter

    private lateinit var fab: Fab
    private lateinit var searchView: SearchView
    private lateinit var searchItem: MenuItem
    private lateinit var changeViewMenuItem: MenuItem

    private var tasksLoadStateHolder: NotesLoadStateAdapter.Holder? = null

    private val defaultNotesMenuProvider = object : MenuProvider {

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            createMenu(menu, menuInflater, R.menu.menu_fragment_notes)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return handleOnMenuItemSelected(menuItem)
        }
    }
    private val trashedNotesMenuProvider = object : MenuProvider {

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            createMenu(menu, menuInflater, R.menu.menu_fragment_trashed_notes)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return handleOnMenuItemSelected(menuItem)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentNotesBinding.bind(view)
        notesAdapter = NotesAdapter(viewModel)
        setUpMenu(defaultNotesMenuProvider)

        setupNotesList()

        initFab()

        setFragmentResultListeners()

        binding.cancelSearchImageView.setOnClickListener {
            viewModel.onCancelSearchClick()
        }

        observeEvents()
    }

    private fun setupNotesList() {
        val tryAgainAction: TryAgainAction = { notesAdapter.retry() }

        val footerAdapter = FooterHeaderLoadStateAdapter(tryAgainAction)
        val headerAdapter = FooterHeaderLoadStateAdapter(tryAgainAction)

        val adapterWithLoadState =
            notesAdapter.withLoadStateHeaderAndFooter(headerAdapter, footerAdapter)

        binding.notesRecyclerView.apply {
            setHasFixedSize(true)
            adapter = adapterWithLoadState
            layoutManager = LinearLayoutManager(requireContext())

            setupSwipingFunc(this)
        }

        tasksLoadStateHolder = NotesLoadStateAdapter.Holder(
            binding,
            tryAgainAction
        )

        observeNotes()
        observeLoadState()
    }

    private fun observeNotes() = viewLifecycleOwner.lifecycleScope.launch {
        viewModel.notes.collectLatest {
            notesAdapter.submitData(it)
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeLoadState() = viewLifecycleOwner.lifecycleScope.launch {
        notesAdapter.loadStateFlow.debounce(200).collectLatest { state ->
            if (tasksLoadStateHolder != null) {
                tasksLoadStateHolder!!.bind(state, notesAdapter.itemCount)
            }
        }
    }

    private fun observeEvents() = viewLifecycleOwner.lifecycleScope.launch {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                NotesViewModel.Event.CollapseSearchView -> searchItem.collapseActionView()
                NotesViewModel.Event.HideSearchFields -> binding.searchGroup.isGone = true
                is NotesViewModel.Event.ShowSearchFields -> {
                    binding.searchGroup.isVisible = true
                    binding.searchTextView.text = getString(event.messageRes, event.messageResArg)
                }
                is NotesViewModel.Event.SetChangeViewMenuItemTitle -> {
                    changeViewMenuItem.title = getString(event.titleRes)
                }
                is NotesViewModel.Event.ShowAddNoteScreen -> {
                    val action = NotesFragmentDirections
                        .actionNotesFragmentToAddEditNoteFragment(
                            addImageAtLaunch = event.addImageAtLaunch,
                            screenTitle = getString(R.string.add_note)
                        )
                    findNavController().navigate(action)
                }
                is NotesViewModel.Event.ShowEditNoteScreen -> {
                    val action = NotesFragmentDirections
                        .actionNotesFragmentToAddEditNoteFragment(
                            note = event.note,
                            screenTitle = getString(R.string.edit_note)
                        )
                    findNavController().navigate(action)
                }
                NotesViewModel.Event.InvalidateNotesList -> {
                    notesAdapter.refresh()
                }
                is NotesViewModel.Event.ShowUndoNoteArchivingMessage -> {
                    Snackbar.make(requireView(), event.messageRes, Snackbar.LENGTH_LONG)
                        .setAction(event.undoButtonTextRes) {
                            viewModel.onUndoNoteArchivingClick(event.note)
                        }
                        .show()
                }
                is NotesViewModel.Event.ShowUndoNoteTrashingMessage -> {
                    Snackbar.make(requireView(), event.messageRes, Snackbar.LENGTH_LONG)
                        .setAction(event.undoButtonTextRes) {
                            viewModel.onUndoNoteTrashingClick(event.note)
                        }
                        .show()
                }
                is NotesViewModel.Event.ShowMessage -> {
                    Snackbar.make(requireView(), event.messageRes, Snackbar.LENGTH_LONG)
                        .show()
                }
                NotesViewModel.Event.SetupDefaultNotesMenu -> {
                    (requireActivity() as MenuHost).removeMenuProvider(trashedNotesMenuProvider)
                    setUpMenu(defaultNotesMenuProvider)
                }
                NotesViewModel.Event.SetupTrashedNotesMenu -> {
                    (requireActivity() as MenuHost).removeMenuProvider(defaultNotesMenuProvider)
                    setUpMenu(trashedNotesMenuProvider)
                }
                NotesViewModel.Event.CloseFab -> {
                    closeFab()
                }
                is NotesViewModel.Event.SetScreenTitle -> {
                    (requireActivity() as AppCompatActivity)
                        .supportActionBar?.title = getString(event.titleRes)
                }
                NotesViewModel.Event.HideFab -> {
                    closeFab()
                    binding.fab.fab.visibility = View.GONE
                }
                NotesViewModel.Event.ShowFab -> {
                    binding.fab.fab.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun initFab() {
        fab = Fab(binding.fab.root, binding.notesRecyclerView, false)
        fab.setOnFabItemClickedListener(object : OnFabItemClickedListener {
            override fun onFabItemClick(id: Int) {
                when (id) {
                    R.id.fab_camera -> {
                        viewModel.onFabCameraClick()
                    }
                    R.id.fab_checklist -> {
                        viewModel.onFabCheckListClick()
                    }
                    R.id.fab_note -> {
                        viewModel.onFabNoteClick()
                    }
                }
            }
        })
    }

    private fun closeFab(): Boolean {
        if (fab.isExpanded) {
            fab.performToggle()
            return true
        }
        return false
    }

    override fun onNormalNotesSelected() {
        viewModel.onNormalNotesSelected()
    }

    override fun onArchivedNotesSelected() {
        viewModel.onArchivedNotesSelected()
    }

    override fun onTrashedNotesSelected() {
        viewModel.onTrashedNotesSelected()
    }

    private fun setupSwipingFunc(recyclerView: RecyclerView) {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val note = notesAdapter.peek(viewHolder.bindingAdapterPosition) ?: return
                //lastlySwipedItemViewHolder = viewHolder
                viewModel.onNoteSwiped(note)
            }
        }).attachToRecyclerView(recyclerView)
    }

    private fun setFragmentResultListeners() {

        setFragmentResultListener(NOTE_UPDATED_REQUEST_KEY) { _, bundle ->
            Log.i("!@#setfragmentresult", "notes fragment")
            viewModel.onNoteUpdatedResult(bundle)
        }
        setFragmentResultListener(EMPTY_NOTE_DISCARDED_REQUEST_KEY) { _, bundle ->
            viewModel.onEmptyNoteDiscardedResult(bundle)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {

            when (requestCode) {
                ImagePicker.REQUEST_CODE -> {
                    val uri: Uri = data?.data!!
                    viewModel.onPhotoSelected(uri)
                }
            }
        }
    }

    private fun setUpMenu(provider: MenuProvider) {
        (requireActivity() as MenuHost)
            .addMenuProvider(provider, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupSearchFunctionality(menu: Menu) {
        searchItem = menu.findItem(R.id.action_search)
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                viewModel.onSearchBackButtonClick()
                return true
            }

        })

        searchView = searchItem.actionView as SearchView

        restoreSearchViewState(searchItem)

        searchView.onQueryTextSubmitted {
            viewModel.onSearchQuerySubmitted(it)
        }
    }

    private fun createMenu(menu: Menu, menuInflater: MenuInflater, @MenuRes menuRes: Int) {
        menuInflater.inflate(menuRes, menu)

        setupSearchFunctionality(menu)

        changeViewMenuItem = menu.findItem(R.id.action_change_view)

        viewModel.onCreateMenu()
    }

    private fun handleOnMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_sort_by_title -> {
                viewModel.onSortTasksByTitleSelected()
                true
            }
            R.id.action_sort_by_date_created -> {
                viewModel.onSortTasksByDateCreatedSelected()
                true
            }
            R.id.action_sort_by_date_updated -> {
                viewModel.onSortTasksByDateUpdatedSelected()
                true
            }
            R.id.action_change_view -> {
                viewModel.onChangeViewClick()
                true
            }
            R.id.action_empty_trash -> {
                viewModel.emptyTrash()
                true
            }
            else -> false
        }
    }

    private fun restoreSearchViewState(searchItem: MenuItem) {
        val pendingQuery = viewModel.getSearchQuery()
        if (pendingQuery != null && pendingQuery.isNotEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(pendingQuery, false)
        }
    }
}