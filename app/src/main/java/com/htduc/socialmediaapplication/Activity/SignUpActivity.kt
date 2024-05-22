package com.htduc.socialmediaapplication.Activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.htduc.socialmediaapplication.Model.User
import com.htduc.socialmediaapplication.R
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
        val email = binding.emailEdt.text.toString()
        val password = binding.passEdt.text.toString()
        val name = binding.nameEdt.text.toString()
        val profession = binding.professionEdt.text.toString()

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
            binding.professionEdt.error = "Please type your profession"
            return
        }
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (it.isSuccessful){
                    val id = it.result.user!!.uid
                    val user = User()
                    user.uid = id
                    user.name = name
                    user.profession = profession
                    user.email = email
                    user.password = password
                    database.reference.child("Users").child(id).setValue(user)
                    Toast.makeText(this, "Sign Up Success", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    val bundle = Bundle()
                    bundle.putString("email", email)
                    bundle.putString("password", password)
                    intent.putExtras(bundle)
                    startActivity(intent)
                    finish()
                }else{
                    Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
                }
            }
    }
}