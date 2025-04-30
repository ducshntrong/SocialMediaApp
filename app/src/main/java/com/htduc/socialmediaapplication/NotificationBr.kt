package com.htduc.socialmediaapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

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