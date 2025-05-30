package com.htduc.socialmediaapplication.Activities

import android.os.Bundle
import android.os.Message
import android.transition.TransitionInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.htduc.socialmediaapplication.Models.Messages
import com.htduc.socialmediaapplication.Models.Post
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.databinding.ActivityImageDetailBinding

class ImageDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // gọi trước setContentView để dùng transition
        window.sharedElementEnterTransition = TransitionInflater.from(this)
            .inflateTransition(android.R.transition.move)
        binding = ActivityImageDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val post = intent.getParcelableExtra<Post>("post")
        val message = intent.getParcelableExtra<Messages>("message")
        val comment_img = intent.getStringExtra("comment_img")

        if (post?.postImage != null) {
            Glide.with(this).load(post.postImage).into(binding.fullImageView)
        } else if(message?.imageUrl != null){
            Glide.with(this).load(message.imageUrl).into(binding.fullImageView)
        }else if (!comment_img.isNullOrEmpty()) {
            Glide.with(this).load(comment_img).into(binding.fullImageView)
        }


        binding.btnClose.setOnClickListener { finish() }
    }
}