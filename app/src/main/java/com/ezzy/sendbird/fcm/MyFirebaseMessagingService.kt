package com.ezzy.sendbird.fcm

import android.util.Log
import com.ezzy.sendbird.BaseApplication
import com.ezzy.sendbird.utils.PrefUtils
import com.ezzy.sendbird.utils.PushUtils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sendbird.calls.SendBirdCall.currentUser
import com.sendbird.calls.SendBirdCall.handleFirebaseMessageData
import com.sendbird.calls.SendBirdException

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (handleFirebaseMessageData(remoteMessage.data)) {
            Log.i(
                BaseApplication.TAG,
                "[MyFirebaseMessagingService] onMessageReceived() => " + remoteMessage.data.toString()
            )
        }
    }

    override fun onNewToken(token: String) {
        Log.i(
            BaseApplication.TAG,
            "[MyFirebaseMessagingService] onNewToken(token: $token)"
        )
        if (currentUser != null) {
            PushUtils.registerPushToken(applicationContext, token, object :
                PushUtils.PushTokenHandler {
                override fun onResult(e: SendBirdException?) {
                    if (e != null) {
                        Log.i(
                            BaseApplication.TAG,
                            "[MyFirebaseMessagingService] registerPushTokenForCurrentUser() => e: " + e.message
                        )
                    }
                }

            })
        } else {
            PrefUtils.setPushToken(applicationContext, token)
        }
    }
}