package com.htduc.socialmediaapplication.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.htduc.socialmediaapplication.Adapter.NotificationAdapter
import com.htduc.socialmediaapplication.Model.Notification
import com.htduc.socialmediaapplication.ViewModel.FragmentViewModel
import com.htduc.socialmediaapplication.ViewModel.FragmentViewModelFactory
import com.htduc.socialmediaapplication.databinding.FragmentNotification2Binding


class Notification2Fragment : Fragment() {
    private lateinit var binding: FragmentNotification2Binding
    private lateinit var notificationAdapter: NotificationAdapter
    private var listNotifi = arrayListOf<Notification>()
    private lateinit var fragmentViewModel: FragmentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentNotification2Binding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        fragmentViewModel = ViewModelProviders.of(this, FragmentViewModelFactory(requireActivity().application))[FragmentViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notificationAdapter = NotificationAdapter(requireContext(), listNotifi)
        binding.notification2Rv.layoutManager = LinearLayoutManager(context)
        binding.notification2Rv.setHasFixedSize(true)
        binding.notification2Rv.adapter = notificationAdapter
        fragmentViewModel.listNotification.observe(viewLifecycleOwner){
            notificationAdapter.setNotificationList(it)
            binding.notification2Rv.scrollToPosition(notificationAdapter.itemCount - 1)
        }
    }
}