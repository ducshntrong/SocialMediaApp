package com.htduc.socialmediaapplication.Activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.badge.BadgeDrawable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.htduc.socialmediaapplication.Fragments.HomeFragment
import com.htduc.socialmediaapplication.Fragments.MainSearchFragment
import com.htduc.socialmediaapplication.Fragments.Notification2Fragment
import com.htduc.socialmediaapplication.Fragments.ProfileFragment
import com.htduc.socialmediaapplication.Fragments.StoryFragment
import com.htduc.socialmediaapplication.ViewModel.ChatViewModel
import com.htduc.socialmediaapplication.ViewModel.MainViewModel
import com.htduc.socialmediaapplication.ViewmodelFactories.ChatViewModelFactory
import com.htduc.socialmediaapplication.ViewmodelFactories.MainViewModelFactory
import com.htduc.socialmediaapplication.databinding.ActivityMainBinding
import com.htduc.socialmediaapplication.moderation.TextClassifier
import com.htduc.socialmediaapplication.moderation.UserModerationManager
import com.nafis.bottomnavigation.NafisBottomNavigation


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth:FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var currentId: String? = null
    private lateinit var userModerationManager: UserModerationManager
    private lateinit var textClassifier: TextClassifier
    private lateinit var badgeDrawable: BadgeDrawable

    private lateinit var fragmentViewModel: MainViewModel

    private lateinit var chatViewModel: ChatViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        fragmentViewModel = ViewModelProvider(
            this,
            MainViewModelFactory(application, this)
        )[MainViewModel::class.java]
        setContentView(binding.root)

        chatViewModel = ViewModelProvider(this, ChatViewModelFactory(this))[ChatViewModel::class.java]
        chatViewModel.deleteExpiredNotes()//goi ham de ktr va xoa note khi qua tgian 24h

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        currentId = auth.uid

        userModerationManager = UserModerationManager(database, this)
//        setSupportActionBar(binding.toolbar)
//        supportActionBar?.title = "My Profile"
//        binding.toolbar.visibility = View.GONE

        binding.bottomNavigation.add(NafisBottomNavigation.Model(1,
                com.htduc.socialmediaapplication.R.drawable.ic_home)
        )
        binding.bottomNavigation.add(NafisBottomNavigation.Model(2,
                com.htduc.socialmediaapplication.R.drawable.ic_notification)
        )
        binding.bottomNavigation.add(NafisBottomNavigation.Model(3,
                com.htduc.socialmediaapplication.R.drawable.baseline_add_24)
        )
        binding.bottomNavigation.add(NafisBottomNavigation.Model(4,
                com.htduc.socialmediaapplication.R.drawable.ic_search)
        )
        binding.bottomNavigation.add(NafisBottomNavigation.Model(5,
                com.htduc.socialmediaapplication.R.drawable.ic_profile)
        )

        replaceFragment(HomeFragment())
        binding.bottomNavigation.show(1);

        // Observe danh sách notification thay đổi để cập nhật badge
        fragmentViewModel.listNotification.observe(this) { notifications ->
            // Lọc số thông báo chưa mở
            val unreadCount = notifications.count { !it.checkOpen }

            if (unreadCount > 0) {
                binding.bottomNavigation.setCount(2, unreadCount.toString())
            } else {
                binding.bottomNavigation.clearAllCounts()
            }
        }

        binding.bottomNavigation.setOnClickMenuListener { model: NafisBottomNavigation.Model ->
            when (model.id) {
                1 -> {
                    replaceFragment(HomeFragment())
                }
                2 -> {
                    replaceFragment(Notification2Fragment())
                }
                3 -> {
                    replaceFragment(StoryFragment())
                }
                4 -> {
                    replaceFragment(MainSearchFragment())
                }
                5 -> {
                    replaceFragment(ProfileFragment())
                }
            }
        }

    }

    private fun replaceFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction().apply {
            replace(com.htduc.socialmediaapplication.R.id.frameLayout, fragment)
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
        userModerationManager.checkAndUnblockUser(currentId!!)
    }

    override fun onPause() {
        super.onPause()
        database.reference.child("presence").child(currentId!!).setValue("Offline")
    }

//    override fun onStart() {
//        super.onStart()
//        database.reference.child("notification").child(auth.uid!!)
//            .addValueEventListener(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    if (snapshot.exists()){
//                        for (dataSnap in snapshot.children){
//                            val notification = dataSnap.getValue(Notification::class.java)
//                            if (notification != null){
//                                notification.notificationId = dataSnap.key
//                                sendNotification(notification)
//                            }
//                        }
//                    }
//                }
//                override fun onCancelled(error: DatabaseError) {
//                    TODO("Not yet implemented")
//                }
//
//            })
//    }
//
//    private fun sendNotification(notification: Notification) {
//        val intent = Intent(this, NotificationBr::class.java)
//        val bundle = Bundle()
//        bundle.putParcelable("notification", notification)
//        intent.putExtras(bundle)
//        sendBroadcast(intent)
//    }
}

//ReadableBottomBar
//        binding.readableBottomBar.setOnItemSelectListener(object: ReadableBottomBar.ItemSelectListener{
//            override fun onItemSelected(index: Int) {
//                when(index){
//                    0 -> {
//                        //binding.toolbar.visibility = View.GONE
//                        replaceFragment(HomeFragment())
//                    }
//                    1 -> {
//                        //binding.toolbar.visibility = View.GONE
//                        replaceFragment(Notification2Fragment())
//                    }
//                    2 -> {
//                        //binding.toolbar.visibility = View.GONE
//                        replaceFragment(StoryFragment())
//                    }
//                    3 -> {
//                        //binding.toolbar.visibility = View.GONE
//                        replaceFragment(MainSearchFragment())
//                    }
//                    4 -> {
//                        //binding.toolbar.visibility = View.VISIBLE
//                        replaceFragment(ProfileFragment())
//                    }
//                }
//            }
//
//        })