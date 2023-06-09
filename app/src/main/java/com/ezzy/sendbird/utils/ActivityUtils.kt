package com.ezzy.sendbird.utils

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.ezzy.sendbird.AuthenticateActivity
import com.ezzy.sendbird.MainActivity
import com.ezzy.sendbird.SignInManuallyActivity
import com.ezzy.sendbird.main.ApplicationInformationActivity

object ActivityUtils {
    val START_SIGN_IN_MANUALLY_ACTIVITY_REQUEST_CODE = 1

    fun startAuthenticateActivityAndFinish(activity: Activity) {
        val intent = Intent(activity, AuthenticateActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        activity.startActivity(intent)
        activity.finish()
    }

    fun startSignInManuallyActivityForResult(activity: Activity) {
        val intent = Intent(activity, SignInManuallyActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        activity.startActivityForResult(intent, START_SIGN_IN_MANUALLY_ACTIVITY_REQUEST_CODE)
    }

    fun startMainActivityAndFinish(activity: Activity) {
        val intent = Intent(activity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        activity.startActivity(intent)
        activity.finish()
    }

    fun startApplicationInformationActivity(activity: Activity) {
        val intent = Intent(activity, ApplicationInformationActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        activity.startActivity(intent)
    }
}