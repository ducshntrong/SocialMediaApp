package com.htduc.socialmediaapplication.Adapter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.htduc.socialmediaapplication.Model.Messages
import com.htduc.socialmediaapplication.R
import com.htduc.socialmediaapplication.databinding.DeleteLayout2Binding
import com.htduc.socialmediaapplication.databinding.DeleteLayoutBinding
import com.htduc.socialmediaapplication.databinding.EditMessageDialogBinding
import com.htduc.socialmediaapplication.databinding.ReceiveMsgBinding
import com.htduc.socialmediaapplication.databinding.SendMsgBinding

class MessageAdapter(val cont: Context, private var messages:ArrayList<Messages>,
                     val senderRoom: String, val receiverRoom: String
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val ITEM_SENT = 1
    private val ITEM_RECEIVE = 2

    @SuppressLint("NotifyDataSetChanged")
    fun setMsgList(messages: ArrayList<Messages>){
        this.messages = messages
        notifyDataSetChanged()
    }

    inner class SentMsgHolder(binding: SendMsgBinding): RecyclerView.ViewHolder(binding.root){
        val imgSent = binding.msgImg
        val msgSent = binding.message
        val mLinear = binding.mLinear
        val rootSent = binding.root
    }

    inner class ReceiveMsgHolder(binding: ReceiveMsgBinding): RecyclerView.ViewHolder(binding.root){
        val imgReceive = binding.msgImg
        val msgReceive = binding.message
        val mLinear = binding.mLinear
        val rootReceive = binding.root
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        //ktra senderId của tin nhắn có trùng với uid của người dùng hiện tại không
        return if (FirebaseAuth.getInstance().uid == message.senderId){
            ITEM_SENT
        }else{
            ITEM_RECEIVE
        }
    }
    //RecyclerView hiển thị các item tin nhắn gửi và nhận với các layout và giao diện người
// dùng khác nhau, tùy thuộc vào người gửi và người nhận của tin nhắn.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_SENT) {
            SentMsgHolder(SendMsgBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false))
        } else {
            ReceiveMsgHolder(ReceiveMsgBinding.inflate(
                LayoutInflater.from(parent.context),
                parent, false))
        }
    }

    override fun getItemCount(): Int {
        return  messages.size
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        when (holder) {
            is SentMsgHolder -> {
                if (message.message == "photo") {
                    holder.imgSent.visibility = View.VISIBLE
                    holder.msgSent.visibility = View.GONE
                    holder.mLinear.visibility = View.GONE
                    Glide.with(cont)
                        .load(message.imageUrl)
                        .placeholder(R.drawable.placeholder)
                        .into(holder.imgSent)
                } else {
                    holder.imgSent.visibility = View.GONE
                    holder.msgSent.visibility = View.VISIBLE
                    holder.mLinear.visibility = View.VISIBLE
                    holder.msgSent.text = message.message
                }

                holder.rootSent.setOnLongClickListener {
                    actionOnLongClickSend(message)
                    false
                }
            }

            is ReceiveMsgHolder -> {
                if (message.message == "photo") {
                    holder.imgReceive.visibility = View.VISIBLE
                    holder.msgReceive.visibility = View.GONE
                    holder.mLinear.visibility = View.GONE
                    Glide.with(cont)
                        .load(message.imageUrl)
                        .placeholder(R.drawable.placeholder)
                        .into(holder.imgReceive)
                } else {
                    holder.imgReceive.visibility = View.GONE
                    holder.msgReceive.visibility = View.VISIBLE
                    holder.mLinear.visibility = View.VISIBLE
                    holder.msgReceive.text = message.message
                }

                holder.rootReceive.setOnLongClickListener {
                    actionOnLongClickReceive(message)
                    false
                }
            }
        }
    }

    private fun actionOnLongClickSend(message: Messages) {
        val bindingDelete = DeleteLayoutBinding.inflate(LayoutInflater.from(cont))
        val dialog = AlertDialog.Builder(cont)
            .setTitle("Delete Message")
            .setView(bindingDelete.root)
            .create()
        bindingDelete.everyone.setOnClickListener {
            deleteEveryone(message)
            dialog.dismiss()
        }

        bindingDelete.editMsg.setOnClickListener {
            val dialogBindingEdit = EditMessageDialogBinding.inflate(LayoutInflater.from(cont))
            val buidel = AlertDialog.Builder(cont)
            buidel.setView(dialogBindingEdit.root).apply {
                setTitle("Edit Message")
                setPositiveButton("Edit"){ _: DialogInterface, _: Int ->
                    message.message = dialogBindingEdit.txtInMsg.text.toString().trim()
                    editMsg(message)
                }
            }.show()
            dialog.dismiss()
        }

        bindingDelete.deleteMsg.setOnClickListener {
            deleteMsg(message)
            dialog.dismiss()
        }
        bindingDelete.cancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun actionOnLongClickReceive(message: Messages) {
        val bindingDelete = DeleteLayout2Binding.inflate(LayoutInflater.from(cont))
        val dialog = AlertDialog.Builder(cont)
            .setTitle("Delete Message")
            .setView(bindingDelete.root)
            .create()
        bindingDelete.everyone.setOnClickListener {
            deleteEveryone(message)
            dialog.dismiss()
        }

        bindingDelete.deleteMsg.setOnClickListener {
            deleteMsg(message)
            dialog.dismiss()
        }
        bindingDelete.cancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun deleteEveryone(message: Messages){
        //xác định ID của tin nhắn cần cập nhật trong cơ sở dữ liệu.
        //message.messageId được truyền vào biến it, từ đó xác định ID của tin nhắn cần xoá.
        message.messageId.let {
            //Xoá tin nhắn trong phòng chat của người gửi
            FirebaseDatabase.getInstance().reference.child("chats")
                .child(senderRoom).child("messages")
                .child(it!!).removeValue()
        }
        //cập nhật tin nhắn trong cơ sở dữ liệu cho người nhận tin nhắn.
        message.messageId.let {
            //Xoá tin nhắn trong phòng chat của người nhận
            FirebaseDatabase.getInstance().reference.child("chats")
                .child(receiverRoom).child("messages")
                .child(it!!).removeValue()
        }
    }

    private fun deleteMsg(message: Messages){
        message.messageId.let {
            FirebaseDatabase.getInstance().reference.child("chats")
                .child(senderRoom).child("messages")
                .child(it!!).removeValue()
        }
    }

    private fun editMsg(message: Messages){
        message.messageId.let {
            //edit tin nhắn trong phòng chat của người gửi
            FirebaseDatabase.getInstance().reference.child("chats")
                .child(senderRoom).child("messages")
                .child(it!!).setValue(message)
        }
        message.messageId.let {
            //edit tin nhắn trong phòng chat của người nhận
            FirebaseDatabase.getInstance().reference.child("chats")
                .child(receiverRoom).child("messages")
                .child(it!!).setValue(message)
        }
    }
}