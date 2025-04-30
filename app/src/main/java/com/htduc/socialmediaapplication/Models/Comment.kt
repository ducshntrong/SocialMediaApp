package com.htduc.socialmediaapplication.Models

class Comment(
    var commentBody: String? = null,
    var commentedAt: Long = 0,
    var commentedBy: String? = null,
    var commentImg: String? = null
)