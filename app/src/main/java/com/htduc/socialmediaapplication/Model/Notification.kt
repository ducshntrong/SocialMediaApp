package com.htduc.socialmediaapplication.Model

class Notification (
    var notificationId: String? = null,
    var notificationBy: String? = null,
    var notificationAt: Long = 0,
    var type: String? = null,
    var postId: String? = null,
    var postBy: String? = null,//id của người đăng bài
    var followed: String? = null, //lấy id của người đc followed
    var checkOpen: Boolean = false //đại diện cho tbao có dc mở hay chưa
)