package com.htduc.socialmediaapplication.Activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.htduc.socialmediaapplication.Adapter.FollowAdapter
import com.htduc.socialmediaapplication.Adapter.PostAdapter
import com.htduc.socialmediaapplication.Model.Follow
import com.htduc.socialmediaapplication.Model.Post
import com.htduc.socialmediaapplication.Model.User
import com.htduc.socialmediaapplication.Model.applyClickAnimation
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.ViewModel.FragmentViewModel
import com.htduc.socialmediaapplication.factory.FragmentViewModelFactory
import com.htduc.socialmediaapplication.ViewModel.ProfileViewModel
import com.htduc.socialmediaapplication.databinding.ActivityProfileBinding
import com.squareup.picasso.Picasso

class ProfileActivity : AppCompatActivity() {
    private var currentId: String? = null
    private lateinit var binding: ActivityProfileBinding
    private lateinit var followAdapter: FollowAdapter
    private lateinit var storage: FirebaseStorage
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var user: User
    private var listFollowers = arrayListOf<Follow>()
    private lateinit var postAdapter: PostAdapter
    private  var currentUserPostList  = arrayListOf<Post>()
    private lateinit var fragmentViewModel: FragmentViewModel
    private lateinit var profileViewModel: ProfileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        fragmentViewModel = ViewModelProvider(
            this,
            FragmentViewModelFactory(application, this)
        )[FragmentViewModel::class.java] 
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        currentId = auth.uid

        binding.imgBack.setOnClickListener { finish() }
        user = intent.extras?.getParcelable("user")!!

        // show list follow
        followAdapter = FollowAdapter(this, listFollowers)
        binding.friendRv.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.friendRv.setHasFixedSize(true)
        binding.friendRv.adapter = followAdapter
        profileViewModel.listFollow.observe(this){follow->
            followAdapter.setFollowList(follow)
        }
        profileViewModel.fetchFollowers(user.uid!!)

        applyClickAnimation(this, binding.imgChat){
            val intent = Intent(this, ChatActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable("user", user)
            intent.putExtras(bundle)
            startActivity(intent)
        }

        //xem tình trạng hoạt động
        profileViewModel.status.observe(this){status->
            if (status == "Offline"){
                binding.imgStatus.visibility = View.INVISIBLE
            }else{
                binding.imgStatus.visibility = View.VISIBLE
            }
        }
        profileViewModel.setPresenceStatus(user.uid!!)

        fragmentViewModel.user.observe(this){user->
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
        fragmentViewModel.setProfileUser(user.uid!!)

        //show post
        postAdapter = PostAdapter(this, currentUserPostList)
        binding.postRv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        binding.postRv.isNestedScrollingEnabled = false
        binding.postRv.setHasFixedSize(true)
        binding.postRv.adapter = postAdapter
        profileViewModel.currentUserPostList.observe(this){post->
            postAdapter.setPostList(post)
        }
        profileViewModel.totalLikes.observe(this) { totalLikes ->
            binding.countLike.text = totalLikes.toString()
        }

        profileViewModel.countPosts.observe(this) { countPosts ->
            binding.countPost.text = countPosts.toString()
        }
        profileViewModel.showUserPost(user.uid!!)
        //getCountImage()
    }

//    private fun getCountImage() {//lấy số lượng ảnh của tài khoản
//        val imageRef = storage.reference.child("posts").child(user.uid!!)
//        imageRef.listAll()
//            .addOnSuccessListener { listResult ->
//                val imageCount = listResult.items.size
//                binding.countPhoto.text = imageCount.toString()
//            }
//            .addOnFailureListener {
//                binding.countPhoto.text = "0"
//            }
//    }

    override fun onResume() {
        super.onResume()
        database.reference.child("presence").child(currentId!!).setValue("Online")
    }

    override fun onPause() {
        super.onPause()
        database.reference.child("presence").child(currentId!!).setValue("Offline")
    }
}