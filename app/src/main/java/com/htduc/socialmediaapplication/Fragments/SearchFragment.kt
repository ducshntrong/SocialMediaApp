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
import com.htduc.socialmediaapplication.Adapter.UserAdapter
import com.htduc.socialmediaapplication.Model.User
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.databinding.FragmentSearchBinding

class SearchFragment : Fragment() {
    private lateinit var binding: FragmentSearchBinding
    private lateinit var userAdapter: UserAdapter
    private var listUser = arrayListOf<User>()
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentSearchBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showListUser()
    }

    private fun showListUser() {
        userAdapter = UserAdapter(requireContext())
        binding.userRv.layoutManager = LinearLayoutManager(context)
        binding.userRv.adapter = userAdapter
        database.reference.child("Users")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    listUser.clear()
                    if (snapshot.exists()){
                        for (snapshot1 in snapshot.children){
                            val user = snapshot1.getValue(User::class.java)
                            if (!(user!!.uid).equals(auth.uid))
                                listUser.add(user)
                        }
                        userAdapter.setUserList(listUser)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

}