package com.loanzo.app.util.pdf

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Generates official, production-realistic PDF documents for Demo Data.
 * Fully styled with Loanzo crest, KPI cards, legal clauses, e-signatures,
 * and tamper-evident SHA-256 verification seals.
 */
object DemoDocumentGenerator {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun calculateChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    suspend fun generateDemoVaultDocuments(context: Context, vaultDir: File): Map<String, Pair<File, String>> = withContext(Dispatchers.IO) {
        vaultDir.mkdirs()
        val engine = LoanzoPdfEngine(context)
        val results = mutableMapOf<String, Pair<File, String>>()

        // 1. Sanction Letter - Education Loan 25K
        try {
            val file1 = File(vaultDir, "Sanction_LZ_EDU_25K.pdf")
            val doc1 = PdfDocument()
            val p1Info = PdfDocument.PageInfo.Builder(engine.pageWidth.toInt(), engine.pageHeight.toInt(), 1).create()
            val p1 = doc1.startPage(p1Info)
            val c1 = p1.canvas

            var y1 = engine.drawHeader(
                canvas = c1,
                pageNumber = 1,
                totalPages = 1,
                documentTitle = "Official Loan Sanction Letter",
                documentRefId = "LZ-SANCTION-EDU-25K-2026",
                securityClassification = "OFFICIAL SANCTION ADVICE • BINDING"
            )

            y1 = engine.drawKpiGrid(
                canvas = c1,
                startY = y1,
                cards = listOf(
                    LoanzoPdfEngine.KpiCard("Sanctioned Amount", "₹25,000", "10.5% p.a. Simple", engine.colorEmerald),
                    LoanzoPdfEngine.KpiCard("Tenure", "12 Months", "Monthly Amortization", engine.colorNavyDeep),
                    LoanzoPdfEngine.KpiCard("Monthly EMI", "₹2,350", "Due on 10th of month", engine.colorGold),
                    LoanzoPdfEngine.KpiCard("Credit Score", "780 AAA", "DigiLocker Verified", engine.colorEmerald)
                )
            )

            y1 = engine.drawSectionHeader(c1, y1 + 10f, "1. Borrower & Facility Specifications")
            y1 = engine.drawKeyValueGrid(
                canvas = c1,
                startY = y1 + 5f,
                items = listOf(
                    LoanzoPdfEngine.KeyValue("Borrower Name", "Arjun Mehta"),
                    LoanzoPdfEngine.KeyValue("Borrower ID", "demo_user_arjun"),
                    LoanzoPdfEngine.KeyValue("Lender Name", "Priya Patel"),
                    LoanzoPdfEngine.KeyValue("Lender ID", "demo_lender_priya"),
                    LoanzoPdfEngine.KeyValue("Loan Category", "HIGHER EDUCATION & UPSKILLING"),
                    LoanzoPdfEngine.KeyValue("Course/Institution", "Advanced FinTech Certification"),
                    LoanzoPdfEngine.KeyValue("Guarantor", "Nirmala Devi (Mother)", isHighlight = true),
                    LoanzoPdfEngine.KeyValue("Disbursement Route", "Direct Bank Transfer (ICICI ESCROW)")
                ),
                columns = 2
            )

            y1 = engine.drawSectionHeader(c1, y1 + 10f, "2. Key Loan Terms & Statutory Conditions")
            y1 = engine.drawTable(
                canvas = c1,
                startY = y1 + 5f,
                headers = listOf("Term / Covenant", "Contractual Specification", "Regulatory Basis"),
                rows = listOf(
                    listOf("Annual Percentage Rate (APR)", "10.50% per annum", "RBI/2026/P2P Guidelines"),
                    listOf("Processing Fee", "₹0 (Zero Processing Fee)", "Direct Peer-to-Peer"),
                    listOf("Prepayment / Foreclosure", "Nil penalty (Permitted anytime)", "Borrower Bill of Rights"),
                    listOf("Overdue / Default Interest", "2.0% per month on overdue EMI", "Loanzo Escrow Rulebook"),
                    listOf("Guarantor Guarantee", "100% Joint & Several Liability", "Indian Contract Act 1872")
                ),
                colWidthRatios = listOf(1.5f, 2.0f, 1.5f)
            )

            y1 = engine.drawVerificationStamp(c1, y1 + 15f, "LZ-SANCTION-EDU-25K-2026", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")

            doc1.finishPage(p1)
            FileOutputStream(file1).use { doc1.writeTo(it) }
            doc1.close()
            results["Sanction_LZ_EDU_25K.pdf"] = Pair(file1, calculateChecksum(file1))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Signed Loan Agreement - Education Loan 25K
        try {
            val file2 = File(vaultDir, "Agreement_LZ_EDU_2026.pdf")
            val doc2 = PdfDocument()
            val p2Info = PdfDocument.PageInfo.Builder(engine.pageWidth.toInt(), engine.pageHeight.toInt(), 1).create()
            val p2 = doc2.startPage(p2Info)
            val c2 = p2.canvas

            var y2 = engine.drawHeader(
                canvas = c2,
                pageNumber = 1,
                totalPages = 1,
                documentTitle = "eSigned Tripartite Loan Agreement",
                documentRefId = "LZ-AGREEMENT-EDU-2026-9921",
                securityClassification = "LEGALLY ENFORCEABLE • IT ACT 2000 SEC 65B"
            )

            y2 = engine.drawKpiGrid(
                canvas = c2,
                startY = y2,
                cards = listOf(
                    LoanzoPdfEngine.KpiCard("Principal Facility", "₹25,000", "Disbursed to Borrower", engine.colorNavyDeep),
                    LoanzoPdfEngine.KpiCard("Interest Rate", "10.5% Simple", "Fixed for 12 Months", engine.colorGold),
                    LoanzoPdfEngine.KpiCard("Total Repayable", "₹26,450", "12 Installments of ₹2,350", engine.colorEmerald),
                    LoanzoPdfEngine.KpiCard("Agreement Status", "EXECUTED ✅", "Both Parties eSigned", engine.colorEmerald)
                )
            )

            y2 = engine.drawSectionHeader(c2, y2 + 8f, "1. Contracting Parties & Identity Verifications")
            y2 = engine.drawKeyValueGrid(
                canvas = c2,
                startY = y2 + 5f,
                items = listOf(
                    LoanzoPdfEngine.KeyValue("Lender", "Priya Patel (ID: demo_lender_priya)"),
                    LoanzoPdfEngine.KeyValue("Borrower", "Arjun Mehta (ID: demo_user_arjun)"),
                    LoanzoPdfEngine.KeyValue("Co-Borrower / Guarantor", "Nirmala Devi (Aadhaar Verified)", isHighlight = true),
                    LoanzoPdfEngine.KeyValue("Escrow Platform", "Loanzo Technologies Private Limited"),
                    LoanzoPdfEngine.KeyValue("eSign Method", "Aadhaar eKYC OTP & Biometrics"),
                    LoanzoPdfEngine.KeyValue("Governing Jurisdiction", "New Delhi Commercial Court")
                ),
                columns = 2
            )

            y2 = engine.drawSectionHeader(c2, y2 + 8f, "2. Digital e-Signatures & Audit Manifest")
            y2 = engine.drawTable(
                canvas = c2,
                startY = y2 + 5f,
                headers = listOf("Signatory", "Verification Mode", "Timestamp (IST)", "Signature Status"),
                rows = listOf(
                    listOf("Priya Patel (Lender)", "DigiLocker Aadhaar OTP", "28 Aug 2026, 14:15", "Digitally Verified ✅"),
                    listOf("Arjun Mehta (Borrower)", "DigiLocker Aadhaar OTP", "28 Aug 2026, 14:32", "Digitally Verified ✅"),
                    listOf("Nirmala Devi (Guarantor)", "SMS OTP + KYC Consent", "28 Aug 2026, 14:40", "Guarantor Consent ✅"),
                    listOf("Loanzo Escrow Officer", "HSM PKI Digital Cert", "28 Aug 2026, 14:42", "Platform Sealed 🛡️")
                ),
                colWidthRatios = listOf(1.5f, 1.5f, 1.2f, 1.3f)
            )

            y2 = engine.drawVerificationStamp(c2, y2 + 12f, "LZ-AGREEMENT-EDU-2026-9921", "8f434346648f6b96df89dda901c5176b10a6d83961dd3c1ac88b59b2dc327aa4")

            doc2.finishPage(p2)
            FileOutputStream(file2).use { doc2.writeTo(it) }
            doc2.close()
            results["Agreement_LZ_EDU_2026.pdf"] = Pair(file2, calculateChecksum(file2))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Sanction Letter - Business Loan 50K
        try {
            val file3 = File(vaultDir, "Sanction_LZ_BIZ_50K.pdf")
            val doc3 = PdfDocument()
            val p3Info = PdfDocument.PageInfo.Builder(engine.pageWidth.toInt(), engine.pageHeight.toInt(), 1).create()
            val p3 = doc3.startPage(p3Info)
            val c3 = p3.canvas

            var y3 = engine.drawHeader(
                canvas = c3,
                pageNumber = 1,
                totalPages = 1,
                documentTitle = "Business Credit Sanction Letter",
                documentRefId = "LZ-SANCTION-BIZ-50K-2026",
                securityClassification = "SECURED COMMERCIAL FACILITY"
            )

            y3 = engine.drawKpiGrid(
                canvas = c3,
                startY = y3,
                cards = listOf(
                    LoanzoPdfEngine.KpiCard("Sanctioned Facility", "₹50,000", "Inventory Stock Credit", engine.colorEmerald),
                    LoanzoPdfEngine.KpiCard("Tenure", "6 Months", "Bullet EMI Schedule", engine.colorNavyDeep),
                    LoanzoPdfEngine.KpiCard("Interest Rate", "12.0% p.a.", "Simple Reducing Balance", engine.colorGold),
                    LoanzoPdfEngine.KpiCard("Gold Collateral", "48.5 Grams", "Valued at ₹2,10,000", engine.colorEmerald)
                )
            )

            y3 = engine.drawSectionHeader(c3, y3 + 10f, "1. Borrower Enterprise & Collateral Details")
            y3 = engine.drawKeyValueGrid(
                canvas = c3,
                startY = y3 + 5f,
                items = listOf(
                    LoanzoPdfEngine.KeyValue("Borrower Business", "Sharma Electronics & Mobile Store"),
                    LoanzoPdfEngine.KeyValue("Business Owner", "Rahul Sharma (demo_borrower_rahul)"),
                    LoanzoPdfEngine.KeyValue("GSTIN", "07AAAAA0000A1Z5 (Verified)"),
                    LoanzoPdfEngine.KeyValue("Lender", "You (Loanzo Super Admin & Lender)"),
                    LoanzoPdfEngine.KeyValue("Collateral Pledged", "24K Gold Bangles & Necklace (48.5g)", isHighlight = true),
                    LoanzoPdfEngine.KeyValue("Vault Custody Locker", "DEL-VAULT-042 (Tamper Seal #TS-891024)"),
                    LoanzoPdfEngine.KeyValue("Field Inspection", "Completed by Agent Vikas Sharma"),
                    LoanzoPdfEngine.KeyValue("LTV Ratio", "23.8% (Conservative Safe Margin)")
                ),
                columns = 2
            )

            y3 = engine.drawSectionHeader(c3, y3 + 10f, "2. Repayment Schedule & Disbursement Terms")
            y3 = engine.drawTable(
                canvas = c3,
                startY = y3 + 5f,
                headers = listOf("Installment", "Principal", "Interest (12%)", "Total EMI", "Due Date"),
                rows = listOf(
                    listOf("EMI #1", "₹8,334", "₹500", "₹8,834", "05 Sep 2026 (PAID ✅)"),
                    listOf("EMI #2", "₹8,334", "₹417", "₹8,751", "05 Oct 2026 (SCHEDULED)"),
                    listOf("EMI #3", "₹8,334", "₹333", "₹8,667", "05 Nov 2026 (SCHEDULED)"),
                    listOf("EMI #4", "₹8,334", "₹250", "₹8,584", "05 Dec 2026 (SCHEDULED)"),
                    listOf("EMI #5-6", "₹16,664", "₹250", "₹16,914", "Jan-Feb 2027 (SCHEDULED)")
                ),
                colWidthRatios = listOf(1.2f, 1.2f, 1.2f, 1.2f, 1.7f)
            )

            y3 = engine.drawVerificationStamp(c3, y3 + 15f, "LZ-SANCTION-BIZ-50K-2026", "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2")

            doc3.finishPage(p3)
            FileOutputStream(file3).use { doc3.writeTo(it) }
            doc3.close()
            results["Sanction_LZ_BIZ_50K.pdf"] = Pair(file3, calculateChecksum(file3))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. eSigned Loan Agreement - Business Loan 50K
        try {
            val file4 = File(vaultDir, "Agreement_LZ_BIZ_2026.pdf")
            val doc4 = PdfDocument()
            val p4Info = PdfDocument.PageInfo.Builder(engine.pageWidth.toInt(), engine.pageHeight.toInt(), 1).create()
            val p4 = doc4.startPage(p4Info)
            val c4 = p4.canvas

            var y4 = engine.drawHeader(
                canvas = c4,
                pageNumber = 1,
                totalPages = 1,
                documentTitle = "eSigned Secured Business Loan Agreement",
                documentRefId = "LZ-AGREEMENT-BIZ-2026-50K",
                securityClassification = "LEGALLY BINDING SECURED CONTRACT • E-STAMPED"
            )

            y4 = engine.drawKpiGrid(
                canvas = c4,
                startY = y4,
                cards = listOf(
                    LoanzoPdfEngine.KpiCard("Loan Amount", "₹50,000", "Disbursed via IMPS", engine.colorEmerald),
                    LoanzoPdfEngine.KpiCard("Interest Rate", "12.0% p.a.", "Tenure: 6 Months", engine.colorNavyDeep),
                    LoanzoPdfEngine.KpiCard("Collateral Lien", "48.5g Gold", "DEL-VAULT-042 Sealed", engine.colorGold),
                    LoanzoPdfEngine.KpiCard("Execution Status", "ACTIVE ✅", "Aadhaar OTP Signed", engine.colorEmerald)
                )
            )

            y4 = engine.drawSectionHeader(c4, y4 + 8f, "1. Collateral Custody & Lien Schedule")
            y4 = engine.drawKeyValueGrid(
                canvas = c4,
                startY = y4 + 5f,
                items = listOf(
                    LoanzoPdfEngine.KeyValue("Lender (Creditor)", "You / Loanzo Admin Desk"),
                    LoanzoPdfEngine.KeyValue("Borrower (Debtor)", "Rahul Sharma (Sharma Retail)"),
                    LoanzoPdfEngine.KeyValue("Collateral Description", "24K Hallmarked Gold Bangles & Necklace (48.5g)"),
                    LoanzoPdfEngine.KeyValue("Assessed Value", "₹2,10,000 (Certified by Valuer S. Verma)"),
                    LoanzoPdfEngine.KeyValue("Custody Location", "Loanzo Secure Vault Delhi (Locker DEL-042)", isHighlight = true),
                    LoanzoPdfEngine.KeyValue("Tamper Seal Tag", "TS-891024 (Barcode scanned & verified)")
                ),
                columns = 2
            )

            y4 = engine.drawSectionHeader(c4, y4 + 8f, "2. Digital Signatures & e-Stamp Record")
            y4 = engine.drawTable(
                canvas = c4,
                startY = y4 + 5f,
                headers = listOf("Party", "Authentication System", "Certificate Hash", "Status"),
                rows = listOf(
                    listOf("Lender (You)", "DigiLocker HSM Signature", "SHA256:7f4a...88e1", "Signed ✅"),
                    listOf("Borrower (Rahul Sharma)", "Aadhaar OTP + Selfie eSign", "SHA256:3b9c...4412", "Signed ✅"),
                    listOf("Field Agent (Vikas Sharma)", "Agent Biometric App GPS Check", "GPS: 28.6139, 77.2090", "Inspected ✅"),
                    listOf("Central Vault Custodian", "Dual-Key Digital Seal Tag", "Locker DEL-VAULT-042", "Sealed 🔐")
                ),
                colWidthRatios = listOf(1.5f, 1.6f, 1.4f, 1.0f)
            )

            y4 = engine.drawVerificationStamp(c4, y4 + 12f, "LZ-AGREEMENT-BIZ-2026-50K", "b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3")

            doc4.finishPage(p4)
            FileOutputStream(file4).use { doc4.writeTo(it) }
            doc4.close()
            results["Agreement_LZ_BIZ_2026.pdf"] = Pair(file4, calculateChecksum(file4))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 5. Repayment Receipt - EMI #1
        try {
            val file5 = File(vaultDir, "Receipt_EMI1_LZ_BIZ_50K.pdf")
            val doc5 = PdfDocument()
            val p5Info = PdfDocument.PageInfo.Builder(engine.pageWidth.toInt(), engine.pageHeight.toInt(), 1).create()
            val p5 = doc5.startPage(p5Info)
            val c5 = p5.canvas

            var y5 = engine.drawHeader(
                canvas = c5,
                pageNumber = 1,
                totalPages = 1,
                documentTitle = "Official Repayment Receipt (EMI #1)",
                documentRefId = "LZ-RECEIPT-2026-EMI-001",
                securityClassification = "STATUTORY PAYMENT VOUCHER • VERIFIED"
            )

            y5 = engine.drawKpiGrid(
                canvas = c5,
                startY = y5,
                cards = listOf(
                    LoanzoPdfEngine.KpiCard("Amount Paid", "₹8,834.00", "Full EMI Cleared", engine.colorEmerald),
                    LoanzoPdfEngine.KpiCard("Principal Portion", "₹8,334.00", "Credit to Principal", engine.colorNavyDeep),
                    LoanzoPdfEngine.KpiCard("Interest Portion", "₹500.00", "12% p.a. Accrued", engine.colorGold),
                    LoanzoPdfEngine.KpiCard("Overdue / Late Fee", "₹0.00", "Zero Penalties (On-time)", engine.colorEmerald)
                )
            )

            y5 = engine.drawSectionHeader(c5, y5 + 10f, "1. Transaction Banking Telemetry")
            y5 = engine.drawKeyValueGrid(
                canvas = c5,
                startY = y5 + 5f,
                items = listOf(
                    LoanzoPdfEngine.KeyValue("Transaction UTR / Ref", "UPI/329481928491", isHighlight = true),
                    LoanzoPdfEngine.KeyValue("Payment Channel", "Unified Payments Interface (UPI)"),
                    LoanzoPdfEngine.KeyValue("Payer VPA", "rahul.retail@oksbi (Rahul Sharma)"),
                    LoanzoPdfEngine.KeyValue("Payee Escrow VPA", "loanzo.escrow@icici (Loanzo Escrow Desk)"),
                    LoanzoPdfEngine.KeyValue("Associated Loan ID", "demo_loan_lent_1 (Business 50K)"),
                    LoanzoPdfEngine.KeyValue("Payment Timestamp", "05 Sep 2026, 11:24:18 AM IST"),
                    LoanzoPdfEngine.KeyValue("Outstanding Balance", "₹41,666.00 (5 EMIs remaining)"),
                    LoanzoPdfEngine.KeyValue("Next Installment Due", "05 Oct 2026 (₹8,751.00)")
                ),
                columns = 2
            )

            y5 = engine.drawSectionHeader(c5, y5 + 10f, "2. Settlement Certification")
            y5 = engine.drawTable(
                canvas = c5,
                startY = y5 + 5f,
                headers = listOf("Settlement Node", "Transaction ID", "Timestamp", "Clearance Status"),
                rows = listOf(
                    listOf("NPCI UPI Gateway", "UPI/329481928491", "05 Sep 2026, 11:24:18", "SUCCESS ✅"),
                    listOf("Escrow ICICI Bank", "TXN-ESC-981293", "05 Sep 2026, 11:24:20", "FUNDS CREDITED ✅"),
                    listOf("Loan Amortization Engine", "LEDGER-POST-001", "05 Sep 2026, 11:24:21", "BALANCE UPDATED ✅")
                ),
                colWidthRatios = listOf(1.5f, 1.5f, 1.4f, 1.1f)
            )

            y5 = engine.drawVerificationStamp(c5, y5 + 15f, "LZ-RECEIPT-2026-EMI-001", "c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4")

            doc5.finishPage(p5)
            FileOutputStream(file5).use { doc5.writeTo(it) }
            doc5.close()
            results["Receipt_EMI1_LZ_BIZ_50K.pdf"] = Pair(file5, calculateChecksum(file5))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 6. NOC & Clearance Certificate - Medical Loan 15K
        try {
            val file6 = File(vaultDir, "NOC_LZ_MED_15K.pdf")
            val doc6 = PdfDocument()
            val p6Info = PdfDocument.PageInfo.Builder(engine.pageWidth.toInt(), engine.pageHeight.toInt(), 1).create()
            val p6 = doc6.startPage(p6Info)
            val c6 = p6.canvas

            var y6 = engine.drawHeader(
                canvas = c6,
                pageNumber = 1,
                totalPages = 1,
                documentTitle = "No Objection Certificate & Lien Release Deed",
                documentRefId = "LZ-NOC-MED-2026-0042",
                securityClassification = "FINAL INDEBTEDNESS DISCHARGE • COMPLETE CLOSURE"
            )

            y6 = engine.drawKpiGrid(
                canvas = c6,
                startY = y6,
                cards = listOf(
                    LoanzoPdfEngine.KpiCard("Original Facility", "₹15,000", "Medical Emergency Loan", engine.colorNavyDeep),
                    LoanzoPdfEngine.KpiCard("Total Repaid", "₹15,440", "Repaid Ahead of Schedule", engine.colorEmerald),
                    LoanzoPdfEngine.KpiCard("Outstanding Balance", "₹0.00", "Zero Debt Balance", engine.colorEmerald),
                    LoanzoPdfEngine.KpiCard("Lien Status", "RELEASED 🔓", "Collateral Handed Over", engine.colorEmerald)
                )
            )

            y6 = engine.drawSectionHeader(c6, y6 + 10f, "1. Full Discharge & Encumbrance Release")
            y6 = engine.drawKeyValueGrid(
                canvas = c6,
                startY = y6 + 5f,
                items = listOf(
                    LoanzoPdfEngine.KeyValue("Borrower", "Rahul Sharma (demo_borrower_rahul)"),
                    LoanzoPdfEngine.KeyValue("Lender / Creditor", "You (Loanzo Member Desk)"),
                    LoanzoPdfEngine.KeyValue("Loan Facility ID", "demo_loan_closed_1"),
                    LoanzoPdfEngine.KeyValue("Closure Date", "10 May 2026 (Prepayment Settled)"),
                    LoanzoPdfEngine.KeyValue("Collateral Released", "22K Gold Ring + Fixed Deposit Receipt", isHighlight = true),
                    LoanzoPdfEngine.KeyValue("Locker Unsealed", "DEL-VAULT-019 (Tamper Tag #TS-772109)"),
                    LoanzoPdfEngine.KeyValue("Handover Officer", "Field Agent Vikas Sharma (Signed & Verified)"),
                    LoanzoPdfEngine.KeyValue("Credit Bureau Report", "Reported as CLOSED - PAID IN FULL")
                ),
                columns = 2
            )

            y6 = engine.drawSectionHeader(c6, y6 + 10f, "2. Official Platform Certification")
            y6 = engine.drawTable(
                canvas = c6,
                startY = y6 + 5f,
                headers = listOf("Verification Authority", "Audit Role", "Timestamp", "Closure Endorsement"),
                rows = listOf(
                    listOf("Loanzo Legal Desk", "Contract Discharge Signatory", "10 May 2026", "No Claim Pending ✅"),
                    listOf("Central Vault Officer", "Collateral Custody Release", "10 May 2026", "Physical Item Released ✅"),
                    listOf("Field Inspection Unit", "Handover Confirmation to Debtor", "11 May 2026", "Handover Acknowledged ✅")
                ),
                colWidthRatios = listOf(1.5f, 1.5f, 1.2f, 1.3f)
            )

            y6 = engine.drawVerificationStamp(c6, y6 + 15f, "LZ-NOC-MED-2026-0042", "d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5")

            doc6.finishPage(p6)
            FileOutputStream(file6).use { doc6.writeTo(it) }
            doc6.close()
            results["NOC_LZ_MED_15K.pdf"] = Pair(file6, calculateChecksum(file6))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        results
    }
}
