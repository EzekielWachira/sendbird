package com.ezzy.sendbird.call

import android.annotation.TargetApi
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ezzy.sendbird.BaseApplication
import com.ezzy.sendbird.R
import com.ezzy.sendbird.utils.AuthenticationUtils
import com.ezzy.sendbird.utils.BroadcastUtils
import com.ezzy.sendbird.utils.EndResultUtils
import com.ezzy.sendbird.utils.UserInfoUtils
import com.sendbird.calls.AudioDevice
import com.sendbird.calls.DirectCall
import com.sendbird.calls.SendBirdCall.currentUser
import com.sendbird.calls.SendBirdCall.getCall
import com.sendbird.calls.handler.DirectCallListener
import java.util.Timer
import java.util.TimerTask

abstract class CallActivity : AppCompatActivity() {
    enum class STATE {
        STATE_ACCEPTING, STATE_OUTGOING, STATE_CONNECTED, STATE_ENDING, STATE_ENDED
    }

    var mContext: Context? = null
    var mState: STATE? = null
    private var mCallId: String? = null
    var mIsVideoCall = false
    var mCalleeIdToDial: String? = null
    private var mDoDial = false
    private var mDoAccept = false
    protected var mDoLocalVideoStart = false
    private var mDoEnd = false
    var mDirectCall: DirectCall? = null
    var mIsAudioEnabled = false
    private var mEndingTimer: Timer? = null

    //+ Views
    var mLinearLayoutInfo: LinearLayout? = null
    var mImageViewProfile: ImageView? = null
    var mTextViewUserId: TextView? = null
    var mTextViewStatus: TextView? = null
    var mLinearLayoutRemoteMute: LinearLayout? = null
    var mTextViewRemoteMute: TextView? = null
    var mRelativeLayoutRingingButtons: RelativeLayout? = null
    var mImageViewDecline: ImageView? = null
    var mLinearLayoutConnectingButtons: LinearLayout? = null
    var mImageViewAudioOff: ImageView? = null
    var mImageViewBluetooth: ImageView? = null
    var mImageViewEnd: ImageView? = null
    protected abstract val layoutResourceId: Int

    protected abstract fun setAudioDevice(
        currentAudioDevice: AudioDevice?,
        availableAudioDevices: Set<AudioDevice>?
    )

    protected abstract fun startCall(amICallee: Boolean)

    //- abstract methods
    //+ CallService
    private var mCallService: CallService? = null
    private var mBound = false

    //- CallService
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(BaseApplication.TAG, "[CallActivity] onCreate()")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        window.decorView.systemUiVisibility = systemUiVisibility
        setContentView(layoutResourceId)
        mContext = this
        bindCallService()
        init()
        initViews()
        setViews()
        setAudioDevice()
        setCurrentState()
        if (mDoEnd) {
            Log.i(BaseApplication.TAG, "[CallActivity] init() => (mDoEnd == true)")
            end()
            return
        }
        checkAuthentication()
    }

    private fun init() {
        val intent = intent
        mState = intent.getSerializableExtra(CallService.EXTRA_CALL_STATE) as STATE?
        mCallId = intent.getStringExtra(CallService.EXTRA_CALL_ID)
        mIsVideoCall = intent.getBooleanExtra(CallService.EXTRA_IS_VIDEO_CALL, false)
        mCalleeIdToDial = intent.getStringExtra(CallService.EXTRA_CALLEE_ID_TO_DIAL)
        mDoDial = intent.getBooleanExtra(CallService.EXTRA_DO_DIAL, false)
        mDoAccept = intent.getBooleanExtra(CallService.EXTRA_DO_ACCEPT, false)
        mDoLocalVideoStart = intent.getBooleanExtra(CallService.EXTRA_DO_LOCAL_VIDEO_START, false)
        mDoEnd = intent.getBooleanExtra(CallService.EXTRA_DO_END, false)
        Log.i(
            BaseApplication.TAG,
            ("[CallActivity] init() => (mState: " + mState + ", mCallId: " + mCallId + ", mIsVideoCall: " + mIsVideoCall
                    + ", mCalleeIdToDial: " + mCalleeIdToDial + ", mDoDial: " + mDoDial + ", mDoAccept: " + mDoAccept + ", mDoLocalVideoStart: " + mDoLocalVideoStart
                    + ", mDoEnd: " + mDoEnd + ")")
        )
        if (mCallId != null) {
            mDirectCall = getCall(mCallId!!)
            setListener(mDirectCall)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.i(BaseApplication.TAG, "[CallActivity] onNewIntent()")
        mDoEnd = intent.getBooleanExtra(CallService.EXTRA_DO_END, false)
        if (mDoEnd) {
            Log.i(BaseApplication.TAG, "[CallActivity] onNewIntent() => (mDoEnd == true)")
            end()
        }
    }

    open fun initViews() {
        mLinearLayoutInfo = findViewById(R.id.linear_layout_info)
        mImageViewProfile = findViewById(R.id.image_view_profile)
        mTextViewUserId = findViewById(R.id.text_view_user_id)
        mTextViewStatus = findViewById(R.id.text_view_status)
        mLinearLayoutRemoteMute = findViewById(R.id.linear_layout_remote_mute)
        mTextViewRemoteMute = findViewById(R.id.text_view_remote_mute)
        mRelativeLayoutRingingButtons = findViewById(R.id.relative_layout_ringing_buttons)
        mImageViewDecline = findViewById(R.id.image_view_decline)
        mLinearLayoutConnectingButtons = findViewById(R.id.linear_layout_connecting_buttons)
        mImageViewAudioOff = findViewById(R.id.image_view_audio_off)
        mImageViewBluetooth = findViewById(R.id.image_view_bluetooth)
        mImageViewEnd = findViewById(R.id.image_view_end)
    }

    open fun setViews() {
        mImageViewDecline!!.setOnClickListener({ view: View? -> end() })
        if (mDirectCall != null) {
            mIsAudioEnabled = mDirectCall!!.isLocalAudioEnabled
        } else {
            mIsAudioEnabled = true
        }
        mImageViewAudioOff!!.isSelected = !mIsAudioEnabled
        mImageViewAudioOff!!.setOnClickListener { _: View? ->
            if (mDirectCall != null) {
                if (mIsAudioEnabled) {
                    Log.i(BaseApplication.TAG, "[CallActivity] mute()")
                    mDirectCall!!.muteMicrophone()
                    mIsAudioEnabled = false
                    mImageViewAudioOff!!.isSelected = true
                } else {
                    Log.i(BaseApplication.TAG, "[CallActivity] unmute()")
                    mDirectCall!!.unmuteMicrophone()
                    mIsAudioEnabled = true
                    mImageViewAudioOff!!.isSelected = false
                }
            }
        }
        mImageViewEnd!!.setOnClickListener { _: View? -> end() }
    }

    private fun setAudioDevice() {
        if (mDirectCall != null) {
            setAudioDevice(mDirectCall!!.currentAudioDevice, mDirectCall!!.availableAudioDevices)
        }
    }

    private fun setCurrentState() {
        setState(mState, mDirectCall)
    }

    protected fun setListener(call: DirectCall?) {
        Log.i(BaseApplication.TAG, "[CallActivity] setListener()")
        call?.setListener(object : DirectCallListener() {
            override fun onConnected(call: DirectCall) {
                Log.i(BaseApplication.TAG, "[CallActivity] onConnected()")
                setState(STATE.STATE_CONNECTED, call)
            }

            override fun onEnded(call: DirectCall) {
                Log.i(BaseApplication.TAG, "[CallActivity] onEnded()")
                setState(STATE.STATE_ENDED, call)
                BroadcastUtils.sendCallLogBroadcast(mContext, call.callLog)
            }

            override fun onRemoteVideoSettingsChanged(call: DirectCall) {
                Log.i(BaseApplication.TAG, "[CallActivity] onRemoteVideoSettingsChanged()")
            }

            override fun onLocalVideoSettingsChanged(call: DirectCall) {
                Log.i(BaseApplication.TAG, "[CallActivity] onLocalVideoSettingsChanged()")
                if (this@CallActivity is VideoCallActivity) {
                    this@CallActivity.setLocalVideoSettings(call)
                }
            }

            override fun onRemoteAudioSettingsChanged(call: DirectCall) {
                Log.i(BaseApplication.TAG, "[CallActivity] onRemoteAudioSettingsChanged()")
                setRemoteMuteInfo(call)
            }

            override fun onAudioDeviceChanged(
                call: DirectCall,
                currentAudioDevice: AudioDevice?,
                availableAudioDevices: MutableSet<AudioDevice>
            ) {
                Log.i(
                    BaseApplication.TAG,
                    "[CallActivity] onAudioDeviceChanged(currentAudioDevice: $currentAudioDevice, availableAudioDevices: $availableAudioDevices)"
                )
                setAudioDevice(currentAudioDevice, availableAudioDevices)
            }
        })
    }

    private fun checkAuthentication() {
        if (currentUser == null) {
            AuthenticationUtils.autoAuthenticate(mContext, object :
                AuthenticationUtils.AutoAuthenticateHandler {
                override fun onResult(userId: String?) {
                    if (userId == null) {
                        finishWithEnding("autoAuthenticate() failed.")
                        return
                    }
                    ready()
                }

            })
        } else {
            ready()
        }
    }

    private fun ready() {
        if (mDoDial) {
            mDoDial = false
            startCall(false)
        } else if (mDoAccept) {
            mDoAccept = false
            startCall(true)
        }
    }

    protected open fun setState(state: STATE?, call: DirectCall?): Boolean {
        mState = state
        updateCallService()
        when (state) {
            STATE.STATE_ACCEPTING -> {
                mLinearLayoutInfo!!.visibility = View.VISIBLE
                mLinearLayoutRemoteMute!!.visibility = View.GONE
                mRelativeLayoutRingingButtons!!.visibility = View.VISIBLE
                mLinearLayoutConnectingButtons!!.visibility = View.GONE
                if (mIsVideoCall) {
                    setInfo(call, getString(R.string.calls_incoming_video_call))
                } else {
                    setInfo(call, getString(R.string.calls_incoming_voice_call))
                }
                mImageViewDecline!!.setBackgroundResource(R.drawable.btn_call_decline)
                setInfo(call, getString(R.string.calls_connecting_call))
            }

            STATE.STATE_OUTGOING -> {
                mLinearLayoutInfo!!.visibility = View.VISIBLE
                mImageViewProfile!!.visibility = View.GONE
                mLinearLayoutRemoteMute!!.visibility = View.GONE
                mRelativeLayoutRingingButtons!!.visibility = View.GONE
                mLinearLayoutConnectingButtons!!.visibility = View.VISIBLE
                if (mIsVideoCall) {
                    setInfo(call, getString(R.string.calls_video_calling))
                } else {
                    setInfo(call, getString(R.string.calls_calling))
                }
            }

            STATE.STATE_CONNECTED -> {
                mImageViewProfile!!.visibility = View.VISIBLE
                mLinearLayoutRemoteMute!!.visibility = View.VISIBLE
                mRelativeLayoutRingingButtons!!.visibility = View.GONE
                mLinearLayoutConnectingButtons!!.visibility = View.VISIBLE
                setRemoteMuteInfo(call)
            }

            STATE.STATE_ENDING -> {
                mLinearLayoutInfo!!.visibility = View.VISIBLE
                mImageViewProfile!!.visibility = View.VISIBLE
                mLinearLayoutRemoteMute!!.visibility = View.GONE
                mRelativeLayoutRingingButtons!!.visibility = View.GONE
                mLinearLayoutConnectingButtons!!.visibility = View.GONE
                if (mIsVideoCall) {
                    setInfo(call, getString(R.string.calls_ending_video_call))
                } else {
                    setInfo(call, getString(R.string.calls_ending_voice_call))
                }
            }

            STATE.STATE_ENDED -> {
                mLinearLayoutInfo!!.visibility = View.VISIBLE
                mImageViewProfile!!.visibility = View.VISIBLE
                mLinearLayoutRemoteMute!!.visibility = View.GONE
                mRelativeLayoutRingingButtons!!.visibility = View.GONE
                mLinearLayoutConnectingButtons!!.visibility = View.GONE
                var status = ""
                if (call != null) {
                    status =
                        mContext?.let { EndResultUtils.getEndResultString(it, call.endResult) }.toString()
                }
                setInfo(call, status)
                finishWithEnding(status)
            }

            else -> {}
        }
        return true
    }

    protected fun setInfo(call: DirectCall?, status: String?) {
        val remoteUser = (call?.remoteUser)
        if (remoteUser != null) {
            UserInfoUtils.setProfileImage(mContext, remoteUser, mImageViewProfile)
        }
        mTextViewUserId!!.text = getRemoteNicknameOrUserId(call)
        mTextViewStatus!!.visibility = View.VISIBLE
        if (status != null) {
            mTextViewStatus!!.text = status
        }
    }

    private fun getRemoteNicknameOrUserId(call: DirectCall?): String? {
        var remoteNicknameOrUserId = mCalleeIdToDial
        if (call != null) {
            remoteNicknameOrUserId = UserInfoUtils.getNicknameOrUserId(call.remoteUser)
        }
        return remoteNicknameOrUserId
    }

    private fun setRemoteMuteInfo(call: DirectCall?) {
        if ((call != null) && !call.isRemoteAudioEnabled && (call.remoteUser != null)) {
            mTextViewRemoteMute!!.text = getString(
                R.string.calls_muted_this_call,
                UserInfoUtils.getNicknameOrUserId(call.remoteUser)
            )
            mLinearLayoutRemoteMute!!.visibility = View.VISIBLE
        } else {
            mLinearLayoutRemoteMute!!.visibility = View.GONE
        }
    }

    override fun onBackPressed() {}
    private fun end() {
        if (mDirectCall != null) {
            Log.i(BaseApplication.TAG, "[CallActivity] end()")
            if (mState == STATE.STATE_ENDING || mState == STATE.STATE_ENDED) {
                Log.i(BaseApplication.TAG, "[CallActivity] Already ending call.")
                return
            }
            if (mDirectCall!!.isEnded) {
                setState(STATE.STATE_ENDED, mDirectCall)
            } else {
                setState(STATE.STATE_ENDING, mDirectCall)
                mDirectCall!!.end()
            }
        } else {
            Log.i(BaseApplication.TAG, "[CallActivity] end() => (mDirectCall == null)")
            finishWithEnding("(mDirectCall == null)")
        }
    }

    protected fun finishWithEnding(log: String) {
        Log.i(BaseApplication.TAG, "[CallActivity] finishWithEnding($log)")
        if (mEndingTimer == null) {
            mEndingTimer = Timer()
            mEndingTimer!!.schedule(object : TimerTask() {
                override fun run() {
                    runOnUiThread({
                        Log.i(BaseApplication.TAG, "[CallActivity] finish()")
                        finish()
                        unbindCallService()
                        stopCallService()
                    })
                }
            }, ENDING_TIME_MS.toLong())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(BaseApplication.TAG, "[CallActivity] onDestroy()")
        unbindCallService()
    }

    //+ CallService
    private val mCallServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            Log.i(BaseApplication.TAG, "[CallActivity] onServiceConnected()")
            val callBinder: CallService.CallBinder = iBinder as CallService.CallBinder
            mCallService = callBinder.service
            mBound = true
            updateCallService()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.i(BaseApplication.TAG, "[CallActivity] onServiceDisconnected()")
            mBound = false
        }
    }

    private fun bindCallService() {
        Log.i(BaseApplication.TAG, "[CallActivity] bindCallService()")
        bindService(Intent(this, CallService::class.java), mCallServiceConnection, BIND_AUTO_CREATE)
    }

    private fun unbindCallService() {
        Log.i(BaseApplication.TAG, "[CallActivity] unbindCallService()")
        if (mBound) {
            unbindService(mCallServiceConnection)
            mBound = false
        }
    }

    private fun stopCallService() {
        Log.i(BaseApplication.TAG, "[CallActivity] stopCallService()")
        CallService.stopService(mContext)
    }

    protected fun updateCallService() {
        if (mCallService != null) {
            Log.i(BaseApplication.TAG, "[CallActivity] updateCallService()")
            val serviceData: CallService.ServiceData = CallService.ServiceData()
            serviceData.isHeadsUpNotification = false
            serviceData.remoteNicknameOrUserId = getRemoteNicknameOrUserId(mDirectCall)
            serviceData.callState = mState
            serviceData.callId = (if (mDirectCall != null) mDirectCall!!.callId else mCallId)
            serviceData.isVideoCall = mIsVideoCall
            serviceData.calleeIdToDial = mCalleeIdToDial
            serviceData.doDial = mDoDial
            serviceData.doAccept = mDoAccept
            serviceData.doLocalVideoStart = mDoLocalVideoStart
            mCallService!!.updateNotification(serviceData)
        }
    } //- CallService

    companion object {
        val ENDING_TIME_MS = 1000

        @get:TargetApi(19)
        private val systemUiVisibility: Int
            private get() {
                var flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    flags = flags or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                }
                return flags
            }
    }
}