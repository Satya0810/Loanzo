package com.loanzo.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.util.Log
import androidx.core.content.FileProvider
import com.loanzo.app.data.entity.LoanEntity
import com.loanzo.app.data.entity.RepaymentEntity
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Generates PDF and CSV reports for loan data.
 * Uses Android's built-in PdfDocument API — no external dependencies.
 */
object ReportExporter {

    private const val TAG = "ReportExporter"
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    // ── PDF Generation ──────────────────────────────────────────

    fun generateLoanSummaryPdf(context: Context, loan: LoanEntity, repayments: List<RepaymentEntity>): File? {
        return try {
            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true }
            val headerPaint = Paint().apply { textSize = 14f; isFakeBoldText = true; color = android.graphics.Color.DKGRAY }
            val bodyPaint = Paint().apply { textSize = 12f }
            val smallPaint = Paint().apply { textSize = 10f; color = android.graphics.Color.GRAY }

            var y = 50f
            canvas.drawText("Loanzo — Loan Summary Report", 40f, y, titlePaint); y += 30f
            canvas.drawText("Generated: ${dateFormat.format(Date())}", 40f, y, smallPaint); y += 30f

            // Loan Details
            canvas.drawText("Loan Details", 40f, y, headerPaint); y += 22f
            canvas.drawText("Loan ID: ${loan.loanId}", 40f, y, bodyPaint); y += 18f
            canvas.drawText("Purpose: ${loan.purpose}", 40f, y, bodyPaint); y += 18f
            canvas.drawText("Type: ${loan.loanType}", 40f, y, bodyPaint); y += 18f
            canvas.drawText("Status: ${loan.status}", 40f, y, bodyPaint); y += 18f
            canvas.drawText("Sanctioned: ₹${String.format(java.util.Locale.getDefault(), "%,.2f", loan.sanctionedAmount)}", 40f, y, bodyPaint); y += 18f
            canvas.drawText("Outstanding: ₹${String.format(java.util.Locale.getDefault(), "%,.2f", loan.outstandingAmount)}", 40f, y, bodyPaint); y += 18f
            canvas.drawText("Interest: ${loan.interestRate}% (${loan.interestModel})", 40f, y, bodyPaint); y += 18f
            canvas.drawText("Tenure: ${loan.tenureMonths} months", 40f, y, bodyPaint); y += 18f
            canvas.drawText("Repayment Frequency: ${loan.repaymentFrequency}", 40f, y, bodyPaint); y += 18f
            canvas.drawText("Created: ${dateFormat.format(Date(loan.createdAt))}", 40f, y, bodyPaint); y += 30f

            // Repayment Summary
            val totalPaid = repayments.filter { it.status == "PAID" }.sumOf { it.amount }
            val totalInterest = repayments.filter { it.status == "PAID" }.sumOf { it.interestComponent }
            val totalPenalty = repayments.sumOf { it.penalty }

            canvas.drawText("Repayment Summary", 40f, y, headerPaint); y += 22f
            canvas.drawText("Total Repayments: ${repayments.size}", 40f, y, bodyPaint); y += 18f
            canvas.drawText("Total Paid: ₹${String.format(java.util.Locale.getDefault(), "%,.2f", totalPaid)}", 40f, y, bodyPaint); y += 18f
            canvas.drawText("Total Interest Paid: ₹${String.format(java.util.Locale.getDefault(), "%,.2f", totalInterest)}", 40f, y, bodyPaint); y += 18f
            canvas.drawText("Total Penalties: ₹${String.format(java.util.Locale.getDefault(), "%,.2f", totalPenalty)}", 40f, y, bodyPaint); y += 30f

            // Repayment Table Header
            if (repayments.isNotEmpty()) {
                canvas.drawText("Repayment History", 40f, y, headerPaint); y += 22f
                canvas.drawText("Date", 40f, y, smallPaint)
                canvas.drawText("Amount", 180f, y, smallPaint)
                canvas.drawText("Status", 300f, y, smallPaint)
                canvas.drawText("Penalty", 420f, y, smallPaint)
                y += 16f

                for (repayment in repayments.take(25)) { // Limit to 1 page
                    val dateStr = dateFormat.format(Date(repayment.dueDate))
                    canvas.drawText(dateStr, 40f, y, bodyPaint)
                    canvas.drawText("₹${String.format(java.util.Locale.getDefault(), "%,.0f", repayment.amount)}", 180f, y, bodyPaint)
                    canvas.drawText(repayment.status, 300f, y, bodyPaint)
                    canvas.drawText("₹${String.format(java.util.Locale.getDefault(), "%,.0f", repayment.penalty)}", 420f, y, bodyPaint)
                    y += 16f
                    if (y > 800f) break
                }
            }

            doc.finishPage(page)

            val file = File(context.cacheDir, "loan_summary_${loan.loanId.take(8)}.pdf")
            doc.writeTo(file.outputStream())
            doc.close()
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error generating PDF", e)
            null
        }
    }

    fun generateInterestCertificatePdf(context: Context, loan: LoanEntity, repayments: List<RepaymentEntity>): File? {
        return try {
            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true }
            val headerPaint = Paint().apply { textSize = 14f; isFakeBoldText = true }
            val bodyPaint = Paint().apply { textSize = 12f }
            val smallPaint = Paint().apply { textSize = 10f; color = android.graphics.Color.GRAY }

            val paidRepayments = repayments.filter { it.status == "PAID" }
            val totalInterest = paidRepayments.sumOf { it.interestComponent }
            val totalPrincipal = paidRepayments.sumOf { it.principalComponent }

            var y = 50f
            canvas.drawText("Interest Certificate", 40f, y, titlePaint); y += 24f
            canvas.drawText("Financial Year ${Calendar.getInstance().get(Calendar.YEAR)}", 40f, y, smallPaint); y += 30f

            canvas.drawText("Loan Details", 40f, y, headerPaint); y += 22f
            canvas.drawText("Loan ID: ${loan.loanId}", 40f, y, bodyPaint); y += 18f
            canvas.drawText("Purpose: ${loan.purpose}", 40f, y, bodyPaint); y += 18f
            canvas.drawText("Interest Rate: ${loan.interestRate}% (${loan.interestModel})", 40f, y, bodyPaint); y += 30f

            canvas.drawText("Interest Summary", 40f, y, headerPaint); y += 22f
            canvas.drawText("Total Principal Repaid: ₹${String.format(java.util.Locale.getDefault(), "%,.2f", totalPrincipal)}", 40f, y, bodyPaint); y += 18f
            canvas.drawText("Total Interest Paid: ₹${String.format(java.util.Locale.getDefault(), "%,.2f", totalInterest)}", 40f, y, bodyPaint); y += 18f
            canvas.drawText("Number of EMIs Paid: ${paidRepayments.size}", 40f, y, bodyPaint); y += 30f

            canvas.drawText("This certificate is generated by Loanzo for informational purposes.", 40f, y, smallPaint)

            doc.finishPage(page)

            val file = File(context.cacheDir, "interest_cert_${loan.loanId.take(8)}.pdf")
            doc.writeTo(file.outputStream())
            doc.close()
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error generating interest certificate PDF", e)
            null
        }
    }

    // ── CSV Generation ──────────────────────────────────────────

    fun generateRepaymentCsv(context: Context, loan: LoanEntity, repayments: List<RepaymentEntity>): File? {
        return try {
            val file = File(context.cacheDir, "repayments_${loan.loanId.take(8)}.csv")
            FileWriter(file).use { writer ->
                writer.append("Due Date,Paid Date,Amount,Principal,Interest,Penalty,Status,Note\n")
                for (r in repayments) {
                    writer.append("${dateFormat.format(Date(r.dueDate))},")
                    writer.append("${if (r.paidDate != null) dateFormat.format(Date(r.paidDate)) else "N/A"},")
                    writer.append("${r.amount},${r.principalComponent},${r.interestComponent},${r.penalty},")
                    writer.append("${r.status},\"${r.note}\"\n")
                }
            }
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error generating CSV", e)
            null
        }
    }

    // ── Share Intent ─────────────────────────────────────────────

    fun shareFile(context: Context, file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Export Report")
            if (context !is android.app.Activity) {
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing file", e)
        }
    }
}
