package com.meriniguan.notepad.screens.addeditnote

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.github.dhaval2404.imagepicker.ImagePicker
import com.meriniguan.notepad.R
import com.meriniguan.notepad.databinding.FragmentAddEditNoteBinding
import com.meriniguan.notepad.screens.addeditnote.adapters.ImagesAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class AddEditNoteFragment : Fragment(R.layout.fragment_add_edit_note) {

    private val viewModel: AddEditNoteViewModel by viewModels()

    private lateinit var binding: FragmentAddEditNoteBinding
    private lateinit var adapter: ImagesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAddEditNoteBinding.bind(view)
        adapter = ImagesAdapter(requireContext(), viewModel)
        setUpMenu()

        fillFields()

        addTextChangedListeners()

        setupImagesList()

        observeEvents()
        observeOnBackPressed()
    }

    private fun fillFields() {
        binding.apply {
            titleEditText.setText(viewModel.title)
            contentEditText.setText(viewModel.content)
        }
    }

    private fun addTextChangedListeners() {
        binding.apply {
            titleEditText.addTextChangedListener {
                viewModel.title = it.toString()
            }
            contentEditText.addTextChangedListener {
                viewModel.content = it.toString()
            }
        }
    }

    private fun setupImagesList() {
        binding.apply {
            imagesRecyclerView.adapter = adapter
            imagesRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
            imagesRecyclerView.setHasFixedSize(true)
        }
    }

    private fun setUpMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_fragment_add_note, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    android.R.id.home -> {
                        viewModel.onSupportNavigateUp()
                        return true
                    }
                    R.id.action_attach_image -> {
                        viewModel.onAttachImageClick()
                    }
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun observeEvents() = viewLifecycleOwner.lifecycleScope.launchWhenStarted {
        viewModel.eventFlow.collect { event ->
            when (event) {
                AddEditNoteViewModel.Event.ShowSelectMethodOfTakingImageScreen -> {
                    val pictureDialog = AlertDialog.Builder(requireContext())
                    pictureDialog.setTitle(getString(R.string.select_action))
                    val pictureDialogItem = arrayOf(getString(R.string.select_photo_from_gallery),
                        getString(R.string.capture_photo_from_camera))
                    pictureDialog.setItems(pictureDialogItem) { dialog, which ->

                        when (which) {
                            0 -> viewModel.onPickImageFromGalleryClick()
                            1 -> viewModel.onCaptureImageWithCameraClick()
                        }
                    }

                    pictureDialog.show()
                }
                is AddEditNoteViewModel.Event.SetFragmentResult -> {
                    setFragmentResult(
                        event.requestKey,
                        bundleOf(event.resultKey to event.result)
                    )
                }
                AddEditNoteViewModel.Event.StartObservingImages -> {
                    observeImages()
                }
                AddEditNoteViewModel.Event.ShowPickImageWithCameraScreen -> {
                    captureImageWithCamera()
                }
                AddEditNoteViewModel.Event.ShowPickImageFromGalleryScreen -> {
                    pickImageFromGallery()
                }
                AddEditNoteViewModel.Event.NavigateBack -> {
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun captureImageWithCamera() {
        ImagePicker.with(this)
            .cameraOnly()
            .start()
    }

    private fun pickImageFromGallery() {
        ImagePicker.with(this)
            .galleryOnly()
            .start()
    }

    private fun observeImages() = viewLifecycleOwner.lifecycleScope.launchWhenStarted {
        viewModel.imagesFlow.collectLatest {
            viewModel.onImagesListCollected(it.size)
            adapter.images = it
        }
    }

    private fun observeOnBackPressed() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.onBackPressed()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {

            when (requestCode) {
                ImagePicker.REQUEST_CODE -> {
                    val uri: Uri = data?.data!!
                    viewModel.onImageSelected(uri)
                }
            }

        }
    }

    override fun onStop() {
        viewModel.onFragmentStop()
        super.onStop()
    }
}