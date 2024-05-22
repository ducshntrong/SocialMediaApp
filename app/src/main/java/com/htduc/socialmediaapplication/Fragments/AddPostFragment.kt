package com.htduc.socialmediaapplication.Fragments

import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.htduc.socialmediaapplication.Model.Post
import com.htduc.socialmediaapplication.Model.User
import com.htduc.socialmediaapplication.Model.applyClickAnimation
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.databinding.FragmentAddPostBinding
import com.squareup.picasso.Picasso
import java.util.Date


class AddPostFragment : Fragment() {
    private lateinit var binding: FragmentAddPostBinding
    private lateinit var storage: FirebaseStorage
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var selectedImage: Uri? = null
    private var dialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentAddPostBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        dialog = ProgressDialog(requireContext(), ProgressDialog.STYLE_SPINNER)
        dialog?.setTitle("Post Uploading")
        dialog?.setMessage("Please Wait...")
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setProfileUser()

        val imagePickCallback = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            selectedImage = uri
            // Xử lý ảnh đã chọn ở đây
            if (selectedImage != null) {
                binding.postImg.setImageURI(selectedImage)
                binding.postImg.visibility = View.VISIBLE
                binding.btnPost.background = ContextCompat.getDrawable(requireContext(), R.drawable.follow_btn_bg)
                binding.btnPost.setTextColor(requireContext().resources.getColor(R.color.white))
                binding.btnPost.isEnabled = true
            }
        }
        applyClickAnimation(requireContext(), binding.pickImg){
            imagePickCallback.launch("image/*")
        }

        binding.postDescription.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val description = binding.postDescription.text.toString()
                if (description.isNotEmpty() || selectedImage != null){
                    binding.btnPost.background = ContextCompat.getDrawable(context!!, R.drawable.follow_btn_bg)
                    binding.btnPost.setTextColor(context!!.resources.getColor(R.color.white))
                    binding.btnPost.isEnabled = true
                }else{
                    binding.btnPost.background = ContextCompat.getDrawable(context!!, R.drawable.follow_action_btn)
                    binding.btnPost.setTextColor(context!!.resources.getColor(R.color.grey))
                    binding.btnPost.isEnabled = false
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })

        binding.btnPost.setOnClickListener {
            uploadPost()
        }

    }

    private fun uploadPost() {
        val reference = storage.reference.child("posts").child(auth.uid!!)
            .child(Date().time.toString())
        dialog?.show()

        if (selectedImage != null) {
            reference.putFile(selectedImage!!)
                .addOnSuccessListener { task ->
                    task.metadata?.reference?.downloadUrl
                        ?.addOnSuccessListener { url ->
                            val imageUrl = url.toString()
                            val post = Post()
                            post.postImage = imageUrl
                            post.postedBy = auth.uid
                            post.postDescription = binding.postDescription.text.toString()
                            post.postedAt = Date().time

                            database.reference.child("posts")
                                .push()//push() được sd để tạo một khóa con duy nhất cho một nút trong csdl
                                .setValue(post)
                                .addOnSuccessListener {
                                    dialog?.dismiss()
                                    Toast.makeText(requireContext(), "Posted Successfully", Toast.LENGTH_SHORT).show()
                                    binding.postDescription.setText("")
                                    binding.postImg.visibility = View.GONE
                                }
                        }
                }
        } else {
            val post = Post()
            post.postImage = ""
            post.postedBy = auth.uid
            post.postDescription = binding.postDescription.text.toString()
            post.postedAt = Date().time

            database.reference.child("posts")
                .push()
                .setValue(post)
                .addOnSuccessListener {
                    dialog?.dismiss()
                    Toast.makeText(requireContext(), "Posted Successfully", Toast.LENGTH_SHORT).show()
                    binding.postDescription.setText("")
                }
        }
    }

    private fun setProfileUser() {
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
                            binding.username.text = user.name
                            binding.profession.text = user.profession
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

}