package com.htduc.socialmediaapplication.Fragments

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.htduc.socialmediaapplication.Activities.AddPostActivity
import com.htduc.socialmediaapplication.Activities.MessageActivity
import com.htduc.socialmediaapplication.Adapters.PostAdapter
import com.htduc.socialmediaapplication.Adapters.StoryAdapter
import com.htduc.socialmediaapplication.Models.Post
import com.htduc.socialmediaapplication.Models.Story
import com.htduc.socialmediaapplication.Models.applyClickAnimation
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.ViewModel.FragmentViewModel
import com.htduc.socialmediaapplication.ViewmodelFactories.FragmentViewModelFactory
import com.htduc.socialmediaapplication.databinding.FragmentHomeBinding
import com.squareup.picasso.Picasso

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var fragmentViewModel: FragmentViewModel
    private lateinit var storyAdapter: StoryAdapter
    private lateinit var postAdapter: PostAdapter
    private var listStory = arrayListOf<Story>()
    private var listPost = arrayListOf<Post>()
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private var selectedImgStory: Uri? = null
    private var dialog: ProgressDialog? = null
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(layoutInflater)
        fragmentViewModel = ViewModelProvider(
            requireActivity(),
            FragmentViewModelFactory(requireActivity().application, requireContext())
        )[FragmentViewModel::class.java]

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
                Picasso.get()
                    .load(user.profilePhoto)
                    .placeholder(R.drawable.avt)
                    .into(binding.storyImg)
            }
        }
        fragmentViewModel.setProfileUser(auth.uid!!)

        storyAdapter = StoryAdapter(requireContext(), listStory)
        binding.storyRv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.storyRv.setHasFixedSize(true)
        binding.storyRv.setItemViewCacheSize(13)
        binding.storyRv.isNestedScrollingEnabled = false
        binding.storyRv.adapter = storyAdapter
        fragmentViewModel.listStory.observe(viewLifecycleOwner){ story->
            if (storyAdapter.getStoryList() != story) {
                storyAdapter.setStoryList(story)
            }
        }

        binding.postRv.showShimmerAdapter()
        postAdapter = PostAdapter(requireContext(), listPost)
        binding.postRv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.postRv.isNestedScrollingEnabled = false
        binding.postRv.adapter = postAdapter
        fragmentViewModel.listPost.observe(viewLifecycleOwner){ post->
            if (postAdapter.getPostList() != post) {
                binding.postRv.hideShimmerAdapter()
                postAdapter.setPostList(post)
            }
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri->
            selectedImgStory = uri
            if (selectedImgStory != null){
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

        applyClickAnimation(requireContext(), binding.pickImg){

        }

        binding.addPost.setOnClickListener {
            val intent = Intent(context, AddPostActivity::class.java)
            val activityOptions = ActivityOptionsCompat.makeCustomAnimation(
                requireContext(), R.anim.slide_up, R.anim.fade_out
            )
            context?.startActivity(intent, activityOptions.toBundle())
        }

        applyClickAnimation(requireContext(), binding.btnMsg){
            val intent = Intent(context, MessageActivity::class.java)
            context?.startActivity(intent)
        }
    }

}