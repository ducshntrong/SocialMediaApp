package com.htduc.socialmediaapplication.Service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.text.Html
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.htduc.socialmediaapplication.Models.Notification
import com.htduc.socialmediaapplication.Models.User
import com.htduc.socialmediaapplication.MyApplication
import com.htduc.socialmediaapplication.R

class NotificationService: Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            val notification = intent?.extras?.getParcelable("notification", Notification::class.java)
//            handleNotification(notification)
//        }
        return START_STICKY
    }

    private fun handleNotification(notification: Notification?) {
        // Lấy dữ liệu người dùng từ Firebase
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(notification?.notificationBy!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        showNotification(notification, user)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Xử lý lỗi
                }
            })
    }

    @SuppressLint("ForegroundServiceType")
    private fun showNotification(notification: Notification, user: User) {
        val message = when (notification.type) {
            "like" -> Html.fromHtml("<b>"+user.name+"</b>" + " like your post")
            "comment" -> Html.fromHtml("<b>"+user.name+"</b>" + " commented your post")
            "follow" -> Html.fromHtml("<b>"+user.name+"</b>" + " started following you")
            else -> "New notification"
        }
        val notifications = NotificationCompat.Builder(baseContext, MyApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(resources.getString(R.string.app_name))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        startForeground(1, notifications)
    }
}