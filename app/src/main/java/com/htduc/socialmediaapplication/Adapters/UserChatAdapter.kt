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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.htduc.socialmediaapplication.Activities.AddNoteActivity
import com.htduc.socialmediaapplication.Activities.ChatActivity
import com.htduc.socialmediaapplication.Activities.NoteActivity
import com.htduc.socialmediaapplication.Models.Note
import com.htduc.socialmediaapplication.Models.User
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.databinding.ItemCreateStoryBinding
import com.htduc.socialmediaapplication.databinding.UserChatSearchBinding
import com.squareup.picasso.Picasso

class UserChatAdapter(val context: Context, private val  onAddStoryClick: () -> Unit)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var listUser: ArrayList<User> = arrayListOf()

    companion object {
        private const val VIEW_TYPE_CREATE_STORY = 0
        private const val VIEW_TYPE_USER = 1
    }

    // ViewHolder cho create story
    inner class CreateStoryViewHolder(binding: ItemCreateStoryBinding) : RecyclerView.ViewHolder(binding.root) {
        val imgProfile = binding.profileImage
        val btnAdd = binding.btnAdd
        val txtNote = binding.txtNote
        val circle1 = binding.circle
        val circle2 = binding.circle2
        var user:User? = null
        var myNote: Note? = null
    }

    // ViewHolder cho user
    inner class UserChatViewHolder(binding: UserChatSearchBinding) : RecyclerView.ViewHolder(binding.root) {
        val imgProfile = binding.profileImage
        val status = binding.imgStatus
        val username = binding.username
        val txtNote = binding.txtNote
        val circle1 = binding.circle
        val circle2 = binding.circle2
        val root = binding.root
        var note: Note? = null
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setUserList(listUser: ArrayList<User>) {
        this.listUser = listUser
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_CREATE_STORY else VIEW_TYPE_USER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CREATE_STORY -> {
                CreateStoryViewHolder(ItemCreateStoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
            else -> {
                UserChatViewHolder(UserChatSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
        }
    }

    override fun getItemCount(): Int {
        return listUser.size + 1 // +1 để thêm create story
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CreateStoryViewHolder) {
            // Gán dữ liệu cho create story
            val uid = FirebaseAuth.getInstance().uid

            FirebaseDatabase.getInstance().reference.child("Users")
                .child(uid!!).addValueEventListener(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        if (user != null) {
                            holder.user = user // Lưu trữ thông tin người dùng trong holder
                            if (!user.profilePhoto.isNullOrEmpty()) {
                                Picasso.get().load(user.profilePhoto).into(holder.imgProfile)
                            } else {
                                Picasso.get().load(R.drawable.avt).into(holder.imgProfile) // Ảnh mặc định
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            // Lấy ghi chú của chính người dùng
            FirebaseDatabase.getInstance().reference.child("Notes").child(uid)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (data in snapshot.children) {
                                val note = data.getValue(Note::class.java)
                                if (note != null) {
                                    holder.myNote = note
                                    holder.txtNote.text = note.content
                                    return // Dừng ngay khi có ghi chú đầu tiên
                                }
                            }
                        } else {
                            holder.myNote = null
                            holder.txtNote.text = "Viết ghi chú..."
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })



            holder.btnAdd.setOnClickListener { onAddStoryClick() }
            holder.imgProfile.setOnClickListener { onAddStoryClick() }
            holder.txtNote.setOnClickListener {
                val intent = if (holder.myNote != null){
                    Intent(context, NoteActivity::class.java)
                } else {
                    Intent(context, AddNoteActivity::class.java)
                }
                val bundle = Bundle().apply {
                    putParcelable("user", holder.user)
                    putParcelable("note", holder.myNote)
                }
                intent.putExtras(bundle)
                val activityOptions = ActivityOptionsCompat.makeCustomAnimation(
                    context, R.anim.slide_up, R.anim.fade_out
                )
                context.startActivity(intent, activityOptions.toBundle())
            }
        } else if (holder is UserChatViewHolder) {
            val user = listUser[position - 1] // Trừ 1 vì vị trí đầu tiên là create story
            holder.username.text = user.name
            holder.txtNote.text = user.profession
            Picasso.get()
                .load(user.profilePhoto)
                .placeholder(R.drawable.avt)
                .into(holder.imgProfile)

            // Xử lý trạng thái online/offline
            FirebaseDatabase.getInstance().reference.child("presence").child(user.uid!!)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val status = snapshot.getValue(String::class.java)
                            holder.status.visibility = if (status == "Offline") View.INVISIBLE else View.VISIBLE
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })

            // Lấy ghi chú của từng user
            FirebaseDatabase.getInstance().reference.child("Notes").child(user.uid!!)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (data in snapshot.children) {
                                val note = data.getValue(Note::class.java)
                                if (note != null) {
                                    holder.note = note
                                    holder.txtNote.text = note.content
                                    holder.circle1.visibility = View.VISIBLE
                                    holder.circle2.visibility = View.VISIBLE
                                    holder.txtNote.visibility = View.VISIBLE
                                    return // Dừng ngay khi có ghi chú đầu tiên
                                }
                            }
                        } else {
                            holder.note = null
                            holder.circle1.visibility = View.INVISIBLE
                            holder.circle2.visibility = View.INVISIBLE
                            holder.txtNote.visibility = View.INVISIBLE
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })


            holder.txtNote.setOnClickListener {
                val intent = Intent(context, NoteActivity::class.java)
                val bundle = Bundle().apply {
                    putParcelable("user", user)
                    putParcelable("note", holder.note)
                }
                intent.putExtras(bundle)
                val activityOptions = ActivityOptionsCompat.makeCustomAnimation(
                    context, R.anim.slide_up, R.anim.fade_out
                )
                context.startActivity(intent, activityOptions.toBundle())
            }

            holder.imgProfile.setOnClickListener {
                openActivity(context, ChatActivity(), user)
            }
            holder.username.setOnClickListener {
                openActivity(context, ChatActivity(), user)
            }
        }
    }

    private fun openActivity(context: Context, activity: Activity, user: User) {
        val intent = Intent(context, activity::class.java)
        val bundle = Bundle()
        bundle.putParcelable("user", user)
        intent.putExtras(bundle)
        context.startActivity(intent)
    }
}
