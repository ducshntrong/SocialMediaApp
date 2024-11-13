package com.htduc.socialmediaapplication

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.htduc.socialmediaapplication.Model.Notification
import com.htduc.socialmediaapplication.Model.User

class NotificationBr: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//           val notification = intent?.extras?.getParcelable("notification", Notification::class.java)
//            val  serviceIntent = Intent(context, NotificationService::class.java)
//            val bundle = Bundle()
//            bundle.putParcelable("notification", notification)
//            serviceIntent.putExtras(bundle)
//            context?.startService(serviceIntent)
//        }
    }

}