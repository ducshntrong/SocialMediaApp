package com.htduc.socialmediaapplication.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import com.htduc.socialmediaapplication.databinding.ItemHistoryBinding

class SearchHistoryAdapter(
    private val items: List<String>,
    private val onItemClick: (String) -> Unit,
    private val onItemClickDelete: (Int) -> Unit,
): RecyclerView.Adapter<SearchHistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(binding: ItemHistoryBinding): RecyclerView.ViewHolder(binding.root){
        val text = binding.tvQuery
        val delete = binding.btnDelete
        val root = binding.root
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        return HistoryViewHolder(ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.text.text = items[position]

        holder.root.setOnClickListener {
            onItemClick.invoke(items[position])
        }
        holder.delete.setOnClickListener {
            onItemClickDelete.invoke(position)
        }
    }
}