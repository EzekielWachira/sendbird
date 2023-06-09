package com.ezzy.sendbird.utils

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.ezzy.sendbird.BaseApplication
import com.google.firebase.messaging.FirebaseMessaging
import com.sendbird.calls.SendBirdCall.registerPushToken
import com.sendbird.calls.SendBirdException
import com.sendbird.calls.handler.CompletionHandler

object PushUtils {
    fun getPushToken(context: Context?, handler: (token: String?, e: SendBirdException?) -> Unit) {
        Log.i(BaseApplication.TAG, "[PushUtils] getPushToken()")
        val savedToken = PrefUtils.getPushToken(context!!)
        if (TextUtils.isEmpty(savedToken)) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful()) {
                    Log.i(
                        BaseApplication.TAG,
                        "[PushUtils] getPushToken() => getInstanceId failed",
                        task.getException()
                    )
                    handler.invoke(
                        null,
                        SendBirdException(
                            if (task.getException() != null) task.getException()?.message else ""
                        )
                    )
                    return@addOnCompleteListener
                }
                val pushToken =
                    if (task.result != null) task.result else ""
                Log.i(
                    BaseApplication.TAG,
                    "[PushUtils] getPushToken() => pushToken: $pushToken"
                )
                handler.invoke(pushToken, null)
            }
        } else {
            Log.i(BaseApplication.TAG, "[PushUtils] savedToken: $savedToken")
            handler.invoke(savedToken, null)
        }
    }

    fun registerPushToken(context: Context?, pushToken: String, handler: PushTokenHandler?) {
        Log.i(
            BaseApplication.TAG,
            "[PushUtils] registerPushToken(pushToken: $pushToken)"
        )
        registerPushToken(pushToken, false, CompletionHandler { e: SendBirdException? ->
            if (e != null) {
                Log.i(
                    BaseApplication.TAG,
                    "[PushUtils] registerPushToken() => e: " + e.message
                )
                PrefUtils.setPushToken(context!!, pushToken)
                handler?.onResult(e)
                return@CompletionHandler
            }
            Log.i(BaseApplication.TAG, "[PushUtils] registerPushToken() => OK")
            PrefUtils.setPushToken(context!!, pushToken)
            handler?.onResult(null)
        })
    }

    interface GetPushTokenHandler {
        fun onResult(token: String?, e: SendBirdException?)
    }

    interface PushTokenHandler {
        fun onResult(e: SendBirdException?)
    }
}
