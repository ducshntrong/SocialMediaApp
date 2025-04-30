package com.htduc.socialmediaapplication.Adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.htduc.socialmediaapplication.Activities.CommentActivity
import com.htduc.socialmediaapplication.Activities.ProfileActivity
import com.htduc.socialmediaapplication.Models.Notification
import com.htduc.socialmediaapplication.Models.User
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.databinding.Notification2RvBinding
import com.squareup.picasso.Picasso

class NotificationAdapter(val context: Context, private var listNotifi: ArrayList<Notification>)
    : RecyclerView.Adapter<NotificationAdapter.NotificationHolder>(){

    @SuppressLint("NotifyDataSetChanged")
    fun setNotificationList(listNotifi: ArrayList<Notification>){
        this.listNotifi = listNotifi
        notifyDataSetChanged()
    }

    inner class NotificationHolder(binding: Notification2RvBinding): RecyclerView.ViewHolder(binding.root) {
        val profile = binding.profileImage
        val notification = binding.notification
        val time = binding.time
        val openNotification = binding.openNotification
        var user:User? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationHolder {
        return NotificationHolder(Notification2RvBinding.inflate(LayoutInflater
            .from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return listNotifi.size
    }

    override fun onBindViewHolder(holder: NotificationHolder, position: Int) {
        val notification = listNotifi[position]
        val type = notification.type
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(notification.notificationBy!!)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null){
                        holder.user = user
                        Picasso.get().load(user.profilePhoto)
                            .placeholder(R.drawable.avt)
                            .into(holder.profile)
                        val time = TimeAgo.using(notification.notificationAt)
                        holder.time.text = time
                        when (type) {
                            "like" -> holder.notification.text = Html.fromHtml("<b>"+user.name+"</b>" + " like your post")
                            "comment" -> holder.notification.text = Html.fromHtml("<b>"+user.name+"</b>" + " commented your post")
                            "follow" -> holder.notification.text = Html.fromHtml("<b>"+user.name+"</b>" + " started following you")
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })

        holder.openNotification.setOnClickListener {
            if (!type.equals("follow")){
                FirebaseDatabase.getInstance().reference
                    .child("notification")
                    .child(notification.postBy!!)
                    .child(notification.notificationId!!)
                    .child("checkOpen")
                    .setValue(true)
                val intent = Intent(context, CommentActivity::class.java)
                intent.putExtra("postId", notification.postId)
                intent.putExtra("postedBy", notification.postBy)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                holder.openNotification.setBackgroundColor(Color.parseColor("#FFFFFF"))
            }else{
                FirebaseDatabase.getInstance().reference
                    .child("notification")
                    .child(notification.followed!!)
                    .child(notification.notificationId!!)
                    .child("checkOpen")
                    .setValue(true)
                val intent = Intent(context, ProfileActivity::class.java)
                val bundle = Bundle()
                bundle.putParcelable("user", holder.user)
                intent.putExtras(bundle)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                holder.openNotification.setBackgroundColor(Color.parseColor("#FFFFFF"))
            }
        }
        val checkOpen = notification.checkOpen
        if (checkOpen)
            holder.openNotification.setBackgroundColor(Color.parseColor("#FFFFFF"))
        else
            holder.openNotification.setBackgroundColor(Color.parseColor("#EFEFEF"))
    }
}