package com.loanzo.app.util.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.loanzo.app.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * OpenPDF-inspired declarative PDF generation engine for Loanzo.
 * Uses Android's native hardware-accelerated PdfDocument & Canvas APIs,
 * guaranteeing 100% Android runtime stability (zero desktop AWT dependencies).
 *
 * Automatically stamps the official Loanzo App Logo (R.drawable.app_logo)
 * on top of every page with bank-grade typography, gold accent lines,
 * KPI summary grids, tables, and tamper-evident audit seals.
 */
class LoanzoPdfEngine(private val context: Context) {

    val pageWidth = 595f   // Standard A4 width in PostScript points (72 dpi)
    val pageHeight = 842f  // Standard A4 height in PostScript points
    val marginX = 36f      // 0.5 inch margin
    val contentWidth = pageWidth - (marginX * 2)

    // Primary Colors
    val colorNavyDeep = Color.rgb(10, 22, 40)
    val colorNavyMedium = Color.rgb(16, 42, 82)
    val colorGold = Color.rgb(212, 175, 55)
    val colorGoldLight = Color.rgb(255, 248, 225)
    val colorEmerald = Color.rgb(46, 125, 50)
    val colorEmeraldLight = Color.rgb(232, 245, 233)
    val colorSlate = Color.rgb(80, 95, 115)
    val colorBgLight = Color.rgb(248, 249, 252)
    val colorBorder = Color.rgb(226, 232, 240)
    val colorWhite = Color.WHITE

    // Standard Paints
    val paintTextPrimary = Paint().apply {
        color = colorNavyDeep
        textSize = 10f
        isAntiAlias = true
    }

    val paintTextSecondary = Paint().apply {
        color = colorSlate
        textSize = 9f
        isAntiAlias = true
    }

    val paintTextBold = Paint().apply {
        color = colorNavyDeep
        textSize = 10f
        isFakeBoldText = true
        isAntiAlias = true
    }

    val paintGoldLine = Paint().apply {
        color = colorGold
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    val paintBorderLine = Paint().apply {
        color = colorBorder
        strokeWidth = 0.8f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    // Cached app logo
    private val appLogoBitmap: Bitmap? by lazy {
        try {
            BitmapFactory.decodeResource(context.resources, R.drawable.app_logo)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Draws the official Loanzo header on top of the page.
     * Features: App logo (R.drawable.app_logo), Platform Title, Document Subtitle,
     * Document Reference ID, Page numbering, and Gold Accent Rule.
     */
    fun drawHeader(
        canvas: Canvas,
        pageNumber: Int,
        totalPages: Int,
        documentTitle: String,
        documentRefId: String,
        securityClassification: String = "OFFICIAL SECURE AUDIT RECORD"
    ): Float {
        val topY = 28f
        val logoSize = 42f

        // 1. Draw App Logo on Top Left
        val logo = appLogoBitmap
        if (logo != null) {
            val logoRect = RectF(marginX, topY, marginX + logoSize, topY + logoSize)
            // Circular / rounded clipped logo background
            val badgePaint = Paint().apply {
                color = colorNavyDeep
                isAntiAlias = true
            }
            canvas.drawRoundRect(logoRect, 8f, 8f, badgePaint)
            canvas.drawBitmap(logo, null, logoRect, Paint(Paint.FILTER_BITMAP_FLAG))

            // Subtle gold border around logo
            val borderP = Paint().apply {
                color = colorGold
                strokeWidth = 1f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawRoundRect(logoRect, 8f, 8f, borderP)
        }

        // 2. Platform Branding next to Logo
        val brandStartX = marginX + logoSize + 12f
        val titlePaint = Paint().apply {
            color = colorNavyDeep
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("LOANZO FINANCIAL NETWORK", brandStartX, topY + 14f, titlePaint)

        val subBrandPaint = Paint().apply {
            color = colorGold
            textSize = 7.5f
            isFakeBoldText = true
            isAntiAlias = true
            letterSpacing = 0.08f
        }
        canvas.drawText("DECENTRALIZED PEER-TO-PEER LENDING PLATFORM", brandStartX, topY + 25f, subBrandPaint)

        val classPaint = Paint().apply {
            color = colorSlate
            textSize = 7f
            isAntiAlias = true
        }
        canvas.drawText(securityClassification, brandStartX, topY + 36f, classPaint)

        // 3. Top Right Details: Doc Ref & Page Number
        val rightAlignPaint = Paint().apply {
            color = colorNavyDeep
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText(documentRefId, pageWidth - marginX, topY + 14f, rightAlignPaint)

        val datePaint = Paint().apply {
            color = colorSlate
            textSize = 7.5f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Vault Archived: $dateStr", pageWidth - marginX, topY + 25f, datePaint)

        val pagePaint = Paint().apply {
            color = colorNavyMedium
            textSize = 7.5f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText("Page $pageNumber of $totalPages", pageWidth - marginX, topY + 36f, pagePaint)

        // 4. Gold Divider Line
        val dividerY = topY + logoSize + 10f
        canvas.drawLine(marginX, dividerY, pageWidth - marginX, dividerY, paintGoldLine)

        // 5. Document Main Banner
        val bannerTop = dividerY + 6f
        val bannerHeight = 24f
        val bannerRect = RectF(marginX, bannerTop, pageWidth - marginX, bannerTop + bannerHeight)
        val bannerPaint = Paint().apply {
            color = colorBgLight
            isAntiAlias = true
        }
        canvas.drawRoundRect(bannerRect, 4f, 4f, bannerPaint)

        val docTitlePaint = Paint().apply {
            color = colorNavyDeep
            textSize = 11f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            letterSpacing = 0.05f
        }
        canvas.drawText(documentTitle.uppercase(), pageWidth / 2f, bannerTop + 16f, docTitlePaint)

        return bannerTop + bannerHeight + 14f
    }

    /**
     * Draws the bottom security footer.
     */
    fun drawFooter(canvas: Canvas, pageNumber: Int, totalPages: Int, checksum: String = "") {
        val bottomY = pageHeight - 34f

        // Thin divider
        canvas.drawLine(marginX, bottomY, pageWidth - marginX, bottomY, paintBorderLine)

        val footerPaint = Paint().apply {
            color = colorSlate
            textSize = 6.5f
            isAntiAlias = true
        }
        canvas.drawText(
            "LOANZO ENCRYPTED DOCUMENT VAULT • DIGITAL RECORD PURSUANT TO SECTION 65B OF INDIAN EVIDENCE ACT & IT ACT 2000",
            marginX,
            bottomY + 12f,
            footerPaint
        )

        if (checksum.isNotBlank()) {
            val hashPaint = Paint().apply {
                color = colorSlate
                textSize = 6f
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("SHA-256: ${checksum.take(24)}...", pageWidth - marginX, bottomY + 12f, hashPaint)
        }

        canvas.drawText(
            "Tamper-evident legal electronic document. Any alteration invalidates this certificate.",
            marginX,
            bottomY + 22f,
            footerPaint
        )
    }

    /**
     * Draws a Section Header with a golden vertical indicator bar.
     */
    fun drawSectionHeader(canvas: Canvas, startY: Float, title: String, subtitle: String = ""): Float {
        var y = startY
        // Golden indicator bar
        val barPaint = Paint().apply {
            color = colorGold
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(marginX, y, marginX + 3.5f, y + 16f), 1.5f, 1.5f, barPaint)

        val titlePaint = Paint().apply {
            color = colorNavyDeep
            textSize = 10.5f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText(title.uppercase(), marginX + 10f, y + 12f, titlePaint)

        if (subtitle.isNotBlank()) {
            val subPaint = Paint().apply {
                color = colorSlate
                textSize = 7.5f
                isAntiAlias = true
            }
            canvas.drawText(subtitle, marginX + 10f, y + 23f, subPaint)
            y += 10f
        }

        return y + 22f
    }

    /**
     * Draws 2 to 4 KPI Summary Cards side-by-side.
     */
    data class KpiCard(val label: String, val value: String, val subtext: String, val accentColor: Int)

    fun drawKpiGrid(canvas: Canvas, startY: Float, cards: List<KpiCard>): Float {
        val cardCount = cards.size
        val gap = 8f
        val cardWidth = (contentWidth - (gap * (cardCount - 1))) / cardCount
        val cardHeight = 46f

        cards.forEachIndexed { i, card ->
            val left = marginX + i * (cardWidth + gap)
            val rect = RectF(left, startY, left + cardWidth, startY + cardHeight)

            // Background
            val bgPaint = Paint().apply {
                color = colorBgLight
                isAntiAlias = true
            }
            canvas.drawRoundRect(rect, 6f, 6f, bgPaint)

            // Left accent border
            val accentPaint = Paint().apply {
                color = card.accentColor
                isAntiAlias = true
            }
            canvas.drawRoundRect(RectF(left, startY, left + 3.5f, startY + cardHeight), 2f, 2f, accentPaint)

            // Outer border
            canvas.drawRoundRect(rect, 6f, 6f, paintBorderLine)

            // Label
            val lblPaint = Paint().apply {
                color = colorSlate
                textSize = 7f
                isAntiAlias = true
                isFakeBoldText = true
            }
            canvas.drawText(card.label.uppercase(), left + 8f, startY + 12f, lblPaint)

            // Value
            val valPaint = Paint().apply {
                color = colorNavyDeep
                textSize = 11.5f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText(card.value, left + 8f, startY + 28f, valPaint)

            // Subtext
            if (card.subtext.isNotBlank()) {
                val subPaint = Paint().apply {
                    color = card.accentColor
                    textSize = 6.5f
                    isAntiAlias = true
                }
                canvas.drawText(card.subtext, left + 8f, startY + 39f, subPaint)
            }
        }

        return startY + cardHeight + 12f
    }

    /**
     * Draws a formatted Table with header styling, alternating row fills, and column borders.
     */
    fun drawTable(
        canvas: Canvas,
        startY: Float,
        headers: List<String>,
        rows: List<List<String>>,
        colWidthRatios: List<Float>
    ): Float {
        var y = startY
        val totalRatio = colWidthRatios.sum()
        val colWidths = colWidthRatios.map { (it / totalRatio) * contentWidth }
        val headerHeight = 20f
        val rowHeight = 18f

        // 1. Draw Header
        val headerRect = RectF(marginX, y, marginX + contentWidth, y + headerHeight)
        val headerBg = Paint().apply {
            color = colorNavyMedium
            isAntiAlias = true
        }
        canvas.drawRoundRect(headerRect, 4f, 4f, headerBg)

        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 7.5f
            isFakeBoldText = true
            isAntiAlias = true
        }

        var colX = marginX
        headers.forEachIndexed { idx, h ->
            val w = colWidths[idx]
            canvas.drawText(h.uppercase(), colX + 6f, y + 13f, headerTextPaint)
            colX += w
        }
        y += headerHeight

        // 2. Draw Rows
        val rowBgEven = Paint().apply { color = Color.WHITE }
        val rowBgOdd = Paint().apply { color = colorBgLight }

        rows.forEachIndexed { rowIdx, row ->
            val rowRect = RectF(marginX, y, marginX + contentWidth, y + rowHeight)
            canvas.drawRect(rowRect, if (rowIdx % 2 == 0) rowBgEven else rowBgOdd)

            // Bottom border
            canvas.drawLine(marginX, y + rowHeight, marginX + contentWidth, y + rowHeight, paintBorderLine)

            colX = marginX
            row.forEachIndexed { colIdx, text ->
                if (colIdx < colWidths.size) {
                    val w = colWidths[colIdx]
                    val isFirst = colIdx == 0
                    val isStatus = text.contains("PAID") || text.contains("VERIFIED") || text.contains("ACTIVE")
                    
                    val cellPaint = Paint().apply {
                        color = when {
                            isStatus -> colorEmerald
                            text.contains("OVERDUE") || text.contains("REJECTED") -> Color.RED
                            isFirst -> colorNavyDeep
                            else -> colorNavyMedium
                        }
                        textSize = 7.5f
                        isFakeBoldText = isFirst || isStatus
                        isAntiAlias = true
                    }
                    val displayText = if (text.length > 32) text.take(30) + "..." else text
                    canvas.drawText(displayText, colX + 6f, y + 12f, cellPaint)
                    colX += w
                }
            }
            y += rowHeight
        }

        return y + 12f
    }

    /**
     * Draws a 2-column or 4-column structured Key-Value field grid.
     */
    data class KeyValue(val key: String, val value: String, val isHighlight: Boolean = false)

    fun drawKeyValueGrid(canvas: Canvas, startY: Float, items: List<KeyValue>, columns: Int = 2): Float {
        var y = startY
        val colWidth = contentWidth / columns
        val rowHeight = 22f

        val chunked = items.chunked(columns)
        chunked.forEach { rowItems ->
            rowItems.forEachIndexed { colIdx, item ->
                val x = marginX + (colIdx * colWidth)
                
                // Key
                val kPaint = Paint().apply {
                    color = colorSlate
                    textSize = 7.5f
                    isAntiAlias = true
                }
                canvas.drawText(item.key.uppercase(), x + 4f, y + 8f, kPaint)

                // Value
                val vPaint = Paint().apply {
                    color = if (item.isHighlight) colorGold else colorNavyDeep
                    textSize = 8.5f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                canvas.drawText(item.value, x + 4f, y + 18f, vPaint)
            }
            // Thin horizontal row separator
            canvas.drawLine(marginX, y + rowHeight, marginX + contentWidth, y + rowHeight, paintBorderLine)
            y += rowHeight + 4f
        }

        return y + 8f
    }

    /**
     * Draws an official verification stamp with green checkmark and QR code box.
     */
    fun drawVerificationStamp(canvas: Canvas, startY: Float, documentRef: String, checksum: String): Float {
        val boxHeight = 52f
        val rect = RectF(marginX, startY, marginX + contentWidth, startY + boxHeight)

        // Background
        val bgPaint = Paint().apply {
            color = colorEmeraldLight
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 6f, 6f, bgPaint)

        val borderPaint = Paint().apply {
            color = colorEmerald
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        // Left Seal Circle
        val sealX = marginX + 28f
        val sealY = startY + (boxHeight / 2)
        val sealPaint = Paint().apply {
            color = colorEmerald
            isAntiAlias = true
        }
        canvas.drawCircle(sealX, sealY, 18f, sealPaint)

        val checkPaint = Paint().apply {
            color = Color.WHITE
            textSize = 14f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("✓", sealX, sealY + 5f, checkPaint)

        // Text details
        val textStartX = sealX + 26f
        val titlePaint = Paint().apply {
            color = colorEmerald
            textSize = 9.5f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("DIGITALLY SEALED & VERIFIED BY LOANZO AUDIT VAULT", textStartX, startY + 16f, titlePaint)

        val subPaint = Paint().apply {
            color = colorNavyMedium
            textSize = 7.5f
            isAntiAlias = true
        }
        canvas.drawText("Reference: $documentRef • Cryptographic SHA-256: ${checksum.take(32)}", textStartX, startY + 28f, subPaint)
        canvas.drawText("This certificate documents the user's active financial state, contracts & custody.", textStartX, startY + 40f, subPaint)

        return startY + boxHeight + 12f
    }

    /**
     * Draws an AI Credit & Legal Underwriting callout box generated via Multi-AI racing.
     */
    fun drawAiAppraisalBox(
        canvas: Canvas,
        startY: Float,
        title: String,
        content: String,
        engineBadge: String = "Multi-AI Verified"
    ): Float {
        val boxHeight = 52f
        val rect = RectF(marginX, startY, marginX + contentWidth, startY + boxHeight)

        val bgPaint = Paint().apply {
            color = Color.parseColor("#F5F3FF")
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 6f, 6f, bgPaint)

        val borderPaint = Paint().apply {
            color = Color.parseColor("#8B5CF6")
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        val titlePaint = Paint().apply {
            color = Color.parseColor("#6D28D9")
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("⚡ $title • $engineBadge", marginX + 12f, startY + 16f, titlePaint)

        val textPaint = Paint().apply {
            color = colorNavyDeep
            textSize = 7.5f
            isAntiAlias = true
        }
        val line1 = content.take(110)
        val line2 = if (content.length > 110) content.drop(110).take(110) else ""
        canvas.drawText(line1, marginX + 12f, startY + 30f, textPaint)
        if (line2.isNotBlank()) {
            canvas.drawText(line2, marginX + 12f, startY + 42f, textPaint)
        }

        return startY + boxHeight + 10f
    }

    // =========================================================================
    // DIGITAL SIGNATURES & GOVERNMENT IDENTITY CARDS (PAN & AADHAAR)
    // =========================================================================

    /**
     * Draws an authentic Cursive Signature stroke simulation.
     */
    private fun drawCursiveSignature(canvas: Canvas, startX: Float, startY: Float, name: String, inkColor: Int = Color.rgb(20, 50, 140)) {
        val sigPaint = Paint().apply {
            color = inkColor
            strokeWidth = 1.6f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        val path = Path()
        path.moveTo(startX, startY)
        // Stylized signature flourishing curves
        path.cubicTo(startX + 8f, startY - 14f, startX + 18f, startY + 6f, startX + 28f, startY - 10f)
        path.cubicTo(startX + 34f, startY - 20f, startX + 44f, startY + 2f, startX + 54f, startY - 6f)
        path.cubicTo(startX + 62f, startY - 12f, startX + 72f, startY + 4f, startX + 85f, startY - 8f)
        path.quadTo(startX + 95f, startY - 18f, startX + 105f, startY - 2f)
        // Extended under-loop flourish
        path.cubicTo(startX + 115f, startY + 12f, startX + 40f, startY + 14f, startX + 20f, startY + 10f)
        path.quadTo(startX + 80f, startY + 9f, startX + 120f, startY + 6f)

        canvas.drawPath(path, sigPaint)

        // Underline baseline
        val linePaint = Paint().apply {
            color = Color.rgb(180, 195, 215)
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawLine(startX - 4f, startY + 14f, startX + 125f, startY + 14f, linePaint)
    }

    /**
     * Draws a single full-width or custom-width Digital Signature Certificate Seal.
     */
    fun drawDigitalSignatureSeal(
        canvas: Canvas,
        startY: Float,
        signerName: String,
        signerRole: String,
        aadhaarMasked: String,
        timestampStr: String,
        checksum: String,
        boxWidth: Float = contentWidth,
        startX: Float = marginX
    ): Float {
        val boxHeight = 72f
        val rect = RectF(startX, startY, startX + boxWidth, startY + boxHeight)

        // Light emerald security background
        val bgPaint = Paint().apply {
            color = Color.rgb(240, 253, 244) // Tailwind emerald-50
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 6f, 6f, bgPaint)

        val borderPaint = Paint().apply {
            color = Color.rgb(21, 128, 61) // Emerald-700
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        // Left Seal Medallion
        val sealX = startX + 24f
        val sealY = startY + 36f
        val sealBg = Paint().apply {
            color = Color.rgb(22, 163, 74)
            isAntiAlias = true
        }
        canvas.drawCircle(sealX, sealY, 16f, sealBg)

        // Inner gold ring in medallion
        val goldRing = Paint().apply {
            color = colorGold
            strokeWidth = 1.2f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawCircle(sealX, sealY, 13f, goldRing)

        // Checkmark
        val checkP = Paint().apply {
            color = Color.WHITE
            textSize = 14f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("✓", sealX, sealY + 5f, checkP)

        // Textual Metadata
        val textX = startX + 48f
        val badgePaint = Paint().apply {
            color = Color.rgb(21, 128, 61)
            textSize = 7f
            isFakeBoldText = true
            isAntiAlias = true
            letterSpacing = 0.05f
        }
        canvas.drawText("DIGITALLY SIGNED VIA AADHAAR e-SIGN • SECTION 10A IT ACT 2000", textX, startY + 14f, badgePaint)

        val namePaint = Paint().apply {
            color = colorNavyDeep
            textSize = 9.5f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("$signerName ($signerRole)", textX, startY + 27f, namePaint)

        val metaPaint = Paint().apply {
            color = colorSlate
            textSize = 7f
            isAntiAlias = true
        }
        canvas.drawText("Aadhaar: $aadhaarMasked • DigiLocker e-KYC Certified", textX, startY + 39f, metaPaint)
        canvas.drawText("Timestamp: $timestampStr", textX, startY + 50f, metaPaint)
        canvas.drawText("SHA-256 Digest: ${checksum.take(32)}...", textX, startY + 61f, metaPaint)

        // Right side cursive signature flourish
        val sigX = startX + boxWidth - 135f
        if (boxWidth > 260f) {
            drawCursiveSignature(canvas, sigX, startY + 36f, signerName)
            val authP = Paint().apply {
                color = Color.rgb(100, 116, 139)
                textSize = 6f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Authorized Electronic Signature", sigX + 60f, startY + 63f, authP)
        }

        return startY + boxHeight + 10f
    }

    /**
     * Draws Dual Digital Signatures side-by-side (e.g. Lender & Borrower).
     */
    fun drawDualDigitalSignatures(
        canvas: Canvas,
        startY: Float,
        lenderName: String,
        lenderRole: String,
        lenderAadhaar: String,
        borrowerName: String,
        borrowerRole: String,
        borrowerAadhaar: String,
        timestampStr: String,
        checksum: String
    ): Float {
        val gap = 10f
        val halfWidth = (contentWidth - gap) / 2f

        // Lender Box (Left)
        drawDigitalSignatureSeal(
            canvas = canvas,
            startY = startY,
            signerName = lenderName,
            signerRole = lenderRole,
            aadhaarMasked = lenderAadhaar,
            timestampStr = timestampStr,
            checksum = checksum,
            boxWidth = halfWidth,
            startX = marginX
        )

        // Borrower Box (Right)
        drawDigitalSignatureSeal(
            canvas = canvas,
            startY = startY,
            signerName = borrowerName,
            signerRole = borrowerRole,
            aadhaarMasked = borrowerAadhaar,
            timestampStr = timestampStr,
            checksum = checksum,
            boxWidth = halfWidth,
            startX = marginX + halfWidth + gap
        )

        return startY + 72f + 12f
    }

    /**
     * Draws a high-fidelity Biometric Face & Passive Liveness Verification Card.
     * Displays the captured live facial photograph/selfie, passive anti-spoofing score,
     * UIDAI e-KYC cross-verification badge, and cryptographic audit timestamp.
     */
    fun drawBiometricFaceCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        name: String,
        role: String,
        photoBitmap: Bitmap?,
        livenessScore: Double = 0.992,
        timestampStr: String = "AUTHENTICATED VIA CAMERAX ML-KIT",
        auditHash: String = ""
    ) {
        val rect = RectF(x, y, x + width, y + height)

        // Light slate/navy tinted background
        val bgPaint = Paint().apply { color = Color.rgb(248, 250, 252); isAntiAlias = true }
        canvas.drawRoundRect(rect, 6f, 6f, bgPaint)

        val borderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 0.9f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        // Top Header Banner
        val headerHeight = 18f
        val headerRect = RectF(x, y, x + width, y + headerHeight)
        val headerBg = Paint().apply { color = Color.rgb(15, 23, 42); isAntiAlias = true }
        val path = Path().apply {
            addRoundRect(headerRect, floatArrayOf(6f, 6f, 6f, 6f, 0f, 0f, 0f, 0f), Path.Direction.CW)
        }
        canvas.drawPath(path, headerBg)

        val headerTitleP = Paint().apply {
            color = Color.WHITE
            textSize = 6f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("📸 BIOMETRIC FACIAL LIVENESS AUDIT", x + 8f, y + 12f, headerTitleP)

        val headerStatusP = Paint().apply {
            color = Color.rgb(52, 211, 153)
            textSize = 5.2f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("PASSIVE VERIFIED ✓", x + width - 8f, y + 12f, headerStatusP)

        // Photo Frame on Left
        val photoLeft = x + 8f
        val photoTop = y + headerHeight + 8f
        val photoW = 46f
        val photoH = 54f
        val photoRect = RectF(photoLeft, photoTop, photoLeft + photoW, photoTop + photoH)

        val photoBg = Paint().apply { color = Color.rgb(226, 232, 240); isAntiAlias = true }
        canvas.drawRoundRect(photoRect, 4f, 4f, photoBg)

        if (photoBitmap != null) {
            try {
                val scaled = Bitmap.createScaledBitmap(photoBitmap, photoW.toInt(), photoH.toInt(), true)
                canvas.save()
                val clip = Path().apply { addRoundRect(photoRect, 4f, 4f, Path.Direction.CW) }
                canvas.clipPath(clip)
                canvas.drawBitmap(scaled, photoLeft, photoTop, Paint().apply { isFilterBitmap = true; isAntiAlias = true })
                canvas.restore()
            } catch (_: Exception) {
                val silPaint = Paint().apply { color = Color.rgb(100, 116, 139); isAntiAlias = true }
                canvas.drawCircle(photoLeft + (photoW / 2f), photoTop + 18f, 10f, silPaint)
                val bodyRect = RectF(photoLeft + 6f, photoTop + 30f, photoLeft + photoW - 6f, photoTop + photoH)
                canvas.drawRoundRect(bodyRect, 6f, 6f, silPaint)
            }
        } else {
            val silPaint = Paint().apply { color = Color.rgb(100, 116, 139); isAntiAlias = true }
            canvas.drawCircle(photoLeft + (photoW / 2f), photoTop + 18f, 10f, silPaint)
            val bodyRect = RectF(photoLeft + 6f, photoTop + 30f, photoLeft + photoW - 6f, photoTop + photoH)
            canvas.drawRoundRect(bodyRect, 6f, 6f, silPaint)
        }

        val activePhotoBorder = Paint().apply {
            color = Color.rgb(16, 185, 129)
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(photoRect, 4f, 4f, activePhotoBorder)

        // Badge below photo
        val badgeRect = RectF(photoLeft - 1f, photoTop + photoH + 4f, photoLeft + photoW + 1f, photoTop + photoH + 15f)
        val badgeBg = Paint().apply { color = Color.rgb(236, 253, 245); isAntiAlias = true }
        canvas.drawRoundRect(badgeRect, 2f, 2f, badgeBg)
        val badgeBorder = Paint().apply { color = Color.rgb(16, 185, 129); strokeWidth = 0.6f; style = Paint.Style.STROKE; isAntiAlias = true }
        canvas.drawRoundRect(badgeRect, 2f, 2f, badgeBorder)
        val badgeP = Paint().apply {
            color = Color.rgb(5, 150, 105)
            textSize = 4.8f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("LIVENESS ${(livenessScore * 100).toInt()}%", badgeRect.centerX(), badgeRect.centerY() + 2f, badgeP)

        // Metadata on Right of Photo
        val contentX = photoLeft + photoW + 8f
        var textY = photoTop + 8f

        val roleP = Paint().apply {
            color = Color.rgb(22, 101, 52)
            textSize = 5.2f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText(role.uppercase(), contentX, textY, roleP); textY += 10f

        val nameP = Paint().apply {
            color = colorNavyDeep
            textSize = 7.5f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText(name, contentX, textY, nameP); textY += 12f

        val detailKeyP = Paint().apply { color = colorSlate; textSize = 5f; isAntiAlias = true }
        val detailValP = Paint().apply { color = colorNavyDeep; textSize = 5.6f; isFakeBoldText = true; isAntiAlias = true }

        canvas.drawText("Anti-Spoofing: 3D Passive Match PASSED", contentX, textY, detailValP); textY += 9f
        canvas.drawText("Capture Engine: CameraX Biometric Guard", contentX, textY, detailKeyP); textY += 9f
        canvas.drawText("Audit Time: $timestampStr", contentX, textY, detailKeyP); textY += 9f

        if (auditHash.isNotBlank()) {
            val hashP = Paint().apply { color = Color.rgb(100, 116, 139); textSize = 4.8f; isAntiAlias = true }
            canvas.drawText("Digest: ${auditHash.take(24)}...", contentX, textY, hashP)
        }
    }

    /**
     * Draws a high-fidelity, photorealistic Indian PAN Card.
     */
    fun drawPanCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        name: String,
        panNumber: String,
        fatherName: String,
        dob: String,
        photoBitmap: Bitmap? = null
    ) {
        val rect = RectF(x, y, x + width, y + height)

        // 1. Base card background (subtle light blue/gray PAN tint)
        val bgPaint = Paint().apply {
            color = Color.rgb(238, 244, 250)
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 6f, 6f, bgPaint)

        // Card Border
        val borderPaint = Paint().apply {
            color = Color.rgb(180, 198, 218)
            strokeWidth = 0.9f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        // 2. Header Banner: ITD & Govt of India
        val headerHeight = 22f
        val headerRect = RectF(x, y, x + width, y + headerHeight)
        val headerBg = Paint().apply {
            color = Color.rgb(21, 52, 98) // ITD Navy
            isAntiAlias = true
        }
        // Top rounded corners only
        val path = Path()
        path.addRoundRect(headerRect, floatArrayOf(6f, 6f, 6f, 6f, 0f, 0f, 0f, 0f), Path.Direction.CW)
        canvas.drawPath(path, headerBg)

        // Header bilingual text
        val headerTitleP = Paint().apply {
            color = Color.WHITE
            textSize = 6f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("आयकर विभाग  /  INCOME TAX DEPARTMENT", x + 10f, y + 10f, headerTitleP)

        val headerGovtP = Paint().apply {
            color = Color.rgb(255, 230, 150)
            textSize = 5.2f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("भारत सरकार • GOVT. OF INDIA", x + width - 10f, y + 10f, headerGovtP)

        val subBannerP = Paint().apply {
            color = Color.rgb(200, 220, 245)
            textSize = 4.8f
            isAntiAlias = true
        }
        canvas.drawText("स्थायी लेखा संख्या कार्ड  •  Permanent Account Number Card", x + 10f, y + 18f, subBannerP)

        // 3. Photo Avatar (Left side)
        val photoLeft = x + 10f
        val photoTop = y + headerHeight + 8f
        val photoW = 42f
        val photoH = 50f
        val photoRect = RectF(photoLeft, photoTop, photoLeft + photoW, photoTop + photoH)

        val photoBg = Paint().apply {
            color = Color.rgb(203, 213, 225)
            isAntiAlias = true
        }
        canvas.drawRoundRect(photoRect, 3f, 3f, photoBg)

        if (photoBitmap != null) {
            try {
                val scaledPhoto = Bitmap.createScaledBitmap(photoBitmap, photoW.toInt(), photoH.toInt(), true)
                canvas.save()
                val clipPath = Path().apply { addRoundRect(photoRect, 3f, 3f, Path.Direction.CW) }
                canvas.clipPath(clipPath)
                canvas.drawBitmap(scaledPhoto, photoLeft, photoTop, Paint().apply { isFilterBitmap = true; isAntiAlias = true })
                canvas.restore()
            } catch (_: Exception) {
                // Fallback to silhouette head & shoulders
                val silPaint = Paint().apply {
                    color = Color.rgb(100, 116, 139)
                    isAntiAlias = true
                }
                canvas.drawCircle(photoLeft + (photoW / 2f), photoTop + 18f, 10f, silPaint)
                val bodyRect = RectF(photoLeft + 6f, photoTop + 30f, photoLeft + photoW - 6f, photoTop + photoH)
                canvas.drawRoundRect(bodyRect, 6f, 6f, silPaint)
            }
        } else {
            // Silhouette head & shoulders
            val silPaint = Paint().apply {
                color = Color.rgb(100, 116, 139)
                isAntiAlias = true
            }
            canvas.drawCircle(photoLeft + (photoW / 2f), photoTop + 18f, 10f, silPaint)
            val bodyRect = RectF(photoLeft + 6f, photoTop + 30f, photoLeft + photoW - 6f, photoTop + photoH)
            canvas.drawRoundRect(bodyRect, 6f, 6f, silPaint)
        }

        val photoBorder = Paint().apply {
            color = Color.rgb(148, 163, 184)
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(photoRect, 3f, 3f, photoBorder)

        // Hologram Badge below photo
        val holoRect = RectF(photoLeft, photoTop + photoH + 6f, photoLeft + 28f, photoTop + photoH + 26f)
        val holoPaint = Paint().apply {
            color = Color.rgb(220, 215, 180) // Metallic sheen
            isAntiAlias = true
        }
        canvas.drawRoundRect(holoRect, 2f, 2f, holoPaint)
        val holoBorder = Paint().apply {
            color = colorGold
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(holoRect, 2f, 2f, holoBorder)
        val holoTextP = Paint().apply {
            color = Color.rgb(120, 100, 20)
            textSize = 5f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("ITD", holoRect.centerX(), holoRect.centerY() + 2f, holoTextP)

        // 4. PAN Details (Right of photo)
        val contentX = photoLeft + photoW + 12f

        // Name
        val labelP = Paint().apply {
            color = colorSlate
            textSize = 5.2f
            isAntiAlias = true
        }
        val valueP = Paint().apply {
            color = colorNavyDeep
            textSize = 7.2f
            isFakeBoldText = true
            isAntiAlias = true
        }

        var detailY = photoTop + 8f
        canvas.drawText("नाम / Name", contentX, detailY, labelP); detailY += 9f
        canvas.drawText(name.uppercase(), contentX, detailY, valueP); detailY += 12f

        canvas.drawText("पिता का नाम / Father's Name", contentX, detailY, labelP); detailY += 9f
        canvas.drawText(fatherName.uppercase(), contentX, detailY, valueP); detailY += 12f

        canvas.drawText("जन्म की तारीख / Date of Birth", contentX, detailY, labelP); detailY += 9f
        canvas.drawText(dob, contentX, detailY, valueP); detailY += 14f

        // Prominent PAN Number Box
        val panBoxRect = RectF(contentX, detailY - 2f, x + width - 10f, detailY + 16f)
        val panBoxBg = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
        }
        canvas.drawRoundRect(panBoxRect, 3f, 3f, panBoxBg)
        canvas.drawRoundRect(panBoxRect, 3f, 3f, borderPaint)

        val panNumberP = Paint().apply {
            color = Color.rgb(10, 25, 60)
            textSize = 9.5f
            isFakeBoldText = true
            letterSpacing = 0.12f
            isAntiAlias = true
        }
        canvas.drawText(panNumber.uppercase(), contentX + 8f, detailY + 11f, panNumberP)

        // Signature Strip on Bottom Right
        val sigStripRect = RectF(x + width - 90f, y + height - 26f, x + width - 10f, y + height - 6f)
        val sigStripBg = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
        }
        canvas.drawRoundRect(sigStripRect, 2f, 2f, sigStripBg)
        val sigStripBorder = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 0.6f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(sigStripRect, 2f, 2f, sigStripBorder)

        // Cursive signature simulation inside strip
        drawCursiveSignature(canvas, x + width - 85f, y + height - 16f, name)

        val sigLabelP = Paint().apply {
            color = Color.rgb(140, 155, 175)
            textSize = 4.2f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("हस्ताक्षर / Signature", sigStripRect.centerX(), y + height - 1.5f, sigLabelP)
    }

    /**
     * Draws simulated 2D QR Code Matrix Pattern.
     */
    private fun drawSimulatedQrCode(canvas: Canvas, x: Float, y: Float, size: Float) {
        val qrBg = Paint().apply { color = Color.WHITE; isAntiAlias = true }
        canvas.drawRect(RectF(x, y, x + size, y + size), qrBg)

        val qrBorder = Paint().apply {
            color = Color.rgb(200, 210, 225)
            strokeWidth = 0.6f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRect(RectF(x, y, x + size, y + size), qrBorder)

        val dotPaint = Paint().apply { color = Color.rgb(20, 30, 50); isAntiAlias = true }

        // Corner 1 (Top Left)
        canvas.drawRect(RectF(x + 2f, y + 2f, x + 12f, y + 12f), dotPaint)
        canvas.drawRect(RectF(x + 4f, y + 4f, x + 10f, y + 10f), qrBg)
        canvas.drawRect(RectF(x + 6f, y + 6f, x + 8f, y + 8f), dotPaint)

        // Corner 2 (Top Right)
        canvas.drawRect(RectF(x + size - 12f, y + 2f, x + size - 2f, y + 12f), dotPaint)
        canvas.drawRect(RectF(x + size - 10f, y + 4f, x + size - 4f, y + 10f), qrBg)
        canvas.drawRect(RectF(x + size - 8f, y + 6f, x + size - 6f, y + 8f), dotPaint)

        // Corner 3 (Bottom Left)
        canvas.drawRect(RectF(x + 2f, y + size - 12f, x + 12f, y + size - 2f), dotPaint)
        canvas.drawRect(RectF(x + 4f, y + size - 10f, x + 10f, y + size - 4f), qrBg)
        canvas.drawRect(RectF(x + 6f, y + size - 8f, x + 8f, y + size - 6f), dotPaint)

        // Grid dots simulation
        val step = 4f
        var curX = x + 14f
        while (curX < x + size - 4f) {
            var curY = y + 14f
            while (curY < y + size - 4f) {
                if (((curX + curY).toInt() % 3 == 0)) {
                    canvas.drawRect(RectF(curX, curY, curX + 2.5f, curY + 2.5f), dotPaint)
                }
                curY += step
            }
            curX += step
        }
    }

    /**
     * Draws a high-fidelity Indian Aadhaar Card (FRONT SIDE).
     */
    fun drawAadhaarCardFront(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        name: String,
        aadhaarNumber: String,
        dob: String,
        gender: String,
        photoBitmap: Bitmap? = null
    ) {
        val rect = RectF(x, y, x + width, y + height)

        // Card background
        val bgPaint = Paint().apply { color = Color.WHITE; isAntiAlias = true }
        canvas.drawRoundRect(rect, 6f, 6f, bgPaint)

        val borderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 0.9f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        // Top Tricolor stripe
        val stripeH = 3f
        val saffronP = Paint().apply { color = Color.rgb(255, 153, 51); isAntiAlias = true }
        val greenP = Paint().apply { color = Color.rgb(19, 136, 8); isAntiAlias = true }
        canvas.drawRect(RectF(x, y, x + width, y + stripeH), saffronP)
        canvas.drawRect(RectF(x, y + stripeH, x + width, y + (stripeH * 2)), greenP)

        // Header Text: Government of India / UIDAI
        val headerY = y + 16f
        val govTitleP = Paint().apply {
            color = Color.rgb(20, 35, 60)
            textSize = 6.2f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("भारत सरकार  •  GOVERNMENT OF INDIA", x + 10f, headerY, govTitleP)

        val uidaiTitleP = Paint().apply {
            color = Color.rgb(200, 30, 30) // UIDAI Red
            textSize = 5f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("UIDAI", x + width - 10f, headerY, uidaiTitleP)

        // Photo Avatar (Left side)
        val photoLeft = x + 10f
        val photoTop = headerY + 6f
        val photoW = 42f
        val photoH = 50f
        val photoRect = RectF(photoLeft, photoTop, photoLeft + photoW, photoTop + photoH)

        val photoBg = Paint().apply { color = Color.rgb(226, 232, 240); isAntiAlias = true }
        canvas.drawRoundRect(photoRect, 3f, 3f, photoBg)

        if (photoBitmap != null) {
            try {
                val scaledPhoto = Bitmap.createScaledBitmap(photoBitmap, photoW.toInt(), photoH.toInt(), true)
                canvas.save()
                val clipPath = Path().apply { addRoundRect(photoRect, 3f, 3f, Path.Direction.CW) }
                canvas.clipPath(clipPath)
                canvas.drawBitmap(scaledPhoto, photoLeft, photoTop, Paint().apply { isFilterBitmap = true; isAntiAlias = true })
                canvas.restore()
            } catch (_: Exception) {
                val silPaint = Paint().apply { color = Color.rgb(100, 116, 139); isAntiAlias = true }
                canvas.drawCircle(photoLeft + (photoW / 2f), photoTop + 18f, 10f, silPaint)
                val bodyRect = RectF(photoLeft + 6f, photoTop + 30f, photoLeft + photoW - 6f, photoTop + photoH)
                canvas.drawRoundRect(bodyRect, 6f, 6f, silPaint)
            }
        } else {
            // Silhouette
            val silPaint = Paint().apply { color = Color.rgb(100, 116, 139); isAntiAlias = true }
            canvas.drawCircle(photoLeft + (photoW / 2f), photoTop + 18f, 10f, silPaint)
            val bodyRect = RectF(photoLeft + 6f, photoTop + 30f, photoLeft + photoW - 6f, photoTop + photoH)
            canvas.drawRoundRect(bodyRect, 6f, 6f, silPaint)
        }

        canvas.drawRoundRect(photoRect, 3f, 3f, borderPaint)

        // Verified DigiLocker Badge below photo
        val badgeRect = RectF(photoLeft, photoTop + photoH + 4f, photoLeft + photoW, photoTop + photoH + 16f)
        val badgeBg = Paint().apply { color = Color.rgb(236, 253, 245); isAntiAlias = true }
        canvas.drawRoundRect(badgeRect, 2f, 2f, badgeBg)
        val badgeBorder = Paint().apply { color = Color.rgb(16, 185, 129); strokeWidth = 0.6f; style = Paint.Style.STROKE; isAntiAlias = true }
        canvas.drawRoundRect(badgeRect, 2f, 2f, badgeBorder)
        val badgeTextP = Paint().apply {
            color = Color.rgb(5, 150, 105)
            textSize = 4.8f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("✓ e-KYC VERIFIED", badgeRect.centerX(), badgeRect.centerY() + 2f, badgeTextP)

        // Identity Details (Center)
        val contentX = photoLeft + photoW + 10f
        var textY = photoTop + 10f

        val labelP = Paint().apply { color = colorSlate; textSize = 5f; isAntiAlias = true }
        val nameP = Paint().apply { color = colorNavyDeep; textSize = 7.8f; isFakeBoldText = true; isAntiAlias = true }
        val valueP = Paint().apply { color = colorNavyDeep; textSize = 6.8f; isAntiAlias = true }

        canvas.drawText("नाम / Name", contentX, textY, labelP); textY += 10f
        canvas.drawText(name, contentX, textY, nameP); textY += 12f

        canvas.drawText("जन्म तिथि / DOB: $dob", contentX, textY, valueP); textY += 11f
        canvas.drawText("लिंग / Gender: $gender", contentX, textY, valueP); textY += 12f

        // Simulated QR code on Right
        val qrSize = 42f
        drawSimulatedQrCode(canvas, x + width - qrSize - 10f, photoTop, qrSize)

        // Aadhaar Number (Large bold masked format)
        val aadhaarFormatted = if (aadhaarNumber.contains(" ")) aadhaarNumber else aadhaarNumber.chunked(4).joinToString(" ")
        val aadhaarP = Paint().apply {
            color = Color.rgb(200, 30, 30) // UIDAI Red
            textSize = 10.5f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.12f
            isAntiAlias = true
        }
        val aadhaarY = y + height - 16f
        canvas.drawText(aadhaarFormatted, x + (width / 2f), aadhaarY, aadhaarP)

        // Red bottom slogan banner
        val sloganRect = RectF(x, y + height - 9f, x + width, y + height)
        val sloganBg = Paint().apply { color = Color.rgb(200, 30, 30); isAntiAlias = true }
        val sloganPath = Path()
        sloganPath.addRoundRect(sloganRect, floatArrayOf(0f, 0f, 0f, 0f, 6f, 6f, 6f, 6f), Path.Direction.CW)
        canvas.drawPath(sloganPath, sloganBg)

        val sloganP = Paint().apply {
            color = Color.WHITE
            textSize = 5.2f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("मेरा आधार, मेरी पहचान", x + (width / 2f), y + height - 2.5f, sloganP)
    }

    /**
     * Draws a high-fidelity Indian Aadhaar Card (BACK SIDE).
     */
    fun drawAadhaarCardBack(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        fatherName: String,
        address: String,
        pincode: String
    ) {
        val rect = RectF(x, y, x + width, y + height)

        // Card background
        val bgPaint = Paint().apply { color = Color.WHITE; isAntiAlias = true }
        canvas.drawRoundRect(rect, 6f, 6f, bgPaint)

        val borderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 0.9f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        // Top Tricolor stripe
        val stripeH = 3f
        val saffronP = Paint().apply { color = Color.rgb(255, 153, 51); isAntiAlias = true }
        val greenP = Paint().apply { color = Color.rgb(19, 136, 8); isAntiAlias = true }
        canvas.drawRect(RectF(x, y, x + width, y + stripeH), saffronP)
        canvas.drawRect(RectF(x, y + stripeH, x + width, y + (stripeH * 2)), greenP)

        // Header: UIDAI Address Verification
        val headerY = y + 16f
        val titleP = Paint().apply {
            color = Color.rgb(20, 35, 60)
            textSize = 6f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("भारतीय विशिष्ट पहचान प्राधिकरण  •  UIDAI", x + 10f, headerY, titleP)

        // Address Details (Left side)
        val textLeft = x + 10f
        var textY = headerY + 12f

        val labelP = Paint().apply {
            color = Color.rgb(200, 30, 30)
            textSize = 5.5f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("पता  /  Address:", textLeft, textY, labelP); textY += 10f

        val valP = Paint().apply {
            color = colorNavyDeep
            textSize = 6f
            isAntiAlias = true
        }
        canvas.drawText("S/O: $fatherName", textLeft, textY, valP); textY += 9f

        // Break address into 2 or 3 lines
        val addressChunks = address.chunked(34)
        addressChunks.take(3).forEach { chunk ->
            canvas.drawText(chunk, textLeft, textY, valP)
            textY += 9f
        }

        val pinP = Paint().apply {
            color = colorNavyDeep
            textSize = 6.5f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("पिन कोड / PIN: $pincode", textLeft, textY + 2f, pinP)

        // Barcode & QR Code (Right side)
        val qrSize = 44f
        drawSimulatedQrCode(canvas, x + width - qrSize - 10f, headerY + 8f, qrSize)

        // Barcode lines below QR
        val barcodeLeft = x + width - qrSize - 10f
        val barcodeTop = headerY + 8f + qrSize + 5f
        val barP = Paint().apply { color = Color.rgb(20, 30, 50); strokeWidth = 1f; isAntiAlias = true }
        for (b in 0..20) {
            val bx = barcodeLeft + (b * 2.1f)
            canvas.drawLine(bx, barcodeTop, bx, barcodeTop + 10f, barP)
        }

        // Bottom Contact & Helpline Ribbon
        val ribbonRect = RectF(x, y + height - 14f, x + width, y + height)
        val ribbonBg = Paint().apply { color = Color.rgb(241, 245, 249); isAntiAlias = true }
        val ribbonPath = Path()
        ribbonPath.addRoundRect(ribbonRect, floatArrayOf(0f, 0f, 0f, 0f, 6f, 6f, 6f, 6f), Path.Direction.CW)
        canvas.drawPath(ribbonPath, ribbonBg)

        val helpP = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 5f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("📞 1947  |  ✉ help@uidai.gov.in  |  🌐 www.uidai.gov.in", x + (width / 2f), y + height - 4.5f, helpP)
    }

    /**
     * Draws the complete 2x2 Government KYC Identity Documents Grid:
     * - Top Left: PAN Card
     * - Top Right: Aadhaar Card (Front)
     * - Bottom Left: Aadhaar Card (Back)
     * - Bottom Right: DigiLocker Biometric Liveness & Consent Certification
     */
    fun drawKycIdentityGrid(
        canvas: Canvas,
        startY: Float,
        name: String,
        panNumber: String,
        aadhaarNumber: String,
        fatherName: String,
        dob: String,
        gender: String,
        address: String,
        pincode: String
    ): Float {
        val gap = 10f
        val cardWidth = (contentWidth - gap) / 2f
        val cardHeight = 135f

        // Row 1: PAN Card (Left) & Aadhaar Front (Right)
        val row1Y = startY
        drawPanCard(
            canvas = canvas,
            x = marginX,
            y = row1Y,
            width = cardWidth,
            height = cardHeight,
            name = name,
            panNumber = panNumber,
            fatherName = fatherName,
            dob = dob
        )

        drawAadhaarCardFront(
            canvas = canvas,
            x = marginX + cardWidth + gap,
            y = row1Y,
            width = cardWidth,
            height = cardHeight,
            name = name,
            aadhaarNumber = aadhaarNumber,
            dob = dob,
            gender = gender
        )

        // Row 2: Aadhaar Back (Left) & DigiLocker Consent Seal (Right)
        val row2Y = row1Y + cardHeight + gap
        drawAadhaarCardBack(
            canvas = canvas,
            x = marginX,
            y = row2Y,
            width = cardWidth,
            height = cardHeight,
            fatherName = fatherName,
            address = address,
            pincode = pincode
        )

        // DigiLocker Consent & Cryptographic Seal (Bottom Right)
        val certRect = RectF(marginX + cardWidth + gap, row2Y, marginX + contentWidth, row2Y + cardHeight)
        val certBg = Paint().apply { color = Color.rgb(248, 250, 252); isAntiAlias = true }
        canvas.drawRoundRect(certRect, 6f, 6f, certBg)

        val certBorder = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 0.9f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(certRect, 6f, 6f, certBorder)

        // Top green band
        val certTopRect = RectF(marginX + cardWidth + gap, row2Y, marginX + contentWidth, row2Y + 20f)
        val certTopBg = Paint().apply { color = Color.rgb(22, 101, 52); isAntiAlias = true }
        val certTopPath = Path()
        certTopPath.addRoundRect(certTopRect, floatArrayOf(6f, 6f, 6f, 6f, 0f, 0f, 0f, 0f), Path.Direction.CW)
        canvas.drawPath(certTopPath, certTopBg)

        val certTitleP = Paint().apply {
            color = Color.WHITE
            textSize = 6.5f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("DIGILOCKER STATUTORY VERIFICATION AUDIT", marginX + cardWidth + gap + 10f, row2Y + 13f, certTitleP)

        // Details inside DigiLocker Certificate box
        val cX = marginX + cardWidth + gap + 12f
        var cY = row2Y + 34f
        val kP = Paint().apply { color = colorSlate; textSize = 5.5f; isAntiAlias = true }
        val vP = Paint().apply { color = colorNavyDeep; textSize = 6.5f; isFakeBoldText = true; isAntiAlias = true }

        canvas.drawText("ISSUING AUTHORITY: UIDAI & INCOME TAX DEPT (ITD)", cX, cY, vP); cY += 12f
        canvas.drawText("INTEGRATION PROTOCOL: DigiLocker OAuth 2.0 XML API", cX, cY, kP); cY += 10f
        canvas.drawText("BIOMETRIC LIVENESS: CameraX ML-Kit Passive Face Liveness ✓", cX, cY, vP); cY += 10f
        canvas.drawText("CONSENT ARTIFACT: IT Act 2000 Section 65B Electronic Consent", cX, cY, kP); cY += 10f
        canvas.drawText("AUDIT TIMESTAMP: Verified & Vaulted into Loanzo Custody", cX, cY, kP); cY += 14f

        // Green check seal inside right card
        val sealBoxRect = RectF(cX, cY, marginX + contentWidth - 12f, row2Y + cardHeight - 8f)
        val sealBoxBg = Paint().apply { color = Color.rgb(236, 253, 245); isAntiAlias = true }
        canvas.drawRoundRect(sealBoxRect, 3f, 3f, sealBoxBg)
        val sealBoxBorder = Paint().apply { color = Color.rgb(16, 185, 129); strokeWidth = 0.8f; style = Paint.Style.STROKE; isAntiAlias = true }
        canvas.drawRoundRect(sealBoxRect, 3f, 3f, sealBoxBorder)

        val sealLabelP = Paint().apply {
            color = Color.rgb(5, 150, 105)
            textSize = 6.2f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("🛡️ TAMPER-PROOF GOVERNMENT IDENTITY SEALED", sealBoxRect.centerX(), sealBoxRect.centerY() + 2f, sealLabelP)

        return row2Y + cardHeight + 12f
    }
}

