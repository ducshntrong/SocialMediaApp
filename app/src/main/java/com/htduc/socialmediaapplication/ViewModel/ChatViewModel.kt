package com.htduc.socialmediaapplication.ViewModel

import android.app.ProgressDialog
import com.htduc.socialmediaapplication.moderation.NSFWDetector
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.htduc.socialmediaapplication.Model.Chats
import com.htduc.socialmediaapplication.Model.Messages
import com.htduc.socialmediaapplication.Model.Note
import com.htduc.socialmediaapplication.Model.User
import com.htduc.socialmediaapplication.moderation.UserModerationManager
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ChatViewModel(private val context: Context) : ViewModel() {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val nsfwDetector = NSFWDetector(context)

    private val _listMsg = MutableLiveData<ArrayList<Messages>>()
    val listMsg: LiveData<ArrayList<Messages>> = _listMsg

    private val _chatList = MutableLiveData<ArrayList<Chats>>()
    val chatList: LiveData<ArrayList<Chats>> = _chatList

    private val moderationManager = UserModerationManager(database, context)

    fun fetchMessage(senderRoom: String){
        database.reference.child("chats")
            .child(senderRoom).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val mListMsg = arrayListOf<Messages>()
                    mListMsg.clear()
                    for (snapshot1 in snapshot.children){
                        val message = snapshot1.getValue(Messages::class.java)
                        //gán giá trị của messageId bằng key của child hiện tại,
                        // để đảm bảo rằng messageId được gán cho đúng tin nhắn tương ứng.
                        message!!.messageId = snapshot1.key
                        mListMsg.add(message)
                    }
                    _listMsg.value = mListMsg
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    fun fetchChatList() {
        val currentUserId = auth.currentUser?.uid ?: return
        database.reference.child("chats")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val chatListData = arrayListOf<Chats>()
                    for (chatSnapshot in snapshot.children) {
                        val chatId = chatSnapshot.key ?: continue

                        // Ktra user có có bắt đầu bằng currentUserId không
                        if (!chatId.startsWith(currentUserId)) continue

                        val lastMsg = chatSnapshot.child("lastMsg").getValue(String::class.java) ?: "No message"
                        val lastMsgTime = chatSnapshot.child("lastMsgTime").getValue(Long::class.java) ?: 0L

                        // Tìm ID của người còn lại trong phòng chat
                        val otherUserId = chatId.replaceFirst(currentUserId, "")

                        // Lấy thông tin của người còn lại
                        database.reference.child("Users").child(otherUserId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(userSnapshot: DataSnapshot) {
                                    val user = userSnapshot.getValue(User::class.java) ?: User()

                                    val chat = Chats(user, lastMsg, lastMsgTime)
                                    chatListData.add(chat)

                                    _chatList.value = ArrayList(chatListData.sortedByDescending { it.lastMessageTime })
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun pickImgMessage(
        selectedImage: Uri,
        senderRoom: String,
        receiveRoom: String,
        senderUid: String,
        dialog: ProgressDialog
    ) {
        try {
            dialog.show() // Hiển thị dialog khi bắt đầu tải ảnh
            val nsfwScore = nsfwDetector.detectNSFW(context, selectedImage)
            if (nsfwScore < 0.70) {
                moderationManager.showDialogViolation()
                moderationManager.handleViolation(senderUid)
                dialog.dismiss() // Ẩn dialog nếu phát hiện ảnh vi phạm
                return
            }

            val calendar = Calendar.getInstance()
            val reference = storage.reference.child("chats")
                .child(senderRoom).child("${calendar.timeInMillis}.jpg")

            reference.putFile(selectedImage)
                .addOnSuccessListener { task ->
                    task.metadata!!.reference!!.downloadUrl
                        .addOnSuccessListener { url ->
                            val imageUrl = url.toString()
                            val date = Date()
                            val message = Messages(null, "photo", senderUid, imageUrl, date.time)
                            sendMessageToFirebase(message, date.time, senderRoom, receiveRoom)
                            dialog.dismiss() // Ẩn dialog sau khi tải thành công
                        }
                }
                .addOnFailureListener {
                    dialog.dismiss() // Ẩn dialog nếu có lỗi
                    Toast.makeText(context, "Lỗi tải ảnh!", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            dialog.dismiss() // Ẩn dialog trong trường hợp có lỗi
            e.printStackTrace()
            Toast.makeText(context, "Lỗi xử lý ảnh!", Toast.LENGTH_SHORT).show()
        }
    }


    fun onClickSendMsg(
        selectedImage: Uri?, message: Messages, time: Long,
        senderRoom: String, receiveRoom: String, dialog: ProgressDialog
    ) {
        if (selectedImage != null) {
            pickImgMessage(selectedImage, senderRoom, receiveRoom, message.senderId!!, dialog)
        } else {
            sendMessageToFirebase(message, time, senderRoom, receiveRoom)
        }
    }


    private fun sendMessageToFirebase(message: Messages, time: Long, senderRoom: String, receiveRoom:String){
        val randomKey = database.reference.push().key
        //đối tượng HashMap để lưu trữ thông tin tin nhắn cuối cùng (lastMsg)
        // và thời gian tin nhắn cuối cùng (lastMsgTime).
        val lastMsgObj = HashMap<String, Any>()
        lastMsgObj["lastMsg"] = message.message!! //gán giá trị
        lastMsgObj["lastMsgTime"] = time

        //cập nhật dữ liệu tin nhắn cuối cùng và thời gian tin nhắn cuối cùng cho phòng chat
        // của người gửi thông qua việc gọi updateChildren()
        database.reference.child("chats").child(senderRoom)
            .updateChildren(lastMsgObj)
        database.reference.child("chats").child(receiveRoom)
            .updateChildren(lastMsgObj) //của người nhận
        //Thêm tin nhắn mới vào phòng chat của người gửi
        database.reference.child("chats").child(senderRoom)
            .child("messages")
            .child(randomKey!!)
            .setValue(message).addOnSuccessListener {
                //Thêm tin nhắn mới vào phòng chat của người nhận
                database.reference.child("chats")
                    .child(receiveRoom)
                    .child("messages")
                    .child(randomKey)
                    .setValue(message)
                    .addOnSuccessListener {  }
            }
    }

    fun uploadNote(userId: String, content: String, onSaveNoteComplete: (Boolean) -> Unit) {
        val dbRef = database.reference.child("Notes").child(userId)
        val noteId = dbRef.push().key ?: return
        val note = Note(noteId, userId, content, System.currentTimeMillis())

        dbRef.child(noteId).setValue(note)
            .addOnSuccessListener { onSaveNoteComplete(true) }
            .addOnFailureListener { onSaveNoteComplete(false) }
    }


    fun deleteNote(userId: String, onDeleteComplete: (Boolean) -> Unit) {//
        val dbRef = database.reference.child("Notes").child(userId)

        dbRef.removeValue() // Xóa toàn bộ node Notes/userId
            .addOnSuccessListener { onDeleteComplete(true) }
            .addOnFailureListener { onDeleteComplete(false) }
    }

    fun deleteExpiredNotes() {
        val dbRef = database.reference.child("Notes")
        val now = System.currentTimeMillis()
        val expiryTime = 24 * 60 * 60 * 1000 // 24h

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) { // Lặp qua từng user
                    for (noteSnapshot in userSnapshot.children) { // Lặp qua từng note
                        val note = noteSnapshot.getValue(Note::class.java)
                        if (note?.timestamp != null && (note.timestamp + expiryTime < now)) {
                            noteSnapshot.ref.removeValue() // Xóa note hết hạn
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0) return "Unknown"

        val now = Calendar.getInstance()
        val messageTime = Calendar.getInstance().apply { timeInMillis = timestamp }
        val diffMillis = now.timeInMillis - messageTime.timeInMillis
        val diffSeconds = diffMillis / 1000
        val diffMinutes = diffSeconds / 60
        val diffHours = diffMinutes / 60
        val diffDays = diffHours / 24

        return when {
            diffMinutes < 1 -> "Vừa xong"
            diffMinutes < 60 -> "$diffMinutes phút trước"
            diffHours < 24 -> "$diffHours giờ trước"
            diffDays == 1L -> "Hôm qua"
            diffDays < 7 -> "$diffDays ngày trước"
            diffDays < 30 -> "${diffDays / 7} tuần trước"
            diffDays < 365 -> "${diffDays / 30} tháng trước"
            else -> "${diffDays / 365} năm trước"
        }
    }

}
