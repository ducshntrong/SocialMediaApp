package com.htduc.socialmediaapplication.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.htduc.socialmediaapplication.Activity.CommentActivity
import com.htduc.socialmediaapplication.Activity.ProfileActivity
import com.htduc.socialmediaapplication.Fragments.ProfileFragment
import com.htduc.socialmediaapplication.Model.Notification
import com.htduc.socialmediaapplication.Model.Post
import com.htduc.socialmediaapplication.Model.User
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.databinding.DasboardRvSampleBinding
import com.iammert.library.readablebottombar.ReadableBottomBar
import com.squareup.picasso.Picasso
import java.util.Date

class PostAdapter(private val context: Context, private var listPost: ArrayList<Post>)
    :RecyclerView.Adapter<PostAdapter.DashboardHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun setPostList(listPost: ArrayList<Post>){
        this.listPost = listPost
        notifyDataSetChanged()
    }
    inner class DashboardHolder(binding: DasboardRvSampleBinding): RecyclerView.ViewHolder(binding.root) {
        val profile = binding.profileImage
        val postImage = binding.postImg
        val postDescription = binding.postDescription
        val save = binding.saveImg
        val name = binding.userName
        val time = binding.time
        val like = binding.like
        val comment = binding.comment
        val share = binding.share
        val layoutUser = binding.layout1Child
        val root = binding.root
        var user:User? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardHolder {
        return DashboardHolder(DasboardRvSampleBinding.inflate(LayoutInflater.from(parent.context)
            , parent, false))
    }

    override fun getItemCount(): Int {
        return listPost.size
    }

    @SuppressLint("CommitTransaction")
    override fun onBindViewHolder(holder: DashboardHolder, position: Int) {
        val post = listPost[position]
        if (post.postImage.equals("")){
            holder.postImage.visibility = View.GONE
        }else{
            Glide.with(context)
                .load(post.postImage)
                .placeholder(R.drawable.placeholder)
                .into(holder.postImage);
            holder.postImage.visibility = View.VISIBLE
        }
        val description = post.postDescription
        if (description.equals("")){
            holder.postDescription.visibility = View.GONE
        }else{
            holder.postDescription.text = description
            holder.postDescription.visibility = View.VISIBLE
        }
        holder.like.text = post.postLike.toString()
        holder.comment.text = post.commentCount.toString()

        FirebaseDatabase.getInstance().reference.child("Users")
            .child(post.postedBy!!).addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        holder.user = user // Lưu trữ thông tin người dùng trong holder
                        Picasso.get().load(user.profilePhoto)
                            .placeholder(R.drawable.avt)
                            .into(holder.profile)
                        holder.name.text = user.name
                        val time = TimeAgo.using(post.postedAt!!)
                        holder.time.text = time
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

        val currentUserId = FirebaseAuth.getInstance().uid!!
        val likesRef = FirebaseDatabase.getInstance().reference
            .child("posts")
            .child(post.postId!!)
            .child("likes")
            .child(currentUserId)
        likesRef.addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){//check liệu người dùng đã like chưa
                        //nếu like r
                        holder.like.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.heart2, 0, 0, 0)
                        holder.like.setOnClickListener {
                            likesRef.removeValue().addOnSuccessListener {
                                    // Xóa thành công, cập nhật lại số lượng like
                                    val postLikeRef = FirebaseDatabase.getInstance().reference
                                        .child("posts")
                                        .child(post.postId!!)
                                        .child("postLike")
                                    postLikeRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                                            val currentPostLikes = dataSnapshot.getValue(Int::class.java)
                                            if (currentPostLikes != null) {
                                                postLikeRef.setValue(currentPostLikes - 1)
                                                    .addOnSuccessListener {
                                                        // Huỷ like thành công, cập nhật giao diện
                                                        holder.like.setCompoundDrawablesWithIntrinsicBounds(
                                                            R.drawable.heart1, 0, 0, 0)
                                                        //xử lý xoá thông báo khi click huỷ like
                                                        //orderByChild("postId").equalTo(post.postId) để tìm kiếm các thông báo
                                                        // có thuộc tính "postId" bằng giá trị post.postId mong muốn.
                                                        FirebaseDatabase.getInstance().reference
                                                            .child("notification")
                                                            .child(post.postedBy!!)
                                                            .orderByChild("postId")
                                                            .equalTo(post.postId)
                                                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                                                for (childSnapshot in dataSnapshot.children) {
                                                                    val notification = childSnapshot.getValue(Notification::class.java)
                                                                    // nếu id người gửi tbao trùng vs id người dùng đang thao tác
                                                                    //và là hành động like thì xoá đi tbao đó
                                                                    if (notification?.notificationBy == currentUserId &&
                                                                        notification.type == "like") {
                                                                        childSnapshot.ref.removeValue()
                                                                    }
                                                                }
                                                            }

                                                            override fun onCancelled(databaseError: DatabaseError) {
                                                            }
                                                        })
                                                    }
                                            }
                                        }
                                        override fun onCancelled(databaseError: DatabaseError) {
                                            // Xử lý khi có lỗi
                                        }
                                    })
                                }
                        }
                    }else{//nếu chưa like
                        holder.like.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.heart1, 0, 0, 0)
                        holder.like.setOnClickListener {
                            likesRef.setValue(true).addOnSuccessListener {
                                    FirebaseDatabase.getInstance().reference
                                        .child("posts")
                                        .child(post.postId!!)
                                        .child("postLike")
                                        .setValue(post.postLike + 1).addOnSuccessListener {
                                            holder.like.setCompoundDrawablesWithIntrinsicBounds(
                                                R.drawable.heart2, 0, 0, 0)
                                            //ktr nếu id người đăng khác với id người đang thao tác
                                            if (post.postedBy != currentUserId){
                                                val notification = Notification()
                                                notification.notificationBy = currentUserId
                                                notification.notificationAt = Date().time
                                                notification.postId = post.postId
                                                notification.postBy = post.postedBy
                                                notification.type = "like"
                                                FirebaseDatabase.getInstance().reference
                                                    .child("notification")
                                                    .child(post.postedBy!!)
                                                    .push()
                                                    .setValue(notification)
                                            }
                                        }
                                }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })

        holder.comment.setOnClickListener {
            val intent = Intent(context, CommentActivity::class.java)
            intent.putExtra("postId", post.postId)
            intent.putExtra("postedBy", post.postedBy)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }

        holder.layoutUser.setOnClickListener {
            if (post.postedBy == FirebaseAuth.getInstance().uid){
                (context as AppCompatActivity).supportFragmentManager
                    .beginTransaction().replace(R.id.frameLayout, ProfileFragment()).commit()
                // Chọn Tab tương ứng trong Bottom Navigation Bar
                context.findViewById<ReadableBottomBar>(R.id.readableBottomBar)
                    .selectItem(4)
            }else{
                val intent = Intent(context, ProfileActivity::class.java)
                val bundle = Bundle()
                bundle.putParcelable("user", holder.user)
                intent.putExtras(bundle)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        }

    }
}