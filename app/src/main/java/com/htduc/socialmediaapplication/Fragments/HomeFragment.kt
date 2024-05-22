package com.htduc.socialmediaapplication.Fragments

import android.app.ProgressDialog
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.firebase.storage.FirebaseStorage
import com.htduc.socialmediaapplication.Adapter.PostAdapter
import com.htduc.socialmediaapplication.Adapter.StoryAdapter
import com.htduc.socialmediaapplication.DeleteStoryJobService
import com.htduc.socialmediaapplication.Model.Post
import com.htduc.socialmediaapplication.Model.Story
import com.htduc.socialmediaapplication.Model.User
import com.htduc.socialmediaapplication.Model.UserStories
import com.htduc.socialmediaapplication.Model.applyClickAnimation
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.databinding.FragmentHomeBinding
import com.squareup.picasso.Picasso
import java.util.Date

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var storyAdapter: StoryAdapter
    private lateinit var postAdapter: PostAdapter
    private  var listStory = arrayListOf<Story>()
    private  var listPost = arrayListOf<Post>()
    private lateinit var storage: FirebaseStorage
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private var selectedImgStory: Uri? = null
    private var dialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentHomeBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        dialog = ProgressDialog(requireContext(), ProgressDialog.STYLE_SPINNER)
        dialog?.setTitle("Story Uploading")
        dialog?.setMessage("Please Wait...")
        dialog?.setCancelable(false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database.reference.child("Users").child(auth.uid!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        val user = snapshot.getValue(User::class.java)
                        if (user != null){
                            Picasso.get()
                                .load(user.profilePhoto)
                                .placeholder(R.drawable.avt)
                                .into(binding.profileImage)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })

        storyAdapter = StoryAdapter(requireContext(), listStory)
        binding.storyRv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, true)
        binding.storyRv.setHasFixedSize(true)
        binding.storyRv.setItemViewCacheSize(13)
        binding.storyRv.isNestedScrollingEnabled = false
        binding.storyRv.adapter = storyAdapter
        showListStory()

        binding.postRv.showShimmerAdapter()
        postAdapter = PostAdapter(requireContext(), listPost)
        binding.postRv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true)
        binding.postRv.isNestedScrollingEnabled = false
        showListPost()

        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri->
            selectedImgStory = uri
            if (selectedImgStory != null){
                binding.storyImg.setImageURI(uri)
                uploadStory()
            }
        }
        applyClickAnimation(requireContext(), binding.btnAddStory){
            galleryLauncher.launch("image/*")
        }
        binding.addStoryImg.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
    }

    private fun showListStory() {
        database.reference.child("stories")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    listStory.clear()
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
                            listStory.add(story)
                        }
                        storyAdapter.setStoryList(listStory)
                        binding.storyRv.scrollToPosition(storyAdapter.itemCount - 1)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    private fun uploadStory() {
        val reference = storage.reference.child("stories")
            .child(auth.uid!!).child(Date().time.toString())
        dialog?.show()
        reference.putFile(selectedImgStory!!).addOnSuccessListener { task->
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
                            dialog?.dismiss()
                            //Lập lịch để xoá story sau 24h đăng
                            val bundle = PersistableBundle()
                            bundle.putString("storyId", storyId)
                            bundle.putString("userId", auth.uid)
                            val jobScheduler = requireContext().getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                            val componentName = ComponentName(requireContext(), DeleteStoryJobService::class.java)
                            val jobInfo = JobInfo.Builder(storyId.hashCode(), componentName)
                                .setMinimumLatency(24*60*60*1000)//đặt time là 24h
                                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                                .setExtras(bundle)
                                .build()
                            jobScheduler.schedule(jobInfo)
                        }
                }
        }
    }

    private fun showListPost() {
        database.reference.child("posts")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    listPost.clear()
                    if (snapshot.exists()){
                        for (dataSnap in snapshot.children){
                            val post = dataSnap.getValue(Post::class.java)
                            post!!.postId = dataSnap.key //laays id cuar post
                            listPost.add(post)
                        }
                        binding.postRv.adapter = postAdapter
                        binding.postRv.hideShimmerAdapter()
                        postAdapter.setPostList(listPost)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }


}