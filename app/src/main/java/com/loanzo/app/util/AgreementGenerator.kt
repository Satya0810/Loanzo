package com.loanzo.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
import com.loanzo.app.data.entity.LoanEntity
import com.loanzo.app.data.entity.UserEntity
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Generates the multi-page Loan Agreement PDF with e-signatures and KYC details.
 */
object AgreementGenerator {

    private const val TAG = "AgreementGenerator"
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    suspend fun generateAgreementPdf(
        context: Context,
        loan: LoanEntity,
        lender: UserEntity,
        borrower: UserEntity
    ): File? = withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        try {
            
            // Shared Paints
            val titlePaint = Paint().apply { textSize = 22f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
            val headerPaint = Paint().apply { textSize = 16f; isFakeBoldText = true; color = android.graphics.Color.DKGRAY }
            val bodyPaint = Paint().apply { textSize = 12f }
            val bodyBoldPaint = Paint().apply { textSize = 12f; isFakeBoldText = true }
            val smallPaint = Paint().apply { textSize = 10f; color = android.graphics.Color.GRAY; textAlign = Paint.Align.CENTER }

            val pageWidth = 595
            val pageHeight = 842

            // ================= PAGE 1: COVER & LOAN TERMS =================
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            var page = doc.startPage(pageInfo)
            var canvas = page.canvas

            var y = 60f
            canvas.drawText("LOAN AGREEMENT", pageWidth / 2f, y, titlePaint); y += 40f
            
            canvas.drawText("This Loan Agreement is executed on ${dateFormat.format(Date())}", pageWidth / 2f, y, smallPaint); y += 40f

            canvas.drawText("PARTIES TO THE AGREEMENT", 40f, y, headerPaint); y += 25f

            // Lender Details
            canvas.drawText("1. LENDER", 40f, y, bodyBoldPaint); y += 20f
            canvas.drawText("Name: ${lender.name}", 60f, y, bodyPaint); y += 18f
            canvas.drawText("PAN: ${if (lender.panNumber.isNotEmpty()) lender.panNumber else "Not Provided"}", 60f, y, bodyPaint); y += 18f
            canvas.drawText("Phone: ${lender.phone}", 60f, y, bodyPaint); y += 18f
            canvas.drawText("Email: ${lender.email.ifEmpty { "Not Provided" }}", 60f, y, bodyPaint); y += 30f

            // Borrower Details
            canvas.drawText("2. BORROWER", 40f, y, bodyBoldPaint); y += 20f
            canvas.drawText("Name: ${borrower.name}", 60f, y, bodyPaint); y += 18f
            canvas.drawText("PAN: ${if (borrower.panNumber.isNotEmpty()) borrower.panNumber else "Not Provided"}", 60f, y, bodyPaint); y += 18f
            canvas.drawText("Phone: ${borrower.phone}", 60f, y, bodyPaint); y += 18f
            canvas.drawText("Email: ${borrower.email.ifEmpty { "Not Provided" }}", 60f, y, bodyPaint); y += 40f

            canvas.drawText("LOAN TERMS", 40f, y, headerPaint); y += 25f
            
            val termsLeftX = 60f
            val termsRightX = 250f
            
            canvas.drawText("Loan Amount:", termsLeftX, y, bodyBoldPaint)
            canvas.drawText("₹${String.format(Locale.getDefault(), "%,.2f", loan.sanctionedAmount)}", termsRightX, y, bodyPaint); y += 20f
            
            canvas.drawText("Purpose:", termsLeftX, y, bodyBoldPaint)
            canvas.drawText(loan.purpose, termsRightX, y, bodyPaint); y += 20f
            
            canvas.drawText("Tenure:", termsLeftX, y, bodyBoldPaint)
            canvas.drawText("${loan.tenureMonths} Months", termsRightX, y, bodyPaint); y += 20f
            
            canvas.drawText("Interest Rate:", termsLeftX, y, bodyBoldPaint)
            canvas.drawText("${loan.interestRate}% (${loan.interestModel})", termsRightX, y, bodyPaint); y += 20f
            
            canvas.drawText("Repayment Freq:", termsLeftX, y, bodyBoldPaint)
            canvas.drawText(loan.repaymentFrequency, termsRightX, y, bodyPaint); y += 40f

            doc.finishPage(page)

            // ================= PAGE 2: RULES & PENALTIES =================
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
            page = doc.startPage(pageInfo)
            canvas = page.canvas
            y = 60f

            canvas.drawText("RULES & PENALTIES", 40f, y, headerPaint); y += 30f

            canvas.drawText("1. Late Payment Penalty:", 40f, y, bodyBoldPaint); y += 20f
            canvas.drawText("If an EMI is not paid within the grace period of ${loan.penaltyGraceDays} days, a penalty will be applied.", 60f, y, bodyPaint); y += 20f
            canvas.drawText("Model: ${loan.penaltyModel}", 60f, y, bodyPaint); y += 20f
            canvas.drawText("Rate: ${loan.penaltyRate}%", 60f, y, bodyPaint); y += 20f
            canvas.drawText("Cap: ${loan.penaltyCapPercent}% of the outstanding amount.", 60f, y, bodyPaint); y += 40f

            canvas.drawText("2. Default Conditions:", 40f, y, bodyBoldPaint); y += 20f
            canvas.drawText("Failure to repay for 3 consecutive months may result in the loan being marked as DEFAULTED.", 60f, y, bodyPaint); y += 20f
            canvas.drawText("The Lender reserves the right to initiate legal recovery proceedings.", 60f, y, bodyPaint); y += 40f

            canvas.drawText("3. Prepayment & Foreclosure:", 40f, y, bodyBoldPaint); y += 20f
            canvas.drawText("The Borrower may prepay the loan partially or fully at any time without foreclosure charges.", 60f, y, bodyPaint); y += 40f

            doc.finishPage(page)

            // ================= PAGE 3: KYC DETAILS =================
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 3).create()
            page = doc.startPage(pageInfo)
            canvas = page.canvas
            y = 60f

            canvas.drawText("KYC VERIFICATION DETAILS", 40f, y, headerPaint); y += 30f

            canvas.drawText("LENDER KYC", 40f, y, bodyBoldPaint); y += 20f
            canvas.drawText("Status: ${lender.kycStatus}", 60f, y, bodyPaint); y += 20f
            canvas.drawText("Aadhaar Verified: ${if (lender.aadhaarVerified) "YES" else "NO"}", 60f, y, bodyPaint); y += 20f
            canvas.drawText("Selfie Liveness Verified: ${if (lender.selfieVerified) "YES" else "NO"}", 60f, y, bodyPaint); y += 40f

            canvas.drawText("BORROWER KYC", 40f, y, bodyBoldPaint); y += 20f
            canvas.drawText("Status: ${borrower.kycStatus}", 60f, y, bodyPaint); y += 20f
            canvas.drawText("Aadhaar Verified: ${if (borrower.aadhaarVerified) "YES" else "NO"}", 60f, y, bodyPaint); y += 20f
            canvas.drawText("Selfie Liveness Verified: ${if (borrower.selfieVerified) "YES" else "NO"}", 60f, y, bodyPaint); y += 40f

            doc.finishPage(page)

            // ================= PAGE 4: SIGNATURES =================
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 4).create()
            page = doc.startPage(pageInfo)
            canvas = page.canvas
            y = 60f

            canvas.drawText("SIGNATURES & EXECUTION", 40f, y, headerPaint); y += 40f

            // Helper to download bitmap from URL
            fun downloadBitmap(urlString: String?): Bitmap? {
                if (urlString.isNullOrEmpty()) return null
                return try {
                    val url = URL(urlString)
                    val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                    connection.doInput = true
                    connection.connect()
                    val input: InputStream = connection.inputStream
                    BitmapFactory.decodeStream(input)
                } catch (e: Exception) {
                    Log.e(TAG, "Error downloading image from $urlString", e)
                    null
                }
            }

            val imgPaint = Paint().apply { isFilterBitmap = true; isAntiAlias = true }

            // LENDER SIGNATURE
            canvas.drawText("Lender Signature", 40f, y, bodyBoldPaint); y += 20f
            if (loan.lenderSignedAt != null) {
                canvas.drawText("Signed At: ${dateFormat.format(Date(loan.lenderSignedAt))}", 40f, y, bodyPaint); y += 20f
                canvas.drawText("Biometric Verified: YES", 40f, y, bodyPaint); y += 10f
                
                // Draw Signature Bitmap
                val sigBmp = downloadBitmap(loan.lenderSignatureUrl)
                if (sigBmp != null) {
                    val scaledBmp = Bitmap.createScaledBitmap(sigBmp, 200, 100, true)
                    canvas.drawBitmap(scaledBmp, 40f, y, imgPaint)
                    y += 110f
                } else {
                    canvas.drawText("[Signature Image Missing]", 40f, y, bodyPaint)
                    y += 40f
                }

                // Draw Selfie Bitmap
                canvas.drawText("Liveness Selfie:", 40f, y, bodyPaint); y += 10f
                val selfieBmp = downloadBitmap(loan.lenderSelfieUrl)
                if (selfieBmp != null) {
                    val scaledSelfie = Bitmap.createScaledBitmap(selfieBmp, 100, 100, true)
                    canvas.drawBitmap(scaledSelfie, 40f, y, imgPaint)
                    y += 110f
                } else {
                    canvas.drawText("[Selfie Image Missing]", 40f, y, bodyPaint)
                    y += 40f
                }
            } else {
                canvas.drawText("PENDING", 40f, y, bodyPaint); y += 40f
            }

            y += 40f

            // BORROWER SIGNATURE
            canvas.drawText("Borrower Signature", 40f, y, bodyBoldPaint); y += 20f
            if (loan.borrowerSignedAt != null) {
                canvas.drawText("Signed At: ${dateFormat.format(Date(loan.borrowerSignedAt))}", 40f, y, bodyPaint); y += 20f
                canvas.drawText("Biometric Verified: YES", 40f, y, bodyPaint); y += 10f
                
                // Draw Signature Bitmap
                val sigBmp = downloadBitmap(loan.borrowerSignatureUrl)
                if (sigBmp != null) {
                    val scaledBmp = Bitmap.createScaledBitmap(sigBmp, 200, 100, true)
                    canvas.drawBitmap(scaledBmp, 40f, y, imgPaint)
                    y += 110f
                } else {
                    canvas.drawText("[Signature Image Missing]", 40f, y, bodyPaint)
                    y += 40f
                }

                // Draw Selfie Bitmap
                canvas.drawText("Liveness Selfie:", 40f, y, bodyPaint); y += 10f
                val selfieBmp = downloadBitmap(loan.borrowerSelfieUrl)
                if (selfieBmp != null) {
                    val scaledSelfie = Bitmap.createScaledBitmap(selfieBmp, 100, 100, true)
                    canvas.drawBitmap(scaledSelfie, 40f, y, imgPaint)
                    y += 110f
                } else {
                    canvas.drawText("[Selfie Image Missing]", 40f, y, bodyPaint)
                    y += 40f
                }
            } else {
                canvas.drawText("PENDING", 40f, y, bodyPaint); y += 40f
            }

            doc.finishPage(page)

            val file = File(context.cacheDir, "agreement_${loan.loanId.take(8)}.pdf")
            file.outputStream().use { out ->
                doc.writeTo(out)
            }
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error generating agreement PDF", e)
            null
        } finally {
            try {
                doc.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Generates a No Objection Certificate (NOC) & Debt Satisfaction Clearance PDF when loan is repaid in full.
     */
    suspend fun generateLoanNocCertificate(
        context: Context,
        loan: LoanEntity,
        lender: UserEntity,
        borrower: UserEntity
    ): File? = withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        try {
            val pageWidth = 595
            val pageHeight = 842

            val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true; textAlign = Paint.Align.CENTER; color = android.graphics.Color.rgb(10, 22, 40) }
            val subtitlePaint = Paint().apply { textSize = 13f; textAlign = Paint.Align.CENTER; color = android.graphics.Color.DKGRAY }
            val headerPaint = Paint().apply { textSize = 14f; isFakeBoldText = true; color = android.graphics.Color.rgb(16, 42, 82) }
            val bodyPaint = Paint().apply { textSize = 11f; color = android.graphics.Color.BLACK }
            val bodyBoldPaint = Paint().apply { textSize = 11f; isFakeBoldText = true }
            val sealPaint = Paint().apply { textSize = 10f; isFakeBoldText = true; color = android.graphics.Color.rgb(46, 125, 50); textAlign = Paint.Align.CENTER }

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas

            // Gold decorative border
            val borderPaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = android.graphics.Color.rgb(255, 193, 7) }
            canvas.drawRect(25f, 25f, pageWidth - 25f, pageHeight - 25f, borderPaint)

            var y = 70f
            canvas.drawText("NO OBJECTION CERTIFICATE (NOC)", pageWidth / 2f, y, titlePaint); y += 24f
            canvas.drawText("& LOAN CLEARANCE SATISFACTION LETTER", pageWidth / 2f, y, subtitlePaint); y += 40f

            canvas.drawText("Certificate Ref: NOC-${loan.loanId.take(8).uppercase()}", pageWidth / 2f, y, Paint().apply { textSize = 10f; textAlign = Paint.Align.CENTER; color = android.graphics.Color.GRAY }); y += 30f

            // Declaration statement
            canvas.drawText("TO WHOMSOEVER IT MAY CONCERN", 45f, y, headerPaint); y += 25f
            
            val text1 = "This is to certify that the financial credit facility detailed below has been SATISFIED IN FULL"
            val text2 = "with ZERO outstanding balance, and the Lender confirms no further claims or liabilities exist."
            canvas.drawText(text1, 45f, y, bodyPaint); y += 18f
            canvas.drawText(text2, 45f, y, bodyPaint); y += 35f

            // Facility Particulars
            canvas.drawText("FACILITY PARTICULARS", 45f, y, headerPaint); y += 20f
            canvas.drawText("Loan Reference ID: ${loan.loanId}", 45f, y, bodyPaint); y += 18f
            canvas.drawText("Principal Sanctioned: INR ${loan.sanctionedAmount}", 45f, y, bodyPaint); y += 18f
            canvas.drawText("Total Disbursed: INR ${loan.disbursedAmount}", 45f, y, bodyPaint); y += 18f
            canvas.drawText("Outstanding Balance: INR 0.00 (Fully Settled & Discharged)", 45f, y, bodyBoldPaint); y += 18f
            canvas.drawText("Loan Purpose: ${loan.purpose}", 45f, y, bodyPaint); y += 30f

            // Parties Table
            canvas.drawText("CONTRACTING PARTIES", 45f, y, headerPaint); y += 20f
            canvas.drawText("Borrower: ${borrower.name} (Phone: ${borrower.phone})", 45f, y, bodyPaint); y += 18f
            canvas.drawText("Lender: ${lender.name} (Phone: ${lender.phone})", 45f, y, bodyPaint); y += 18f
            canvas.drawText("Settlement Date: ${dateFormat.format(Date(loan.closedAt ?: System.currentTimeMillis()))}", 45f, y, bodyPaint); y += 45f

            // Digital Clearance Seal
            canvas.drawText("★ OFFICIAL DIGITAL CLEARANCE & NO LIEN CERTIFIED ★", pageWidth / 2f, y, sealPaint); y += 16f
            canvas.drawText("VERIFIED BY LOANZO DECENTRALIZED SMART AUDIT ENGINE", pageWidth / 2f, y, sealPaint); y += 40f

            doc.finishPage(page)

            val outputDir = File(context.filesDir, "noc_certificates").apply { mkdirs() }
            val outputFile = File(outputDir, "NOC_${loan.loanId.take(8)}.pdf")
            outputFile.outputStream().use { out -> doc.writeTo(out) }
            outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate NOC: ${e.message}", e)
            null
        } finally {
            try {
                doc.close()
            } catch (_: Exception) {}
        }
    }
}
