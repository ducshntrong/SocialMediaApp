package com.htduc.socialmediaapplication.Models

class Chats(
    var user: User = User(),
    var lastMessage: String = "",
    var lastMessageTime: Long = 0
)