package com.htduc.socialmediaapplication.Model

import android.os.Parcel
import android.os.Parcelable

class Notification (
    var notificationId: String? = null,
    var notificationBy: String? = null,
    var notificationAt: Long = 0,
    var type: String? = null,
    var postId: String? = null,
    var postBy: String? = null,//id của người đăng bài
    var followed: String? = null, //lấy id của người đc followed
    var checkOpen: Boolean = false //đại diện cho tbao có dc mở hay chưa
)//: Parcelable {
//    constructor(parcel: Parcel) : this(
//        parcel.readString(),
//        parcel.readString(),
//        parcel.readLong(),
//        parcel.readString(),
//        parcel.readString(),
//        parcel.readString(),
//        parcel.readString(),
//        parcel.readByte() != 0.toByte()
//    ) {
//    }
//
//    override fun writeToParcel(parcel: Parcel, flags: Int) {
//        parcel.writeString(notificationId)
//        parcel.writeString(notificationBy)
//        parcel.writeLong(notificationAt)
//        parcel.writeString(type)
//        parcel.writeString(postId)
//        parcel.writeString(postBy)
//        parcel.writeString(followed)
//        parcel.writeByte(if (checkOpen) 1 else 0)
//    }
//
//    override fun describeContents(): Int {
//        return 0
//    }
//
//    companion object CREATOR : Parcelable.Creator<Notification> {
//        override fun createFromParcel(parcel: Parcel): Notification {
//            return Notification(parcel)
//        }
//
//        override fun newArray(size: Int): Array<Notification?> {
//            return arrayOfNulls(size)
//        }
//    }
//}