package com.htduc.socialmediaapplication.Activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.htduc.socialmediaapplication.Model.Note
import com.htduc.socialmediaapplication.Model.User
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.ViewModel.ChatViewModel
import com.htduc.socialmediaapplication.bottomSheets.MoreOptionsBottomSheetNote
import com.htduc.socialmediaapplication.databinding.ActivityNoteBinding
import com.htduc.socialmediaapplication.factory.ChatViewModelFactory
import com.squareup.picasso.Picasso

class NoteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNoteBinding
    private lateinit var chatViewModel: ChatViewModel
    private var user: User? = null
    private var note: Note? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        chatViewModel = ViewModelProvider(this, ChatViewModelFactory(this))[ChatViewModel::class.java]

        user = intent.extras?.getParcelable("user")
        note = intent.extras?.getParcelable("note")

        user?.let {
            binding.username.text = it.name
            if (!it.profilePhoto.isNullOrEmpty()) {
                Picasso.get().load(it.profilePhoto).into(binding.profileImage)
                Picasso.get().load(it.profilePhoto).into(binding.avtImage)
            }
        }
        binding.txtNote.text = note?.content
        val formattedTime = chatViewModel.formatTimestamp(note?.timestamp!!)
        binding.timeNote.text = formattedTime

        binding.btnClose.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.slide_down)
        }
        binding.btnMore.setOnClickListener {
            val bottomSheet = MoreOptionsBottomSheetNote(this, note!!)
            bottomSheet.show(supportFragmentManager, "MoreOptionsBottomSheetNote")
        }
    }

}