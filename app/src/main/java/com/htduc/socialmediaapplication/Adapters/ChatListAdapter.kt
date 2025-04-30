package com.htduc.socialmediaapplication.Adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.htduc.socialmediaapplication.Activities.ChatActivity
import com.htduc.socialmediaapplication.Models.Chats
import com.htduc.socialmediaapplication.Models.applyClickAnimation
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.databinding.UserChatBinding
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ChatListAdapter(val context: Context, private var listUserChat: ArrayList<Chats>)
    : RecyclerView.Adapter<ChatListAdapter.UserChatViewHolder>() {

    inner class UserChatViewHolder(binding: UserChatBinding): RecyclerView.ViewHolder(binding.root){
        val imgProfile = binding.profileImage
        val status = binding.imgStatus
        val username = binding.username
        val txtLastMessage = binding.txtLastMessage
        val lastMessageTime = binding.timeMessage
        val btnCamera = binding.btnCamera
        val root = binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setUserChatList(listUserChat: ArrayList<Chats>){
        this.listUserChat = listUserChat
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ChatListAdapter.UserChatViewHolder {
        return UserChatViewHolder(UserChatBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return listUserChat.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ChatListAdapter.UserChatViewHolder, position: Int) {
        val chat = listUserChat[position]
        holder.username.text = chat.user.name
        holder.txtLastMessage.text = "${trimMessage(chat.lastMessage)} • "
        val formattedTime = formatTimestamp(chat.lastMessageTime)
        holder.lastMessageTime.text = formattedTime

        if (!chat.user.profilePhoto.isNullOrEmpty()) {
            Picasso.get()
                .load(chat.user.profilePhoto)
                .placeholder(R.drawable.avt) // Ảnh hiển thị khi đang tải
                .error(R.drawable.avt) // Ảnh hiển thị khi có lỗi
                .into(holder.imgProfile)
        } else {
            holder.imgProfile.setImageResource(R.drawable.avt) // Hình mặc định nếu không có ảnh
        }


        //cập nhật trình trạng off và on
        FirebaseDatabase.getInstance().reference.child("presence").child(chat.user.uid!!)
            .addValueEventListener(object : ValueEventListener {
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

        holder.root.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable("user", chat.user)
            intent.putExtras(bundle)
            context.startActivity(intent)
        }

        applyClickAnimation(context, holder.btnCamera){

        }

    }

    fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0) return "Unknown"

        val now = Calendar.getInstance()
        val messageTime = Calendar.getInstance().apply { timeInMillis = timestamp }
        val diffMillis = now.timeInMillis - messageTime.timeInMillis
        val diffSeconds = diffMillis / 1000
        val diffMinutes = diffSeconds / 60
        val diffHours = diffMinutes / 60
        val diffDays = diffHours / 24

        return when {
            diffMinutes < 1 -> "Vừa xong"
            diffHours < 1 -> "$diffMinutes phút trước"
            diffDays < 1 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
            diffDays == 1L -> "Hôm qua"
            diffDays < 7 -> "$diffDays ngày"
            diffDays < 30 -> "${diffDays / 7} tuần"
            diffDays < 365 -> "${diffDays / 30} tháng"
            else -> "${diffDays / 365} năm"
        }
    }

    fun trimMessage(message: String, maxLength: Int = 15): String {
        return if (message.length > maxLength) {
            "${message.substring(0, maxLength)}..."
        } else {
            message
        }
    }

}