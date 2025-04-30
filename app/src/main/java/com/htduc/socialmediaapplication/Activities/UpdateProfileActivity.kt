package com.htduc.socialmediaapplication.Activities

import android.app.ProgressDialog
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.htduc.socialmediaapplication.Models.applyClickAnimation
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.ViewModel.FragmentViewModel
import com.htduc.socialmediaapplication.ViewmodelFactories.FragmentViewModelFactory
import com.htduc.socialmediaapplication.ViewModel.ProfileViewModel
import com.htduc.socialmediaapplication.databinding.ActivityUpdateProfileBinding
import com.squareup.picasso.Picasso

class UpdateProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUpdateProfileBinding
    private lateinit var storage: FirebaseStorage
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var coverPhoto: Uri? = null
    private var profilePhoto: Uri? = null
    private var dialog: ProgressDialog? = null
    private var currentId: String? = null
    private lateinit var fragmentViewModel: FragmentViewModel
    private lateinit var profileViewModel: ProfileViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dialog = ProgressDialog(this)
        dialog?.setMessage("Updating Profile...")
        dialog?.setCancelable(false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        fragmentViewModel = ViewModelProvider(
            this,
            FragmentViewModelFactory(application, this)
        )[FragmentViewModel::class.java]
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        currentId = auth.uid

        fragmentViewModel.user.observe(this){user->
            if (user != null){
                Picasso.get()
                    .load(user.profilePhoto)
                    .placeholder(R.drawable.avt)
                    .into(binding.profileImage)
                Picasso.get()
                    .load(user.coverPhoto)
                    .placeholder(R.drawable.placeholder)
                    .into(binding.coverPhoto)
                binding.nameEdt.setText(user.name)
                binding.profession.setText(user.profession)
                binding.phone.setText(user.phone.toString())
                binding.birthdayEdt.setText(user.birthday)
                // Chọn giá trị giới tính của người dùng
                val selectedGender = user.gender
                val genderPosition = resources.getStringArray(R.array.gender_options).indexOf(selectedGender)
                binding.genderSpinner.setSelection(genderPosition)
            }
        }
        fragmentViewModel.setProfileUser(auth.uid!!)
        binding.back.setOnClickListener { finish() }

        val pickCoverPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                binding.coverPhoto.setImageURI(it)
                coverPhoto = it
            }
        }
        applyClickAnimation(this, binding.pickCoverPhoto) {
            pickCoverPhoto.launch("image/*")
        }
        val pickProfilePhoto = registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                binding.profileImage.setImageURI(it)
                profilePhoto = it
            }
        }
        applyClickAnimation(this, binding.imgCamera){
            pickProfilePhoto.launch("image/*")
        }

        binding.save.setOnClickListener {
            val name = binding.nameEdt.text.toString()
            val profession = binding.profession.text.toString()
            val phone = binding.phone.text.toString().toInt()
            val birthday = binding.birthdayEdt.text.toString()
            val gender = binding.genderSpinner.selectedItem.toString()

            dialog?.show()
            profileViewModel.saveProfile(name, profession, phone, birthday, gender,
                profilePhoto, coverPhoto){
                if (it){
                    dialog?.dismiss()
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        database.reference.child("presence").child(currentId!!).setValue("Online")
    }

    override fun onPause() {
        super.onPause()
        database.reference.child("presence").child(currentId!!).setValue("Offline")
    }
}