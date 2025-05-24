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
import androidx.lifecycle.ViewModelProviders
import com.htduc.socialmediaapplication.Models.Post
import com.htduc.socialmediaapplication.Models.applyClickAnimation
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.ViewModel.CommentViewModel
import com.htduc.socialmediaapplication.ViewmodelFactories.CommentViewmodelFactory
import com.htduc.socialmediaapplication.ViewModel.FragmentViewModel
import com.htduc.socialmediaapplication.ViewmodelFactories.FragmentViewModelFactory
import com.htduc.socialmediaapplication.databinding.ActivityUpdatePostBinding
import com.squareup.picasso.Picasso

class UpdatePostActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUpdatePostBinding
    private lateinit var fragmentViewModel: FragmentViewModel
    private lateinit var commentViewModel: CommentViewModel
    private lateinit var post :Post
    private var selectImg: Uri? = null
    private var dialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdatePostBinding.inflate(layoutInflater)
        dialog = ProgressDialog(this)
        dialog?.setTitle("Post Uploading")
        dialog?.setMessage("Please wait...")
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        setContentView(binding.root)

        post = intent.getParcelableExtra("post_data")!!
        fragmentViewModel = ViewModelProvider(
            this,
            FragmentViewModelFactory(application, this)
        )[FragmentViewModel::class.java]
        commentViewModel = ViewModelProviders.of(this,
            CommentViewmodelFactory(this.application)
        )[CommentViewModel::class.java]

        fragmentViewModel.user.observe(this){ user ->
            if (user !== null){
                Picasso.get()
                    .load(user.profilePhoto)
                    .placeholder(R.drawable.avt)
                    .into(binding.profileImage)
                binding.username.text = user.name
                binding.profession.text = user.profession
            }
        }
        fragmentViewModel.setProfileUser(post.postedBy!!)
        setDataToView()

        val imgPickCallback = registerForActivityResult(ActivityResultContracts.GetContent()){ uri: Uri? ->
            selectImg = uri
            if (selectImg != null){
                binding.postImg.setImageURI(uri)
                binding.postImg.visibility = View.VISIBLE
                binding.btnPost.background = ContextCompat.getDrawable(applicationContext, R.drawable.follow_btn_bg)
                binding.btnPost.setTextColor(applicationContext.resources.getColor(R.color.white))
                binding.btnPost.isEnabled = true
            }
        }

        applyClickAnimation(applicationContext, binding.pickImg){
            imgPickCallback.launch("image/*")
        }

        binding.btnPost.setOnClickListener{
            dialog?.show()
            val newDescription = binding.postDescription.text.toString()
            fragmentViewModel.updatePost(post.postId!!, selectImg, newDescription){ isSuccess ->
                if (isSuccess){
                    dialog?.dismiss()
                    finish()
                }
            }
        }

        binding.imgBack.setOnClickListener { finish() }

        binding.postDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val description = binding.postDescription.text.toString()
                if (description.isNotEmpty() || selectImg != null){
                    binding.btnPost.background = ContextCompat.getDrawable(applicationContext, R.drawable.follow_btn_bg)
                    binding.btnPost.setTextColor(applicationContext.resources.getColor(R.color.white))
                    binding.btnPost.isEnabled = true
                }else{
                    binding.btnPost.background = ContextCompat.getDrawable(applicationContext, R.drawable.follow_action_btn)
                    binding.btnPost.setTextColor(applicationContext.resources.getColor(R.color.grey))
                    binding.btnPost.isEnabled = false
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })
    }

    private fun setDataToView() {
        commentViewModel.post.observe(this){ post ->
            if (post != null){
                if (post.postImage.equals("")){
                    binding.postImg.visibility = View.GONE
                }else{
                    Picasso.get()
                        .load(post.postImage)
                        .placeholder(R.drawable.placeholder)
                        .into(binding.postImg);
                    binding.postImg.visibility = View.VISIBLE
                }
                val description = post.postDescription
                if (description.equals("")){
                    binding.postDescription.visibility = View.GONE
                }else{
                    binding.postDescription.setText(description)
                    binding.postDescription.visibility = View.VISIBLE
                }
            }
        }
        commentViewModel.getDataOfPost(post.postId!!)
    }
}