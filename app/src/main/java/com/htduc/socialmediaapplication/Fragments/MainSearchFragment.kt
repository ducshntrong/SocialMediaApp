package com.htduc.socialmediaapplication.Fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.databinding.FragmentMainSearchBinding
import com.htduc.socialmediaapplication.databinding.FragmentSearchBinding

class MainSearchFragment : Fragment() {
    private lateinit var binding: FragmentMainSearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentMainSearchBinding.inflate(layoutInflater)
        // Inflate the layout for this fragment
        childFragmentManager.beginTransaction()
            .replace(R.id.childFragmentContainer, SearchFragment())
            .commit()
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.searchUser.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus){
                binding.btnBack.visibility = View.VISIBLE
                binding.textSearch.visibility = View.GONE
                navigationToSearchFragment2()
            }
        }

        binding.searchUser.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    val fragment = childFragmentManager.findFragmentById(R.id.childFragmentContainer)
                    if (fragment is SearchFragment2){
                        fragment.searchUsersByQuery(it)
                    }
                    saveSearchHistory(it)
                    updateSearchHistoryFragment()
                }
                return true
            }

            override fun onQueryTextChange(query: String?): Boolean {
                return false
            }

        })

        binding.btnBack.setOnClickListener {
            childFragmentManager.popBackStack()
            binding.textSearch.visibility = View.VISIBLE
            binding.btnBack.visibility = View.GONE
            binding.searchUser.clearFocus()//Xóa focus khỏi SearchView
        }
    }

    // Hàm chuyển đổi fragment con
    private fun navigationToSearchFragment2(){
        childFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.enter_from_right,
                R.anim.exit_to_left,
                R.anim.enter_from_left,
                R.anim.exit_to_right
            )
            .replace(R.id.childFragmentContainer, SearchFragment2())
            .addToBackStack(null)
            .commit()

    }

    // Lưu lịch sử tìm kiếm vào SharedPreferences
    private fun saveSearchHistory(query: String){
        val prefs = requireContext().getSharedPreferences("search_history", Context.MODE_PRIVATE)
        //Set k cho phép trùng lặp, nên nếu users tìm từ cũ, nó không bị thêm lại lần nữa.
        val history = prefs.getStringSet("history_set", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        history.add(query)
        prefs.edit().putStringSet("history_set", history).apply()
    }

    // Cập nhật RecyclerView trong SearchFragment2 khi users tìm kiếm xong
    private fun updateSearchHistoryFragment(){
        val fragment = childFragmentManager.findFragmentById(R.id.childFragmentContainer)
        if (fragment is SearchFragment2) //Ktr xem fragment hiện tại có phải là SearchFragment2 k.
            fragment.updateHistory()
    }
}