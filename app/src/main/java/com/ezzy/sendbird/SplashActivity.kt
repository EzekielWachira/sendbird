package com.ezzy.sendbird

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ezzy.sendbird.utils.ActivityUtils
import com.ezzy.sendbird.utils.AuthenticationUtils
import com.ezzy.sendbird.utils.ToastUtils
import java.util.Timer
import java.util.TimerTask

class SplashActivity : AppCompatActivity() {
    private val SPLASH_TIME_MS = 1000

    private var mContext: Context? = null
    private var mTimer: Timer? = null
    private var mAutoAuthenticateResult: Boolean = false
    private var mEncodedAuthInfo: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        mContext = this
        setTimer()
        if (!hasDeepLink()) {
            autoAuthenticate()
        }
    }

    private fun hasDeepLink(): Boolean {
        var result = false
        val intent = intent
        if (intent != null) {
            val data = intent.data
            if (data != null) {
                val scheme = data.scheme
                if (scheme != null && scheme == "sendbird") {
                    Log.i(BaseApplication.TAG, "[SplashActivity] deep link: $data")
                    mEncodedAuthInfo = data.host
                    if (!TextUtils.isEmpty(mEncodedAuthInfo)) {
                        result = true
                    }
                }
            }
        }
        return result
    }

    private fun setTimer() {
        mTimer = Timer()
        mTimer!!.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    mTimer = null
                    if (!TextUtils.isEmpty(mEncodedAuthInfo)) {
                        AuthenticationUtils.authenticateWithEncodedAuthInfo(
                            this@SplashActivity,
                            mEncodedAuthInfo
                        ) { isSuccess, hasInvalidValue ->
                            if (isSuccess) {
                                ActivityUtils.startMainActivityAndFinish(this@SplashActivity)
                            } else {
                                if (hasInvalidValue) {
                                    ToastUtils.showToast(
                                        this@SplashActivity,
                                        getString(R.string.calls_invalid_deep_link)
                                    )
                                } else {
                                    ToastUtils.showToast(
                                        this@SplashActivity,
                                        getString(R.string.calls_deep_linking_to_authenticate_failed)
                                    )
                                }
                                finish()
                            }
                        }
                        return@runOnUiThread
                    }
                    if (mAutoAuthenticateResult != null) {
                        if (mAutoAuthenticateResult) {
                            ActivityUtils.startMainActivityAndFinish(this@SplashActivity)
                        } else {
                            ActivityUtils.startAuthenticateActivityAndFinish(this@SplashActivity)
                        }
                    }
                }
            }
        }, SPLASH_TIME_MS.toLong())
    }

    private fun autoAuthenticate() {
        AuthenticationUtils.autoAuthenticate(mContext,
            object : AuthenticationUtils.AutoAuthenticateHandler {
                override fun onResult(userId: String?) {
                    if (mTimer != null) {
                        mAutoAuthenticateResult = !TextUtils.isEmpty(userId)
                    } else {
                        if (userId != null) {
                            ActivityUtils.startMainActivityAndFinish(this@SplashActivity)
                        } else {
                            ActivityUtils.startAuthenticateActivityAndFinish(this@SplashActivity)
                        }
                    }
                }
            }
        )
    }

    override fun onBackPressed() {
        if (mTimer != null) {
            mTimer!!.cancel()
            mTimer = null
        }
        super.onBackPressed()
    }
}