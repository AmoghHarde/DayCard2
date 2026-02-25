package com.amogh.dailyday

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class DailyImageWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    private val tag = "DailyImageWorker"
    private val prefs = appContext.getSharedPreferences("daily_day_prefs", Context.MODE_PRIVATE)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val context = applicationContext

            val today = LocalDate.now()
            val english = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase(Locale.ENGLISH)
            val czech = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("cs", "CZ")).uppercase(Locale("cs", "CZ"))
            val marathi = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("mr", "IN"))
            val dateStr = today.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))

            val bitmap = renderBitmap(
                width = 1080,
                height = 1080,
                line1 = english,
                line2 = czech,
                line3 = marathi,
                line4 = dateStr
            )

            val uri = getOrCreateImageUri(context)
            writeBitmapToUri(context, uri, bitmap)

            return@withContext Result.success()
        } catch (t: Throwable) {
            Log.e(tag, "Failed to generate daily image", t)
            return@withContext Result.retry()
        }
    }

    private fun renderBitmap(
        width: Int,
        height: Int,
        line1: String,
        line2: String,
        line3: String,
        line4: String
    ): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            Color.parseColor("#0F172A"),
            Color.parseColor("#1E293B"),
            Shader.TileMode.CLAMP
        )
        bgPaint.shader = shader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val white = Color.WHITE
        val gray = Color.parseColor("#D1D5DB")

        val p1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = white
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 88f
        }
        val p2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = white
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 72f
        }
        val p3 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = white
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textSize = 72f
        }
        val p4 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gray
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 48f
        }

        val cx = width / 2f
        val baseY = height / 2f
        val spacing = 90f

        canvas.drawText(line1, cx, baseY - spacing, p1)
        canvas.drawText(line2, cx, baseY, p2)
        canvas.drawText(line3, cx, baseY + spacing, p3)
        canvas.drawText(line4, cx, baseY + spacing * 2, p4)

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.parseColor("#334155")
        }
        canvas.drawRect(6f, 6f, width - 6f, height - 6f, border)

        return bmp
    }

    private fun getOrCreateImageUri(context: Context): Uri {
        prefs.getString("daily_image_uri", null)?.let { s ->
            val uri = Uri.parse(s)
            if (exists(context, uri)) return uri
        }

        val relativePath = "Pictures/DayCard2"
        val fileName = "DAILY_DAY.png"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val newUri = context.contentResolver.insert(collection, values)
            ?: throw IllegalStateException("Failed to insert MediaStore row")

        if (android.os.Build.VERSION.SDK_INT >= 29) {
            val cv = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            context.contentResolver.update(newUri, cv, null, null)
        }

        prefs.edit().putString("daily_image_uri", newUri.toString()).apply()
        return newUri
    }

    private fun writeBitmapToUri(context: Context, uri: Uri, bitmap: Bitmap) {
        val out: OutputStream? = try {
            context.contentResolver.openOutputStream(uri, "wt")
        } catch (_: Exception) {
            context.contentResolver.openOutputStream(uri)
        }
        out?.use { os ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
            os.flush()
        } ?: throw IllegalStateException("Failed to open output stream for $uri")
    }

    private fun exists(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns._ID), null, null, null)
                ?.use { c -> c.moveToFirst() } ?: false
        } catch (_: Exception) {
            false
        }
    }
}
