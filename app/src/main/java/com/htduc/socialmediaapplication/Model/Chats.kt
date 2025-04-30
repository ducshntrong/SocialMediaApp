package com.htduc.socialmediaapplication.Model

class Chats(
    var user: User = User(),
    var lastMessage: String = "",
    var lastMessageTime: Long = 0
)