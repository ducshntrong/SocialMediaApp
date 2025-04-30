package com.htduc.socialmediaapplication.Activities

import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.htduc.socialmediaapplication.Models.applyClickAnimation
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.ViewModel.FragmentViewModel
import com.htduc.socialmediaapplication.ViewmodelFactories.FragmentViewModelFactory
import com.htduc.socialmediaapplication.databinding.ActivityAddPostBinding
import com.squareup.picasso.Picasso

class AddPostActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddPostBinding
    private var selectedImage: Uri? = null
    private var dialog: ProgressDialog? = null
    private lateinit var fragmentViewModel: FragmentViewModel
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo ProgressDialog
        dialog = ProgressDialog(this, ProgressDialog.STYLE_SPINNER).apply {
            setTitle("Post Uploading")
            setMessage("Please Wait...")
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }

        // Khởi tạo Firebase Auth và ViewModel
        auth = FirebaseAuth.getInstance()
        fragmentViewModel = ViewModelProvider(
            this,
            FragmentViewModelFactory(application, this)
        )[FragmentViewModel::class.java]

        // Lấy dữ liệu user từ ViewModel
        fragmentViewModel.user.observe(this) { user ->
            user?.let {
                Picasso.get()
                    .load(it.profilePhoto)
                    .placeholder(R.drawable.avt)
                    .into(binding.profileImage)
                binding.username.text = it.name
                binding.profession.text = it.profession
            }
        }
        fragmentViewModel.setProfileUser(auth.uid!!)

        // Mở bộ chọn ảnh
        val imagePickCallback = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            selectedImage = uri
            selectedImage?.let {
                binding.postImg.setImageURI(it)
                binding.postImg.visibility = View.VISIBLE
                enablePostButton(true)
            }
        }
        applyClickAnimation(this, binding.pickImg) {
            imagePickCallback.launch("image/*")
        }

        binding.imgBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.slide_down)
        }

        // Lắng nghe thay đổi văn bản mô tả bài viết
        binding.postDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val description = binding.postDescription.text.toString()
                enablePostButton(description.isNotEmpty() || selectedImage != null)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Xử lý sự kiện nút đăng bài
        binding.btnPost.setOnClickListener {
            dialog?.show()
            fragmentViewModel.uploadPost(selectedImage, binding.postDescription.text.toString()) { isSuccess ->
                if (isSuccess) {
                    dialog?.dismiss()
                    binding.postDescription.setText("")
                    binding.postImg.visibility = View.GONE
                    enablePostButton(false)
                }
            }
        }
    }

    private fun enablePostButton(enable: Boolean) {
        if (enable) {
            binding.btnPost.background = ContextCompat.getDrawable(this, R.drawable.follow_btn_bg)
            binding.btnPost.setTextColor(resources.getColor(R.color.white))
            binding.btnPost.isEnabled = true
        } else {
            binding.btnPost.background = ContextCompat.getDrawable(this, R.drawable.follow_action_btn)
            binding.btnPost.setTextColor(resources.getColor(R.color.grey))
            binding.btnPost.isEnabled = false
        }
    }
}
