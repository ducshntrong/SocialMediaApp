package com.htduc.socialmediaapplication.Activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.htduc.socialmediaapplication.Adapter.FollowAdapter
import com.htduc.socialmediaapplication.Adapter.PostAdapter
import com.htduc.socialmediaapplication.Model.Follow
import com.htduc.socialmediaapplication.Model.Post
import com.htduc.socialmediaapplication.Model.User
import com.htduc.socialmediaapplication.Model.applyClickAnimation
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.databinding.ActivityProfileBinding
import com.squareup.picasso.Picasso
import java.util.Date

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
    private var postedBy:String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        currentId = auth.uid

        binding.imgBack.setOnClickListener { finish() }
        user = intent.extras?.getParcelable("user")!!

        followAdapter = FollowAdapter(this, listFollowers)
        binding.friendRv.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.friendRv.setHasFixedSize(true)
        binding.friendRv.adapter = followAdapter
        getListFollowers()

        applyClickAnimation(this, binding.imgChat){
            val intent = Intent(this, ChatActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable("user", user)
            intent.putExtras(bundle)
            startActivity(intent)
        }

        setPresenceStatus()
        setDataProfile()
        showPostUser()
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

    private fun showPostUser() {
        postAdapter = PostAdapter(this, currentUserPostList)
        binding.postRv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        binding.postRv.isNestedScrollingEnabled = false
        binding.postRv.setHasFixedSize(true)
        binding.postRv.adapter = postAdapter
        val currentUserUid = user.uid
        var totalLikes = 0
        var countPosts = 0
        database.reference.child("posts").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUserPostList.clear()
                if (snapshot.exists()) {
                    for (dataSnapshot in snapshot.children) {
                        val post = dataSnapshot.getValue(Post::class.java)
                        if (post != null){
                            if (post.postedBy == currentUserUid) {
                                post.postId = dataSnapshot.key //lay id cua post
                                totalLikes += post.postLike
                                currentUserPostList.add(post)
                                countPosts++
                            }
                        }
                    }
                    postAdapter.setPostList(currentUserPostList)
                    binding.countLike.text = totalLikes.toString()
                    binding.countPost.text = countPosts.toString()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Xử lý khi có lỗi
            }
        })
    }

    private fun setPresenceStatus() {
        //cập nhật trình trạng off và on
        database.reference.child("presence").child(user.uid!!)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        val status = snapshot.getValue(String::class.java)
                        if (status != null){
                            if (status == "Offline"){
                                binding.imgStatus.visibility = View.INVISIBLE
                            }else{
                                binding.imgStatus.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    private fun getListFollowers() {
        database.reference.child("Users").child(user.uid!!).child("followers")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    listFollowers.clear()
                    if (snapshot.exists()){
                        for (dataSnap in snapshot.children){
                            val follow = dataSnap.getValue(Follow::class.java)
                            if (follow != null) {
                                listFollowers.add(follow)
                            }
                        }
                        followAdapter.setFollowList(listFollowers)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    private fun setDataProfile() {
        database.reference.child("Users").child(user.uid!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        val user = snapshot.getValue(User::class.java)
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
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }
    override fun onResume() {
        super.onResume()
        database.reference.child("presence").child(currentId!!).setValue("Online")
    }

    override fun onPause() {
        super.onPause()
        database.reference.child("presence").child(currentId!!).setValue("Offline")
    }
}