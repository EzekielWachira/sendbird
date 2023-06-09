package com.ezzy.sendbird.utils

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import com.ezzy.sendbird.BaseApplication
import com.ezzy.sendbird.R
import com.sendbird.calls.AuthenticateParams
import com.sendbird.calls.SendBirdCall.applicationId
import com.sendbird.calls.SendBirdCall.authenticate
import com.sendbird.calls.SendBirdCall.currentUser
import com.sendbird.calls.SendBirdCall.deauthenticate
import com.sendbird.calls.SendBirdCall.registerPushToken
import com.sendbird.calls.SendBirdCall.unregisterPushToken
import com.sendbird.calls.SendBirdException
import com.sendbird.calls.User
import com.sendbird.calls.handler.AuthenticateHandler
import com.sendbird.calls.handler.CompletionHandler
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException

object AuthenticationUtils {
    fun authenticate(
        context: Context?,
        userId: String?,
        accessToken: String?,
        handler: ((isSuccess: Boolean) -> Unit)?
    ) {
        if (userId == null) {
            Log.i(
                BaseApplication.TAG,
                "[AuthenticationUtils] authenticate() => Failed (userId == null)"
            )
            handler?.invoke(false)
            return
        }
        deauthenticate(context,
            object : DeauthenticateHandler {
                override fun onResult(isSuccess: Boolean) {
                    PushUtils.getPushToken(context) { pushToken, e ->
                        if (e != null) {
                            Log.i(
                                BaseApplication.TAG,
                                "[AuthenticationUtils] authenticate() => Failed (e: " + e.message + ")"
                            )
                            handler?.invoke(false)
                            return@getPushToken
                        }
                        Log.i(
                            BaseApplication.TAG,
                            "[AuthenticationUtils] authenticate(userId: $userId)"
                        )
                        authenticate(
                            AuthenticateParams(userId).setAccessToken(accessToken),
                            AuthenticateHandler { user: User?, e1: SendBirdException? ->
                                if (e1 != null) {
                                    Log.i(
                                        BaseApplication.TAG,
                                        "[AuthenticationUtils] authenticate() => Failed (e1: " + e1.message + ")"
                                    )
                                    showToastErrorMessage(context, e1)
                                    handler?.invoke(false)
                                    return@AuthenticateHandler
                                }
                                Log.i(
                                    BaseApplication.TAG,
                                    "[AuthenticationUtils] authenticate() => registerPushToken(pushToken: $pushToken)"
                                )
                                if (pushToken != null) {
                                    registerPushToken(
                                        pushToken,
                                        false,
                                        CompletionHandler { e2: SendBirdException? ->
                                            if (e2 != null) {
                                                Log.i(
                                                    BaseApplication.TAG,
                                                    "[AuthenticationUtils] authenticate() => registerPushToken() => Failed (e2: " + e2.message + ")"
                                                )
                                                showToastErrorMessage(context, e2)
                                                handler?.invoke(false)
                                                return@CompletionHandler
                                            }
                                            PrefUtils.setAppId(context!!, applicationId)
                                            PrefUtils.setUserId(context, userId)
                                            PrefUtils.setAccessToken(context, accessToken)
                                            PrefUtils.setPushToken(context, pushToken)
                                            Log.i(
                                                BaseApplication.TAG,
                                                "[AuthenticationUtils] authenticate() => OK"
                                            )
                                            handler?.invoke(true)
                                        })
                                }
                            })
                    }
                }
            })
    }

    fun authenticateWithEncodedAuthInfo(
        activity: Activity,
        encodedAuthInfo: String?,
        handler: ((isSuccess: Boolean, hasInvalidValue: Boolean) -> Unit)?
    ) {
        var appId: String? = null
        var userId: String? = null
        var accessToken: String? = null
        try {
            if (!TextUtils.isEmpty(encodedAuthInfo)) {
                val jsonString = String(Base64.decode(encodedAuthInfo, Base64.DEFAULT), Charsets.UTF_8)
                val jsonObject = JSONObject(jsonString)
                appId = jsonObject.getString("app_id")
                userId = jsonObject.getString("user_id")
                accessToken = jsonObject.getString("access_token")
            }
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        if (!TextUtils.isEmpty(appId) && !TextUtils.isEmpty(userId)
            && (activity.application as BaseApplication).initSendBirdCall(appId!!)
        ) {
            authenticate(activity, userId, accessToken) { isSuccess ->
                handler?.invoke(isSuccess, false)
            }
        } else {
            handler?.invoke(false, true)
        }
    }

    fun deauthenticate(context: Context?, handler: DeauthenticateHandler?) {
        if (currentUser == null) {
            handler?.onResult(false)
            return
        }
        Log.i(
            BaseApplication.TAG,
            "[AuthenticationUtils] deauthenticate(userId: " + currentUser!!.userId + ")"
        )
        val pushToken = PrefUtils.getPushToken(context!!)
        if (!TextUtils.isEmpty(pushToken)) {
            Log.i(
                BaseApplication.TAG,
                "[AuthenticationUtils] deauthenticate() => unregisterPushToken(pushToken: $pushToken)"
            )
            unregisterPushToken(pushToken!!, CompletionHandler { e: SendBirdException? ->
                if (e != null) {
                    Log.i(
                        BaseApplication.TAG,
                        "[AuthenticationUtils] unregisterPushToken() => Failed (e: " + e.message + ")"
                    )
                    showToastErrorMessage(context, e)
                }
                doDeauthenticate(context, handler)
            })
        } else {
            doDeauthenticate(context, handler)
        }
    }

    private fun doDeauthenticate(context: Context?, handler: DeauthenticateHandler?) {
        deauthenticate(CompletionHandler { e: SendBirdException? ->
            if (e != null) {
                Log.i(
                    BaseApplication.TAG,
                    "[AuthenticationUtils] deauthenticate() => Failed (e: " + e.message + ")"
                )
                showToastErrorMessage(context, e)
            } else {
                Log.i(
                    BaseApplication.TAG,
                    "[AuthenticationUtils] deauthenticate() => OK"
                )
            }
            PrefUtils.setUserId(context!!, null)
            PrefUtils.setAccessToken(context, null)
            PrefUtils.setCalleeId(context, null)
            PrefUtils.setPushToken(context, null)
            handler?.onResult(e == null)
        })
    }

    fun autoAuthenticate(context: Context?, handler: AutoAuthenticateHandler?) {
        Log.i(BaseApplication.TAG, "[AuthenticationUtils] autoAuthenticate()")
        if (currentUser != null) {
            Log.i(
                BaseApplication.TAG,
                "[AuthenticationUtils] autoAuthenticate(userId: " + currentUser!!.userId + ") => OK (SendBirdCall.getCurrentUser() != null)"
            )
            handler?.onResult(currentUser!!.userId)
            return
        }
        val userId = PrefUtils.getUserId(context!!)
        val accessToken = PrefUtils.getAccessToken(context)
        val pushToken = PrefUtils.getPushToken(context)
        if (!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(pushToken)) {
            Log.i(
                BaseApplication.TAG,
                "[AuthenticationUtils] autoAuthenticate() => authenticate(userId: $userId)"
            )
            authenticate(
                AuthenticateParams(userId!!).setAccessToken(accessToken),
                AuthenticateHandler { user: User?, e: SendBirdException? ->
                    if (e != null) {
                        Log.i(
                            BaseApplication.TAG,
                            "[AuthenticationUtils] autoAuthenticate() => authenticate() => Failed (e: " + e.message + ")"
                        )
                        showToastErrorMessage(context, e)
                        handler?.onResult(null)
                        return@AuthenticateHandler
                    }
                    Log.i(
                        BaseApplication.TAG,
                        "[AuthenticationUtils] autoAuthenticate() => registerPushToken(pushToken: $pushToken)"
                    )
                    registerPushToken(
                        pushToken!!,
                        false,
                        CompletionHandler { e1: SendBirdException? ->
                            if (e1 != null) {
                                Log.i(
                                    BaseApplication.TAG,
                                    "[AuthenticationUtils] autoAuthenticate() => registerPushToken() => Failed (e1: " + e1.message + ")"
                                )
                                showToastErrorMessage(context, e1)
                                handler?.onResult(null)
                                return@CompletionHandler
                            }
                            Log.i(
                                BaseApplication.TAG,
                                "[AuthenticationUtils] autoAuthenticate() => authenticate() => OK (Authenticated)"
                            )
                            handler?.onResult(userId)
                        })
                })
        } else {
            Log.i(
                BaseApplication.TAG,
                "[AuthenticationUtils] autoAuthenticate() => Failed (No userId and pushToken)"
            )
            handler?.onResult(null)
        }
    }

    private fun showToastErrorMessage(context: Context?, e: SendBirdException) {
        if (context != null) {
            if (e.code == 400111) {
                ToastUtils.showToast(
                    context,
                    context.getString(R.string.calls_invalid_notifications_setting_in_dashboard)
                )
            } else {
                ToastUtils.showToast(context, e.message)
            }
        }
    }

    interface AuthenticateHandler {
        fun onResult(isSuccess: Boolean)
    }

    interface CompletionWithDetailHandler {
        fun onCompletion(isSuccess: Boolean, hasInvalidValue: Boolean)
    }

    interface DeauthenticateHandler {
        fun onResult(isSuccess: Boolean)
    }

    interface AutoAuthenticateHandler {
        fun onResult(userId: String?)
    }
}