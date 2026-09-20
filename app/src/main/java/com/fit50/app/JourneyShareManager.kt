package com.fit50.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JourneyShareManager(private val activity: Activity) {

    private val forest = Color.parseColor("#1F2B24")
    private val cream = Color.parseColor("#FAF7F2")
    private val cream2 = Color.parseColor("#F2EDE4")
    private val accent = Color.parseColor("#E85A2C")
    private val ink = Color.parseColor("#1A1A1A")
    private val ink2 = Color.parseColor("#4A4A4A")
    private val lime = Color.parseColor("#D4E85C")

    fun share(platform: String, payloadJson: String): Pair<Boolean, String> {
        return runCatching {
            val data = JSONObject(payloadJson)
            val total = data.optInt("total", 0)
            val mins = data.optInt("mins", 0)
            val streak = data.optInt("streak", 0)
            val week = data.optInt("week", 0)

            val text = buildString {
                append("Fit50+ · המסע שלי\n")
                append("אימונים שהושלמו: ").append(total).append('\n')
                append("דקות תנועה: ").append(mins).append('\n')
                append("רצף נוכחי: ").append(streak).append(" ימים\n")
                append("השבוע: ").append(week).append(" אימונים")
            }

            when (platform.lowercase(Locale.ROOT)) {
                "copy" -> {
                    val clipboard =
                        activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText("Fit50+ · המסע שלי", text)
                    )
                    true to "המסע שלך הועתק ללוח"
                }

                "save" -> {
                    val bitmap = createJourneyCard(total, mins, streak, week)
                    saveToGallery(bitmap)
                    bitmap.recycle()
                    true to "כרטיס המסע נשמר בגלריה"
                }

                else -> {
                    val bitmap = createJourneyCard(total, mins, streak, week)
                    val uri = cacheBitmap(bitmap)
                    bitmap.recycle()
                    openShare(platform, uri, text)
                    true to "השיתוף נפתח"
                }
            }
        }.getOrElse { error ->
            false to (error.localizedMessage ?: "השיתוף נכשל")
        }
    }

    private fun createJourneyCard(
        total: Int,
        mins: Int,
        streak: Int,
        week: Int
    ): Bitmap {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(cream)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = forest
        canvas.drawRoundRect(
            RectF(54f, 54f, 1026f, 330f),
            48f,
            48f,
            paint
        )

        val logoY = 205f
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textSize = 108f
        paint.textAlign = Paint.Align.LEFT
        paint.color = cream
        canvas.drawText("Fit", 92f, logoY, paint)

        paint.color = accent
        canvas.drawText("50", 280f, logoY, paint)

        paint.textSize = 62f
        paint.color = cream
        canvas.drawText("+", 455f, 165f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 32f
        paint.color = Color.argb(190, 250, 247, 242)
        canvas.drawText("MOVE • BUILD • LIVE", 92f, 272f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textSize = 76f
        paint.color = ink
        canvas.drawText("המסע שלי", 980f, 440f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 32f
        paint.color = ink2
        val dateText = SimpleDateFormat("dd.MM.yyyy", Locale("he", "IL")).format(Date())
        canvas.drawText(dateText, 980f, 492f, paint)

        val cards = listOf(
            Triple("אימונים", total.toString(), accent),
            Triple("דקות תנועה", mins.toString(), forest),
            Triple("רצף ימים", streak.toString(), lime),
            Triple("השבוע", week.toString(), ink)
        )

        val left = 70f
        val top = 570f
        val gap = 28f
        val cardWidth = (width - left * 2 - gap) / 2f
        val cardHeight = 250f

        cards.forEachIndexed { index, item ->
            val row = index / 2
            val col = index % 2
            val x = left + col * (cardWidth + gap)
            val y = top + row * (cardHeight + gap)

            paint.color = cream2
            canvas.drawRoundRect(
                RectF(x, y, x + cardWidth, y + cardHeight),
                34f,
                34f,
                paint
            )

            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            paint.textSize = 82f
            paint.color = item.third
            canvas.drawText(
                item.second,
                x + cardWidth / 2f,
                y + 112f,
                paint
            )

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 31f
            paint.color = ink2
            canvas.drawText(
                item.first,
                x + cardWidth / 2f,
                y + 180f,
                paint
            )
        }

        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 34f
        paint.color = forest
        canvas.drawText(
            "Fit50+ · ממשיכים לנוע",
            width / 2f,
            1245f,
            paint
        )

        return bitmap
    }

    private fun cacheBitmap(bitmap: Bitmap): Uri {
        val dir = File(activity.cacheDir, "shared_journey")
        if (!dir.exists()) dir.mkdirs()

        val file = File(
            dir,
            "fit50_journey_${System.currentTimeMillis()}.png"
        )

        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }

        return FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            file
        )
    }

    private fun saveToGallery(bitmap: Bitmap): Uri {
        val resolver = activity.contentResolver
        val fileName = "Fit50_Journey_${System.currentTimeMillis()}.png"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/Fit50+"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: error("לא הצלחנו ליצור קובץ בגלריה")

        resolver.openOutputStream(uri)?.use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                error("לא הצלחנו לשמור את התמונה")
            }
        } ?: error("לא הצלחנו לפתוח את קובץ התמונה")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }

        return uri
    }

    private fun openShare(
        platform: String,
        uri: Uri,
        text: String
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "Fit50+ · המסע שלי")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri("Fit50+ journey", uri)
        }

        val targetPackage = when (platform.lowercase(Locale.ROOT)) {
            "whatsapp" -> "com.whatsapp"
            "instagram", "stories" -> "com.instagram.android"
            "facebook" -> "com.facebook.katana"
            else -> null
        }

        if (targetPackage != null) {
            intent.setPackage(targetPackage)
            try {
                activity.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                intent.setPackage(null)
            }
        }

        activity.startActivity(
            Intent.createChooser(intent, "שתף את המסע שלך")
        )
    }
}
