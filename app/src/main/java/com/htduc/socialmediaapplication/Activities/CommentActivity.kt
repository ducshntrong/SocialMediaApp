package com.htduc.socialmediaapplication.Activities

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.htduc.socialmediaapplication.Adapters.CommentAdapter
import com.htduc.socialmediaapplication.Models.Comment
import com.htduc.socialmediaapplication.Models.User
import com.htduc.socialmediaapplication.Models.applyClickAnimation
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.moderation.UserModerationManager
import com.htduc.socialmediaapplication.ViewModel.CommentViewModel
import com.htduc.socialmediaapplication.ViewmodelFactories.CommentViewmodelFactory
import com.htduc.socialmediaapplication.ViewModel.MainViewModel
import com.htduc.socialmediaapplication.ViewmodelFactories.MainViewModelFactory
import com.htduc.socialmediaapplication.databinding.ActivityCommentBinding
import com.squareup.picasso.Picasso

class CommentActivity : AppCompatActivity() {
    private lateinit var binding : ActivityCommentBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private var selectedImgCmt: Uri? = null
    private var dialog: ProgressDialog? = null
    private var postId:String? = null
    private var postedBy:String? = null
    private lateinit var commentAdapter: CommentAdapter
    private var listComment = arrayListOf<Comment>()
    private lateinit var fragmentViewModel: MainViewModel
    private lateinit var commentViewModel: CommentViewModel
    private lateinit var userModerationManager: UserModerationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar2)
        this.title = "Comments"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)// hiển thị nút back

        dialog = ProgressDialog(this, ProgressDialog.STYLE_SPINNER)
        dialog?.setTitle("Comment Uploading")
        dialog?.setMessage("Please Wait...")
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        fragmentViewModel = ViewModelProvider(
            this,
            MainViewModelFactory(application, this)
        )[MainViewModel::class.java]
        //commentViewModel = ViewModelProvider(this)[CommentViewModel::class.java]
        commentViewModel = ViewModelProvider(this, CommentViewmodelFactory(this))[CommentViewModel::class.java]

        userModerationManager = UserModerationManager(database, this)

        postId = intent.getStringExtra("postId")
        postedBy = intent.getStringExtra("postedBy")

        //get data of user
        fragmentViewModel.user.observe(this){user->
            if (user != null){
                Picasso.get()
                    .load(user.profilePhoto)
                    .placeholder(R.drawable.avt)
                    .into(binding.profileImage);
                binding.username.text = user.name
            }
        }
        fragmentViewModel.setProfileUser(postedBy!!)
        setDataToView()

        //show list comment
        commentAdapter = CommentAdapter(this)
        binding.rvComment.layoutManager = LinearLayoutManager(this)
        binding.rvComment.setHasFixedSize(true)
        binding.rvComment.adapter = commentAdapter
        commentViewModel.listComment.observe(this){comment->
            commentAdapter.setCommentList(comment)
        }
        commentViewModel.showListComment(postId!!)

        // Lấy thông tin người dùng từ Firebase
        val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(auth.uid!!)
        userRef.get().addOnSuccessListener { snapshot ->
            val currentUser = snapshot.getValue(User::class.java)
            if (currentUser != null ){
                if (userModerationManager.canSendMessage(currentUser)){
                    binding.linear02.visibility = View.VISIBLE
                    binding.alert.visibility = View.GONE
                } else{
                    binding.linear02.visibility = View.GONE
                    binding.alert.visibility = View.VISIBLE
                }
            }
        }

        applyClickAnimation(this, binding.btnSend){
            val edtMessage = binding.edtMessage.text.toString().trim()
            if (edtMessage.isNotEmpty() || selectedImgCmt != null){
                dialog?.show()
                commentViewModel.sendComment(selectedImgCmt, edtMessage, postId!!, postedBy!!){
                    if (it){
                        dialog?.dismiss()
                        binding.cmtImg.visibility = View.GONE
                        binding.edtMessage.setText("")
                    }else{
                        dialog?.dismiss()
                    }
                }
            }else{
                Toast.makeText(this, "Please enter something", Toast.LENGTH_SHORT).show()
            }
        }

        val imagePickCallback = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            selectedImgCmt = uri
            if (selectedImgCmt != null) {
                binding.cmtImg.setImageURI(selectedImgCmt)
                binding.cmtImg.visibility = View.VISIBLE
            }
        }
        applyClickAnimation(this, binding.attachment){
            imagePickCallback.launch("image/*")
        }

        binding.imgPost.setOnClickListener {
            //lấy dữ liệu post hiện tại từ LiveData
            commentViewModel.post.value?.let { post ->
                val intent = Intent(this, ImageDetailActivity::class.java)
                intent.putExtra("post", post)

                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this,
                    binding.imgPost,      // view dùng để chuyển tiếp
                    "postImageTransition"  // tên transitionName phải giống ở cả 2 Activity
                )
                startActivity(intent, options.toBundle())

            }
        }

        binding.like.setOnClickListener {}
    }

    private fun setDataToView() {
        //get Data Of Post
        commentViewModel.post.observe(this){post->
            if (post != null) {
                if (post.postImage.equals("")){
                    binding.relay1.visibility = View.GONE
                }else{
                    Picasso.get()
                        .load(post.postImage)
                        .placeholder(R.drawable.placeholder)
                        .into(binding.imgPost);
                    binding.relay1.visibility = View.VISIBLE
                }
                val description = post.postDescription
                if (description.equals("")){
                    binding.postDescription.visibility = View.GONE
                }else{
                    binding.postDescription.text = description
                    binding.postDescription.visibility = View.VISIBLE
                }
                binding.like.text = post.postLike.toString()
                binding.comment.text = post.commentCount.toString()
            }
        }
        commentViewModel.getDataOfPost(postId!!)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        userModerationManager.checkAndUnblockUser(auth.uid!!)
    }

}