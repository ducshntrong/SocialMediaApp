package com.htduc.socialmediaapplication.Activity

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.htduc.socialmediaapplication.Model.User
import com.htduc.socialmediaapplication.Model.applyClickAnimation
import com.htduc.socialmediaapplication.R
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
    private var mUser:User? = null
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
        currentId = auth.uid

        setProfileUser()
        binding.back.setOnClickListener { finish() }

        val pickCoverPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) {
            // Xử lý ảnh đã chọn ở đây
            if (it != null) {
                binding.coverPhoto.setImageURI(it)
                coverPhoto = it
            }
        }
        applyClickAnimation(this, binding.pickCoverPhoto) {
            pickCoverPhoto.launch("image/*")
        }
        val pickProfilePhoto = registerForActivityResult(ActivityResultContracts.GetContent()) {
            // Xử lý ảnh đã chọn ở đây
            if (it != null) {
                binding.profileImage.setImageURI(it)
                profilePhoto = it
            }
        }
        applyClickAnimation(this, binding.imgCamera){
            pickProfilePhoto.launch("image/*")
        }

        //Để chỉ update các trường dữ liệu có trong code trên mà không ảnh hưởng đến các trường khác
        // có thể sử dụng phương thức updateChildren() thay vì setValue().
        binding.save.setOnClickListener {
            val name = binding.nameEdt.text.toString()
            val profession = binding.profession.text.toString()
            val phone = binding.phone.text.toString().toInt()
            val birthday = binding.birthdayEdt.text.toString()
            val gender = binding.genderSpinner.selectedItem.toString()
            //tạo 1 HashMap chứa các cặp key-value của các trường cần update
            val updates = HashMap<String, Any?>()
            updates["name"] = name
            updates["profession"] = profession
            updates["phone"] = phone
            updates["birthday"] = birthday
            updates["gender"] = gender

            if (profilePhoto != null) {
                pickProfilePhoto()
            }
            if (coverPhoto != null) {
                pickCoverPhoto()
            }

            dialog?.show()
            database.reference.child("Users")
                .child(auth.uid!!)
                .updateChildren(updates)
                .addOnSuccessListener {
                    dialog?.dismiss()
                    finish()
                }
        }
    }

    private fun pickProfilePhoto() {
        val reference = storage.reference.child("ProfilePhoto").child(auth.uid!!)
        reference.putFile(profilePhoto!!)
            .addOnSuccessListener { task ->
                task.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { url ->
                        val imageUrl = url.toString()
                        val uid = auth.uid
                        database.reference.child("Users").child(uid!!)
                            .child("profilePhoto").setValue(imageUrl)
                    }
            }
    }

    private fun pickCoverPhoto() {
        val reference = storage.reference.child("CoverPhoto").child(auth.uid!!)
        reference.putFile(coverPhoto!!)
            .addOnSuccessListener { task ->
                task.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { url ->
                        val imageUrl = url.toString()
                        val uid = auth.uid
                        database.reference.child("Users").child(uid!!)
                            .child("coverPhoto").setValue(imageUrl)
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
                            mUser = user
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
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
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