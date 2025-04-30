package com.htduc.socialmediaapplication.Models

import android.os.Parcel
import android.os.Parcelable

class Messages(
    var messageId: String? = null,
    var message: String? = null,
    var senderId: String? = null,
    var imageUrl: String? = null,
    var timeStamp: Long = 0
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readLong()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(messageId)
        parcel.writeString(message)
        parcel.writeString(senderId)
        parcel.writeString(imageUrl)
        parcel.writeLong(timeStamp)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Messages> {
        override fun createFromParcel(parcel: Parcel): Messages {
            return Messages(parcel)
        }

        override fun newArray(size: Int): Array<Messages?> {
            return arrayOfNulls(size)
        }
    }
}