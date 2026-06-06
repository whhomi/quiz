package com.quizhelper.app.util

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.CHINA)
    private val fullDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.CHINA)

    fun formatTimestamp(ts: Long): String = dateFormat.format(Date(ts))
    fun formatTimestampFull(ts: Long): String = fullDateFormat.format(Date(ts))

    fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "${m}分${s}秒" else "${s}秒"
    }

    fun formatCountdown(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "${String.format("%02d", m)}:${String.format("%02d", s)}"
    }
}
