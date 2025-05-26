package com.htduc.socialmediaapplication.Fragments

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.htduc.socialmediaapplication.Activities.LoginActivity
import com.htduc.socialmediaapplication.Activities.UpdateProfileActivity
import com.htduc.socialmediaapplication.Adapters.FollowAdapter
import com.htduc.socialmediaapplication.Adapters.PostAdapter
import com.htduc.socialmediaapplication.Models.Follow
import com.htduc.socialmediaapplication.Models.Post
import com.htduc.socialmediaapplication.Models.applyClickAnimation
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.ViewModel.FragmentViewModel
import com.htduc.socialmediaapplication.ViewmodelFactories.FragmentViewModelFactory
import com.htduc.socialmediaapplication.ViewModel.ProfileViewModel
import com.htduc.socialmediaapplication.databinding.FragmentProfileBinding
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private lateinit var fragmentViewModel: FragmentViewModel
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var followAdapter: FollowAdapter
    private var listFollowers = arrayListOf<Follow>()
    private lateinit var storage: FirebaseStorage
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var coverPhoto: Uri? = null
    private var profilePhoto: Uri? = null
    private var dialog: ProgressDialog? = null
    private lateinit var postAdapter: PostAdapter
    private  var currentUserPostList  = arrayListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentProfileBinding.inflate(layoutInflater)
        dialog = ProgressDialog(requireContext())
        dialog?.setMessage("Updating Image...")
        dialog?.setCancelable(false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        fragmentViewModel = ViewModelProvider(
            this,
            FragmentViewModelFactory(requireActivity().application, requireContext())
        )[FragmentViewModel::class.java]
        profileViewModel = ViewModelProvider(requireActivity())[ProfileViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //show followers
        followAdapter = FollowAdapter(requireContext(), listFollowers)
        binding.friendRv.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.friendRv.setHasFixedSize(true)
        binding.friendRv.adapter = followAdapter
        profileViewModel.listFollow.observe(viewLifecycleOwner){follow->
            followAdapter.setFollowList(follow)
        }
        profileViewModel.fetchFollowers(auth.uid!!)

        fragmentViewModel.user.observe(viewLifecycleOwner){user->
            if (user != null){
                Picasso.get()
                    .load(user.profilePhoto)
                    .placeholder(R.drawable.avt)
                    .into(binding.profileImage)
                Picasso.get()
                    .load(user.coverPhoto)
                    .placeholder(R.drawable.placeholder)
                    .into(binding.coverPhoto)
                binding.userName.text = user.name
                binding.nickname.text = user.profession
                binding.followers.text = user.followerCount.toString()
            }
        }
        fragmentViewModel.setProfileUser(auth.uid!!)

        //show post
        postAdapter = PostAdapter(requireContext(), currentUserPostList)
        binding.postRv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)
        binding.postRv.isNestedScrollingEnabled = false
        binding.postRv.setHasFixedSize(true)
        binding.postRv.adapter = postAdapter
        profileViewModel.currentUserPostList.observe(viewLifecycleOwner){post->
            postAdapter.setPostList(post)
        }
        profileViewModel.totalLikes.observe(viewLifecycleOwner) { totalLikes ->
            binding.countLike.text = totalLikes.toString()
        }

        profileViewModel.countPosts.observe(viewLifecycleOwner) { countPosts ->
            binding.countPost.text = countPosts.toString()
        }
        profileViewModel.showUserPost(auth.uid!!)
        //getCountImage()

        val pickCoverPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) {
            binding.coverPhoto.setImageURI(it)
            coverPhoto = it
            // Xử lý ảnh đã chọn ở đây
            if (coverPhoto != null) {
                dialog?.show()
                profileViewModel.uploadCoverPhoto(coverPhoto!!){isSuccess->
                    if (isSuccess){
                        dialog?.dismiss()
                    }
                }
            }
        }
        applyClickAnimation(requireContext(), binding.pickCoverPhoto) {
            pickCoverPhoto.launch("image/*")
        }
        val pickProfilePhoto = registerForActivityResult(ActivityResultContracts.GetContent()) {
            binding.profileImage.setImageURI(it)
            profilePhoto = it
            // Xử lý ảnh đã chọn ở đây
            if (profilePhoto != null) {
                dialog?.show()
                profileViewModel.uploadProfilePhoto(profilePhoto!!){isSuccess->
                    if (isSuccess){
                        dialog?.dismiss()
                    }
                }
            }
        }
        applyClickAnimation(requireContext(), binding.imgCamera){
            pickProfilePhoto.launch("image/*")
        }

        applyClickAnimation(requireContext(), binding.imgLogout){
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            activity?.finish();
        }

        applyClickAnimation(requireContext(),binding.imgAddFr) {
            val intent = Intent(requireContext(), UpdateProfileActivity::class.java)
            startActivity(intent)
        }

    }

//    private fun getCountImage() { //lấy số lượng ảnh của tài khoản
//        val imageRef = storage.reference.child("posts").child(auth.uid!!)
//        imageRef.listAll()
//            .addOnSuccessListener { listResult ->
//                val imageCount = listResult.items.size
//                binding.countPhoto.text = imageCount.toString()
//            }
//            .addOnFailureListener {
//                binding.countPhoto.text = "0"
//            }
//    }


}