package com.htduc.socialmediaapplication.Model

import android.os.Parcel
import android.os.Parcelable

data class Post(
   var postId: String? = null,
   var postImage: String? = null,
   var postedBy: String? = null,
   var postDescription: String? = null,
   var postedAt: Long? = null,
   var postLike: Int = 0,
   var commentCount: Int = 0
) : Parcelable {
   constructor(parcel: Parcel) : this(
      parcel.readString(),
      parcel.readString(),
      parcel.readString(),
      parcel.readString(),
      parcel.readValue(Long::class.java.classLoader) as? Long,
      parcel.readInt(),
      parcel.readInt()
   )

   override fun writeToParcel(parcel: Parcel, flags: Int) {
      parcel.writeString(postId)
      parcel.writeString(postImage)
      parcel.writeString(postedBy)
      parcel.writeString(postDescription)
      parcel.writeValue(postedAt)
      parcel.writeInt(postLike)
      parcel.writeInt(commentCount)
   }

   override fun describeContents(): Int {
      return 0
   }

   companion object CREATOR : Parcelable.Creator<Post> {
      override fun createFromParcel(parcel: Parcel): Post {
         return Post(parcel)
      }

      override fun newArray(size: Int): Array<Post?> {
         return arrayOfNulls(size)
      }
   }
}
