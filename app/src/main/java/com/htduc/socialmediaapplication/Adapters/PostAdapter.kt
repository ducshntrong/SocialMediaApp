package com.htduc.socialmediaapplication.Adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.htduc.socialmediaapplication.Activities.CommentActivity
import com.htduc.socialmediaapplication.Activities.ProfileActivity
import com.htduc.socialmediaapplication.Fragments.ProfileFragment
import com.htduc.socialmediaapplication.Models.Notification
import com.htduc.socialmediaapplication.Models.Post
import com.htduc.socialmediaapplication.Models.User
import com.htduc.socialmediaapplication.BottomSheets.MoreOptionsBottomSheetPost
import com.htduc.socialmediaapplication.Models.Story
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.databinding.DasboardRvSampleBinding
import com.iammert.library.readablebottombar.ReadableBottomBar
import com.squareup.picasso.Picasso
import java.util.Date

class PostAdapter(private val context: Context, private var listPost: ArrayList<Post>)
    :RecyclerView.Adapter<PostAdapter.DashboardHolder>() {
    private val userCache = mutableMapOf<String, User>()
    private val userListeners = mutableMapOf<String, ValueEventListener>()

    @SuppressLint("NotifyDataSetChanged")
    fun setPostList(listPost: ArrayList<Post>){
        this.listPost = listPost
        notifyDataSetChanged()
    }

    fun getPostList(): ArrayList<Post> {
        return listPost
    }

    inner class DashboardHolder(binding: DasboardRvSampleBinding): RecyclerView.ViewHolder(binding.root) {
        val profile = binding.profileImage
        val postImage = binding.postImg
        val postDescription = binding.postDescription
        val name = binding.userName
        val time = binding.time
        val like = binding.like
        val comment = binding.comment
        val share = binding.share
        val layoutUser = binding.layout1Child
        val more = binding.more
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
    override fun onBindViewHolder(holder: DashboardHolder, @SuppressLint("RecyclerView") position: Int) {
        val post = listPost[position]

        // Hiển thị ảnh bài viết nếu có
        if (post.postImage.isNullOrEmpty()) {
            holder.postImage.visibility = View.GONE
        } else {
            holder.postImage.visibility = View.VISIBLE
            Glide.with(context)
                .load(post.postImage)
                .placeholder(R.drawable.placeholder)
                .into(holder.postImage)
        }

        // Hiển thị mô tả nếu có
        if (post.postDescription.isNullOrEmpty()) {
            holder.postDescription.visibility = View.GONE
        } else {
            holder.postDescription.visibility = View.VISIBLE
            holder.postDescription.text = post.postDescription
        }

        // Gán like, comment, thời gian
        holder.like.text = post.postLike.toString()
        holder.comment.text = post.commentCount.toString()
        post.postedAt?.let {
            holder.time.text = TimeAgo.using(it)
        }

        // Lấy thông tin người đăng bài
        post.postedBy?.let { postedById ->
            val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(postedById)

            // Nếu đã có trong cache
            if (userCache.containsKey(postedById)) {
                val user = userCache[postedById]
                holder.user = user
                Picasso.get().load(user?.profilePhoto)
                    .placeholder(R.drawable.avt)
                    .into(holder.profile)
                holder.name.text = user?.name
            } else {
                // Nếu chưa có, tạo listener chỉ 1 lần
                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        user?.let {
                            userCache[postedById] = it  // lưu vào cache
                            notifyItemChanged(position) // cập nhật lại item sau khi có data
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                }

                userListeners[postedById] = listener
                userRef.addValueEventListener(listener)
            }

            // Xử lý nút like
            val currentUserId = FirebaseAuth.getInstance().uid ?: return
            val likesRef = FirebaseDatabase.getInstance().reference
                .child("posts").child(post.postId ?: return)
                .child("likes").child(currentUserId)

            likesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val liked = snapshot.exists()
                    val heartIcon = if (liked) R.drawable.heart2 else R.drawable.heart1
                    holder.like.setCompoundDrawablesWithIntrinsicBounds(heartIcon, 0, 0, 0)

                    holder.like.setOnClickListener {
                        if (liked) {
                            likesRef.removeValue().addOnSuccessListener {
                                updateLikeCount(post.postId!!, -1, holder)
                                removeLikeNotification(post, currentUserId)
                            }
                        } else {
                            likesRef.setValue(true).addOnSuccessListener {
                                updateLikeCount(post.postId!!, +1, holder)
                                if (postedById != currentUserId) {
                                    sendLikeNotification(post, currentUserId)
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }

        holder.comment.setOnClickListener {
            val intent = Intent(context, CommentActivity::class.java)
            intent.putExtra("postId", post.postId)
            intent.putExtra("postedBy", post.postedBy)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }

        holder.more.setOnClickListener {
            val bottomSheet = MoreOptionsBottomSheetPost(context, post)
            bottomSheet.show((context as AppCompatActivity).supportFragmentManager, "MoreOptionsBottomSheetPost")
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
    override fun onViewRecycled(holder: DashboardHolder) {
        super.onViewRecycled(holder)
        holder.user?.let { user ->
            val uid = user.uid
            userListeners[uid]?.let { listener ->
                FirebaseDatabase.getInstance().reference
                    .child("Users").child(uid!!)
                    .removeEventListener(listener)
                userListeners.remove(uid)
            }
        }
    }



    private fun updateLikeCount(postId: String, delta: Int, holder: DashboardHolder) {
        val postLikeRef = FirebaseDatabase.getInstance().reference
            .child("posts").child(postId).child("postLike")

        postLikeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentLikes = snapshot.getValue(Int::class.java) ?: 0
                val updatedLikes = (currentLikes + delta).coerceAtLeast(0)
                postLikeRef.setValue(updatedLikes) // dùng lại biến
                holder.like.text = updatedLikes.toString()
                val icon = if (delta > 0) R.drawable.heart2 else R.drawable.heart1
                holder.like.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun removeLikeNotification(post: Post, currentUserId: String) {
        //xử lý xoá thông báo khi click huỷ like
        //orderByChild("postId").equalTo(post.postId) để tìm kiếm các thông báo
        // có thuộc tính "postId" bằng giá trị post.postId mong muốn.
        FirebaseDatabase.getInstance().reference
            .child("notification").child(post.postedBy!!)
            .orderByChild("postId").equalTo(post.postId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val notification = child.getValue(Notification::class.java)
                        if (notification?.notificationBy == currentUserId && notification.type == "like") {
                            child.ref.removeValue()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun sendLikeNotification(post: Post, currentUserId: String) {
        val notification = Notification().apply {
            notificationBy = currentUserId
            notificationAt = Date().time
            postId = post.postId
            postBy = post.postedBy
            type = "like"
        }

        FirebaseDatabase.getInstance().reference
            .child("notification").child(post.postedBy!!)
            .push().setValue(notification)
    }

}

//class PostAdapter(private val context: Context, private var listPost: ArrayList<Post>)
//    :RecyclerView.Adapter<PostAdapter.DashboardHolder>() {
//
//    @SuppressLint("NotifyDataSetChanged")
//    fun setPostList(listPost: ArrayList<Post>){
//        this.listPost = listPost
//        notifyDataSetChanged()
//    }
//
//    inner class DashboardHolder(binding: DasboardRvSampleBinding): RecyclerView.ViewHolder(binding.root) {
//        val profile = binding.profileImage
//        val postImage = binding.postImg
//        val postDescription = binding.postDescription
//        val name = binding.userName
//        val time = binding.time
//        val like = binding.like
//        val comment = binding.comment
//        val share = binding.share
//        val layoutUser = binding.layout1Child
//        val more = binding.more
//        val root = binding.root
//        var user:User? = null
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardHolder {
//        return DashboardHolder(DasboardRvSampleBinding.inflate(LayoutInflater.from(parent.context)
//            , parent, false))
//    }
//
//    override fun getItemCount(): Int {
//        return listPost.size
//    }
//
//    @SuppressLint("CommitTransaction")
//    override fun onBindViewHolder(holder: DashboardHolder, position: Int) {
//        val post = listPost[position]
//        if (post.postImage.isNullOrEmpty()){
//            holder.postImage.visibility = View.GONE
//        }else{
//            Glide.with(context)
//                .load(post.postImage)
//                .placeholder(R.drawable.placeholder)
//                .into(holder.postImage);
//            holder.postImage.visibility = View.VISIBLE
//        }
//        val description = post.postDescription
//        if (post.postImage.isNullOrEmpty()){
//            holder.postDescription.visibility = View.GONE
//        }else{
//            holder.postDescription.text = description
//            holder.postDescription.visibility = View.VISIBLE
//        }
//        holder.like.text = post.postLike.toString()
//        holder.comment.text = post.commentCount.toString()
//        val time = TimeAgo.using(post.postedAt!!)
//        holder.time.text = time
//
//        FirebaseDatabase.getInstance().reference.child("Users")
//            .child(post.postedBy!!).addListenerForSingleValueEvent(object : ValueEventListener{
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    val user = snapshot.getValue(User::class.java)
//                    if (user != null) {
//                        holder.user = user // Lưu trữ thông tin người dùng trong holder
//                        Picasso.get().load(user.profilePhoto)
//                            .placeholder(R.drawable.avt)
//                            .into(holder.profile)
//                        holder.name.text = user.name
//                    }
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    TODO("Not yet implemented")
//                }
//            })
//
//        val currentUserId = FirebaseAuth.getInstance().uid!!
//        val likesRef = FirebaseDatabase.getInstance().reference
//            .child("posts")
//            .child(post.postId!!)
//            .child("likes")
//            .child(currentUserId)
//        likesRef.addListenerForSingleValueEvent(object : ValueEventListener{
//            override fun onDataChange(snapshot: DataSnapshot) {
//                if (snapshot.exists()){//check liệu người dùng đã like chưa
//                    //nếu like r
//                    holder.like.setCompoundDrawablesWithIntrinsicBounds(
//                        R.drawable.heart2, 0, 0, 0)
//                    holder.like.setOnClickListener {
//                        likesRef.removeValue().addOnSuccessListener {
//                            // Xóa thành công, cập nhật lại số lượng like
//                            val postLikeRef = FirebaseDatabase.getInstance().reference
//                                .child("posts")
//                                .child(post.postId!!)
//                                .child("postLike")
//                            postLikeRef.addListenerForSingleValueEvent(object : ValueEventListener {
//                                override fun onDataChange(dataSnapshot: DataSnapshot) {
//                                    val currentPostLikes = dataSnapshot.getValue(Int::class.java)
//                                    if (currentPostLikes != null) {
//                                        postLikeRef.setValue(currentPostLikes - 1)
//                                            .addOnSuccessListener {
//                                                // Huỷ like thành công, cập nhật giao diện
//                                                holder.like.setCompoundDrawablesWithIntrinsicBounds(
//                                                    R.drawable.heart1, 0, 0, 0)
//                                                removeLikeNotification(post, currentUserId)
//                                            }
//                                    }
//                                }
//                                override fun onCancelled(databaseError: DatabaseError) {
//                                    // Xử lý khi có lỗi
//                                }
//                            })
//                        }
//                    }
//                }else{//nếu chưa like
//                    holder.like.setCompoundDrawablesWithIntrinsicBounds(
//                        R.drawable.heart1, 0, 0, 0)
//                    holder.like.setOnClickListener {
//                        likesRef.setValue(true).addOnSuccessListener {
//                            FirebaseDatabase.getInstance().reference
//                                .child("posts")
//                                .child(post.postId!!)
//                                .child("postLike")
//                                .setValue(post.postLike + 1).addOnSuccessListener {
//                                    holder.like.setCompoundDrawablesWithIntrinsicBounds(
//                                        R.drawable.heart2, 0, 0, 0)
//                                    //ktr nếu id người đăng khác với id người đang thao tác
//                                    if (post.postedBy != currentUserId){
//                                        sendLikeNotification(post, currentUserId)
//                                    }
//                                }
//                        }
//                    }
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                TODO("Not yet implemented")
//            }
//
//        })
//
//        holder.comment.setOnClickListener {
//            val intent = Intent(context, CommentActivity::class.java)
//            intent.putExtra("postId", post.postId)
//            intent.putExtra("postedBy", post.postedBy)
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//            context.startActivity(intent)
//        }
//
//        holder.more.setOnClickListener {
//            val bottomSheet = MoreOptionsBottomSheetPost(context, post)
//            bottomSheet.show((context as AppCompatActivity).supportFragmentManager, "MoreOptionsBottomSheetPost")
//        }
//
//        holder.layoutUser.setOnClickListener {
//            if (post.postedBy == FirebaseAuth.getInstance().uid){
//                (context as AppCompatActivity).supportFragmentManager
//                    .beginTransaction().replace(R.id.frameLayout, ProfileFragment()).commit()
//                // Chọn Tab tương ứng trong Bottom Navigation Bar
//                context.findViewById<ReadableBottomBar>(R.id.readableBottomBar)
//                    .selectItem(4)
//            }else{
//                val intent = Intent(context, ProfileActivity::class.java)
//                val bundle = Bundle()
//                bundle.putParcelable("user", holder.user)
//                intent.putExtras(bundle)
//                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                context.startActivity(intent)
//            }
//        }
//
//    }
//
//    private fun removeLikeNotification(post: Post, currentUserId: String) {
//        //xử lý xoá thông báo khi click huỷ like
//        //orderByChild("postId").equalTo(post.postId) để tìm kiếm các thông báo
//        // có thuộc tính "postId" bằng giá trị post.postId mong muốn.
//        FirebaseDatabase.getInstance().reference
//            .child("notification").child(post.postedBy!!)
//            .orderByChild("postId").equalTo(post.postId)
//            .addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    for (child in snapshot.children) {
//                        val notification = child.getValue(Notification::class.java)
//                        if (notification?.notificationBy == currentUserId && notification.type == "like") {
//                            child.ref.removeValue()
//                        }
//                    }
//                }
//
//                override fun onCancelled(error: DatabaseError) {}
//            })
//    }
//
//    private fun sendLikeNotification(post: Post, currentUserId: String) {
//        val notification = Notification().apply {
//            notificationBy = currentUserId
//            notificationAt = Date().time
//            postId = post.postId
//            postBy = post.postedBy
//            type = "like"
//        }
//
//        FirebaseDatabase.getInstance().reference
//            .child("notification").child(post.postedBy!!)
//            .push().setValue(notification)
//    }
//
//}