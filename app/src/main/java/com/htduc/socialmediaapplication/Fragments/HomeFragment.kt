package com.htduc.socialmediaapplication.Fragments

import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.htduc.socialmediaapplication.Adapter.PostAdapter
import com.htduc.socialmediaapplication.Adapter.StoryAdapter
import com.htduc.socialmediaapplication.Model.Post
import com.htduc.socialmediaapplication.Model.Story
import com.htduc.socialmediaapplication.Model.applyClickAnimation
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.ViewModel.FragmentViewModel
import com.htduc.socialmediaapplication.ViewModel.FragmentViewModelFactory
import com.htduc.socialmediaapplication.databinding.FragmentHomeBinding
import com.squareup.picasso.Picasso

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var fragmentViewModel: FragmentViewModel
    private lateinit var storyAdapter: StoryAdapter
    private lateinit var postAdapter: PostAdapter
    private  var listStory = arrayListOf<Story>()
    private  var listPost = arrayListOf<Post>()
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private var selectedImgStory: Uri? = null
    private var dialog: ProgressDialog? = null
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentHomeBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        fragmentViewModel = ViewModelProviders.of(this, FragmentViewModelFactory(requireActivity().application))[FragmentViewModel::class.java]

        dialog = ProgressDialog(requireContext(), ProgressDialog.STYLE_SPINNER)
        dialog?.setTitle("Story Uploading")
        dialog?.setMessage("Please Wait...")
        dialog?.setCancelable(false)
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //set image of user
        fragmentViewModel.user.observe(viewLifecycleOwner){ user->
            if (user!=null){
                Picasso.get()
                    .load(user.profilePhoto)
                    .placeholder(R.drawable.avt)
                    .into(binding.profileImage)
            }
        }
        fragmentViewModel.setProfileUser(auth.uid!!)

        storyAdapter = StoryAdapter(requireContext(), listStory)
        binding.storyRv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, true)
        binding.storyRv.setHasFixedSize(true)
        binding.storyRv.setItemViewCacheSize(13)
        binding.storyRv.isNestedScrollingEnabled = false
        binding.storyRv.adapter = storyAdapter
        fragmentViewModel.listStory.observe(viewLifecycleOwner){ story->
            storyAdapter.setStoryList(story)
            binding.storyRv.scrollToPosition(storyAdapter.itemCount - 1)
        }

        binding.postRv.showShimmerAdapter()
        postAdapter = PostAdapter(requireContext(), listPost)
        binding.postRv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)
        binding.postRv.isNestedScrollingEnabled = false
        fragmentViewModel.listPost.observe(viewLifecycleOwner){ post->
            binding.postRv.adapter = postAdapter
            binding.postRv.hideShimmerAdapter()
            postAdapter.setPostList(post)
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri->
            selectedImgStory = uri
            if (selectedImgStory != null){
                binding.storyImg.setImageURI(uri)
                dialog?.show()
                fragmentViewModel.uploadStory(selectedImgStory!!) { isSuccess ->
                    if (isSuccess) {
                        dialog?.dismiss()
                    }
                }
            }
        }
        applyClickAnimation(requireContext(), binding.btnAddStory){
            galleryLauncher.launch("image/*")
        }
        binding.addStoryImg.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
    }

}