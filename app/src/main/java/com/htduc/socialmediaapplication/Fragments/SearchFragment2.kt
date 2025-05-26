package com.htduc.socialmediaapplication.Fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.Query
import com.htduc.socialmediaapplication.Adapters.SearchHistoryAdapter
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.databinding.FragmentSearch2Binding

class SearchFragment2 : Fragment() {
    private lateinit var binding: FragmentSearch2Binding
    private lateinit var adapter: SearchHistoryAdapter
    private val historyList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSearch2Binding.inflate(layoutInflater)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SearchHistoryAdapter(historyList)
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.historyRecyclerView.adapter = adapter

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
}