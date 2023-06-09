package com.ezzy.sendbird.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.sendbird.calls.DirectCallLog

private const val TAG = "BroadcastUtils"
object BroadcastUtils {
    val INTENT_ACTION_ADD_CALL_LOG = "com.sendbird.calls.quickstart.intent.action.ADD_CALL_LOG"
    val INTENT_EXTRA_CALL_LOG = "call_log"

    fun sendCallLogBroadcast(context: Context?, callLog: DirectCallLog?) {
        if (context != null && callLog != null) {
            Log.i(TAG, "[BroadcastUtils] sendCallLogBroadcast()")
            val intent = Intent(INTENT_ACTION_ADD_CALL_LOG)
            intent.putExtra(INTENT_EXTRA_CALL_LOG, callLog)
            context.sendBroadcast(intent)
        }
    }
}