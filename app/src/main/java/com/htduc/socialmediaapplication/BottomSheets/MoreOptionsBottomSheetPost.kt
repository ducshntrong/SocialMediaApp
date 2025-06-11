package com.htduc.socialmediaapplication.BottomSheets

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.htduc.socialmediaapplication.Activities.UpdatePostActivity
import com.htduc.socialmediaapplication.Models.Post
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.ViewModel.MainViewModel
import com.htduc.socialmediaapplication.ViewmodelFactories.MainViewModelFactory
import com.htduc.socialmediaapplication.databinding.BottomSheetPostBinding

class MoreOptionsBottomSheetPost(private val context: Context, private val post: Post) : BottomSheetDialogFragment(){
    private lateinit var  binding: BottomSheetPostBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var fragmentViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = BottomSheetPostBinding.inflate(layoutInflater, container, false)
        fragmentViewModel = ViewModelProvider(
            this,
            MainViewModelFactory(requireActivity().application, requireContext())
        )[MainViewModel::class.java]
        auth = FirebaseAuth.getInstance()
        val currentUserId = auth.currentUser?.uid

        if (currentUserId != post.postedBy){
            binding.edit.visibility = View.GONE
            binding.delete.visibility = View.GONE
            binding.infor.visibility = View.VISIBLE
            binding.block.visibility = View.VISIBLE
            binding.report.visibility = View.VISIBLE
            binding.notInterested.visibility = View.VISIBLE
        }

        binding.edit.setOnClickListener {
            val intent = Intent(context, UpdatePostActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable("post_data", post)
            intent.putExtras(bundle)
            context.startActivity(intent)
            dismiss()
        }
        binding.delete.setOnClickListener {
            dismiss()
            AlertDialog.Builder(context)
                .setTitle("Xoá bài viết")
                .setMessage("Bạn có chắc chắn muốn xoá bài viết này?")
                .setPositiveButton("Xoá"){ dialogInterface: DialogInterface, i: Int ->
                    fragmentViewModel.deletePost(post.postId!!, post.postImage){ isDeleted ->
                        if (isDeleted) {
                            Toast.makeText(requireContext(), "Xoá bài viết thành công!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Lỗi khi xoá bài viết!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Huỷ", null)
                .show()
        }
        return binding.root
    }
}