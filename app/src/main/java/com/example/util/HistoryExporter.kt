package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.HistoryItem
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object HistoryExporter {

    private fun getFileName(extension: String): String {
        val dateString = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(Date())
        return "CalPro_History_$dateString.$extension"
    }

    fun exportToTxt(context: Context, historyList: List<HistoryItem>): File? {
        val fileName = getFileName("txt")
        val content = StringBuilder()
        content.append("=========================================\n")
        content.append("           CAL PRO HISTORY REPORT        \n")
        content.append("         Presented by Aura Tools         \n")
        content.append("=========================================\n\n")

        if (historyList.isEmpty()) {
            content.append("No history records found.\n")
        } else {
            historyList.forEach { item ->
                content.append("Tool:    ${item.toolName}\n")
                content.append("Input:   ${item.inputs}\n")
                content.append("Output:  ${item.outputs}\n")
                content.append("Date:    ${item.dateString} (${item.timeString})\n")
                content.append("-----------------------------------------\n")
            }
        }

        return try {
            saveToDownloads(context, fileName, "text/plain") { out ->
                out.write(content.toString().toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportToPdf(context: Context, historyList: List<HistoryItem>): File? {
        val fileName = getFileName("pdf")
        val pdfDocument = PdfDocument()
        
        // Single page document for simplicity, size A4 (595 x 842 points)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        
        val paintTitle = Paint().apply {
            color = android.graphics.Color.rgb(15, 76, 129) // Deep Professional Blue
            textSize = 20f
            isFakeBoldText = true
        }
        
        val paintSubtitle = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 10f
        }
        
        val paintHeader = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 14f
            isFakeBoldText = true
        }
        
        val paintTextBold = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 11f
            isFakeBoldText = true
        }
        
        val paintText = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 11f
        }
        
        val paintDivider = Paint().apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 50f
        
        // Header
        canvas.drawText("CAL PRO", 40f, y, paintTitle)
        canvas.drawText("App Builder CalPro - History Report", 350f, y - 10f, paintSubtitle)
        canvas.drawText("Presented by Aura Tools", 350f, y + 5f, paintSubtitle)
        y += 40f
        
        canvas.drawLine(40f, y, 555f, y, paintDivider)
        y += 30f
        
        canvas.drawText("CALCULATION HISTORY LOG", 40f, y, paintHeader)
        y += 30f

        if (historyList.isEmpty()) {
            canvas.drawText("No calculations or history recorded yet.", 40f, y, paintText)
        } else {
            // Draw list
            historyList.take(15).forEach { item -> // Keep to one page log for simplicity
                if (y > 780f) {
                    return@forEach
                }
                
                canvas.drawText("${item.toolName}", 40f, y, paintTextBold)
                canvas.drawText("${item.dateString} at ${item.timeString}", 350f, y, paintSubtitle)
                y += 20f
                
                canvas.drawText("Input:  ${item.inputs}", 60f, y, paintText)
                y += 18f
                
                canvas.drawText("Result: ${item.outputs}", 60f, y, paintTextBold)
                y += 25f
                
                canvas.drawLine(40f, y, 555f, y, paintDivider)
                y += 20f
            }
        }
        
        pdfDocument.finishPage(page)
        
        return try {
            val exportedFile = saveToDownloads(context, fileName, "application/pdf") { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            exportedFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun saveToDownloads(context: Context, fileName: String, mimeType: String, writer: (OutputStream) -> Unit): File? {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri).use { outputStream ->
                    if (outputStream != null) {
                        writer(outputStream)
                    }
                }
                return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { out ->
                writer(out)
            }
            return file
        }
        return null
    }
}
