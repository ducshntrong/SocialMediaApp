package com.htduc.socialmediaapplication.Adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.htduc.socialmediaapplication.Fragments.Notification2Fragment
import com.htduc.socialmediaapplication.Fragments.RequestFragment

class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle):
    FragmentStateAdapter(fragmentManager, lifecycle){
    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> Notification2Fragment()
            else -> Notification2Fragment()
        }
    }
}