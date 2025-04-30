package com.htduc.socialmediaapplication.Adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.htduc.socialmediaapplication.Activities.ProfileActivity
import com.htduc.socialmediaapplication.Models.Follow
import com.htduc.socialmediaapplication.Models.User
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.databinding.FriendsRvBinding
import com.squareup.picasso.Picasso

class FollowAdapter(val context: Context, private var listFollow:ArrayList<Follow>)
    :RecyclerView.Adapter<FollowAdapter.FriendHolder>(){

    @SuppressLint("NotifyDataSetChanged")
    fun setFollowList(listFollow: ArrayList<Follow>){
        this.listFollow = listFollow
        notifyDataSetChanged()
    }
    inner class FriendHolder(binding: FriendsRvBinding): RecyclerView.ViewHolder(binding.root) {
        val profileImage = binding.profileImage
        val root = binding.root
        var user:User? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendHolder {
        return FriendHolder(FriendsRvBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return listFollow.size
    }

    override fun onBindViewHolder(holder: FriendHolder, position: Int) {
        val follow = listFollow[position]
        FirebaseDatabase.getInstance().reference
            .child("Users")
            .child(follow.followedBy!!)//id của người đã theo dõi
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    if (user?.profilePhoto != null) {
                        holder.user = user
                        Picasso.get()
                            .load(user.profilePhoto)
                            .placeholder(R.drawable.avt)
                            .into(holder.profileImage)
                    } else {
                        // Xử lý khi giá trị user hoặc profilePhoto là null
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })

        holder.profileImage.setOnClickListener {
            if (follow.followedBy != FirebaseAuth.getInstance().uid){
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