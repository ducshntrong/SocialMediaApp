package com.htduc.socialmediaapplication.Activities

import com.htduc.socialmediaapplication.ViewmodelFactories.ChatViewModelFactory
import android.annotation.SuppressLint
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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.htduc.socialmediaapplication.Adapters.MessageAdapter
import com.htduc.socialmediaapplication.Models.Messages
import com.htduc.socialmediaapplication.Models.User
import com.htduc.socialmediaapplication.Models.applyClickAnimation
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.moderation.UserModerationManager
import com.htduc.socialmediaapplication.ViewModel.ChatViewModel
import com.htduc.socialmediaapplication.ViewModel.ProfileViewModel
import com.htduc.socialmediaapplication.databinding.ActivityChatBinding
import com.htduc.socialmediaapplication.moderation.TextClassifier
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
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var userModerationManager: UserModerationManager
    private lateinit var textClassifier: TextClassifier

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        textClassifier = TextClassifier(this)

        dialog = ProgressDialog(this)
        dialog?.setMessage("Uploading Image...")
        dialog?.setCancelable(false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
//        chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]
        chatViewModel = ViewModelProvider(this, ChatViewModelFactory(this))[ChatViewModel::class.java]

        userModerationManager = UserModerationManager(database!!, this)

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

        // Lấy thông tin người dùng từ Firebase
        val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(senderUid!!)
        userRef.get().addOnSuccessListener { snapshot ->
            val currentUser = snapshot.getValue(User::class.java)
            if (currentUser != null ){
                if (userModerationManager.canSendMessage(currentUser)){
                    binding.linear02.visibility = View.VISIBLE
                    binding.alert.visibility = View.GONE
                } else{
                    binding.linear02.visibility = View.GONE
                    binding.alert.visibility = View.VISIBLE
                }
            }
        }

        val imagePickCallback = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedImage = uri
                binding.msgImg.setImageURI(selectedImage)
                binding.msgImg.visibility = View.VISIBLE
//                chatViewModel.pickImgMessage(uri, senderRoom!!, receiveRoom!!, senderUid!!) { success ->
//                    if (success) {
//                        dialog?.dismiss()
//                        selectedImage = null
//                    }
//                }
            }
        }

        applyClickAnimation(this, binding.btnSend) {
            val messageTxt = binding.edtMessage.text.toString().trim()
            val safeText = textClassifier.cleanTextIfToxic(messageTxt, "message")
            val message = Messages(null, safeText, senderUid, null, Date().time)
            if (safeText.isNotEmpty() || selectedImage != null) {
                binding.edtMessage.setText("")
                chatViewModel.onClickSendMsg(selectedImage, message, Date().time, senderRoom!!, receiveRoom!!, dialog!!)
                binding.msgImg.visibility = View.GONE
            } else {
                Toast.makeText(this, "Please enter something", Toast.LENGTH_SHORT).show()
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
        profileViewModel.status.observe(this){status->
            if (status == "Offline"){
                binding.status.visibility = View.GONE
                binding.circle.visibility = View.GONE
            }else{
                binding.status.text = status
                binding.status.visibility = View.VISIBLE
                binding.circle.visibility = View.VISIBLE
            }
        }
        profileViewModel.setPresenceStatus(receiveUid!!)
    }

    private fun setMessageToView(){
        chatViewModel.listMsg.observe(this){listMsg->
            messageAdapter.setMsgList(listMsg)
            binding.messageRecView.adapter = messageAdapter
            //messageRecView luôn cuộn đến item cuói để hiển thị tin nhắn mới nhất
            binding.messageRecView.scrollToPosition(messageAdapter.itemCount - 1)
        }
        chatViewModel.fetchMessage(senderRoom!!)
    }

    override fun onResume() {
        super.onResume()
        database!!.reference.child("presence").child(currentId!!).setValue("Online")
        userModerationManager.checkAndUnblockUser(currentId!!)
    }

    override fun onPause() {
        super.onPause()
        database!!.reference.child("presence").child(currentId!!).setValue("Offline")
    }

    override fun onDestroy() {
        super.onDestroy()
        textClassifier.close()
    }
}