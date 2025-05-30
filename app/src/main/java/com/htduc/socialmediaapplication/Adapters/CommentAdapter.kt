package com.htduc.socialmediaapplication.Adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.htduc.socialmediaapplication.Activities.ImageDetailActivity
import com.htduc.socialmediaapplication.Activities.ProfileActivity
import com.htduc.socialmediaapplication.Models.Comment
import com.htduc.socialmediaapplication.Models.User
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.databinding.UserCmtBinding
import com.squareup.picasso.Picasso

class CommentAdapter(val context: Context): RecyclerView.Adapter<CommentAdapter.CommentHolder>() {
    private var listComment: ArrayList<Comment> = arrayListOf()

    @SuppressLint("NotifyDataSetChanged")
    fun setCommentList(listComment: ArrayList<Comment>){
        this.listComment = listComment
        notifyDataSetChanged()
    }

    inner class CommentHolder(binding: UserCmtBinding): RecyclerView.ViewHolder(binding.root) {
        val imgUser = binding.profileImage
        val username = binding.userName
        val cmtBody = binding.comment
        val imgCmt = binding.cmtImg
        val time = binding.time
        var user:User? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentHolder {
        return CommentHolder(UserCmtBinding.inflate(LayoutInflater.from(parent.context),
            parent, false))
    }

    override fun getItemCount(): Int {
        return listComment.size
    }

    override fun onBindViewHolder(holder: CommentHolder, position: Int) {
        val comment = listComment[position]
        if (comment.commentImg.equals("")){
            holder.imgCmt.visibility = View.GONE
        }else{
            Picasso.get()
                .load(comment.commentImg)
                .placeholder(R.drawable.placeholder)
                .into(holder.imgCmt);
            holder.imgCmt.visibility = View.VISIBLE
        }
        val description = comment.commentBody
        if (description.equals("")){
            holder.cmtBody.visibility = View.GONE
        }else{
            holder.cmtBody.text = description
            holder.cmtBody.visibility = View.VISIBLE
        }
        val time = TimeAgo.using(comment.commentedAt)
        holder.time.text = time

        FirebaseDatabase.getInstance().reference
            .child("Users").child(comment.commentedBy!!)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        holder.user = user
                        Picasso.get().load(user.profilePhoto)
                            .placeholder(R.drawable.avt)
                            .into(holder.imgUser)
                        holder.username.text = user.name
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })

        holder.imgUser.setOnClickListener {
            clickToProfileUser(comment, holder.user!!)
        }
        holder.username.setOnClickListener {
            clickToProfileUser(comment, holder.user!!)
        }

        holder.imgCmt.setOnClickListener {
            val intent = Intent(context, ImageDetailActivity::class.java)
            intent.putExtra("comment_img", comment.commentImg)

            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                (context as Activity),
                holder.imgCmt,      // view dùng để chuyển tiếp
                "postImageTransition"  // tên transitionName phải giống ở cả 2 Activity
            )
            context.startActivity(intent, options.toBundle())
        }
    }

    private fun clickToProfileUser(comment: Comment, user: User) {
        if (comment.commentedBy != FirebaseAuth.getInstance().uid){
            val intent = Intent(context, ProfileActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable("user", user)
            intent.putExtras(bundle)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }
}