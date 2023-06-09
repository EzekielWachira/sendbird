package com.ezzy.sendbird.call

import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import com.ezzy.sendbird.BaseApplication
import com.ezzy.sendbird.R
import com.ezzy.sendbird.utils.ToastUtils
import com.sendbird.calls.AcceptParams
import com.sendbird.calls.AudioDevice
import com.sendbird.calls.CallOptions
import com.sendbird.calls.DialParams
import com.sendbird.calls.DirectCall
import com.sendbird.calls.DirectCallUserRole
import com.sendbird.calls.SendBirdCall.dial
import com.sendbird.calls.SendBirdException
import com.sendbird.calls.SendBirdVideoView
import com.sendbird.calls.handler.DialHandler
import org.webrtc.RendererCommon

class VideoCallActivity : CallActivity() {
    override val layoutResourceId: Int
        get() = R.layout.activity_video_call

    private var mIsVideoEnabled = false

    //+ Views
    private lateinit var mVideoViewFullScreen: SendBirdVideoView
    private lateinit var mViewConnectingVideoViewFullScreenFg: View
    private lateinit var mRelativeLayoutVideoViewSmall: RelativeLayout
    private lateinit var mVideoViewSmall: SendBirdVideoView
    private lateinit var mImageViewCameraSwitch: ImageView
    private lateinit var mImageViewVideoOff: ImageView
    override fun setAudioDevice(
        currentAudioDevice: AudioDevice?,
        availableAudioDevices: Set<AudioDevice>?
    ) {
        if (currentAudioDevice == AudioDevice.SPEAKERPHONE) {
            mImageViewBluetooth!!.isSelected = false
        } else if (currentAudioDevice == AudioDevice.BLUETOOTH) {
            mImageViewBluetooth!!.isSelected = true
        }

        if (availableAudioDevices!!.contains(AudioDevice.BLUETOOTH)) {
            mImageViewBluetooth!!.isEnabled = true
        } else if (!mImageViewBluetooth!!.isSelected) {
            mImageViewBluetooth!!.isEnabled = false
        }
    }

    override fun startCall(amICallee: Boolean) {
        val callOptions = CallOptions()
        callOptions.setVideoEnabled(mIsVideoEnabled).setAudioEnabled(mIsAudioEnabled)

        if (amICallee) {
            callOptions.setLocalVideoView(mVideoViewSmall).setRemoteVideoView(mVideoViewFullScreen)
        } else {
            callOptions.setLocalVideoView(mVideoViewFullScreen).setRemoteVideoView(mVideoViewSmall)
        }

        if (amICallee) {
            Log.i(BaseApplication.TAG, "[VideoCallActivity] accept()")
            if (mDirectCall != null) {
                mDirectCall!!.accept(AcceptParams().setCallOptions(callOptions))
            }
        } else {
            Log.i(BaseApplication.TAG, "[VideoCallActivity] dial()")
            mDirectCall = dial(DialParams(mCalleeIdToDial!!).setVideoCall(mIsVideoCall)
                .setCallOptions(callOptions),
                DialHandler { _: DirectCall?, e: SendBirdException? ->
                    if (e != null) {
                        Log.i(
                            BaseApplication.TAG,
                            "[VideoCallActivity] dial() => e: " + e.message
                        )
                        if (e.message != null) {
                            ToastUtils.showToast(mContext, e.message)
                        }
                        finishWithEnding(e.message!!)
                        return@DialHandler
                    }
                    Log.i(BaseApplication.TAG, "[VideoCallActivity] dial() => OK")
                    updateCallService()
                })
            setListener(mDirectCall)
        }
    }

    override fun initViews() {
        super.initViews()
        Log.i(BaseApplication.TAG, "[VideoCallActivity] initViews()")
        mVideoViewFullScreen = findViewById(R.id.video_view_fullscreen)
        mViewConnectingVideoViewFullScreenFg =
            findViewById(R.id.view_connecting_video_view_fullscreen_fg)
        mRelativeLayoutVideoViewSmall = findViewById(R.id.relative_layout_video_view_small)
        mVideoViewSmall = findViewById(R.id.video_view_small)
        mImageViewCameraSwitch = findViewById(R.id.image_view_camera_switch)
        mImageViewVideoOff = findViewById(R.id.image_view_video_off)
    }

    override fun setViews() {
        super.setViews()
        mVideoViewFullScreen.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        mVideoViewFullScreen.setZOrderMediaOverlay(false)
        mVideoViewFullScreen.setEnableHardwareScaler(true)

        mVideoViewSmall.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        mVideoViewSmall.setZOrderMediaOverlay(true)
        mVideoViewSmall.setEnableHardwareScaler(true)

        if (mDirectCall != null) {
            if (mDirectCall!!.myRole === DirectCallUserRole.CALLER && mState === STATE.STATE_OUTGOING) {
                mDirectCall!!.setLocalVideoView(mVideoViewFullScreen)
                mDirectCall!!.setRemoteVideoView(mVideoViewSmall)
            } else {
                mDirectCall!!.setLocalVideoView(mVideoViewSmall)
                mDirectCall!!.setRemoteVideoView(mVideoViewFullScreen)
            }
        }

        mImageViewCameraSwitch.setOnClickListener { view: View? ->
            if (mDirectCall != null) {
                mDirectCall!!.switchCamera { e ->
                    if (e != null) {
                        Log.i(
                            BaseApplication.TAG,
                            "[VideoCallActivity] switchCamera(e: " + e.message + ")"
                        )
                    }
                }
            }
        }

        if (mDirectCall != null && !mDoLocalVideoStart) {
            mIsVideoEnabled = mDirectCall!!.isLocalVideoEnabled
        } else {
            mIsVideoEnabled = true
        }
        mImageViewVideoOff.isSelected = !mIsVideoEnabled
        mImageViewVideoOff.setOnClickListener { view: View? ->
            if (mDirectCall != null) {
                if (mIsVideoEnabled) {
                    Log.i(BaseApplication.TAG, "[VideoCallActivity] stopVideo()")
                    mDirectCall!!.stopVideo()
                    mIsVideoEnabled = false
                    mImageViewVideoOff.isSelected = true
                } else {
                    Log.i(BaseApplication.TAG, "[VideoCallActivity] startVideo()")
                    mDirectCall!!.startVideo()
                    mIsVideoEnabled = true
                    mImageViewVideoOff.isSelected = false
                }
            }
        }

        mImageViewBluetooth!!.isEnabled = false
        mImageViewBluetooth!!.setOnClickListener { view ->
            mImageViewBluetooth!!.isSelected = !mImageViewBluetooth!!.isSelected
            if (mDirectCall != null) {
                if (mImageViewBluetooth!!.isSelected) {
                    mDirectCall!!.selectAudioDevice(AudioDevice.BLUETOOTH) { e ->
                        if (e != null) {
                            mImageViewBluetooth!!.isSelected = false
                        }
                    }
                } else {
                    mDirectCall!!.selectAudioDevice(AudioDevice.WIRED_HEADSET) { e ->
                        if (e != null) {
                            mDirectCall!!.selectAudioDevice(AudioDevice.SPEAKERPHONE, null)
                        }
                    }
                }
            }
        }
    }

    override fun setState(state: STATE?, call: DirectCall?): Boolean {
        if (!super.setState(state, call)) {
            return false
        }

        when (state) {
            STATE.STATE_ACCEPTING -> {
                mVideoViewFullScreen.visibility = View.GONE
                mViewConnectingVideoViewFullScreenFg.visibility = View.GONE
                mRelativeLayoutVideoViewSmall.visibility = View.GONE
                mImageViewCameraSwitch.visibility = View.GONE
            }

            STATE.STATE_OUTGOING -> {
                mVideoViewFullScreen.visibility = View.VISIBLE
                mViewConnectingVideoViewFullScreenFg.visibility = View.VISIBLE
                mRelativeLayoutVideoViewSmall.visibility = View.GONE
                mImageViewCameraSwitch.visibility = View.VISIBLE
                mImageViewVideoOff.visibility = View.VISIBLE
            }

            STATE.STATE_CONNECTED -> {
                mVideoViewFullScreen.visibility = View.VISIBLE
                mViewConnectingVideoViewFullScreenFg.visibility = View.GONE
                mRelativeLayoutVideoViewSmall.visibility = View.VISIBLE
                mImageViewCameraSwitch.visibility = View.VISIBLE
                mImageViewVideoOff.visibility = View.VISIBLE
                mLinearLayoutInfo!!.visibility = View.GONE
                if (call != null && call.myRole == DirectCallUserRole.CALLER) {
                    call.setLocalVideoView(mVideoViewSmall)
                    call.setRemoteVideoView(mVideoViewFullScreen)
                }
            }

            STATE.STATE_ENDING, STATE.STATE_ENDED -> {
                mLinearLayoutInfo!!.visibility = View.VISIBLE
                mVideoViewFullScreen.visibility = View.GONE
                mViewConnectingVideoViewFullScreenFg.visibility = View.GONE
                mRelativeLayoutVideoViewSmall.visibility = View.GONE
                mImageViewCameraSwitch.visibility = View.GONE
            }

            else -> {}
        }
        return true
    }

    fun setLocalVideoSettings(call: DirectCall) {
        mIsVideoEnabled = call.isLocalVideoEnabled
        Log.i(
            BaseApplication.TAG,
            "[VideoCallActivity] setLocalVideoSettings() => isLocalVideoEnabled(): $mIsVideoEnabled"
        )
        mImageViewVideoOff.isSelected = !mIsVideoEnabled
    }


    override fun onStart() {
        super.onStart()
        Log.i(BaseApplication.TAG, "[VideoCallActivity] onStart()")
        if (mDirectCall != null && mDoLocalVideoStart) {
            mDoLocalVideoStart = false
            updateCallService()
            mDirectCall!!.startVideo()
        }
    }

    override fun onStop() {
        super.onStop()
        Log.i(BaseApplication.TAG, "[VideoCallActivity] onStop()")
        if (mDirectCall != null && mDirectCall!!.isLocalVideoEnabled) {
            mDirectCall!!.stopVideo()
            mDoLocalVideoStart = true
            updateCallService()
        }
    }


}