package com.htduc.socialmediaapplication.Adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.htduc.socialmediaapplication.Activities.ProfileActivity
import com.htduc.socialmediaapplication.Models.Story
import com.htduc.socialmediaapplication.Models.User
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.databinding.StoryRvDesign2Binding
import com.squareup.picasso.Picasso
import omari.hamza.storyview.StoryView
import omari.hamza.storyview.callback.StoryClickListeners
import omari.hamza.storyview.model.MyStory


class StoryAdapter2(private val context: Context, private var listStory: ArrayList<Story>)
    : RecyclerView.Adapter<StoryAdapter2.StoryViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun setStoryList(listStory: ArrayList<Story>){
        this.listStory = listStory
        notifyDataSetChanged()
    }
    inner class StoryViewHolder(binding: StoryRvDesign2Binding): RecyclerView.ViewHolder(binding.root) {
        val profile = binding.profileImage
        val storyImg = binding.storyImg
        val statusCircle = binding.statusCircle
        val name = binding.userName
        val root = binding.root
        var user:User? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        return StoryViewHolder(StoryRvDesign2Binding.inflate(LayoutInflater.from(parent.context)
            , parent, false))
    }

    override fun getItemCount(): Int {
        return listStory.size
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = listStory[position]
        if (story.stories!!.size > 0){
            //lấy ra story cuối cùng trong list stories của đối tượng story
            val lastStory = story.stories!![story.stories!!.size-1]
            Picasso.get().load(lastStory.image)
                .placeholder(R.drawable.placeholder)
                .into(holder.storyImg)
            holder.statusCircle.setPortionsCount(story.stories!!.size)

            FirebaseDatabase.getInstance().reference.child("Users")
                .child(story.storyBy!!).addValueEventListener(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        if (user != null) {
                            holder.user = user
                            Picasso.get().load(user.profilePhoto)
                                .placeholder(R.drawable.avt)
                                .into(holder.profile)
                            holder.name.text = user.name

                            holder.root.setOnClickListener {
                                val myStories = ArrayList<MyStory>()
                                for (stories in story.stories!!){
                                    myStories.add(MyStory(stories.image))
                                }

                                StoryView.Builder((context as AppCompatActivity).supportFragmentManager)
                                    .setStoriesList(myStories) // Required
                                    .setStoryDuration(7000)
                                    .setTitleText(user.name)
                                    .setSubtitleText("")
                                    .setTitleLogoUrl(user.profilePhoto)
                                    .setStoryClickListeners(object : StoryClickListeners {
                                        override fun onDescriptionClickListener(position: Int) {
                                            //your action
                                        }

                                        override fun onTitleIconClickListener(position: Int) {
                                            if (story.storyBy != FirebaseAuth.getInstance().uid){
                                                val intent = Intent(context, ProfileActivity::class.java)
                                                val bundle = Bundle()
                                                bundle.putParcelable("user", holder.user)
                                                intent.putExtras(bundle)
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                context.startActivity(intent)
                                            }
                                        }
                                    }).build().show()

                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }

                })
        }
    }
}