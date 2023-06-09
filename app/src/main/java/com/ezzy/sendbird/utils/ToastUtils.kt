package com.ezzy.sendbird.utils

import android.content.Context
import android.text.TextUtils
import android.widget.Toast

object ToastUtils {
    fun showToast(context: Context?, text: String?) {
        if (context != null && !TextUtils.isEmpty(text)) {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
    }
}
