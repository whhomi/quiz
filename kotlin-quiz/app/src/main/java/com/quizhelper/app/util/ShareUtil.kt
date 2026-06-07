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
            shareImage(context, uri, correctRate)
        }
    }

    private fun generateResultImage(context: Context, score: Double, maxScore: Double,
                                     correctRate: Double, correctCount: Int, totalCount: Int,
                                     durationSeconds: Int, isPass: Boolean): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        val totalH = 1200
        val bitmap = Bitmap.createBitmap(WIDTH, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        var y = PADDING.toFloat()

        // Header: Blue banner
        paint.color = Color.parseColor("#2563EB")
        canvas.drawRoundRect(RectF(PADDING.toFloat(), y, (WIDTH - PADDING).toFloat(), y + 180f), CARD_RADIUS, CARD_RADIUS, paint)

        textPaint.apply { textSize = 44f; color = Color.WHITE; isFakeBoldText = true; isAntiAlias = true }
        canvas.drawText("🏆 墨答 · 考试结果", WIDTH / 2f - 200f, y + 65f, textPaint)

        textPaint.apply { textSize = 38f; color = Color.WHITE; isFakeBoldText = true }
        canvas.drawText(if (isPass) "🎉 恭喜通过！" else "💪 继续加油！", WIDTH / 2f - 150f, y + 130f, textPaint)
        y += 220f

        // Score card
        paint.color = Color.parseColor(if (isPass) "#F0FDF4" else "#FEF2F2")
        canvas.drawRoundRect(RectF(PADDING.toFloat(), y, (WIDTH - PADDING).toFloat(), y + 240f), CARD_RADIUS, CARD_RADIUS, paint)

        textPaint.apply { textSize = 56f; color = Color.parseColor(if (isPass) "#16A34A" else "#DC2626"); isFakeBoldText = true }
        canvas.drawText("${score.toInt()} 分", PADDING + 40f, y + 75f, textPaint)

        textPaint.apply { textSize = 40f; color = Color.GRAY; isFakeBoldText = false }
        canvas.drawText("正确率 ${correctRate.toInt()}%", PADDING + 40f, y + 140f, textPaint)

        textPaint.apply { textSize = 30f; color = Color.DKGRAY; isFakeBoldText = false }
        canvas.drawText("正确: $correctCount / $totalCount 题", WIDTH / 2f + 60f, y + 70f, textPaint)
        canvas.drawText("用时: ${TimeUtils.formatDuration(durationSeconds)}", WIDTH / 2f + 60f, y + 120f, textPaint)
        canvas.drawText("已答: $correctCount / $totalCount", WIDTH / 2f + 60f, y + 170f, textPaint)
        y += 280f

        // Divider
        paint.color = Color.parseColor("#E5E7EB")
        canvas.drawRect(PADDING.toFloat(), y, (WIDTH - PADDING).toFloat(), y + 2f, paint)
        y += 30f

        // Encouragement
        val enc = Encouragement.random()
        paint.color = Color.parseColor("#EFF6FF")
        canvas.drawRoundRect(RectF(PADDING.toFloat(), y, (WIDTH - PADDING).toFloat(), y + 80f), 20f, 20f, paint)
        textPaint.apply { textSize = 32f; color = Color.parseColor("#2563EB"); isFakeBoldText = false }
        canvas.drawText("💪 $enc", PADDING + 30f, y + 50f, textPaint)
        y += 120f

        // QR Code
        val qrCode = generateQRCode("https://github.com/whhomi/quiz/releases", 280)
        if (qrCode != null) {
            val qrX = WIDTH / 2 - 140
            canvas.drawBitmap(qrCode, qrX.toFloat(), y, null)
        }
        textPaint.apply { textSize = 26f; color = Color.GRAY; isFakeBoldText = false }
        canvas.drawText("扫码下载墨答 App", WIDTH / 2f - 110f, y + 320f, textPaint)
        y += 360f

        // Footer
        paint.color = Color.parseColor("#F3F4F6")
        canvas.drawRoundRect(RectF(PADDING.toFloat(), y, (WIDTH - PADDING).toFloat(), totalH - PADDING.toFloat()), CARD_RADIUS, CARD_RADIUS, paint)
        textPaint.apply { textSize = 24f; color = Color.LTGRAY; isFakeBoldText = false }
        canvas.drawText("墨答 - 优雅刷题，从容作答", WIDTH / 2f - 170f, y + 40f, textPaint)

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

    private fun shareImage(context: Context, uri: Uri, correctRate: Double) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "📝 我在墨答完成了考试，正确率 ${correctRate.toInt()}%！💪 ${Encouragement.random()} —— 来自 墨答 App")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享考试结果"))
    }
}
