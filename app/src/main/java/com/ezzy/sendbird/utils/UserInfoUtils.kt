package com.ezzy.sendbird.utils

import android.content.Context
import android.text.TextUtils
import android.widget.ImageView
import android.widget.TextView
import com.ezzy.sendbird.R
import com.sendbird.calls.User

object UserInfoUtils {

    fun setProfileImage(context: Context?, user: User?, imageViewProfile: ImageView?) {
        if (user != null && imageViewProfile != null) {
            val profileUrl = user.profileUrl
            if (TextUtils.isEmpty(profileUrl)) {
                imageViewProfile.setBackgroundResource(R.drawable.icon_avatar)
            } else {
                ImageUtils.displayCircularImageFromUrl(context, user.profileUrl, imageViewProfile)
            }
        }
    }

    fun setNickname(context: Context, user: User?, textViewNickname: TextView?) {
        if (user != null && textViewNickname != null) {
            val nickname = user.nickname
            if (TextUtils.isEmpty(nickname)) {
                textViewNickname.text = context.getString(R.string.calls_empty_nickname)
            } else {
                textViewNickname.text = nickname
            }
        }
    }

    fun setUserId(context: Context, user: User?, textViewUserId: TextView?) {
        if (user != null && textViewUserId != null) {
            textViewUserId.text = context.getString(R.string.calls_user_id_format, user.userId)
        }
    }

    fun setNicknameOrUserId(user: User?, textViewNickname: TextView?) {
        if (user != null && textViewNickname != null) {
            val nickname = user.nickname
            if (TextUtils.isEmpty(nickname)) {
                textViewNickname.text = user.userId
            } else {
                textViewNickname.text = nickname
            }
        }
    }

    fun getNicknameOrUserId(user: User?): String? {
        var nicknameOrUserId: String? = ""
        if (user != null) {
            nicknameOrUserId = user.nickname
            if (TextUtils.isEmpty(nicknameOrUserId)) {
                nicknameOrUserId = user.userId
            }
        }
        return nicknameOrUserId
    }
}
