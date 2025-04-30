package com.htduc.socialmediaapplication.Activities

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.htduc.socialmediaapplication.Models.Note
import com.htduc.socialmediaapplication.Models.User
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.ViewModel.ChatViewModel
import com.htduc.socialmediaapplication.databinding.ActivityAddNoteBinding
import com.htduc.socialmediaapplication.ViewmodelFactories.ChatViewModelFactory
import com.squareup.picasso.Picasso

class AddNoteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddNoteBinding
    private lateinit var chatViewModel: ChatViewModel
    private var user: User? = null
    private var note: Note? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        chatViewModel = ViewModelProvider(this, ChatViewModelFactory(this))[ChatViewModel::class.java]

        binding.edtNote.requestFocus()
        binding.edtNote.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.edtNote, InputMethodManager.SHOW_IMPLICIT)
        }, 200) // Trễ 200ms để đảm bảo bàn phím mở đúng lúc


        user = intent.extras?.getParcelable("user")
        user?.let {
            if (!it.profilePhoto.isNullOrEmpty()){
                Picasso.get().load(it.profilePhoto).into(binding.profileImage)
            }
        }

        binding.btnClose.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.slide_down)
        }

        binding.shareNote.setOnClickListener {
            val content = binding.edtNote.text.toString().trim()
            chatViewModel.uploadNote(user?.uid!!, content){
                if (it){
                    finish()
                    overridePendingTransition(R.anim.fade_in, R.anim.slide_down)
                }
            }
        }

        binding.edtNote.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val description = binding.edtNote.text.toString()
                enableShareButton(description.isNotEmpty())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun enableShareButton(enable: Boolean){
        if (enable){
            binding.shareNote.isEnabled = true
            binding.shareNote.setTextColor(ContextCompat.getColor(this, R.color.green))
        } else {
            binding.shareNote.isEnabled = false
            binding.shareNote.setTextColor(ContextCompat.getColor(this, R.color.derkGrey))
        }
    }
}