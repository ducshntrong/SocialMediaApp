package com.htduc.socialmediaapplication.Activity

import android.app.ProgressDialog
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.htduc.socialmediaapplication.Adapter.MessageAdapter
import com.htduc.socialmediaapplication.Model.Messages
import com.htduc.socialmediaapplication.Model.User
import com.htduc.socialmediaapplication.Model.applyClickAnimation
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.databinding.ActivityChatBinding
import java.util.Calendar
import java.util.Date

class ChatActivity : AppCompatActivity() {
    private var currentId: String? = null
    private lateinit var binding: ActivityChatBinding
    private var user: User? = null
    private var auth: FirebaseAuth? =null
    private var storage: FirebaseStorage? = null
    private var database: FirebaseDatabase? = null
    private var selectedImage: Uri? = null
    private var dialog: ProgressDialog? = null
    private var messages:ArrayList<Messages> = arrayListOf()
    private lateinit var messageAdapter: MessageAdapter
    private var senderRoom:String? = null
    private var receiveRoom:String? = null
    private var senderUid:String? = null
    private var receiveUid:String? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        dialog = ProgressDialog(this)
        dialog?.setMessage("Uploading Image...")
        dialog?.setCancelable(false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        currentId = auth!!.uid

        binding.imgBack.setOnClickListener { finish() }
        user = intent.extras!!.getParcelable("user")
        receiveUid = user!!.uid
        senderUid = auth!!.uid

        setViewToToolbar()

        senderRoom = senderUid + receiveUid
        receiveRoom = receiveUid + senderUid

        messageAdapter = MessageAdapter(this, messages, senderRoom!!, receiveRoom!!)
        binding.messageRecView.layoutManager = LinearLayoutManager(this)
        binding.messageRecView.adapter = messageAdapter
        setMessageToView()

        applyClickAnimation(this, binding.btnSend){
            val date = Date()
            val messageTxt = binding.edtMessage.text.toString().trim()
            val message = Messages(null, messageTxt, senderUid, null, date.time)
            if (messageTxt.isNotEmpty()){
                onClickSendMsg(message, date.time)
            }else{
                Toast.makeText(this, "Please enter something", Toast.LENGTH_SHORT).show()
            }
        }

        val imagePickCallback = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            selectedImage = uri
            // Xử lý ảnh đã chọn ở đây
            if (selectedImage != null) {
                pickImgMessage()
            }
        }
        applyClickAnimation(this, binding.attachment){
            imagePickCallback.launch("image/*")
        }

        handlerTextChanged()
        supportActionBar?.setDisplayShowTitleEnabled(false)

        applyClickAnimation(this, binding.camera){

        }
    }

    private fun onClickSendMsg(message: Messages, time: Long){
        binding.edtMessage.setText("")
        val randomKey = database!!.reference.push().key
        //đối tượng HashMap để lưu trữ thông tin tin nhắn cuối cùng (lastMsg)
        // và thời gian tin nhắn cuối cùng (lastMsgTime).
        val lastMsgObj = HashMap<String, Any>()
        lastMsgObj["lastMsg"] = message.message!! //gán giá trị
        lastMsgObj["lastMsgTime"] = time

        //cập nhật dữ liệu tin nhắn cuối cùng và thời gian tin nhắn cuối cùng cho phòng chat
        // của người gửi thông qua việc gọi updateChildren()
        database!!.reference.child("chats").child(senderRoom!!)
            .updateChildren(lastMsgObj)
        database!!.reference.child("chats").child(receiveRoom!!)
            .updateChildren(lastMsgObj) //của người nhận
        //Thêm tin nhắn mới vào phòng chat của người gửi
        database!!.reference.child("chats").child(senderRoom!!)
            .child("messages")
            .child(randomKey!!)
            .setValue(message).addOnSuccessListener {
                //Thêm tin nhắn mới vào phòng chat của người nhận
                database!!.reference.child("chats")
                    .child(receiveRoom!!)
                    .child("messages")
                    .child(randomKey)
                    .setValue(message)
                    .addOnSuccessListener {  }
            }
    }

    private fun pickImgMessage(){
        // Tiếp tục xử lý các dòng code bên dưới
        val calendar = Calendar.getInstance()
        val reference = storage!!.reference.child("chats")
            .child(senderRoom!!).child(calendar.timeInMillis.toString()+"")
        dialog?.show()
        reference.putFile(selectedImage!!)
            .addOnSuccessListener { task ->
                dialog?.dismiss()
                task.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { url ->
                        val imageUrl = url.toString()
                        val date = Date()
                        val message = Messages(null, "photo", senderUid, imageUrl, date.time)
                        onClickSendMsg(message, date.time)
                    }
            }
    }

    private fun handlerTextChanged(){//xử lý sự kiện khi người dùng thay đổi nội dung của EditText
        val handler = Handler()
        binding.edtMessage.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                database!!.reference.child("presence")
                    .child(senderUid!!).setValue("Typing...")
                handler.removeCallbacksAndMessages(null)
                //thực thi sau khi dừng gõ trong 1 giây
                handler.postDelayed(userStoppedTying, 1000)
            }
            var userStoppedTying = Runnable { //được gọi khi dừng gõ
                database!!.reference.child("presence")//cập nhật status hiện diện
                    .child(senderUid!!).setValue("Online")
            }

        })
    }

    private fun setViewToToolbar(){
        binding.name.text = user!!.name
        Glide.with(this).load(user!!.profilePhoto)
            .placeholder(R.drawable.avt).into(binding.imgProfile)
        //cập nhật trình trạng off và on của người nhận
        database!!.reference.child("presence").child(receiveUid!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        val status = snapshot.getValue(String::class.java)
                        if (status == "Offline"){
                            binding.status.visibility = View.GONE
                            binding.circle.visibility = View.GONE
                        }else{
                            binding.status.text = status
                            binding.status.visibility = View.VISIBLE
                            binding.circle.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    private fun setMessageToView(){
        database!!.reference.child("chats")
            .child(senderRoom!!).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messages.clear()
                    for (snapshot1 in snapshot.children){
                        val message = snapshot1.getValue(Messages::class.java)
                        //gán giá trị của messageId bằng key của child hiện tại,
                        // để đảm bảo rằng messageId được gán cho đúng tin nhắn tương ứng.
                        message!!.messageId = snapshot1.key
                        messages.add(message)
                    }
                    messageAdapter.setMsgList(messages)
                    binding.messageRecView.adapter = messageAdapter
                    //messageRecView luôn cuộn đến item cuói để hiển thị tin nhắn mới nhất
                    binding.messageRecView.scrollToPosition(messageAdapter.itemCount - 1)
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    override fun onResume() {
        super.onResume()
        database!!.reference.child("presence").child(currentId!!).setValue("Online")
    }

    override fun onPause() {
        super.onPause()
        database!!.reference.child("presence").child(currentId!!).setValue("Offline")
    }
}