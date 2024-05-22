package com.htduc.socialmediaapplication.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.htduc.socialmediaapplication.Activity.ProfileActivity
import com.htduc.socialmediaapplication.Model.Follow
import com.htduc.socialmediaapplication.Model.Notification
import com.htduc.socialmediaapplication.Model.User
import com.htduc.socialmediaapplication.Model.applyClickAnimation
import com.htduc.socialmediaapplication.Model.applyClickAnimation2
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.databinding.UserRvBinding
import com.squareup.picasso.Picasso
import java.util.Date

class UserAdapter(val context: Context)
    :RecyclerView.Adapter<UserAdapter.UserHolder>(){
    private var isFollowing:Boolean = false
    private var listUser: ArrayList<User> = arrayListOf()

    @SuppressLint("NotifyDataSetChanged")
    fun setUserList(listUser: ArrayList<User>){
        this.listUser = listUser
        notifyDataSetChanged()
    }
    inner class UserHolder(binding: UserRvBinding): RecyclerView.ViewHolder(binding.root) {
        val imgProfile = binding.profileImage
        val username = binding.username
        val profession = binding.profession
        val btnFollow = binding.btnFollow
        val status = binding.imgStatus
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserHolder {
        return UserHolder(UserRvBinding.inflate(LayoutInflater.from(parent.context), parent ,false))
    }

    override fun getItemCount(): Int {
        return listUser.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: UserHolder, position: Int) {
        val user = listUser[position]
        holder.username.text = user.name
        holder.profession.text = user.profession
        Picasso.get()
            .load(user.profilePhoto)
            .placeholder(R.drawable.avt)
            .into(holder.imgProfile)
        //cập nhật trình trạng off và on
        FirebaseDatabase.getInstance().reference.child("presence").child(user.uid!!)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        val status = snapshot.getValue(String::class.java)
                        if (status != null){
                            if (status == "Offline"){
                                holder.status.visibility = View.INVISIBLE
                            }else{
                                holder.status.visibility = View.VISIBLE
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })

        val currentUserUid = FirebaseAuth.getInstance().uid // Lấy ID của người dùng hiện tại
        FirebaseDatabase.getInstance().reference
            .child("Users")
            .child(user.uid!!)
            .child("followers")
            .child(currentUserUid!!).addListenerForSingleValueEvent(object : ValueEventListener{
                @SuppressLint("NotifyDataSetChanged")
                override fun onDataChange(snapshot: DataSnapshot) {
                    //snapshot.exists(): data đã tồn tại hay chưa
                    if (snapshot.exists()){//nếu như đã follow(data tồn tại) thì thay đổi btn
                        holder.btnFollow.background = ContextCompat.getDrawable(context, R.drawable.follow_action_btn)
                        holder.btnFollow.text = "Following"
                        holder.btnFollow.setTextColor(context.resources.getColor(R.color.derkGrey))
                        holder.btnFollow.isEnabled = false
                    }else{//nếu như chưa follow(data chưa tồn tại)
                        applyClickAnimation2(context, holder.btnFollow){
                            val follow = Follow()
                            follow.followedBy = currentUserUid //gán là id của user hiện tại
                            follow.followedAt = Date().time

                            //lưu trữ thông tin về người theo dõi
                            FirebaseDatabase.getInstance().reference
                                .child("Users")
                                .child(user.uid!!)// ID của người mà người dùng đang theo dõi
                                .child("followers")
                                .child(currentUserUid)
                                .setValue(follow)
                                .addOnSuccessListener {
                                    FirebaseDatabase.getInstance().reference
                                        .child("Users")
                                        .child(user.uid!!)
                                        .child("followerCount")
                                        .setValue(user.followerCount + 1)
                                        .addOnSuccessListener {
                                            holder.btnFollow.background = ContextCompat.getDrawable(context, R.drawable.follow_action_btn)
                                            holder.btnFollow.text = "Following"
                                            holder.btnFollow.setTextColor(context.resources.getColor(R.color.derkGrey))
                                            holder.btnFollow.isEnabled = false
                                            Toast.makeText(context, "You Followed "+user.name, Toast.LENGTH_SHORT).show()

                                            //Notification
                                            val notification = Notification()
                                            notification.notificationBy = currentUserUid
                                            notification.notificationAt = Date().time
                                            notification.followed = user.uid
                                            notification.type = "follow"

                                            FirebaseDatabase.getInstance().reference
                                                .child("notification")
                                                .child(user.uid!!) //id của người đc follow
                                                .push() //id của thông báo
                                                .setValue(notification)
                                        }
                                }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })

        holder.root.setOnClickListener {
            val intent = Intent(context, ProfileActivity::class.java)
            val users = User(user.uid, user.name, user.profession, user.email, user.password
                ,user.coverPhoto, user.profilePhoto, user.birthday, user.phone, user.gender,user.followerCount)
            val bundle = Bundle()
            bundle.putParcelable("user", users)
            intent.putExtras(bundle)
            //sd để chỉ định rằng một hoạt động (activity) mới
            // sẽ được khởi chạy trong một tác vụ (task) mới.
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }
}