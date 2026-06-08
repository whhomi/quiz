package com.quizhelper.app.util

import java.text.SimpleDateFormat
import java.util.*

object ExamCountdown {
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    private val ticketStart = sdf.parse("2026-06-10 10:00")!!.time
    private val ticketEnd = sdf.parse("2026-06-10 18:00")!!.time
    private val examStart = sdf.parse("2026-06-13 15:00")!!.time
    private val examEnd = sdf.parse("2026-06-13 16:40")!!.time
    private val resultTime = sdf.parse("2026-06-24 10:00")!!.time

    data class Info(val icon: String, val title: String, val message: String, val bgColor: Int, val textColor: Int)

    fun get(): Info {
        val now = System.currentTimeMillis()
        val bgBlue = (0xFFEFF6FF).toInt()
        val fgBlue = (0xFF2563EB).toInt()
        val bgAmber = (0xFFFEF3C7).toInt()
        val fgAmber = (0xFFD97706).toInt()
        val bgRed = (0xFFFFF0F0).toInt()
        val fgRed = (0xFFDC2626).toInt()
        val bgGreen = (0xFFF0FDF4).toInt()
        val fgGreen = (0xFF16A34A).toInt()
        val bgPurple = (0xFFF5F3FF).toInt()
        val fgPurple = (0xFF7C3AED).toInt()

        return when {
            now < ticketStart -> ticketCountdown(now, bgBlue, fgBlue)
            now >= ticketStart && now <= ticketEnd -> Info("🖨", "准考证下载中", "下载时间：今日 10:00-18:00\n请及时下载并打印准考证", bgAmber, fgAmber)
            now < examStart -> examCountdown(now, bgRed, fgRed)
            now >= examStart && now <= examEnd -> Info("🏆", "考试进行中", "祝您考试顺利，超常发挥！💪", bgGreen, fgGreen)
            now < resultTime -> resultCountdown(now, bgPurple, fgPurple)
            else -> Info("🎉", "成绩查询已开放", "06月24日 10:00 起可查询成绩\n祝您取得好成绩！", bgGreen, fgGreen)
        }
    }

    private fun ticketCountdown(now: Long, bg: Int, fg: Int): Info = countdownInfo(ticketStart, now, "📋", "准考证下载倒计时", "下载时间：06月10日 10:00-18:00", bg, fg)

    private fun examCountdown(now: Long, bg: Int, fg: Int): Info = countdownInfo(examStart, now, "✏️", "考试倒计时", "考试时间：06月13日 15:00-16:40", bg, fg)

    private fun resultCountdown(now: Long, bg: Int, fg: Int): Info = countdownInfo(resultTime, now, "📊", "成绩即将公布", "查询时间：06月24日 10:00 起", bg, fg)

    private fun countdownInfo(target: Long, now: Long, icon: String, title: String, desc: String, bg: Int, fg: Int): Info {
        val diff = target - now
        val days = (diff / 86400000L).toInt()
        val hours = ((diff % 86400000L) / 3600000L).toInt()
        val mins = ((diff % 3600000L) / 60000L).toInt()
        return Info(icon, title, "距离 $days 天 $hours 小时 $mins 分\n$desc", bg, fg)
    }
}
