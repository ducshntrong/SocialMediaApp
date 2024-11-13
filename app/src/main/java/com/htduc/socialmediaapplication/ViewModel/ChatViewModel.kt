package com.htduc.socialmediaapplication.ViewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.htduc.socialmediaapplication.Model.Follow
import com.htduc.socialmediaapplication.Model.Messages
import java.util.Calendar
import java.util.Date

class ChatViewModel: ViewModel() {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _listMsg = MutableLiveData<ArrayList<Messages>>()
    val listMsg: LiveData<ArrayList<Messages>> = _listMsg

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

    fun pickImgMessage(selectedImage: Uri, senderRoom: String, receiveRoom:String,
                       senderUid:String, onUploadComplete: (Boolean)->Unit){
        val calendar = Calendar.getInstance()
        val reference = storage.reference.child("chats")
            .child(senderRoom).child(calendar.timeInMillis.toString()+"")
        reference.putFile(selectedImage)
            .addOnSuccessListener { task ->
                task.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { url ->
                        val imageUrl = url.toString()
                        val date = Date()
                        val message = Messages(null, "photo", senderUid, imageUrl, date.time)
                        onClickSendMsg(message, date.time, senderRoom,receiveRoom)
                        onUploadComplete(true)
                    }
            }
    }

    fun onClickSendMsg(message: Messages, time: Long, senderRoom: String, receiveRoom:String){
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
}