package com.htduc.socialmediaapplication.Moderation

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.google.firebase.database.FirebaseDatabase
import com.htduc.socialmediaapplication.Models.User
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.databinding.ViolationAlertBinding

class UserModerationManager(private val database: FirebaseDatabase, private val context: Context) {

    private val SPAM_THRESHOLD = 2 // Số lần vi phạm để bị cấm ngay
    private val SPAM_TIME_WINDOW = 10 * 60 * 1000 // 10 phút (đổi sang milliseconds)
    private val BAN_DURATION = 1 * 60 * 1000 // 1 phút (milliseconds) 
    lateinit var dialog: AlertDialog

    // Xử lý vi phạm
    fun handleViolation(userId: String) {
        val userRef = database.reference.child("Users").child(userId) // Tham chiếu đến user trên Firebase

        userRef.get().addOnSuccessListener { snapshot ->
            //Nếu snapshot không có dữ liệu (null), thì thoát khỏi hàm.
            val user = snapshot.getValue(User::class.java) ?: return@addOnSuccessListener

            val now = System.currentTimeMillis()

            // Xóa các vi phạm quá 24h để tránh lưu quá nhiều dữ liệu
            user.violationHistory = user.violationHistory.filter { it > now - 24 * 60 * 60 * 1000 }.toMutableList()

            // Thêm vi phạm mới
            user.violationHistory.add(now)
            user.violationCount = user.violationHistory.size

            // Kiểm tra nếu có >= SPAM_THRESHOLD vi phạm trong SPAM_TIME_WINDOW thì chặn user
            //Lọc ds violationHistory, chỉ giữ lại các vi phạm xảy ra trong vòng SPAM_TIME_WINDOW (10 phút).
            val recentViolations = user.violationHistory.filter { it > now - SPAM_TIME_WINDOW }
            if (recentViolations.size >= SPAM_THRESHOLD) {//Nếu số lần vi phạm trong 10 phút >= 3 lần, thì chặn người dùng.
                user.blockUntil = now + BAN_DURATION // Gán thời gian blockUntil, tức là chặn user đến (thời điểm hiện tại + 1 phút).
            }

            user.isBlocked = user.blockUntil > now //Nếu thời gian blockUntil lớn hơn now, nghĩa là user vẫn bị cấm.

            // Cập nhật lại thông tin user lên Firebase
            userRef.setValue(user)
        }
    }


    // Kiểm tra xem user có thể gửi tin nhắn không
    fun canSendMessage(user: User): Boolean {
        val now = System.currentTimeMillis()

        return if (user.isBlocked && now < user.blockUntil) {//Kiểm tra xem user đang bị chặn hay không.
            val remainingTime = (user.blockUntil - now) / 1000 // Thời gian còn lại tính bằng giây
            //Toast.makeText(context, "Bạn bị cấm chat. Còn ${remainingTime / 60} phút", Toast.LENGTH_LONG).show()
            false // User vẫn bị cấm
        } else {
            true // User không bị cấm
        }
    }


    // Kiểm tra & bỏ cấm nếu hết hạn
    fun checkAndUnblockUser(userId: String) {
        val userRef = database.reference.child("Users").child(userId)

        userRef.get().addOnSuccessListener { snapshot -> //Dùng get() để lấy dữ liệu từ Firebase.
            val user = snapshot.getValue(User::class.java) ?: return@addOnSuccessListener

            val now = System.currentTimeMillis()
            if (user.isBlocked && now >= user.blockUntil) {
                // Nếu thời gian block đã hết, bỏ chặn user
                user.isBlocked = false
                user.violationCount = 0
                user.violationHistory.clear()
                user.blockUntil = 0

                userRef.setValue(user) // Cập nhật Firebase
            }
        }
    }


    fun showDialogViolation() {
        val build = AlertDialog.Builder(context, R.style.Themecustom)
        val dialogBinding = ViolationAlertBinding.inflate(LayoutInflater.from(context))
        build.setView(dialogBinding.root)
        dialogBinding.ok.setOnClickListener { dialog.dismiss() }
        dialog = build.create()
        dialog.show()
    }
}
