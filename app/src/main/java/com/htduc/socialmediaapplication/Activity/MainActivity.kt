package com.htduc.socialmediaapplication.Activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.htduc.socialmediaapplication.Fragments.AddPostFragment
import com.htduc.socialmediaapplication.Fragments.HomeFragment
import com.htduc.socialmediaapplication.Fragments.NotificationFragment
import com.htduc.socialmediaapplication.Fragments.ProfileFragment
import com.htduc.socialmediaapplication.Fragments.SearchFragment
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.databinding.ActivityMainBinding
import com.iammert.library.readablebottombar.ReadableBottomBar

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth:FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var currentId: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        currentId = auth.uid
//        setSupportActionBar(binding.toolbar)
//        supportActionBar?.title = "My Profile"
        replaceFragment(HomeFragment())
//        binding.toolbar.visibility = View.GONE

        //ReadableBottomBar
        binding.readableBottomBar.setOnItemSelectListener(object: ReadableBottomBar.ItemSelectListener{
            override fun onItemSelected(index: Int) {
                when(index){
                    0 -> {
                        //binding.toolbar.visibility = View.GONE
                        replaceFragment(HomeFragment())
                    }
                    1 -> {
                        //binding.toolbar.visibility = View.GONE
                        replaceFragment(NotificationFragment())
                    }
                    2 -> {
                        //binding.toolbar.visibility = View.GONE
                        replaceFragment(AddPostFragment())
                    }
                    3 -> {
                        //binding.toolbar.visibility = View.GONE
                        replaceFragment(SearchFragment())
                    }
                    4 -> {
                        //binding.toolbar.visibility = View.VISIBLE
                        replaceFragment(ProfileFragment())
                    }
                }
            }

        })
    }

    private fun replaceFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.frameLayout, fragment)
            commit()
        }
    }

//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        menuInflater.inflate(R.menu.menu_item, menu)
//        return true
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        when(item.itemId){
//            R.id.setting -> {
//                auth.signOut()
//                val intent = Intent(this, LoginActivity::class.java)
//                startActivity(intent)
//                finish()
//            }
//        }
//        return super.onOptionsItemSelected(item)
//    }
    override fun onResume() {
        super.onResume()
        database.reference.child("presence").child(currentId!!).setValue("Online")
    }

    override fun onPause() {
        super.onPause()
        database.reference.child("presence").child(currentId!!).setValue("Offline")
    }
}