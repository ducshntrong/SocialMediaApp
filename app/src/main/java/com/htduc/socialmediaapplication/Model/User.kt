package com.htduc.socialmediaapplication.Model

import android.animation.AnimatorInflater
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.widget.AppCompatButton
import com.htduc.socialmediaapplication.R

data class User(
    var uid: String? = null,
    var name: String? = null,
    var profession: String? = null,
    var email: String? = null,
    var password: String? = null,
    var coverPhoto: String? = null,
    var profilePhoto: String? = null,
    var birthday: String? = null,
    var phone: Int = 0,
    var gender: String? = null,
    var followerCount: Int = 0,
    var isFollowing: Boolean = false,

    // Biến để quản lý vi phạm và trạng thái chặn người dùng
    var violationCount: Int = 0,                     // Số lần vi phạm
    var violationHistory: MutableList<Long> = mutableListOf(), // Lịch sử vi phạm (timestamp)
    var isBlocked: Boolean = false,                  // Trạng thái bị chặn
    var blockUntil: Long = 0                         // Thời gian bị chặn đến (timestamp)
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.readInt(),
        mutableListOf<Long>().apply {
            parcel.readList(this, Long::class.java.classLoader)
        },
        parcel.readByte() != 0.toByte(),
        parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(uid)
        parcel.writeString(name)
        parcel.writeString(profession)
        parcel.writeString(email)
        parcel.writeString(password)
        parcel.writeString(coverPhoto)
        parcel.writeString(profilePhoto)
        parcel.writeString(birthday)
        parcel.writeInt(phone)
        parcel.writeString(gender)
        parcel.writeInt(followerCount)
        parcel.writeByte(if (isFollowing) 1 else 0)
        parcel.writeInt(violationCount)
        parcel.writeList(violationHistory)
        parcel.writeByte(if (isBlocked) 1 else 0)
        parcel.writeLong(blockUntil)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }
    }
}

fun applyClickAnimation(context: Context, button: ImageButton, clickHandler: () -> Unit) {
    val mAnimator = AnimatorInflater.loadAnimator(context, R.animator.button_pressed)
    button.setOnClickListener {
        mAnimator.setTarget(it)
        mAnimator.start()
        clickHandler.invoke()
    }
}
fun applyClickAnimation2(context: Context, button: Button, clickHandler: () -> Unit) {
    val mAnimator = AnimatorInflater.loadAnimator(context, R.animator.button_pressed)
    button.setOnClickListener {
        mAnimator.setTarget(it)
        mAnimator.start()
        clickHandler.invoke()
    }
}
