package com.htduc.socialmediaapplication.bottomSheets

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.htduc.socialmediaapplication.Model.Note
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.ViewModel.ChatViewModel
import com.htduc.socialmediaapplication.databinding.BottomSheetNoteBinding
import com.htduc.socialmediaapplication.databinding.BottomSheetPostBinding
import com.htduc.socialmediaapplication.factory.ChatViewModelFactory

class MoreOptionsBottomSheetNote(private val context: Context, private val note: Note) : BottomSheetDialogFragment(){
    private lateinit var  binding: BottomSheetNoteBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var chatViewModel: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = BottomSheetNoteBinding.inflate(layoutInflater, container, false)
        chatViewModel = ViewModelProvider(this, ChatViewModelFactory(
            requireActivity().application))[ChatViewModel::class.java]

        auth = FirebaseAuth.getInstance()
        val currentUserId = auth.currentUser?.uid

        if (currentUserId != note.userId){
            binding.report.visibility = View.VISIBLE
            binding.block.visibility = View.VISIBLE
            binding.notInterested.visibility = View.VISIBLE
            binding.delete.visibility = View.GONE
        }

        binding.delete.setOnClickListener {
            chatViewModel.deleteNote(note.userId!!){
                if (it){
                    dismiss()
                    requireActivity().finish()
                    (requireActivity() as? AppCompatActivity)?.overridePendingTransition(R.anim.fade_in, R.anim.slide_down)
                }
            }
        }

        return binding.root
    }
}