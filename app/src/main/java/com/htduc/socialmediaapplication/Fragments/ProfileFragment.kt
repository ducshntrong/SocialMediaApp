package com.htduc.socialmediaapplication.Fragments

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.htduc.socialmediaapplication.Activity.LoginActivity
import com.htduc.socialmediaapplication.Activity.UpdateProfileActivity
import com.htduc.socialmediaapplication.Adapter.FollowAdapter
import com.htduc.socialmediaapplication.Adapter.PostAdapter
import com.htduc.socialmediaapplication.Model.Follow
import com.htduc.socialmediaapplication.Model.Post
import com.htduc.socialmediaapplication.Model.User
import com.htduc.socialmediaapplication.Model.applyClickAnimation
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.databinding.FragmentProfileBinding
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
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
        binding = FragmentProfileBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        dialog = ProgressDialog(requireContext())
        dialog?.setMessage("Updating Image...")
        dialog?.setCancelable(false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        followAdapter = FollowAdapter(requireContext(), listFollowers)
        binding.friendRv.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.friendRv.setHasFixedSize(true)
        binding.friendRv.adapter = followAdapter
        database.reference.child("Users").child(auth.uid!!).child("followers")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    listFollowers.clear()
                    if (snapshot.exists()){
                        for (dataSnap in snapshot.children){
                            val follow = dataSnap.getValue(Follow::class.java)
                            listFollowers.add(follow!!)
                        }
                        followAdapter.setFollowList(listFollowers)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })

        setProfileUser()
        showMyPost()
        //getCountImage()

        val pickCoverPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) {
            binding.coverPhoto.setImageURI(it)
            coverPhoto = it
            // Xử lý ảnh đã chọn ở đây
            if (coverPhoto != null) {
                dialog?.show()
                val reference = storage.reference.child("CoverPhoto").child(auth.uid!!)
                reference.putFile(coverPhoto!!)
                    .addOnSuccessListener { task ->
                        task.metadata!!.reference!!.downloadUrl
                            .addOnSuccessListener { url ->
                                val imageUrl = url.toString()
                                val uid = auth.uid
                                database.reference.child("Users").child(uid!!)
                                    .child("coverPhoto").setValue(imageUrl)
                                    .addOnCompleteListener {
                                        dialog?.dismiss()
                                    }
                                    .addOnFailureListener { err ->
                                        Toast.makeText(
                                            requireContext(),
                                            "Error ${err.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
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
                val reference = storage.reference.child("ProfilePhoto").child(auth.uid!!)
                reference.putFile(profilePhoto!!)
                    .addOnSuccessListener { task ->
                        task.metadata!!.reference!!.downloadUrl
                            .addOnSuccessListener { url ->
                                val imageUrl = url.toString()
                                val uid = auth.uid
                                database.reference.child("Users").child(uid!!)
                                    .child("profilePhoto").setValue(imageUrl)
                                    .addOnCompleteListener {
                                        dialog?.dismiss()
                                    }
                                    .addOnFailureListener { err ->
                                        Toast.makeText(
                                            requireContext(),
                                            "Error ${err.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
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

    private fun showMyPost() {
        postAdapter = PostAdapter(requireContext(), currentUserPostList)
        binding.postRv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)
        binding.postRv.isNestedScrollingEnabled = false
        binding.postRv.setHasFixedSize(true)
        binding.postRv.adapter = postAdapter
        val currentUserUid = auth.uid
        var totalLikes = 0
        var countPosts = 0
        database.reference.child("posts").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUserPostList.clear()
                if (snapshot.exists()) {
                    for (dataSnapshot in snapshot.children) {
                        val post = dataSnapshot.getValue(Post::class.java)
                        if (post?.postedBy == currentUserUid) {
                            //ktr nếu postedBy(id của ng post bài) trùng với id người dùng thì thêm vào ds
                            //nghĩa là chỉ show các post tương ứng với user
                            post!!.postId = dataSnapshot.key
                            totalLikes += post.postLike
                            currentUserPostList.add(post)
                            countPosts++
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

    private fun setProfileUser() {
        database.reference.child("Users").child(auth.uid!!)
            .addListenerForSingleValueEvent(object : ValueEventListener{
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
        setProfileUser()
    }
}