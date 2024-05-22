package com.htduc.socialmediaapplication.Model

import android.os.Parcel
import android.os.Parcelable

class Post(
   var postId: String?= null,
   var postImage: String?= null,
   var postedBy: String?= null,
   var postDescription: String?= null,
   var postedAt: Long?= null,
   var postLike: Int = 0,
   var commentCount: Int = 0
)