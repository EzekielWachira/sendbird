package com.ezzy.sendbird.call

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ezzy.sendbird.BaseApplication
import com.ezzy.sendbird.R
import com.ezzy.sendbird.utils.ToastUtils
import com.ezzy.sendbird.utils.UserInfoUtils
import com.sendbird.calls.DirectCall
import com.sendbird.calls.SendBirdCall.ongoingCallCount

class CallService : Service() {
    private var mContext: Context? = null
    private val mBinder: IBinder = CallBinder()
    private val mServiceData = ServiceData()

    internal inner class CallBinder : Binder() {
        val service: CallService
            get() = this@CallService
    }

    class ServiceData {
        var isHeadsUpNotification = false
        var remoteNicknameOrUserId: String? = null
        var callState: CallActivity.STATE? = null
        var callId: String? = null
        var isVideoCall = false
        var calleeIdToDial: String? = null
        var doDial = false
        var doAccept = false
        var doLocalVideoStart = false
        fun set(serviceData: ServiceData) {
            isHeadsUpNotification = serviceData.isHeadsUpNotification
            remoteNicknameOrUserId = serviceData.remoteNicknameOrUserId
            callState = serviceData.callState
            callId = serviceData.callId
            isVideoCall = serviceData.isVideoCall
            calleeIdToDial = serviceData.calleeIdToDial
            doDial = serviceData.doDial
            doAccept = serviceData.doAccept
            doLocalVideoStart = serviceData.doLocalVideoStart
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.i(BaseApplication.TAG, "[CallService] onBind()")
        return mBinder
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(BaseApplication.TAG, "[CallService] onCreate()")
        mContext = this
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(BaseApplication.TAG, "[CallService] onDestroy()")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i(BaseApplication.TAG, "[CallService] onStartCommand()")
        mServiceData.isHeadsUpNotification =
            intent.getBooleanExtra(EXTRA_IS_HEADS_UP_NOTIFICATION, false)
        mServiceData.remoteNicknameOrUserId =
            intent.getStringExtra(EXTRA_REMOTE_NICKNAME_OR_USER_ID)
        mServiceData.callState = intent.getSerializableExtra(EXTRA_CALL_STATE) as CallActivity.STATE?
        mServiceData.callId = intent.getStringExtra(EXTRA_CALL_ID)
        mServiceData.isVideoCall = intent.getBooleanExtra(EXTRA_IS_VIDEO_CALL, false)
        mServiceData.calleeIdToDial = intent.getStringExtra(EXTRA_CALLEE_ID_TO_DIAL)
        mServiceData.doDial = intent.getBooleanExtra(EXTRA_DO_DIAL, false)
        mServiceData.doAccept = intent.getBooleanExtra(EXTRA_DO_ACCEPT, false)
        mServiceData.doLocalVideoStart = intent.getBooleanExtra(EXTRA_DO_LOCAL_VIDEO_START, false)
        updateNotification(mServiceData)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        Log.i(BaseApplication.TAG, "[CallService] onTaskRemoved()")
        mServiceData.isHeadsUpNotification = true
        updateNotification(mServiceData)
    }

    private fun getNotification(serviceData: ServiceData): Notification {
        val content: String
        content = if (serviceData.isVideoCall) {
            mContext!!.getString(
                R.string.calls_notification_video_calling_content,
                mContext!!.getString(R.string.calls_app_name)
            )
        } else {
            mContext!!.getString(
                R.string.calls_notification_voice_calling_content,
                mContext!!.getString(R.string.calls_app_name)
            )
        }
        val currentTime = System.currentTimeMillis().toInt()
        val channelId = mContext!!.packageName + currentTime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = mContext!!.getString(R.string.calls_app_name)
            val channel = NotificationChannel(
                channelId, channelName,
                if (serviceData.isHeadsUpNotification) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = mContext!!.getSystemService(
                NotificationManager::class.java
            )
            notificationManager?.createNotificationChannel(channel)
        }
        val pendingIntentFlag: Int
        pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val callIntent = getCallActivityIntent(mContext, serviceData, false)
        val callPendingIntent = PendingIntent.getActivity(
            mContext,
            currentTime + 1, callIntent, pendingIntentFlag
        )
        val endIntent = getCallActivityIntent(mContext, serviceData, true)
        val endPendingIntent = PendingIntent.getActivity(
            mContext,
            currentTime + 2, endIntent, pendingIntentFlag
        )
        val builder = NotificationCompat.Builder(mContext!!, channelId)
        builder.setContentTitle(serviceData.remoteNicknameOrUserId)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_sendbird)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    mContext!!.resources,
                    R.drawable.icon_push_oreo
                )
            )
            .setPriority(if (serviceData.isHeadsUpNotification) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
        if (ongoingCallCount > 0) {
            if (serviceData.doAccept) {
                builder.addAction(
                    NotificationCompat.Action(
                        0,
                        mContext!!.getString(R.string.calls_notification_decline),
                        endPendingIntent
                    )
                )
                builder.addAction(
                    NotificationCompat.Action(
                        0,
                        mContext!!.getString(R.string.calls_notification_accept),
                        callPendingIntent
                    )
                )
            } else {
                builder.setContentIntent(callPendingIntent)
                builder.addAction(
                    NotificationCompat.Action(
                        0,
                        mContext!!.getString(R.string.calls_notification_end),
                        endPendingIntent
                    )
                )
            }
        }
        return builder.build()
    }

    fun updateNotification(serviceData: ServiceData) {
        Log.i(
            BaseApplication.TAG,
            "[CallService] updateNotification(isHeadsUpNotification: " + serviceData.isHeadsUpNotification + ", remoteNicknameOrUserId: " + serviceData.remoteNicknameOrUserId
                    + ", callState: " + serviceData.callState + ", callId: " + serviceData.callId + ", isVideoCall: " + serviceData.isVideoCall
                    + ", calleeIdToDial: " + serviceData.calleeIdToDial + ", doDial: " + serviceData.doDial + ", doAccept: " + serviceData.doAccept + ", doLocalVideoStart: " + serviceData.doLocalVideoStart + ")"
        )
        mServiceData.set(serviceData)
        startForeground(NOTIFICATION_ID, getNotification(mServiceData))
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        const val EXTRA_IS_HEADS_UP_NOTIFICATION = "is_heads_up_notification"
        const val EXTRA_REMOTE_NICKNAME_OR_USER_ID = "remote_nickname_or_user_id"
        const val EXTRA_CALL_STATE = "call_state"
        const val EXTRA_CALL_ID = "call_id"
        const val EXTRA_IS_VIDEO_CALL = "is_video_call"
        const val EXTRA_CALLEE_ID_TO_DIAL = "callee_id_to_dial"
        const val EXTRA_DO_DIAL = "do_dial"
        const val EXTRA_DO_ACCEPT = "do_accept"
        const val EXTRA_DO_LOCAL_VIDEO_START = "do_local_video_start"
        const val EXTRA_DO_END = "do_end"
        private fun getCallActivityIntent(
            context: Context?,
            serviceData: ServiceData,
            doEnd: Boolean
        ): Intent {
            val intent: Intent
            intent = if (serviceData.isVideoCall) {
                Intent(context, VideoCallActivity::class.java)
            } else {
                Intent(context, VoiceCallActivity::class.java)
            }
            intent.putExtra(EXTRA_CALL_STATE, serviceData.callState)
            intent.putExtra(EXTRA_CALL_ID, serviceData.callId)
            intent.putExtra(EXTRA_IS_VIDEO_CALL, serviceData.isVideoCall)
            intent.putExtra(EXTRA_CALLEE_ID_TO_DIAL, serviceData.calleeIdToDial)
            intent.putExtra(EXTRA_DO_DIAL, serviceData.doDial)
            intent.putExtra(EXTRA_DO_ACCEPT, serviceData.doAccept)
            intent.putExtra(EXTRA_DO_LOCAL_VIDEO_START, serviceData.doLocalVideoStart)
            intent.putExtra(EXTRA_DO_END, doEnd)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            return intent
        }

        fun dial(context: Context, doDialWithCalleeId: String?, isVideoCall: Boolean) {
            if (ongoingCallCount > 0) {
                ToastUtils.showToast(context, "Ringing.")
                Log.i(
                    BaseApplication.TAG,
                    "[CallService] dial() => SendBirdCall.getOngoingCallCount(): $ongoingCallCount"
                )
                return
            }
            Log.i(BaseApplication.TAG, "[CallService] dial()")
            val serviceData = ServiceData()
            serviceData.isHeadsUpNotification = false
            serviceData.remoteNicknameOrUserId = doDialWithCalleeId
            serviceData.callState = CallActivity.STATE.STATE_OUTGOING
            serviceData.callId = null
            serviceData.isVideoCall = isVideoCall
            serviceData.calleeIdToDial = doDialWithCalleeId
            serviceData.doDial = true
            serviceData.doAccept = false
            serviceData.doLocalVideoStart = false
            startService(context, serviceData)
            context.startActivity(getCallActivityIntent(context, serviceData, false))
        }

        fun onRinging(context: Context?, call: DirectCall) {
            Log.i(BaseApplication.TAG, "[CallService] onRinging()")
            val serviceData = ServiceData()
            serviceData.isHeadsUpNotification = true
            serviceData.remoteNicknameOrUserId = UserInfoUtils.getNicknameOrUserId(call.remoteUser)
            serviceData.callState = CallActivity.STATE.STATE_ACCEPTING
            serviceData.callId = call.callId
            serviceData.isVideoCall = call.isVideoCall
            serviceData.calleeIdToDial = null
            serviceData.doDial = false
            serviceData.doAccept = true
            serviceData.doLocalVideoStart = false
            startService(context, serviceData)
        }

        private fun startService(context: Context?, serviceData: ServiceData) {
            Log.i(BaseApplication.TAG, "[CallService] startService()")
            if (context != null) {
                val intent = Intent(context, CallService::class.java)
                intent.putExtra(EXTRA_IS_HEADS_UP_NOTIFICATION, serviceData.isHeadsUpNotification)
                intent.putExtra(
                    EXTRA_REMOTE_NICKNAME_OR_USER_ID,
                    serviceData.remoteNicknameOrUserId
                )
                intent.putExtra(EXTRA_CALL_STATE, serviceData.callState)
                intent.putExtra(EXTRA_CALL_ID, serviceData.callId)
                intent.putExtra(EXTRA_IS_VIDEO_CALL, serviceData.isVideoCall)
                intent.putExtra(EXTRA_CALLEE_ID_TO_DIAL, serviceData.calleeIdToDial)
                intent.putExtra(EXTRA_DO_DIAL, serviceData.doDial)
                intent.putExtra(EXTRA_DO_ACCEPT, serviceData.doAccept)
                intent.putExtra(EXTRA_DO_LOCAL_VIDEO_START, serviceData.doLocalVideoStart)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stopService(context: Context?) {
            Log.i(BaseApplication.TAG, "[CallService] stopService()")
            if (context != null) {
                val intent = Intent(context, CallService::class.java)
                context.stopService(intent)
            }
        }
    }
}