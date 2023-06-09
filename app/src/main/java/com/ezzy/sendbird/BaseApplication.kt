package com.ezzy.sendbird

import android.app.Application
import android.text.TextUtils
import android.util.Log
import com.ezzy.sendbird.call.CallService
import com.ezzy.sendbird.utils.BroadcastUtils
import com.ezzy.sendbird.utils.PrefUtils
import com.sendbird.calls.DirectCall
import com.sendbird.calls.RoomInvitation
import com.sendbird.calls.SendBirdCall.Options.addDirectCallSound
import com.sendbird.calls.SendBirdCall.SoundType
import com.sendbird.calls.SendBirdCall.addListener
import com.sendbird.calls.SendBirdCall.init
import com.sendbird.calls.SendBirdCall.ongoingCallCount
import com.sendbird.calls.SendBirdCall.removeAllListeners
import com.sendbird.calls.handler.DirectCallListener
import com.sendbird.calls.handler.SendBirdCallListener
import java.util.UUID

const val APP_ID = "50085EC7-490B-4007-8F52-C549658EFD63"


class BaseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "[BaseApplication] onCreate()")
        PrefUtils.getAppId(applicationContext)?.let { initSendBirdCall(it) }
    }

    fun initSendBirdCall(appId: String): Boolean {
        var appId = appId
        Log.i(TAG, "[BaseApplication] initSendBirdCall(appId: $appId)")
        val context = applicationContext
        if (TextUtils.isEmpty(appId)) {
            appId = APP_ID
        }
        if (init(context, appId)) {
            removeAllListeners()
            addListener(UUID.randomUUID().toString(), object : SendBirdCallListener() {
                override fun onInvitationReceived(roomInvitation: RoomInvitation) {
                    Log.i(
                        TAG,
                        "[BaseApplication] onInvitationReceived() => roomInvitation: " + roomInvitation.roomInvitationId
                    )
                }

                override fun onRinging(call: DirectCall) {
                    val ongoingCallCount = ongoingCallCount
                    Log.i(
                        TAG,
                        "[BaseApplication] onRinging() => callId: " + call.callId + ", getOngoingCallCount(): " + ongoingCallCount
                    )
                    if (ongoingCallCount >= 2) {
                        call.end()
                        return
                    }
                    call.setListener(object : DirectCallListener() {
                        override fun onConnected(call: DirectCall) {}
                        override fun onEnded(call: DirectCall) {
                            val ongoingCallCount = ongoingCallCount
                            Log.i(
                                TAG,
                                "[BaseApplication] onEnded() => callId: " + call.callId + ", getOngoingCallCount(): " + ongoingCallCount
                            )
                            BroadcastUtils.sendCallLogBroadcast(context, call.callLog)
                            if (ongoingCallCount == 0) {
                                CallService.stopService(context)
                            }
                        }
                    })
                    CallService.onRinging(context, call)
                }
            })
            addDirectCallSound(SoundType.DIALING, R.raw.dialing)
            addDirectCallSound(SoundType.RINGING, R.raw.ringing)
            addDirectCallSound(SoundType.RECONNECTING, R.raw.reconnecting)
            addDirectCallSound(SoundType.RECONNECTED, R.raw.reconnected)
            return true
        }
        return false
    }

    companion object {
        const val TAG = "BaseApplication"
        const val APP_ID = "50085EC7-490B-4007-8F52-C549658EFD63"

        // multidex
        const val VERSION = "1.4.0"
    }

}