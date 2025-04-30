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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.htduc.socialmediaapplication.Models.applyClickAnimation
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.ViewModel.FragmentViewModel
import com.htduc.socialmediaapplication.ViewmodelFactories.FragmentViewModelFactory
import com.htduc.socialmediaapplication.databinding.FragmentAddPostBinding
import com.squareup.picasso.Picasso


class AddPostFragment : Fragment() {
    private lateinit var binding: FragmentAddPostBinding
    private var selectedImage: Uri? = null
    private var dialog: ProgressDialog? = null
    private lateinit var fragmentViewModel: FragmentViewModel
    private lateinit var auth: FirebaseAuth

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
        fragmentViewModel = ViewModelProvider(
            this,
            FragmentViewModelFactory(requireActivity().application, requireContext())
        )[FragmentViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentViewModel.user.observe(viewLifecycleOwner){user->
            if (user!=null){
                Picasso.get()
                    .load(user.profilePhoto)
                    .placeholder(R.drawable.avt)
                    .into(binding.profileImage)
                binding.username.text = user.name
                binding.profession.text = user.profession
            }
        }
        fragmentViewModel.setProfileUser(auth.uid!!)

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
            dialog?.show()
            fragmentViewModel.uploadPost(selectedImage,binding.postDescription.text.toString()){isSuccess->
                if (isSuccess){
                    dialog?.dismiss()
                    binding.postDescription.setText("")
                    binding.postImg.visibility = View.GONE
                }
            }
        }

    }
}