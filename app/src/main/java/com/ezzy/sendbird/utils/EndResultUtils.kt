package com.ezzy.sendbird.utils

import android.content.Context
import com.ezzy.sendbird.R
import com.sendbird.calls.DirectCallEndResult

object EndResultUtils {
    fun getEndResultString(context: Context, endResult: DirectCallEndResult?): String {
        var endResultString = ""
        when (endResult) {
            DirectCallEndResult.NONE -> {}
            DirectCallEndResult.NO_ANSWER -> endResultString =
                context.getString(R.string.calls_end_result_no_answer)

            DirectCallEndResult.CANCELED -> endResultString =
                context.getString(R.string.calls_end_result_canceled)

            DirectCallEndResult.DECLINED -> endResultString =
                context.getString(R.string.calls_end_result_declined)

            DirectCallEndResult.COMPLETED -> endResultString =
                context.getString(R.string.calls_end_result_completed)

            DirectCallEndResult.TIMED_OUT -> endResultString =
                context.getString(R.string.calls_end_result_timed_out)

            DirectCallEndResult.CONNECTION_LOST -> endResultString =
                context.getString(R.string.calls_end_result_connection_lost)

            DirectCallEndResult.UNKNOWN -> endResultString =
                context.getString(R.string.calls_end_result_unknown)

            DirectCallEndResult.DIAL_FAILED -> endResultString =
                context.getString(R.string.calls_end_result_dial_failed)

            DirectCallEndResult.ACCEPT_FAILED -> endResultString =
                context.getString(R.string.calls_end_result_accept_failed)

            DirectCallEndResult.OTHER_DEVICE_ACCEPTED -> endResultString =
                context.getString(R.string.calls_end_result_other_device_accepted)

            else -> { endResultString = "" }
        }
        return endResultString
    }
}