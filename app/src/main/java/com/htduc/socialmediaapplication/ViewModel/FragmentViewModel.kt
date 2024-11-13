package com.htduc.socialmediaapplication.ViewModel

import android.annotation.SuppressLint
import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.PersistableBundle
import android.view.View
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
import com.htduc.socialmediaapplication.DeleteStoryJobService
import com.htduc.socialmediaapplication.Model.Notification
import com.htduc.socialmediaapplication.Model.Post
import com.htduc.socialmediaapplication.Model.Story
import com.htduc.socialmediaapplication.Model.User
import com.htduc.socialmediaapplication.Model.UserStories
import java.util.Date

class FragmentViewModel(application: Application): ViewModel() {
    @SuppressLint("StaticFieldLeak")
    private val context: Context = application.applicationContext
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _listStory = MutableLiveData<ArrayList<Story>>()
    val listStory : LiveData<ArrayList<Story>> = _listStory

    private val _listPost = MutableLiveData<ArrayList<Post>>()
    val listPost : LiveData<ArrayList<Post>> = _listPost

    private val _listNotification = MutableLiveData<ArrayList<Notification>>()
    val listNotification : LiveData<ArrayList<Notification>> = _listNotification

    private val _listUser = MutableLiveData<ArrayList<User>>()
    val listUser : LiveData<ArrayList<User>> = _listUser

    init {
        showListStory()
        showListPost()
        fetchListNotification()
        showListUser()
    }

    fun setProfileUser(id: String){
        database.reference.child("Users").child(id)
            .addValueEventListener(object :ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        val user = snapshot.getValue(User::class.java)
                        _user.value = user
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    private fun showListStory() {
        database.reference.child("stories")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val mListStory = arrayListOf<Story>()
                    mListStory.clear()
                    if (snapshot.exists()){
                        for (dataSnap in snapshot.children){
                            val story = Story()
                            story.storyBy = dataSnap.key //lấy id của người dùng đã up story
                            //story.storyAt = dataSnap.child("postedBy").getValue(Long::class.java)!!

                            val stories = ArrayList<UserStories>()
                            for (snapshot1 in dataSnap.child("userStories").children){
                                val userStories = snapshot1.getValue(UserStories::class.java)
                                stories.add(userStories!!)
                            }
                            story.stories = stories
                            story.stories!!.reverse()
                            mListStory.add(story)
                        }
                        _listStory.value = mListStory
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }
    private fun showListPost() {
        database.reference.child("posts")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val mListPost = arrayListOf<Post>()
                    mListPost.clear()
                    if (snapshot.exists()){
                        for (dataSnap in snapshot.children){
                            val post = dataSnap.getValue(Post::class.java)
                            post!!.postId = dataSnap.key //laays id cuar post
                            mListPost.add(post)
                        }
                        _listPost.value = mListPost
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    fun uploadStory(selectedImgStory: Uri, onUploadComplete: (Boolean) -> Unit) {
        val reference = storage.reference.child("stories")
            .child(auth.uid!!).child(Date().time.toString())
        reference.putFile(selectedImgStory).addOnSuccessListener { task->
            task.metadata!!.reference!!.downloadUrl
                .addOnSuccessListener { url->
                    val imgUrl = url.toString()
                    val story = Story()
                    story.storyAt = Date().time
                    val databaseStoryRef = database.reference
                        .child("stories")
                        .child(auth.uid!!)
                    val stories = UserStories(imgUrl, story.storyAt)
                    val storyId = databaseStoryRef.child("userStories").push().key
                    databaseStoryRef.child("userStories")
                        .child(storyId!!)
                        .setValue(stories).addOnSuccessListener {
                            //Lập lịch để xoá story sau 24h đăng
                            scheduleDeleteStoryJob(storyId, auth.uid!!)
                            onUploadComplete(true)
                        }.addOnFailureListener {
                            onUploadComplete(false)
                        }
                }
        }
    }
    private fun scheduleDeleteStoryJob(storyId: String, userId: String) {
        val bundle = PersistableBundle()
        bundle.putString("storyId", storyId)
        bundle.putString("userId", userId)
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val componentName = ComponentName(context, DeleteStoryJobService::class.java)
        val jobInfo = JobInfo.Builder(storyId.hashCode(), componentName)
            .setMinimumLatency(24 * 60 * 60 * 1000) // 24 giờ
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setExtras(bundle)
            .build()
        jobScheduler.schedule(jobInfo)
    }

    private fun fetchListNotification(){
        database.reference.child("notification").child(auth.uid!!)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val mListNotification = arrayListOf<Notification>()
                    mListNotification.clear()
                    if (snapshot.exists()){
                        for (dataSnap in snapshot.children){
                            val notification = dataSnap.getValue(Notification::class.java)
                            if (notification != null){
                                notification.notificationId = dataSnap.key
                                mListNotification.add(notification)
                            }
                        }
                        _listNotification.value = mListNotification
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    private fun showListUser() {
        database.reference.child("Users")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    val mListUser = arrayListOf<User>()
                    mListUser.clear()
                    if (snapshot.exists()){
                        for (snapshot1 in snapshot.children){
                            val user = snapshot1.getValue(User::class.java)
                            if (!(user!!.uid).equals(auth.uid))
                                mListUser.add(user)
                        }
                        _listUser.value = mListUser
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    fun uploadPost(selectedImage: Uri?, postDescription: String, onUploadComplete: (Boolean) -> Unit) {
        val reference = storage.reference.child("posts").child(auth.uid!!)
            .child(Date().time.toString())

        if (selectedImage != null) {
            reference.putFile(selectedImage)
                .addOnSuccessListener { task ->
                    task.metadata?.reference?.downloadUrl
                        ?.addOnSuccessListener { url ->
                            val imageUrl = url.toString()
                            val post = Post()
                            post.postImage = imageUrl
                            post.postedBy = auth.uid
                            post.postDescription = postDescription
                            post.postedAt = Date().time

                            database.reference.child("posts")
                                .push()//push() được sd để tạo một khóa con duy nhất cho một nút trong csdl
                                .setValue(post)
                                .addOnSuccessListener {
                                    onUploadComplete(true)
                                }
                        }
                }
        } else {
            val post = Post()
            post.postImage = ""
            post.postedBy = auth.uid
            post.postDescription = postDescription
            post.postedAt = Date().time

            database.reference.child("posts")
                .push()
                .setValue(post)
                .addOnSuccessListener {
                    onUploadComplete(true)
                }
        }
    }
}