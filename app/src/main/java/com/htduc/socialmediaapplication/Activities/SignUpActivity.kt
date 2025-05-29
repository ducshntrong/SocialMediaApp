package com.htduc.socialmediaapplication.Activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.htduc.socialmediaapplication.Models.User
import com.htduc.socialmediaapplication.databinding.ActivitySignUpBinding

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binding.login.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.btnSignup.setOnClickListener {
            signUp()
        }
    }

    private fun signUp() {
        val email = binding.emailEdt.text.toString().trim()
        val password = binding.passEdt.text.toString().trim()
        val name = binding.nameEdt.text.toString().trim()
        val profession = binding.professionEdt.text.toString().trim()

        if (email.isEmpty()){
            binding.emailEdt.error = "Please type your email"
            return
        }else if (password.isEmpty()){
            binding.passEdt.error = "Please type your password"
            return
        }else if (name.isEmpty()){
            binding.nameEdt.error = "Please type your name"
            return
        }else if (profession.isEmpty()){
            binding.professionEdt.error = "Please type your username"
            return
        }else if (name.length > 20){
            binding.nameEdt.error = "Name must be less than 30 characters"
        }else if (profession.length > 20){
            binding.nameEdt.error = "Username must be less than 20 characters"
        }

        // Kiểm tra profession có bị trùng không
        val usersRef = database.reference.child("Users")
        usersRef.orderByChild("profession").equalTo(profession)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        binding.professionEdt.error = "This username is already taken"
                    } else {
                        // username là duy nhất => Tiến hành đăng ký
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val id = task.result.user!!.uid
                                    val user = User().apply {
                                        uid = id
                                        this.name = name
                                        this.profession = profession
                                        this.email = email
                                        this.password = password
                                    }
                                    usersRef.child(id).setValue(user)
                                    Toast.makeText(this@SignUpActivity, "Sign Up Success", Toast.LENGTH_SHORT).show()

                                    val intent = Intent(this@SignUpActivity, LoginActivity::class.java).apply {
                                        putExtra("email", email)
                                        putExtra("password", password)
                                    }
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this@SignUpActivity, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SignUpActivity, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}