package com.htduc.socialmediaapplication.ViewModel

import com.htduc.socialmediaapplication.moderation.NSFWDetector
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.htduc.socialmediaapplication.Models.Comment
import com.htduc.socialmediaapplication.Models.Notification
import com.htduc.socialmediaapplication.Models.Post
import com.htduc.socialmediaapplication.moderation.UserModerationManager
import java.util.Date

class CommentViewModel(private val context: Context): ViewModel() {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val nsfwDetector = NSFWDetector(context)

    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?> = _post

    private val _listComment = MutableLiveData<ArrayList<Comment>>()
    val listComment: LiveData<ArrayList<Comment>> = _listComment

    private val userModerationManager = UserModerationManager(database, context)

    fun getDataOfPost(postId: String){
        database.reference.child("posts").child(postId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val post = snapshot.getValue(Post::class.java)
                    if (post != null) {
                        _post.value = post
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    fun showListComment(postId: String) {
        database.reference
            .child("posts")
            .child(postId)
            .child("comments").addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val mListComment = arrayListOf<Comment>()
                    mListComment.clear()
                    if (snapshot.exists()){
                        for (dataSnap in snapshot.children){
                            val comment = dataSnap.getValue(Comment::class.java)
                            if (comment != null) {
                                mListComment.add(comment)
                            }
                        }
                        _listComment.value = mListComment
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    fun sendComment(selectedImgCmt: Uri?, edtMessage: String, postId: String, postedBy: String,
                            onSaveCmtComplete: (Boolean)->Unit) {
        try {
            if (selectedImgCmt != null){
                val nsfwScore = nsfwDetector.detectNSFW(context, selectedImgCmt)
                if (nsfwScore > 0.70){
                    userModerationManager.showDialogViolation()
                    userModerationManager.handleViolation(auth.uid!!)
                    onSaveCmtComplete(true)
                    return
                }
                val reference = storage.reference.child("comments")
                    .child(auth.uid!!).child(Date().time.toString())
                reference.putFile(selectedImgCmt)
                    .addOnSuccessListener { task ->
                        task.metadata!!.reference!!.downloadUrl
                            .addOnSuccessListener { url ->
                                val imageUrl = url.toString()
                                val comment = Comment(edtMessage, Date().time, auth.uid, imageUrl)
                                database.reference
                                    .child("posts")
                                    .child(postId)
                                    .child("comments")
                                    .push()
                                    .setValue(comment).addOnSuccessListener {
                                        val commentCountRef = database.reference
                                            .child("posts")
                                            .child(postId)
                                            .child("commentCount")
                                        commentCountRef.addListenerForSingleValueEvent(object : ValueEventListener{
                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                var commentCount = 0
                                                if (snapshot.exists()){
                                                    commentCount = snapshot.getValue(Int::class.java)!!
                                                }
                                                commentCountRef.setValue(commentCount+1)
                                                    .addOnSuccessListener {
                                                        onSaveCmtComplete(true)

                                                        sendNotification(postedBy, postId)
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
                val comment = Comment(edtMessage, Date().time, auth.uid, "")
                database.reference
                    .child("posts")
                    .child(postId)
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
                                        onSaveCmtComplete(true)
                                        sendNotification(postedBy, postId)
                                    }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                TODO("Not yet implemented")
                            }
                        })
                    }
            }
        } catch (e: Exception){
            e.printStackTrace()
            Toast.makeText(context, "Loi xu ly anh!", Toast.LENGTH_SHORT).show()
            onSaveCmtComplete(true)
        }
    }

    private fun sendNotification(postedBy: String, postId: String){
        if (postedBy != auth.uid){ //ktr nếu id người đăng khác với id người đang thao tác
            val notification = Notification()
            notification.notificationBy = auth.uid
            notification.notificationAt = Date().time
            notification.postId = postId
            notification.postBy = postedBy //id cua ng dang
            notification.type = "comment"
            FirebaseDatabase.getInstance().reference
                .child("notification")
                .child(postedBy)
                .push()
                .setValue(notification)
        }
    }
}