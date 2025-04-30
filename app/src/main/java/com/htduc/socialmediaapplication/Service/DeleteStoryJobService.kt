package com.htduc.socialmediaapplication.Service

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import com.google.firebase.database.FirebaseDatabase

class DeleteStoryJobService: JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        val storyId = params?.extras?.getString("storyId")
        val userId = params?.extras?.getString("userId")
        if (storyId!=null && userId!=null){
            FirebaseDatabase.getInstance().reference
                .child("stories")
                .child(userId)
                .child("userStories")
                .child(storyId)
                .removeValue()
                .addOnSuccessListener {
                    Log.d("DeleteStoryJobService", "Story deleted successfully")
                    jobFinished(params, false)
                }
                .addOnFailureListener { exception ->
                    Log.e("DeleteStoryJobService", "Failed to delete story: ${exception.message}")
                    jobFinished(params, true) // Nếu quá trình xoá thất bại, trả về true để lên lịch lại công việc
                }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return false
    }
}