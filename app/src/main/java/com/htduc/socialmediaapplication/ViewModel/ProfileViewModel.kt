package com.htduc.socialmediaapplication.ViewModel

import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.htduc.socialmediaapplication.Adapter.PostAdapter
import com.htduc.socialmediaapplication.Model.Follow
import com.htduc.socialmediaapplication.Model.Post

class ProfileViewModel:ViewModel() {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _listFollow = MutableLiveData<ArrayList<Follow>>()
    val listFollow: LiveData<ArrayList<Follow>> = _listFollow

    private val _currentUserPostList = MutableLiveData<ArrayList<Post>>()
    val currentUserPostList: LiveData<ArrayList<Post>> = _currentUserPostList

    private val _totalLikes = MutableLiveData<Int>()
    val totalLikes: LiveData<Int> = _totalLikes
    private val _countPosts = MutableLiveData<Int>()
    val countPosts: LiveData<Int> = _countPosts
    private val _status = MutableLiveData<String?>()
    val status: LiveData<String?> = _status

    fun fetchFollowers(id: String){
        database.reference.child("Users").child(id).child("followers")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val mListFollow = arrayListOf<Follow>()
                    mListFollow.clear()
                    if (snapshot.exists()){
                        for (dataSnap in snapshot.children){
                            val follow = dataSnap.getValue(Follow::class.java)
                            mListFollow.add(follow!!)
                        }
                        _listFollow.value = mListFollow
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    fun showUserPost(currentUserUid: String) {
        database.reference.child("posts").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val mCurrentUserPostList = arrayListOf<Post>()

                // Reset lại giá trị trước khi bắt đầu tính toán lại
                var totalLikes = 0
                var countPosts = 0

                if (snapshot.exists()) {
                    for (dataSnapshot in snapshot.children) {
                        val post = dataSnapshot.getValue(Post::class.java)
                        if (post?.postedBy == currentUserUid) {
                            post.postId = dataSnapshot.key
                            totalLikes += post.postLike
                            mCurrentUserPostList.add(post)
                            countPosts++
                        }
                    }
                    _currentUserPostList.value = mCurrentUserPostList
                    _totalLikes.value = totalLikes
                    _countPosts.value = countPosts
                } else {
                    // Nếu không có bài viết, set giá trị về 0
                    _currentUserPostList.value = arrayListOf()
                    _totalLikes.value = 0
                    _countPosts.value = 0
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Xử lý khi có lỗi
            }
        })
    }

    fun uploadProfilePhoto(profilePhoto: Uri, onUploadComplete: (Boolean)-> Unit){
        val reference = storage.reference.child("ProfilePhoto").child(auth.uid!!)
        reference.putFile(profilePhoto)
            .addOnSuccessListener { task ->
                task.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { url ->
                        val imageUrl = url.toString()
                        val uid = auth.uid
                        database.reference.child("Users").child(uid!!)
                            .child("profilePhoto").setValue(imageUrl)
                            .addOnCompleteListener {
                                onUploadComplete(true)
                            }
                    }
            }
    }

    fun uploadCoverPhoto(coverPhoto: Uri, onUploadComplete: (Boolean)-> Unit){
        val reference = storage.reference.child("coverPhoto").child(auth.uid!!)
        reference.putFile(coverPhoto)
            .addOnSuccessListener { task ->
                task.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { url ->
                        val imageUrl = url.toString()
                        val uid = auth.uid
                        database.reference.child("Users").child(uid!!)
                            .child("coverPhoto").setValue(imageUrl)
                            .addOnCompleteListener {
                                onUploadComplete(true)
                            }
                    }
            }
    }

    //Để chỉ update các trường dữ liệu có trong code trên mà không ảnh hưởng đến các trường khác
    // có thể sử dụng phương thức updateChildren() thay vì setValue().
    fun saveProfile(name: String, profession: String, phone: Int, birthday: String, gender: String,
                    profilePhoto: Uri?, coverPhoto: Uri?, onSaveComplete: (Boolean)-> Unit){
        //tạo 1 HashMap chứa các cặp key-value của các trường cần update
        val updates = HashMap<String, Any?>()
        updates["name"] = name
        updates["profession"] = profession
        updates["phone"] = phone
        updates["birthday"] = birthday
        updates["gender"] = gender

        if (profilePhoto != null) {
            uploadProfilePhoto(profilePhoto){}
        }
        if (coverPhoto != null) {
            uploadCoverPhoto(coverPhoto){}
        }

        database.reference.child("Users")
            .child(auth.uid!!)
            .updateChildren(updates)
            .addOnSuccessListener {
                onSaveComplete(true)
            }
    }


    fun setPresenceStatus(id: String) {
        //cập nhật trình trạng off và on
        database.reference.child("presence").child(id)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        val status = snapshot.getValue(String::class.java)
                        if (status != null){
                            _status.value = status
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }
}