package com.htduc.socialmediaapplication.Activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.htduc.socialmediaapplication.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth:FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bundle = intent.extras
        val email = bundle?.getString("email")
        val password = bundle?.getString("password")
        binding.emailEdt.setText(email)
        binding.passwordEdt.setText(password)

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null){ //đã được đăng nhập
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.register.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
        binding.btnLogin.setOnClickListener {
            actionLogin()
        }

        binding.changePass.setOnClickListener {
            val intent = Intent(this, ChangePassActivity::class.java)
            startActivity(intent)
        }
    }

    private fun actionLogin() {
        val email = binding.emailEdt.text.toString()
        val password = binding.passwordEdt.text.toString()
        if (email.isEmpty()){
            binding.emailEdt.error = "Please type your email"
            return
        }else if (password.isEmpty()){
            binding.passwordEdt.error = "Please type your password"
            return
        }
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (it.isSuccessful){
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }else{
                    Toast.makeText(this, "Email or password is incorrect", Toast.LENGTH_SHORT,).show()
                }
            }
    }

}