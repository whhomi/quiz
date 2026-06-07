package com.quizhelper.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream

object ShareUtil {

    private const val WIDTH = 1080
    private const val PADDING = 60
    private const val CARD_RADIUS = 30f

    fun shareExamResult(context: Context, score: Double, maxScore: Double, correctRate: Double,
                        correctCount: Int, totalCount: Int, durationSeconds: Int, isPass: Boolean) {
        val bitmap = generateResultImage(context, score, maxScore, correctRate,
            correctCount, totalCount, durationSeconds, isPass)
        val uri = saveToCache(context, bitmap)
        if (uri != null) {
            shareImage(context, uri)
        }
    }

    private fun generateResultImage(context: Context, score: Double, maxScore: Double,
                                     correctRate: Double, correctCount: Int, totalCount: Int,
                                     durationSeconds: Int, isPass: Boolean): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 36f; color = Color.DKGRAY }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 42f; color = Color.BLACK; isFakeBoldText = true }

        // Estimate height
        val headerH = 300
        val infoH = 600
        val qrH = 400
        val footerH = 200
        val totalH = headerH + infoH + qrH + footerH + PADDING * 5

        val bitmap = Bitmap.createBitmap(WIDTH, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        var y = PADDING.toFloat()

        // Header section
        paint.color = Color.parseColor("#397DFF")
        canvas.drawRoundRect(RectF(PADDING.toFloat(), y, (WIDTH - PADDING).toFloat(), y + 200f), CARD_RADIUS, CARD_RADIUS, paint)

        boldPaint.apply { textSize = 48f; color = Color.WHITE }
        canvas.drawText("🏆 墨答 · 考试结果", WIDTH / 2f - 200f, y + 80f, boldPaint)

        boldPaint.apply { textSize = 36f; color = Color.WHITE }
        canvas.drawText("${score.toInt()} 分", WIDTH / 2f - 60f, y + 140f, boldPaint)
        y += 240f

        // Stats section
        paint.color = Color.parseColor(if (isPass) "#F0FDF4" else "#FEF2F2")
        canvas.drawRoundRect(RectF(PADDING.toFloat(), y, (WIDTH - PADDING).toFloat(), y + 320f), CARD_RADIUS, CARD_RADIUS, paint)

        val passColor = if (isPass) "#16A34A" else "#DC2626"
        textPaint.apply { textSize = 38f; color = Color.WHITE; isFakeBoldText = true }
        paint.color = Color.parseColor(passColor)
        canvas.drawRoundRect(RectF(WIDTH / 2f - 80f, y + 20f, WIDTH / 2f + 80f, y + 80f), 20f, 20f, paint)
        textPaint.color = Color.WHITE
        canvas.drawText("${score.toInt()}分", WIDTH / 2f - 50f, y + 62f, textPaint)

        textPaint.apply { textSize = 32f; color = Color.GRAY; isFakeBoldText = false }
        canvas.drawText("正确率: ${correctRate.toInt()}%", PADDING + 30f, y + 130f, textPaint)
        canvas.drawText("正确: $correctCount / $totalCount 题", PADDING + 30f, y + 180f, textPaint)
        canvas.drawText("用时: ${TimeUtils.formatDuration(durationSeconds)}", PADDING + 30f, y + 230f, textPaint)

        // Encouragement
        val enc = Encouragement.random()
        paint.color = Color.parseColor("#EFF6FF")
        canvas.drawRoundRect(RectF(PADDING.toFloat(), y + 280f, (WIDTH - PADDING).toFloat(), y + 360f), 20f, 20f, paint)
        textPaint.apply { textSize = 32f; color = Color.parseColor("#2563EB") }
        canvas.drawText("💪 $enc", PADDING + 30f, y + 340f, textPaint)

        y += 400f

        // QR Code section
        val qrCode = generateQRCode("https://github.com/whhomi/quiz/releases", 300)
        if (qrCode != null) {
            val qrX = WIDTH / 2 - 150
            canvas.drawBitmap(qrCode, qrX.toFloat(), y, null)
        }
        textPaint.apply { textSize = 28f; color = Color.GRAY; isFakeBoldText = false }
        canvas.drawText("扫码下载墨答", WIDTH / 2f - 120f, y + 340f, textPaint)
        y += 380f

        // Footer
        textPaint.apply { textSize = 24f; color = Color.LTGRAY }
        canvas.drawText("墨答 - 优雅刷题，从容作答", WIDTH / 2f - 180f, y + 40f, textPaint)

        return bitmap
    }

    private fun generateQRCode(text: String, size: Int): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) { null }
    }

    private fun saveToCache(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val file = File(context.cacheDir, "exam_result_share.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) { null }
    }

    private fun shareImage(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "💪 ${Encouragement.random()} —— 来自 墨答 App")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享考试结果"))
    }
}
