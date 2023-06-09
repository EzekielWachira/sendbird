package com.ezzy.sendbird.utils

import android.app.Activity
import android.content.Intent
import com.ezzy.sendbird.R
import com.google.zxing.integration.android.IntentIntegrator

object QRCodeUtils {
    fun scanQRCode(activity: Activity) {
        IntentIntegrator(activity)
            .setPrompt(activity.getString(R.string.calls_scanning_a_qrcode_description))
            .setBeepEnabled(false)
            .initiateScan()
    }

    fun onActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        handler: CompletionHandler?
    ): Boolean {
        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (result != null) {
                val contents = result.contents
                AuthenticationUtils.authenticateWithEncodedAuthInfo(
                    activity,
                    contents
                ) { isSuccess, hasInvalidValue ->
                    if (!(isSuccess as Boolean) && hasInvalidValue as Boolean) {
                        if (resultCode != Activity.RESULT_CANCELED) {
                            ToastUtils.showToast(
                                activity,
                                activity.getString(R.string.calls_invalid_qrcode)
                            )
                        }
                    }
                    handler?.onCompletion(isSuccess)
                }
            } else {
                handler?.onCompletion(false)
            }
            return true
        }
        handler?.onCompletion(false)
        return false
    }

    interface CompletionHandler {
        fun onCompletion(isSuccess: Boolean)
    }
}