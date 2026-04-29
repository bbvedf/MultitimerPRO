package com.android.multitimerpro.util

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import com.android.multitimerpro.data.HistoryEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.compose.ui.graphics.toArgb
import com.android.multitimerpro.ui.theme.*

object ShareUtils {
    /**
     * Comprime una imagen desde una URI a un ByteArray de tamaño reducido (~20Kb).
     * Útil para subir a Supabase Storage sin desperdiciar ancho de banda/espacio.
     */
    fun compressImage(context: Context, uri: Uri, maxSizeKb: Int = 20): ByteArray? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // 1. Redimensionar a un tamaño pequeño para avatar (max 256px)
            val maxDimension = 256
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                val newWidth = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
                val newHeight = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
                Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            } else {
                originalBitmap
            }

            // 2. Comprimir calidad hasta bajar del tamaño deseado
            var quality = 80
            var byteArray: ByteArray
            do {
                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                byteArray = outputStream.toByteArray()
                quality -= 10
            } while (byteArray.size > maxSizeKb * 1024 && quality > 10)

            byteArray
        } catch (e: Exception) {
            null
        }
    }

    fun generateMedalShareCard(
        context: Context,
        medalName: String,
        medalDesc: String,
        tier: Int,
        rankName: String
    ): Uri? {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Background - Dark Gradient
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(Color.BLACK, ObsidianBlack.toArgb(), ShadowBlack.toArgb()),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Accent Glow
        val accentColor = when(tier) {
            1 -> MedalBronze.toArgb() // Bronze
            2 -> MedalSilver.toArgb() // Silver
            3 -> MedalGold.toArgb() // Gold
            else -> AccentCyan.toArgb() // AccentCyan
        }
        
        val glowPaint = Paint().apply {
            shader = RadialGradient(
                width / 2f, height / 2f, 600f,
                intArrayOf(accentColor and 0x33FFFFFF, Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(width / 2f, height / 2f, 800f, glowPaint)

        // Text Paints
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 80f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val namePaint = Paint().apply {
            color = accentColor
            textSize = 120f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val descPaint = Paint().apply {
            color = SilverGrey.toArgb()
            textSize = 50f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val brandPaint = Paint().apply {
            color = AccentCyan.toArgb() // AccentCyan
            textSize = 40f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.2f
        }

        // Draw Content
        canvas.drawText("ACHIEVEMENT UNLOCKED", width / 2f, 400f, titlePaint)
        
        // Medal Icon (Placeholder for actual SVG/Bitmap)
        val iconPaint = Paint().apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = 20f
        }
        canvas.drawCircle(width / 2f, height / 2f - 100f, 200f, iconPaint)
        
        val tierText = when(tier) {
            1 -> "BRONZE"
            2 -> "SILVER"
            3 -> "GOLD"
            else -> ""
        }
        canvas.drawText(tierText, width / 2f, height / 2f + 200f, descPaint)
        canvas.drawText(medalName.uppercase(), width / 2f, height / 2f + 350f, namePaint)
        
        // Wrap description text
        val bounds = Rect()
        descPaint.getTextBounds(medalDesc, 0, medalDesc.length, bounds)
        canvas.drawText(medalDesc, width / 2f, height / 2f + 450f, descPaint)

        canvas.drawText("RANK: $rankName", width / 2f, height - 300f, brandPaint)
        canvas.drawText("MULTITIMER PRO", width / 2f, height - 150f, brandPaint)

        // Save and get URI
        return try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "medal_share_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }

    fun generateCSV(context: Context, items: List<HistoryEntity>): Uri? {
        val csvHeader = "ID,Timer Name,Category,Duration (ms),Completed At,Notes,Snoozed\n"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        val csvContent = StringBuilder(csvHeader)
        for (item in items) {
            val completedAtStr = dateFormat.format(Date(item.completedAt))
            // Escape quotes and remove/replace line breaks for CSV safety
            val escapedNotes = item.notes.replace("\"", "\"\"").replace("\n", " ").replace("\r", "")
            val escapedName = item.timerName.replace("\"", "\"\"")
            val line = "${item.id},\"$escapedName\",\"${item.category}\",${item.durationMillis},\"$completedAtStr\",\"$escapedNotes\",${item.isSnoozed}\n"
            csvContent.append(line)
        }

        return try {
            val cachePath = File(context.cacheDir, "exports")
            cachePath.mkdirs()
            val file = File(cachePath, "history_export_${System.currentTimeMillis()}.csv")
            file.writeText(csvContent.toString())
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }

    fun generatePDF(context: Context, items: List<HistoryEntity>): Uri? {
        val width = 595 // A4 width in points
        val height = 842 // A4 height in points
        val pdfDocument = android.graphics.pdf.PdfDocument()
        
        val headerPaint = Paint().apply {
            textSize = 20f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = CharcoalGrey.toArgb()
        }
        val subHeaderPaint = Paint().apply {
            textSize = 14f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = SlateGrey.toArgb()
        }
        val textPaint = Paint().apply {
            textSize = 11f
            isAntiAlias = true
            color = EbonyGrey.toArgb()
        }
        val footerPaint = Paint().apply {
            textSize = 10f
            isAntiAlias = true
            color = Color.GRAY
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val totalMillis = items.sumOf { it.durationMillis }
        val totalTimeStr = String.format(Locale.getDefault(), "%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(totalMillis),
            TimeUnit.MILLISECONDS.toMinutes(totalMillis) % 60,
            TimeUnit.MILLISECONDS.toSeconds(totalMillis) % 60
        )

        var currentItemIndex = 0
        var pageNumber = 1
        val itemsPerPageNormal = 35
        val itemsPerPageFirst = 28 // Less on first page due to summary

        while (currentItemIndex < items.size || pageNumber == 1) {
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(width, height, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            var y = 50f

            if (pageNumber == 1) {
                canvas.drawText("MULTITIMER PRO - HISTORY REPORT", 50f, y, headerPaint)
                y += 40f
                canvas.drawText("Summary", 50f, y, subHeaderPaint)
                y += 20f
                canvas.drawText("Total Sessions: ${items.size}", 50f, y, textPaint)
                y += 15f
                canvas.drawText("Total Focused Time: $totalTimeStr", 50f, y, textPaint)
                y += 15f
                canvas.drawText("Report Generated: ${dateFormat.format(Date())}", 50f, y, textPaint)
                y += 40f
                
                canvas.drawRect(50f, y - 25f, width - 50f, y - 23f, Paint().apply { color = Color.LTGRAY })
                canvas.drawText("Session Details", 50f, y, subHeaderPaint)
                y += 30f
            } else {
                canvas.drawText("MULTITIMER PRO - SESSION DETAILS (Cont.)", 50f, y, subHeaderPaint)
                y += 40f
            }

            val itemsLimit = if (pageNumber == 1) itemsPerPageFirst else itemsPerPageNormal
            var countOnPage = 0
            while (currentItemIndex < items.size && countOnPage < itemsLimit) {
                val item = items[currentItemIndex]
                val dateStr = dateFormat.format(Date(item.completedAt))
                val durationStr = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(item.durationMillis),
                    TimeUnit.MILLISECONDS.toMinutes(item.durationMillis) % 60,
                    TimeUnit.MILLISECONDS.toSeconds(item.durationMillis) % 60
                )

                canvas.drawText("$dateStr | ${item.timerName} | $durationStr", 50f, y, textPaint)
                y += 20f
                currentItemIndex++
                countOnPage++
            }

            canvas.drawText("Page $pageNumber", width / 2f - 20f, height - 30f, footerPaint)
            pdfDocument.finishPage(page)
            pageNumber++
            
            if (currentItemIndex >= items.size && items.isNotEmpty()) break
            if (items.isEmpty() && pageNumber > 1) break
        }

        return try {
            val cachePath = File(context.cacheDir, "exports")
            cachePath.mkdirs()
            val file = File(cachePath, "history_export_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }
}
