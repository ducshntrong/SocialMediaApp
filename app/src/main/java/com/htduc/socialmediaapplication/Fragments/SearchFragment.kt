package com.htduc.socialmediaapplication.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.htduc.socialmediaapplication.Adapters.UserAdapter
import com.htduc.socialmediaapplication.ViewModel.MainViewModel
import com.htduc.socialmediaapplication.ViewmodelFactories.MainViewModelFactory
import com.htduc.socialmediaapplication.databinding.FragmentSearchBinding

class SearchFragment : Fragment() {
    private lateinit var binding: FragmentSearchBinding
    private lateinit var userAdapter: UserAdapter
    private lateinit var fragmentViewModel: MainViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        binding = FragmentSearchBinding.inflate(layoutInflater)
        fragmentViewModel = ViewModelProvider(
            this,
            MainViewModelFactory(requireActivity().application, requireContext())
        )[MainViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userAdapter = UserAdapter(requireContext())
        binding.userRv.layoutManager = LinearLayoutManager(context)
        binding.userRv.adapter = userAdapter
        fragmentViewModel.listUser.observe(viewLifecycleOwner){
            userAdapter.setUserList(it)
        }

    }

}