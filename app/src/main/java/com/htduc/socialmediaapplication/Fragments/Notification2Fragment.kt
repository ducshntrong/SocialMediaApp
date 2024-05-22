package com.htduc.socialmediaapplication.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.htduc.socialmediaapplication.Adapter.NotificationAdapter
import com.htduc.socialmediaapplication.Model.Notification
import com.htduc.socialmediaapplication.databinding.FragmentNotification2Binding


class Notification2Fragment : Fragment() {
    private lateinit var binding: FragmentNotification2Binding
    private lateinit var notificationAdapter: NotificationAdapter
    private var listNotifi = arrayListOf<Notification>()
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentNotification2Binding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notificationAdapter = NotificationAdapter(requireContext(), listNotifi)
        binding.notification2Rv.layoutManager = LinearLayoutManager(context)
        binding.notification2Rv.setHasFixedSize(true)
        binding.notification2Rv.adapter = notificationAdapter
        database.reference.child("notification").child(auth.uid!!)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    listNotifi.clear()
                    if (snapshot.exists()){
                        for (dataSnap in snapshot.children){
                            val notification = dataSnap.getValue(Notification::class.java)
                            if (notification != null){
                                notification.notificationId = dataSnap.key
                                listNotifi.add(notification)
                            }
                        }
                        notificationAdapter.setNotificationList(listNotifi)
                        binding.notification2Rv.scrollToPosition(notificationAdapter.itemCount - 1)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

}