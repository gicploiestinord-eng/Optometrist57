package ro.optometrist57.app.print

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import ro.optometrist57.app.data.ConsultationWithPatient
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptRenderer {
    private const val WIDTH_PX = 456 // 57 mm at 203 dpi, typical 58 mm-class thermal head
    private const val WIDTH_PT = 162f // 57 mm in PDF points
    private fun date(ts: Long) = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ro", "RO")).format(Date(ts))

    private fun wrap(value: String, max: Int = 30): List<String> {
        if (value.length <= max) return listOf(value)
        val result = mutableListOf<String>()
        var rest = value
        while (rest.length > max) {
            var cut = rest.lastIndexOf(' ', max)
            if (cut < max / 2) cut = max
            result += rest.substring(0, cut).trim()
            rest = rest.substring(cut).trim()
        }
        if (rest.isNotEmpty()) result += rest
        return result
    }

    private fun lines(data: ConsultationWithPatient): List<String> {
        val c = data.consultation
        val p = data.patient
        val out = mutableListOf("OPTOMETRIE", "BON NR. ${c.receiptNumber}", date(c.dateTime), "------------------------------")
        out += wrap("PACIENT: ${p.name}")
        if (p.phone.isNotBlank()) out += wrap("TEL: ${p.phone}")
        if (c.location.isNotBlank()) out += wrap("LOC: ${c.location}")
        out += "------------------------------"
        out += "OD"
        out += " SPH: ${c.odSph.ifBlank { "-" }}  CYL: ${c.odCyl.ifBlank { "-" }}"
        out += " AX: ${c.odAxis.ifBlank { "-" }}"
        out += "OS"
        out += " SPH: ${c.osSph.ifBlank { "-" }}  CYL: ${c.osCyl.ifBlank { "-" }}"
        out += " AX: ${c.osAxis.ifBlank { "-" }}"
        if (c.dp.isNotBlank()) out += "DP: ${c.dp} mm"
        if (c.add.isNotBlank()) out += "ADD: ${c.add}"
        if (c.vd.isNotBlank()) out += "VD: ${c.vd}"
        if (c.va.isNotBlank()) out += "VA: ${c.va}"
        if (c.notes.isNotBlank()) out += wrap("OBS: ${c.notes}")
        out += "------------------------------"
        out += "Document generat local"
        return out
    }

    fun createPng(context: Context, data: ConsultationWithPatient): File {
        val textSize = 23f
        val lineHeight = 32f
        val rows = lines(data)
        val bmp = Bitmap.createBitmap(WIDTH_PX, (rows.size * lineHeight + 32).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; this.textSize = textSize; typeface = Typeface.create("monospace", Typeface.NORMAL) }
        var y = 30f
        rows.forEachIndexed { i, line ->
            canvas.drawText(line, 12f, y, paint)
            y += lineHeight
        }
        val dir = File(context.filesDir, "receipts").apply { mkdirs() }
        val file = File(dir, "bon-${data.consultation.receiptNumber}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        return file
    }

    fun createPdf(context: Context, data: ConsultationWithPatient): File {
        val rows = lines(data)
        val pageHeight = (rows.size * 16f + 30f).toInt().coerceAtLeast(150)
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(WIDTH_PT.toInt(), pageHeight, 1).create())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 8.2f; typeface = Typeface.create("monospace", Typeface.NORMAL) }
        var y = 18f
        rows.forEach { row -> page.canvas.drawText(row, 5f, y, paint); y += 16f }
        doc.finishPage(page)
        val dir = File(context.filesDir, "receipts").apply { mkdirs() }
        val file = File(dir, "bon-${data.consultation.receiptNumber}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun sharePng(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/png"; putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Trimite bonul către RawBT"))
    }

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"; putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Partajează PDF-ul"))
    }
}
