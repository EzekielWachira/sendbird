package com.ezzy.sendbird.utils

import android.content.Context
import android.content.SharedPreferences
import com.ezzy.sendbird.BaseApplication

object PrefUtils {
    private const val PREF_NAME = "sendbird_calls"
    private const val PREF_KEY_APP_ID = "app_id"
    private const val PREF_KEY_USER_ID = "user_id"
    private const val PREF_KEY_ACCESS_TOKEN = "access_token"
    private const val PREF_KEY_CALLEE_ID = "callee_id"
    private const val PREF_KEY_PUSH_TOKEN = "push_token"
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setAppId(context: Context, appId: String?) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(PREF_KEY_APP_ID, appId).apply()
    }

    fun getAppId(context: Context): String? {
        return getSharedPreferences(context).getString(PREF_KEY_APP_ID, BaseApplication.APP_ID)
    }

    fun setUserId(context: Context, userId: String?) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(PREF_KEY_USER_ID, userId).apply()
    }

    fun getUserId(context: Context): String? {
        return getSharedPreferences(context).getString(PREF_KEY_USER_ID, "")
    }

    fun setAccessToken(context: Context, accessToken: String?) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(PREF_KEY_ACCESS_TOKEN, accessToken).apply()
    }

    fun getAccessToken(context: Context): String? {
        return getSharedPreferences(context).getString(PREF_KEY_ACCESS_TOKEN, "")
    }

    fun setCalleeId(context: Context, calleeId: String?) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(PREF_KEY_CALLEE_ID, calleeId).apply()
    }

    fun getCalleeId(context: Context): String? {
        return getSharedPreferences(context).getString(PREF_KEY_CALLEE_ID, "")
    }

    fun setPushToken(context: Context, pushToken: String?) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(PREF_KEY_PUSH_TOKEN, pushToken).apply()
    }

    fun getPushToken(context: Context): String? {
        return getSharedPreferences(context).getString(PREF_KEY_PUSH_TOKEN, "")
    }
}
