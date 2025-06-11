package com.htduc.socialmediaapplication.Activities

import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.htduc.socialmediaapplication.Adapters.UserChatAdapter
import com.htduc.socialmediaapplication.Adapters.ChatListAdapter
import com.htduc.socialmediaapplication.Models.Chats
import com.htduc.socialmediaapplication.ViewModel.ChatViewModel
import com.htduc.socialmediaapplication.ViewModel.MainViewModel
import com.htduc.socialmediaapplication.databinding.ActivityMessageBinding
import com.htduc.socialmediaapplication.ViewmodelFactories.ChatViewModelFactory
import com.htduc.socialmediaapplication.ViewmodelFactories.MainViewModelFactory

class MessageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMessageBinding
    private lateinit var userAdapter: UserChatAdapter
    private lateinit var chatListAdapter: ChatListAdapter
    private lateinit var fragmentViewModel: MainViewModel
    private lateinit var chatViewModel: ChatViewModel
    private val chatList = ArrayList<Chats>()
    private lateinit var auth: FirebaseAuth
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private var selectedImg: Uri? = null
    private var dialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dialog = ProgressDialog(this, ProgressDialog.STYLE_SPINNER)
        dialog?.setTitle("Story Uploading")
        dialog?.setMessage("Please Wait...")
        dialog?.setCancelable(false)

        auth = FirebaseAuth.getInstance()
        fragmentViewModel = ViewModelProvider(
            this,
            MainViewModelFactory(application, this)
        )[MainViewModel::class.java]
        chatViewModel = ViewModelProvider(this, ChatViewModelFactory(this))[ChatViewModel::class.java]

        binding.imgBack.setOnClickListener { finish() }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()){ uri ->
            selectedImg = uri
            if (selectedImg != null){
                dialog?.show()
                fragmentViewModel.uploadStory(selectedImg!!) { isSuccess ->
                    if (isSuccess) dialog?.dismiss()
                }
            }
        }

        setupRecyclerViews()
        observeViewModels()

        chatViewModel.fetchChatList()
    }

    private fun setupRecyclerViews() {
        userAdapter = UserChatAdapter(this) {
            galleryLauncher.launch("image/*")
        }
        binding.rvUser.adapter = userAdapter

        chatListAdapter = ChatListAdapter(this, chatList)
        binding.rvChat.layoutManager = LinearLayoutManager(this)
        binding.rvChat.adapter = chatListAdapter
    }

    private fun observeViewModels() {
        fragmentViewModel.listUser.observe(this) {
            userAdapter.setUserList(it)
        }

        chatViewModel.chatList.observe(this) { chatList ->
            chatListAdapter.setUserChatList(chatList)
        }
    }
}