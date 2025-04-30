package com.htduc.socialmediaapplication.Fragments

import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.htduc.socialmediaapplication.Adapter.StoryAdapter
import com.htduc.socialmediaapplication.Adapter.StoryAdapter2
import com.htduc.socialmediaapplication.Model.Story
import com.htduc.socialmediaapplication.Model.applyClickAnimation
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.ViewModel.FragmentViewModel
import com.htduc.socialmediaapplication.databinding.FragmentStoryBinding
import com.htduc.socialmediaapplication.factory.FragmentViewModelFactory

class StoryFragment : Fragment() {
    private lateinit var binding: FragmentStoryBinding
    private lateinit var fragmentViewModel: FragmentViewModel
    private lateinit var storyAdapter: StoryAdapter2
    private var listStory = arrayListOf<Story>()
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private var selectedImg: Uri? = null
    private var dialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentStoryBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        dialog = ProgressDialog(requireContext(), ProgressDialog.STYLE_SPINNER)
        dialog?.setTitle("Story Uploading")
        dialog?.setMessage("Please Wait...")
        dialog?.setCancelable(false)
        fragmentViewModel = ViewModelProvider(
            this, FragmentViewModelFactory(requireActivity().application, requireContext())
        )[FragmentViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        storyAdapter = StoryAdapter2(requireContext(), listStory)
        binding.rvStory.layoutManager = GridLayoutManager(requireContext(),
            2, GridLayoutManager.VERTICAL, false)
        binding.rvStory.setHasFixedSize(true)
        binding.rvStory.adapter = storyAdapter
        fragmentViewModel.listStory.observe(viewLifecycleOwner){
            storyAdapter.setStoryList(it)
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()){ uri ->
            selectedImg = uri
            if (selectedImg != null){
                dialog?.show()
                fragmentViewModel.uploadStory(selectedImg!!){ isSuccess ->
                    if (isSuccess){
                        dialog?.dismiss()
                    }
                }
            }
        }

        applyClickAnimation(requireContext(), binding.addStory){
            galleryLauncher.launch("image/*")
        }
    }
}