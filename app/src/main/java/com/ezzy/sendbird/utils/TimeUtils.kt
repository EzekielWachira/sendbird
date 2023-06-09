package com.ezzy.sendbird.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtils {
    fun getTimeString(periodMs: Long): String {
        val result: String
        var totalSec = (periodMs / 1000).toInt()
        var hour = 0
        val min: Int
        val sec: Int
        if (totalSec >= 3600) {
            hour = totalSec / 3600
            totalSec = totalSec % 3600
        }
        min = totalSec / 60
        sec = totalSec % 60
        result = if (hour > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hour, min, sec)
        } else if (min > 0) {
            String.format(Locale.getDefault(), "%d:%02d", min, sec)
        } else {
            String.format(Locale.getDefault(), "0:%02d", sec)
        }
        return result
    }

    fun getTimeStringForHistory(periodMs: Long): String {
        val result: String
        var totalSec = (periodMs / 1000).toInt()
        var hour = 0
        val min: Int
        val sec: Int
        if (totalSec >= 3600) {
            hour = totalSec / 3600
            totalSec = totalSec % 3600
        }
        min = totalSec / 60
        sec = totalSec % 60
        result = if (hour > 0) {
            String.format(Locale.getDefault(), "%dh %dm %ds", hour, min, sec)
        } else if (min > 0) {
            String.format(Locale.getDefault(), "%dm %ds", min, sec)
        } else {
            String.format(Locale.getDefault(), "%ds", sec)
        }
        return result
    }

    fun getDateString(timeMs: Long): String {
        val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd H:mm", Locale.getDefault())
        val dateString = simpleDateFormat.format(Date(timeMs))
        return dateString.lowercase(Locale.getDefault())
    }
}
