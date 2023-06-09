package com.ezzy.sendbird.call

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.widget.ImageView
import com.ezzy.sendbird.BaseApplication
import com.ezzy.sendbird.R
import com.ezzy.sendbird.utils.TimeUtils
import com.ezzy.sendbird.utils.ToastUtils
import com.sendbird.calls.AcceptParams
import com.sendbird.calls.AudioDevice
import com.sendbird.calls.CallOptions
import com.sendbird.calls.DialParams
import com.sendbird.calls.DirectCall
import com.sendbird.calls.SendBirdCall.dial
import com.sendbird.calls.SendBirdException
import com.sendbird.calls.handler.DialHandler
import java.util.Timer
import java.util.TimerTask

class VoiceCallActivity : CallActivity() {

    override val layoutResourceId: Int
        get() = R.layout.activity_voice_call

    private var mCallDurationTimer: Timer? = null

    //+ Views
    private lateinit var mImageViewSpeakerphone: ImageView

    override fun initViews() {
        super.initViews()
        Log.i(BaseApplication.TAG, "[VoiceCallActivity] initViews()")
        mImageViewSpeakerphone = findViewById(R.id.image_view_speakerphone)
    }

    override fun setAudioDevice(
        currentAudioDevice: AudioDevice?,
        availableAudioDevices: Set<AudioDevice>?
    ) {
        when (currentAudioDevice) {
            AudioDevice.SPEAKERPHONE -> {
                mImageViewSpeakerphone.isSelected = true
                mImageViewBluetooth!!.isSelected = false
            }
            AudioDevice.BLUETOOTH -> {
                mImageViewSpeakerphone.isSelected = false
                mImageViewBluetooth!!.isSelected = true
            }
            else -> {
                mImageViewSpeakerphone.isSelected = false
            }
        }

        if (availableAudioDevices!!.contains(AudioDevice.SPEAKERPHONE)) {
            mImageViewSpeakerphone.isEnabled = true
        } else if (!mImageViewSpeakerphone.isSelected) {
            mImageViewSpeakerphone.isEnabled = false
        }

        if (availableAudioDevices.contains(AudioDevice.BLUETOOTH)) {
            mImageViewBluetooth!!.isEnabled = true
        } else if (!mImageViewBluetooth!!.isSelected) {
            mImageViewBluetooth!!.isEnabled = false
        }
    }

    override fun startCall(amICallee: Boolean) {
        val callOptions = CallOptions()
        callOptions.setAudioEnabled(mIsAudioEnabled)

        if (amICallee) {
            Log.i(BaseApplication.TAG, "[VoiceCallActivity] accept()")
            if (mDirectCall != null) {
                mDirectCall!!.accept(AcceptParams().setCallOptions(callOptions))
            }
        } else {
            Log.i(BaseApplication.TAG, "[VoiceCallActivity] dial()")
            mDirectCall = dial(DialParams(mCalleeIdToDial!!).setVideoCall(mIsVideoCall)
                .setCallOptions(callOptions),
                DialHandler { call: DirectCall?, e: SendBirdException? ->
                    if (e != null) {
                        Log.i(
                            BaseApplication.TAG,
                            "[VoiceCallActivity] dial() => e: " + e.message
                        )
                        if (e.message != null) {
                            ToastUtils.showToast(mContext, e.message)
                        }
                        finishWithEnding(e.message!!)
                        return@DialHandler
                    }
                    Log.i(BaseApplication.TAG, "[VoiceCallActivity] dial() => OK")
                    updateCallService()
                })
            setListener(mDirectCall)
        }
    }


    override fun setViews() {
        super.setViews()
        mImageViewSpeakerphone!!.setOnClickListener { view: View? ->
            if (mDirectCall != null) {
                mImageViewSpeakerphone.isSelected = !mImageViewSpeakerphone.isSelected
                if (mImageViewSpeakerphone.isSelected) {
                    mDirectCall!!.selectAudioDevice(AudioDevice.SPEAKERPHONE) { e ->
                        if (e != null) {
                            mImageViewSpeakerphone.isSelected = false
                        }
                    }
                } else {
                    mDirectCall!!.selectAudioDevice(AudioDevice.WIRED_HEADSET) { e ->
                        if (e != null) {
                            mDirectCall!!.selectAudioDevice(AudioDevice.EARPIECE, null)
                        }
                    }
                }
            }
        }
        mImageViewBluetooth!!.isEnabled = false
        mImageViewBluetooth!!.setOnClickListener { view ->
            if (mDirectCall != null) {
                mImageViewBluetooth!!.isSelected = !mImageViewBluetooth!!.isSelected
                if (mImageViewBluetooth!!.isSelected) {
                    mDirectCall!!.selectAudioDevice(AudioDevice.BLUETOOTH) { e ->
                        if (e != null) {
                            mImageViewBluetooth!!.isSelected = false
                        }
                    }
                } else {
                    mDirectCall!!.selectAudioDevice(AudioDevice.WIRED_HEADSET) { e ->
                        if (e != null) {
                            mDirectCall!!.selectAudioDevice(AudioDevice.EARPIECE, null)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun setState(state: STATE?, call: DirectCall?): Boolean {
        if (!super.setState(state, call)) {
            return false
        }
        when (mState) {
            STATE.STATE_ACCEPTING -> cancelCallDurationTimer()
            STATE.STATE_CONNECTED -> {
                setInfo(call, "")
                mLinearLayoutInfo!!.visibility = View.VISIBLE
                call?.let { setCallDurationTimer(it) }
            }

            STATE.STATE_ENDING, STATE.STATE_ENDED -> {
                cancelCallDurationTimer()
            }

            else -> {}
        }
        return true
    }

    private fun setCallDurationTimer(call: DirectCall) {
        if (mCallDurationTimer == null) {
            mCallDurationTimer = Timer()
            mCallDurationTimer?.schedule(object : TimerTask() {
                override fun run() {
                    runOnUiThread {
                        val callDuration: String = TimeUtils.getTimeString(call.duration)
                        mTextViewStatus!!.text = callDuration
                    }
                }
            }, 0, 1000)
        }
    }

    private fun cancelCallDurationTimer() {
        if (mCallDurationTimer != null) {
            mCallDurationTimer?.cancel()
            mCallDurationTimer = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(BaseApplication.TAG, "[VoiceCallActivity] onDestroy()")
        cancelCallDurationTimer()
    }

}