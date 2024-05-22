package com.htduc.socialmediaapplication.Activity

import android.app.ProgressDialog
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.htduc.socialmediaapplication.Adapter.CommentAdapter
import com.htduc.socialmediaapplication.Model.Comment
import com.htduc.socialmediaapplication.Model.Notification
import com.htduc.socialmediaapplication.Model.Post
import com.htduc.socialmediaapplication.Model.User
import com.htduc.socialmediaapplication.Model.applyClickAnimation
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.databinding.ActivityCommentBinding
import com.squareup.picasso.Picasso
import java.util.Date

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar2)
        this.title = "Comments"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)// hiển thị nút back

        dialog = ProgressDialog(this, ProgressDialog.STYLE_SPINNER)
        dialog?.setTitle("Image Uploading")
        dialog?.setMessage("Please Wait...")
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        postId = intent.getStringExtra("postId")
        postedBy = intent.getStringExtra("postedBy")

        setDataToView()
        showListComment()

        applyClickAnimation(this, binding.btnSend){
            sendComment()
        }

        val imagePickCallback = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            selectedImgCmt = uri
            // Xử lý ảnh đã chọn ở đây
            if (selectedImgCmt != null) {
                binding.cmtImg.setImageURI(selectedImgCmt)
                binding.cmtImg.visibility = View.VISIBLE
            }
        }
        applyClickAnimation(this, binding.attachment){
            imagePickCallback.launch("image/*")
        }

        binding.like.setOnClickListener {

        }
    }

    private fun showListComment() {
        commentAdapter = CommentAdapter(this)
        binding.rvComment.layoutManager = LinearLayoutManager(this)
        binding.rvComment.setHasFixedSize(true)
        binding.rvComment.adapter = commentAdapter
        database.reference
            .child("posts")
            .child(postId!!)
            .child("comments").addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    listComment.clear()
                    if (snapshot.exists()){
                        for (dataSnap in snapshot.children){
                            val comment = dataSnap.getValue(Comment::class.java)
                            if (comment != null) {
                                listComment.add(comment)
                            }
                        }
                        commentAdapter.setCommentList(listComment)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    private fun setDataToView() {
        //get data of post
        database.reference.child("posts").child(postId!!)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val post = snapshot.getValue(Post::class.java)
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

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
        //get data of user
        database.reference.child("Users").child(postedBy!!)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null){
                        Picasso.get()
                            .load(user.profilePhoto)
                            .placeholder(R.drawable.avt)
                            .into(binding.profileImage);
                        binding.username.text = user.name
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    private fun sendComment() {
        if (selectedImgCmt != null){
            val reference = storage.reference.child("comments")
                .child(auth.uid!!).child(Date().time.toString())
            dialog?.show()
            reference.putFile(selectedImgCmt!!)
                .addOnSuccessListener { task ->
                    dialog?.dismiss()
                    task.metadata!!.reference!!.downloadUrl
                        .addOnSuccessListener { url ->
                            val imageUrl = url.toString()
                            val comment = Comment(binding.edtMessage.text.toString().trim(),
                                Date().time, auth.uid, imageUrl)
                            database.reference
                                .child("posts")
                                .child(postId!!)
                                .child("comments")
                                .push()
                                .setValue(comment).addOnSuccessListener {
                                    val commentCountRef = database.reference
                                        .child("posts")
                                        .child(postId!!)
                                        .child("commentCount")
                                    commentCountRef.addListenerForSingleValueEvent(object : ValueEventListener{
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            var commentCount = 0
                                            if (snapshot.exists()){
                                                commentCount = snapshot.getValue(Int::class.java)!!
                                            }
                                            commentCountRef.setValue(commentCount+1)
                                                .addOnSuccessListener {
                                                    binding.cmtImg.visibility = View.GONE
                                                    binding.edtMessage.setText("")

                                                    sendNotification()
                                                }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            TODO("Not yet implemented")
                                        }
                                    })
                                }
                        }
                }
        }else{
            val comment = Comment(binding.edtMessage.text.toString().trim(),
                Date().time, auth.uid, "")
            database.reference
                .child("posts")
                .child(postId!!)
                .child("comments")
                .push()
                .setValue(comment).addOnSuccessListener {
                    val commentCountRef = database.reference
                        .child("posts")
                        .child(postId!!)
                        .child("commentCount")
                    commentCountRef.addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            var commentCount = 0
                            if (snapshot.exists()){
                                commentCount = snapshot.getValue(Int::class.java)!!
                            }
                            commentCountRef.setValue(commentCount+1)
                                .addOnSuccessListener {
                                    binding.edtMessage.setText("")
                                    sendNotification()
                                }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            TODO("Not yet implemented")
                        }
                    })
                }
        }
    }

    private fun sendNotification(){
        if (postedBy != auth.uid){ //ktr nếu id người đăng khác với id người đang thao tác
            val notification = Notification()
            notification.notificationBy = auth.uid
            notification.notificationAt = Date().time
            notification.postId = postId
            notification.postBy = postedBy //id cua ng dang
            notification.type = "comment"
            FirebaseDatabase.getInstance().reference
                .child("notification")
                .child(postedBy!!)
                .push()
                .setValue(notification)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }

}