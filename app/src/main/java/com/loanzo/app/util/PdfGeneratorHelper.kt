package com.loanzo.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object PdfGeneratorHelper {

    fun generateLoanAgreementPdf(
        context: Context,
        loanDetails: String,
        signatureBitmap: Bitmap,
        fileName: String = "LoanAgreement_${System.currentTimeMillis()}.pdf"
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)

        val canvas: Canvas = page.canvas
        val paint = Paint()

        // Draw Title
        paint.textSize = 24f
        paint.color = Color.BLACK
        paint.isFakeBoldText = true
        canvas.drawText("Loan Agreement", 200f, 50f, paint)

        // Draw Loan Details
        paint.textSize = 14f
        paint.isFakeBoldText = false
        var yPosition = 100f
        loanDetails.split("\n").forEach { line ->
            canvas.drawText(line, 50f, yPosition, paint)
            yPosition += 20f
        }

        // Draw Signature Label
        yPosition += 50f
        paint.isFakeBoldText = true
        canvas.drawText("Borrower Signature:", 50f, yPosition, paint)

        // Draw Signature Image
        yPosition += 20f
        val scaledSignature = Bitmap.createScaledBitmap(signatureBitmap, 200, 100, true)
        canvas.drawBitmap(scaledSignature, 50f, yPosition, null)

        pdfDocument.finishPage(page)

        // Save to file
        val file = File(context.getExternalFilesDir(null), fileName)
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } finally {
            pdfDocument.close()
        }
    }
}
