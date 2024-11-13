package com.htduc.socialmediaapplication

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MyApplication: Application() {
    companion object{
        const val CHANNEL_ID = "channel1"
    }

    override fun onCreate() {
        super.onCreate()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
//            val notificationChannel = NotificationChannel(CHANNEL_ID, "Notification Service", NotificationManager.IMPORTANCE_HIGH)
//            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(notificationChannel)
//        }
    }
}