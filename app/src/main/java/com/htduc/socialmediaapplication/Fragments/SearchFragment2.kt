package com.htduc.socialmediaapplication.Fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.Query
import com.htduc.socialmediaapplication.Adapters.PostAdapter
import com.htduc.socialmediaapplication.Adapters.SearchHistoryAdapter
import com.htduc.socialmediaapplication.Adapters.SearchUserAdapter
import com.htduc.socialmediaapplication.Models.Post
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.ViewModel.FragmentViewModel
import com.htduc.socialmediaapplication.ViewmodelFactories.FragmentViewModelFactory
import com.htduc.socialmediaapplication.databinding.FragmentSearch2Binding

class SearchFragment2 : Fragment() {
    private lateinit var binding: FragmentSearch2Binding
    private lateinit var adapter: SearchHistoryAdapter
    private lateinit var searchAdapter: SearchUserAdapter
    private lateinit var postAdapter: PostAdapter
    private val historyList = mutableListOf<String>()
    private var listPost = arrayListOf<Post>()
    private lateinit var fragmentViewModel: FragmentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSearch2Binding.inflate(layoutInflater)
        fragmentViewModel = ViewModelProvider(this,
            FragmentViewModelFactory(requireActivity().application, requireContext())
        )[FragmentViewModel::class.java]
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SearchHistoryAdapter(
            historyList,
            onItemClick = {searchUsersByQuery(it)},
            onItemClickDelete = {deleteHistoryItem(it)})
        binding.historyRc.layoutManager = LinearLayoutManager(requireContext())
        binding.historyRc.adapter = adapter

        searchAdapter = SearchUserAdapter(requireContext())
        binding.userSearchRc.layoutManager = GridLayoutManager(requireContext(),
            2, GridLayoutManager.HORIZONTAL, false)
        binding.userSearchRc.adapter = searchAdapter
        fragmentViewModel.searchResultListUser.observe(viewLifecycleOwner){
            searchAdapter.setUserList(it)
        }

        binding.postSearchRc.showShimmerAdapter()
        postAdapter = PostAdapter(requireContext(), listPost)
        binding.postSearchRc.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.postSearchRc.setHasFixedSize(true)
        binding.postSearchRc.isNestedScrollingEnabled = false
        binding.postSearchRc.adapter = postAdapter
        fragmentViewModel.searchResultListPost.observe(viewLifecycleOwner){ post->
            binding.postSearchRc.hideShimmerAdapter()
            postAdapter.setPostList(post)
        }

        loadHistory()

        binding.delete.setOnClickListener {
            val prefs = requireContext().getSharedPreferences("search_history", Context.MODE_PRIVATE)
            prefs.edit().remove("history_set").apply()// Xóa dữ liệu trong SharedPreferences
            historyList.clear()
            adapter.notifyDataSetChanged()
        }

    }

    fun updateHistory(){ loadHistory()}

    @SuppressLint("NotifyDataSetChanged")
    private fun loadHistory(){
        val prefs = requireContext().getSharedPreferences("search_history", Context.MODE_PRIVATE)
        val history = prefs.getStringSet("history_set", emptySet())?.reversed() ?: emptyList()
        historyList.clear()
        historyList.addAll(history)
        adapter.notifyDataSetChanged()
    }

    fun searchUsersByQuery(query: String){
        binding.historyRc.visibility = View.GONE
        binding.constraint.visibility = View.GONE
        binding.userSearchRc.visibility = View.VISIBLE
        binding.tvUser.visibility = View.VISIBLE
        binding.postSearchRc.visibility = View.VISIBLE
        binding.tvPost.visibility = View.VISIBLE
        fragmentViewModel.searchUser(query)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun deleteHistoryItem(position: Int) {
        // Xóa item khỏi danh sách
        historyList.removeAt(position)

        // Lưu lại danh sách mới vào SharedPreferences
        val prefs = requireContext().getSharedPreferences("search_history", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("history_set", historyList.toSet()).apply() // Chuyển sang Set để lưu

        // Cập nhật adapter
        adapter.notifyDataSetChanged()

    }
}