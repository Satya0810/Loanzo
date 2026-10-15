import os
import sys
from reportlab.lib import colors
from reportlab.lib.pagesizes import letter, A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, HRFlowable, KeepTogether
)
from reportlab.pdfgen import canvas

class NumberedCanvas(canvas.Canvas):
    def __init__(self, *args, **kwargs):
        super(NumberedCanvas, self).__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_decorations(num_pages)
            super(NumberedCanvas, self).showPage()
        super(NumberedCanvas, self).save()

    def draw_decorations(self, page_count):
        self.saveState()
        # Header (pages > 1)
        if self._pageNumber > 1:
            self.setFont("Helvetica-Bold", 7.5)
            self.setFillColor(colors.HexColor("#0F2438"))
            self.drawString(36, 758, "LAB 4: REQUIREMENT ELICITATION – I • LOANZO PROTOCOL")
            self.setFont("Helvetica", 7.5)
            self.setFillColor(colors.HexColor("#64748B"))
            self.drawRightString(576, 758, "SOFTWARE ENGINEERING LAB REPORT")
            self.setStrokeColor(colors.HexColor("#CBD5E1"))
            self.setLineWidth(0.75)
            self.line(36, 752, 576, 752)

        # Footer (all pages)
        self.setStrokeColor(colors.HexColor("#CBD5E1"))
        self.setLineWidth(0.75)
        self.line(36, 32, 576, 32)
        self.setFont("Helvetica", 7.5)
        self.setFillColor(colors.HexColor("#64748B"))
        self.drawString(36, 22, "DEPARTMENT OF COMPUTER SCIENCE & ENGINEERING • B.TECH CSE")
        self.drawRightString(576, 22, f"Page {self._pageNumber} of {page_count}")
        self.restoreState()

def build_pdf(filename="Lab4_Requirement_Elicitation_Loanzo.pdf"):
    doc = SimpleDocTemplate(
        filename,
        pagesize=letter,
        leftMargin=36,
        rightMargin=36,
        topMargin=42,
        bottomMargin=42
    )

    styles = getSampleStyleSheet()

    # Color Palette
    c_primary = colors.HexColor("#0F2438")     # Deep Navy
    c_accent = colors.HexColor("#02538F")      # Professional Blue
    c_dark = colors.HexColor("#1E293B")        # Slate Dark
    c_muted = colors.HexColor("#64748B")       # Muted Gray
    c_body = colors.HexColor("#334155")        # Body Slate
    c_border = colors.HexColor("#CBD5E1")      # Border Gray
    c_light = colors.HexColor("#F8FAFC")       # Soft Off-white
    c_blue_bg = colors.HexColor("#EFF6FF")     # Soft Blue Fill
    c_green = colors.HexColor("#059669")       # Green Tag

    # Typography Styles
    title_main = ParagraphStyle('TMain', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=15, leading=18, textColor=c_primary)
    title_sub = ParagraphStyle('TSub', parent=styles['Normal'], fontName='Helvetica', fontSize=8.5, leading=11, textColor=c_accent)
    badge_style = ParagraphStyle('Badge', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=8, leading=10, textColor=colors.white)
    
    sec_hdr = ParagraphStyle('SecHdr', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=10.5, leading=13, textColor=c_primary, spaceBefore=6, spaceAfter=4)
    sec_sub = ParagraphStyle('SecSub', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=9, leading=11, textColor=c_accent, spaceBefore=4, spaceAfter=2)
    
    body_txt = ParagraphStyle('BTxt', parent=styles['Normal'], fontName='Helvetica', fontSize=7.5, leading=10.2, textColor=c_body)
    body_txt_bold = ParagraphStyle('BTxtB', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=7.5, leading=10.2, textColor=c_primary)
    
    tbl_hdr = ParagraphStyle('THdr', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=7.2, leading=9, textColor=colors.white)
    tbl_cell = ParagraphStyle('TCell', parent=styles['Normal'], fontName='Helvetica', fontSize=7, leading=8.8, textColor=c_body)
    tbl_cell_bold = ParagraphStyle('TCellB', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=7, leading=8.8, textColor=c_primary)
    tbl_cell_center = ParagraphStyle('TCellC', parent=styles['Normal'], fontName='Helvetica', fontSize=7, leading=8.8, textColor=c_body, alignment=1)
    tbl_cell_code = ParagraphStyle('TCellCode', parent=styles['Normal'], fontName='Courier-Bold', fontSize=6.8, leading=8.5, textColor=c_accent)

    story = []

    # =========================================================================
    # LAB HEADER BANNER
    # =========================================================================
    header_data = [
        [
            Paragraph("<b>LAB 4: REQUIREMENT ELICITATION – I</b><br/><font size=8 color='#02538F'>Software Engineering & Requirements Engineering Lab • Practical Assignment</font>", title_main),
            Paragraph("<b>PROJECT: LOANZO</b><br/><font size=7.5 color='#64748B'>P2P Microfinance Protocol<br/>Status: Requirements Baseline v1.0</font>", title_sub)
        ]
    ]
    header_tbl = Table(header_data, colWidths=[380, 160])
    header_tbl.setStyle(TableStyle([
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 4),
        ('TOPPADDING', (0, 0), (-1, -1), 0),
        ('LEFTPADDING', (0, 0), (-1, -1), 0),
        ('RIGHTPADDING', (0, 0), (-1, -1), 0),
    ]))
    story.append(header_tbl)
    story.append(HRFlowable(width="100%", thickness=1.5, color=c_primary, spaceBefore=4, spaceAfter=6))

    # Metadata Card
    meta_data = [
        [
            Paragraph("<b>Course:</b> Software Engineering Lab (CSE)", body_txt),
            Paragraph("<b>Lab Unit:</b> Lab 4: Requirement Elicitation – I", body_txt),
            Paragraph("<b>Platform:</b> Modern Android (Kotlin / Compose)", body_txt)
        ],
        [
            Paragraph("<b>Methodology:</b> Agile / Clean Architecture", body_txt),
            Paragraph("<b>Prioritization Framework:</b> MoSCoW", body_txt),
            Paragraph("<b>Deliverable:</b> Preliminary Requirements List (PRL)", body_txt_bold)
        ]
    ]
    meta_tbl = Table(meta_data, colWidths=[180, 180, 180])
    meta_tbl.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), c_light),
        ('BOX', (0, 0), (-1, -1), 0.5, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, colors.HexColor("#E2E8F0")),
        ('TOPPADDING', (0, 0), (-1, -1), 3),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 3),
        ('LEFTPADDING', (0, 0), (-1, -1), 6),
        ('RIGHTPADDING', (0, 0), (-1, -1), 6),
    ]))
    story.append(meta_tbl)
    story.append(Spacer(1, 6))

    # Objective
    story.append(Paragraph("<b>LAB OBJECTIVES & SCOPE</b>", sec_sub))
    story.append(Paragraph(
        "The objective of this lab is to execute the initial phase of <b>Requirement Elicitation</b> for <b>Loanzo</b>—an enterprise-grade, decentralized Peer-to-Peer (P2P) social lending and microfinance Android system. This session focuses on three core activities: (1) Systematically identifying and classifying internal, external, and regulatory stakeholders; (2) Gathering comprehensive Functional Requirements (FRs) covering user onboarding, lending marketplaces, purpose-bound disbursals, collateral vaulting, and legal promissory notes; and (3) Formulating Non-Functional Requirements (NFRs) ensuring sub-15ms query latency, AES-256 hardware encryption, offline-first reliability, and statutory regulatory compliance. The final outcome is a prioritized <b>Preliminary Requirements List (PRL)</b>.",
        body_txt
    ))
    story.append(Spacer(1, 6))

    # =========================================================================
    # ACTIVITY 1: IDENTIFY STAKEHOLDERS
    # =========================================================================
    story.append(Paragraph("<b>ACTIVITY 1: IDENTIFY STAKEHOLDERS</b>", sec_hdr))
    story.append(Paragraph(
        "Stakeholder identification ensures that all perspectives, operational dependencies, and compliance mandates are mapped before system modeling. Stakeholders for Loanzo are classified into Primary, Secondary, and Tertiary/Regulatory tiers.",
        body_txt
    ))
    story.append(Spacer(1, 4))

    sh_table_data = [
        [
            Paragraph("Category", tbl_hdr),
            Paragraph("Stakeholder Group", tbl_hdr),
            Paragraph("System Role & Interaction Profile", tbl_hdr),
            Paragraph("Core Expectations & Concerns", tbl_hdr)
        ],
        [
            Paragraph("<b>Primary User</b>", tbl_cell_bold),
            Paragraph("Borrowers (MSMEs, Individuals)", tbl_cell),
            Paragraph("Posts loan requests, submits KYC, pledges collateral, selects bids, executes e-signed notes, makes UPI repayments.", tbl_cell),
            Paragraph("Transparent reducing-balance interest, zero upfront scam fees, non-coercive recovery, voice-guided vernacular UX.", tbl_cell)
        ],
        [
            Paragraph("<b>Primary User</b>", tbl_cell_bold),
            Paragraph("Private Retail Lenders", tbl_cell),
            Paragraph("Funds loans, places competitive APR bids in social marketplace, reviews borrower credit dossier, monitors portfolio.", tbl_cell),
            Paragraph("Verified counterparty identity, legally enforceable Promissory Notes (Sec 4 NI Act), multi-asset collateral backing, low NPA.", tbl_cell)
        ],
        [
            Paragraph("<b>Primary Operative</b>", tbl_cell_bold),
            Paragraph("Certified Field Agents", tbl_cell),
            Paragraph("Independent doorstep officers conducting physical asset inspection, biometric KYC, and address verification.", tbl_cell),
            Paragraph("Strict role isolation, intuitive inspection checklists, clear dispatch radius (5-50km), real-time per-visit payouts.", tbl_cell)
        ],
        [
            Paragraph("<b>Secondary Partner</b>", tbl_cell_bold),
            Paragraph("Verified Merchants / Suppliers", tbl_cell),
            Paragraph("Equipment dealers, hospitals, tuition centers receiving direct purpose-bound tranche disbursements via UPI.", tbl_cell),
            Paragraph("Instant settlement without chargebacks, automated Re 1 penny-drop validation against commercial tax invoices.", tbl_cell)
        ],
        [
            Paragraph("<b>Secondary Mgmt</b>", tbl_cell_bold),
            Paragraph("Platform Administrator", tbl_cell),
            Paragraph("Reviews field agent empanelment queue, monitors platform portfolio health, handles counterparty dispute escalation.", tbl_cell),
            Paragraph("Comprehensive audit trails, automated delinquency alerts via Telegram Bot, fraud detection, zero regulatory breaches.", tbl_cell)
        ],
        [
            Paragraph("<b>Tertiary / Regulator</b>", tbl_cell_bold),
            Paragraph("Statutory Authorities (RBI, NPCI)", tbl_cell),
            Paragraph("Regulatory oversight (RBI Digital Lending 2022, P2P Directions 2017, NPCI UPI rails, DPDP Act 2023, Section 10A IT Act).", tbl_cell),
            Paragraph("Non-custodial architecture (Rs. 0 held in deposit), fair non-compounding penalty rules (RBI/2023-24/53), privacy compliance.", tbl_cell)
        ]
    ]

    sh_table = Table(sh_table_data, colWidths=[65, 105, 195, 175])
    sh_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_primary),
        ('BOX', (0, 0), (-1, -1), 0.5, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border),
        ('TOPPADDING', (0, 0), (-1, -1), 3),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 3),
        ('LEFTPADDING', (0, 0), (-1, -1), 4),
        ('RIGHTPADDING', (0, 0), (-1, -1), 4),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, c_light]),
    ]))
    story.append(sh_table)
    story.append(Spacer(1, 8))

    # =========================================================================
    # ACTIVITY 2: GATHER FUNCTIONAL REQUIREMENTS (FRs)
    # =========================================================================
    story.append(Paragraph("<b>ACTIVITY 2: GATHER FUNCTIONAL REQUIREMENTS (FRs)</b>", sec_hdr))
    story.append(Paragraph(
        "Functional Requirements (FRs) specify the fundamental actions, business rules, inputs, and outputs executed by the Loanzo system across its end-to-end loan lifecycle.",
        body_txt
    ))
    story.append(Spacer(1, 4))

    fr_data = [
        [
            Paragraph("Req ID", tbl_hdr),
            Paragraph("Functional Module", tbl_hdr),
            Paragraph("Requirement Statement & Business Logic", tbl_hdr)
        ],
        # Module 1
        [
            Paragraph("FR-01", tbl_cell_code),
            Paragraph("Authentication & MFA", tbl_cell_bold),
            Paragraph("The system shall authenticate users via Email/Password, Phone OTP, Google OAuth, and BiometricPrompt (Fingerprint/Face). If biometric credentials change or expire, re-authentication via password shall register biometrics without re-login.", tbl_cell)
        ],
        [
            Paragraph("FR-02", tbl_cell_code),
            Paragraph("KYC & DigiLocker Ingestion", tbl_cell_bold),
            Paragraph("The system shall verify applicant identity by pulling verified Aadhaar and PAN records via DigiLocker and running Google ML Kit on-device facial liveness detection.", tbl_cell)
        ],
        [
            Paragraph("FR-03", tbl_cell_code),
            Paragraph("Encrypted Document Vault", tbl_cell_bold),
            Paragraph("The system shall store sensitive KYC artifacts inside an AES-256 encrypted vault, decrypting credentials strictly upon biometric authentication or password confirmation.", tbl_cell)
        ],
        [
            Paragraph("FR-04", tbl_cell_code),
            Paragraph("Role-Based Access Control", tbl_cell_bold),
            Paragraph("The system shall enforce strict role isolation: Certified Field Agents shall be partitioned into an isolated dispatch workspace and prohibited from borrowing or lending.", tbl_cell)
        ],
        # Module 2
        [
            Paragraph("FR-05", tbl_cell_code),
            Paragraph("Loan Origination Dual Mode", tbl_cell_bold),
            Paragraph("The system shall provide dual origination modes: 'Request Loan' (borrower posts purpose, amount, tenure, max APR) and 'Grant Loan' (lender initiates direct credit offer to counterparty).", tbl_cell)
        ],
        [
            Paragraph("FR-06", tbl_cell_code),
            Paragraph("Reverse-Auction Bidding", tbl_cell_bold),
            Paragraph("The system shall host a real-time P2P marketplace where verified retail lenders place competitive APR and repayment tenure bids against public loan requests.", tbl_cell)
        ],
        [
            Paragraph("FR-07", tbl_cell_code),
            Paragraph("Interactive EMI Simulator", tbl_cell_bold),
            Paragraph("The system shall compute real-time reducing-balance, simple, and flat interest schedules, displaying interactive donut charts for principal-to-interest ratios.", tbl_cell)
        ],
        # Module 3
        [
            Paragraph("FR-08", tbl_cell_code),
            Paragraph("Purpose-Bound Tranches", tbl_cell_bold),
            Paragraph("The system shall support milestone-based tranche decomposition (e.g., Raw Materials, Machinery, Inventory), prohibiting raw cash-in-hand lump-sum disbursements.", tbl_cell)
        ],
        [
            Paragraph("FR-09", tbl_cell_code),
            Paragraph("Direct Merchant Settlement", tbl_cell_bold),
            Paragraph("The system shall route tranche payouts directly to verified supplier/merchant UPI accounts via upi://pay rails with mandatory tax invoice upload.", tbl_cell)
        ],
        [
            Paragraph("FR-10", tbl_cell_code),
            Paragraph("Re 1 Penny-Drop Validation", tbl_cell_bold),
            Paragraph("The system shall execute an automated Re 1 penny-drop validation to verify that the registered bank account name matches the business name on the physical commercial invoice.", tbl_cell)
        ],
        # Module 4
        [
            Paragraph("FR-11", tbl_cell_code),
            Paragraph("Multi-Asset Collateral Vault", tbl_cell_bold),
            Paragraph("The system shall record 5 collateral classes: Precious Bullion (Gold, 75% LTV), Property Deeds (50-60% LTV), IT Equipment (40-50% LTV), Vehicles (55-65% LTV), and B2B Invoices (70-80% LTV).", tbl_cell)
        ],
        [
            Paragraph("FR-12", tbl_cell_code),
            Paragraph("Field Agent Dispatch & Audit", tbl_cell_bold),
            Paragraph("The system shall dispatch certified ground agents to perform geotagged, timestamped asset appraisals and residence verifications with tamper-evident photo capture.", tbl_cell)
        ],
        [
            Paragraph("FR-13", tbl_cell_code),
            Paragraph("Dual-Agent Consensus", tbl_cell_bold),
            Paragraph("The system shall mandate two independent field agent appraisals for high-ticket collateral pledges exceeding Rs. 1,00,000 to eliminate local collusion.", tbl_cell)
        ],
        # Module 5
        [
            Paragraph("FR-14", tbl_cell_code),
            Paragraph("Legal Promissory Note Synthesis", tbl_cell_bold),
            Paragraph("The system shall automatically synthesize a 4-page legally binding Promissory Note under Section 4 of the Negotiable Instruments Act 1881 and an RBI-compliant Key Fact Statement (KFS).", tbl_cell)
        ],
        [
            Paragraph("FR-15", tbl_cell_code),
            Paragraph("Digital e-Sign & Audit Seal", tbl_cell_bold),
            Paragraph("The system shall execute agreements via Aadhaar OTP e-Sign and biometric signature canvas, stamping documents with SHA-256 hash, GPS coordinates, and timestamp.", tbl_cell)
        ],
        # Module 6
        [
            Paragraph("FR-16", tbl_cell_code),
            Paragraph("Dynamic UPI QR & Deep Links", tbl_cell_bold),
            Paragraph("The system shall generate dynamic UPI QR codes and intent deep-links for instant EMI settlement via GPay, PhonePe, and Paytm with automated ledger reconciliation.", tbl_cell)
        ],
        [
            Paragraph("FR-17", tbl_cell_code),
            Paragraph("Fair Penal Compounding Engine", tbl_cell_bold),
            Paragraph("The system shall calculate non-compounding daily overdue charges capped at a 2% monthly ceiling in compliance with RBI Fair Lending circular RBI/2023-24/53.", tbl_cell)
        ],
        [
            Paragraph("FR-18", tbl_cell_code),
            Paragraph("Emergency Restructuring & NOC", tbl_cell_bold),
            Paragraph("The system shall permit formal recording of mutually agreed EMI extensions/moratoriums during verified distress and auto-generate digital No Objection Certificates (NOC).", tbl_cell)
        ],
        [
            Paragraph("FR-19", tbl_cell_code),
            Paragraph("Multi-LLM Speculative Assistant", tbl_cell_bold),
            Paragraph("The system shall run parallel speculative AI queries across Gemini, SambaNova, and Cloudflare Workers AI with fallback to an on-device heuristic engine for vernacular explanations.", tbl_cell)
        ],
        [
            Paragraph("FR-20", tbl_cell_code),
            Paragraph("Automated Telegram Alert Desk", tbl_cell_bold),
            Paragraph("The system shall dispatch real-time loan lifecycle events, delinquency notices, and dispute filings via an asynchronous Telegram Bot (@Loanzo_bot) and FCM push notifications.", tbl_cell)
        ]
    ]

    fr_table = Table(fr_data, colWidths=[40, 120, 380])
    fr_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_primary),
        ('BOX', (0, 0), (-1, -1), 0.5, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border),
        ('TOPPADDING', (0, 0), (-1, -1), 2.5),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 2.5),
        ('LEFTPADDING', (0, 0), (-1, -1), 4),
        ('RIGHTPADDING', (0, 0), (-1, -1), 4),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, c_light]),
    ]))
    story.append(fr_table)
    story.append(Spacer(1, 8))

    # =========================================================================
    # ACTIVITY 3: GATHER NON-FUNCTIONAL REQUIREMENTS (NFRs)
    # =========================================================================
    story.append(Paragraph("<b>ACTIVITY 3: GATHER NON-FUNCTIONAL REQUIREMENTS (NFRs)</b>", sec_hdr))
    story.append(Paragraph(
        "Non-Functional Requirements (NFRs) define system quality attributes, operational thresholds, security controls, reliability, and regulatory boundaries.",
        body_txt
    ))
    story.append(Spacer(1, 4))

    nfr_data = [
        [
            Paragraph("Req ID", tbl_hdr),
            Paragraph("Quality Attribute", tbl_hdr),
            Paragraph("Metric / Operational Target", tbl_hdr),
            Paragraph("Architectural Implementation", tbl_hdr)
        ],
        [
            Paragraph("NFR-P01", tbl_cell_code),
            Paragraph("Database Query Latency", tbl_cell_bold),
            Paragraph("Sub-15ms local query execution on low-end Android Go hardware (2GB RAM).", tbl_cell),
            Paragraph("Normalized Room SQLite v22 schemas with compiled index DAOs.", tbl_cell)
        ],
        [
            Paragraph("NFR-P02", tbl_cell_code),
            Paragraph("App Launch Time", tbl_cell_bold),
            Paragraph("Cold start to interactive Home Dashboard in under 1.8 seconds with zero flicker.", tbl_cell),
            Paragraph("SplashWarmupCoordinator evaluating session state in ambient pulse gate.", tbl_cell)
        ],
        [
            Paragraph("NFR-P03", tbl_cell_code),
            Paragraph("AI Inference Latency", tbl_cell_bold),
            Paragraph("First-token time-to-deliver under 1,200ms across unstable 4G networks.", tbl_cell),
            Paragraph("Speculative channel-based racing across 3 parallel LLM endpoints.", tbl_cell)
        ],
        [
            Paragraph("NFR-S01", tbl_cell_code),
            Paragraph("Encryption at Rest", tbl_cell_bold),
            Paragraph("100% of stored KYC credentials, PINs, and notes encrypted via AES-256-GCM.", tbl_cell),
            Paragraph("Hardware-backed Android KeyStore / StrongBox integration.", tbl_cell)
        ],
        [
            Paragraph("NFR-S02", tbl_cell_code),
            Paragraph("Encryption in Transit", tbl_cell_bold),
            Paragraph("Zero plaintext transmission across network interfaces.", tbl_cell),
            Paragraph("Enforced TLS 1.3 with SHA-256 SSL certificate pinning.", tbl_cell)
        ],
        [
            Paragraph("NFR-S03", tbl_cell_code),
            Paragraph("Data Privacy & Minimization", tbl_cell_bold),
            Paragraph("Zero contact-book scraping, zero gallery snooping, full DPDP Act compliance.", tbl_cell),
            Paragraph("Isolated document sandbox with explicit user permission scopes.", tbl_cell)
        ],
        [
            Paragraph("NFR-R01", tbl_cell_code),
            Paragraph("Offline-First Resiliency", tbl_cell_bold),
            Paragraph("Core features (loans view, EMI calculator, draft agreements) work with 0% network.", tbl_cell),
            Paragraph("Local Room persistence with atomic delta sync via Android WorkManager.", tbl_cell)
        ],
        [
            Paragraph("NFR-R02", tbl_cell_code),
            Paragraph("System Availability", tbl_cell_bold),
            Paragraph("Cloud synchronization and Telegram notification endpoints maintain 99.9% uptime.", tbl_cell),
            Paragraph("Serverless microservices on Vercel Edge with Firebase cloud fallback.", tbl_cell)
        ],
        [
            Paragraph("NFR-U01", tbl_cell_code),
            Paragraph("Vernacular Accessibility", tbl_cell_bold),
            Paragraph("Runtime language switching across 21+ Indian languages under 150ms.", tbl_cell),
            Paragraph("Google GTX translation engine with in-memory LRU caching.", tbl_cell)
        ],
        [
            Paragraph("NFR-U02", tbl_cell_code),
            Paragraph("Accidental Action Prevention", tbl_cell_bold),
            Paragraph("Zero unintended loan approvals or repayment submissions.", tbl_cell),
            Paragraph("Draggable SwipeToConfirm slider with tactile haptic feedback.", tbl_cell)
        ],
        [
            Paragraph("NFR-C01", tbl_cell_code),
            Paragraph("Non-Custodial Architecture", tbl_cell_bold),
            Paragraph("Platform maintains Rs. 0.00 escrow/deposit balance (100% direct settlement).", tbl_cell),
            Paragraph("Zero pooling architecture exempting app from NBFC banking license.", tbl_cell)
        ],
        [
            Paragraph("NFR-C02", tbl_cell_code),
            Paragraph("Statutory Legal Admissibility", tbl_cell_bold),
            Paragraph("Executed Promissory Notes admissible in Indian summary civil suits (Order 37).", tbl_cell),
            Paragraph("Section 4 NI Act 1881 format + Section 65B Indian Evidence Act audit hash.", tbl_cell)
        ]
    ]

    nfr_table = Table(nfr_data, colWidths=[45, 110, 195, 190])
    nfr_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_accent),
        ('BOX', (0, 0), (-1, -1), 0.5, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border),
        ('TOPPADDING', (0, 0), (-1, -1), 2.5),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 2.5),
        ('LEFTPADDING', (0, 0), (-1, -1), 4),
        ('RIGHTPADDING', (0, 0), (-1, -1), 4),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, c_light]),
    ]))
    story.append(nfr_table)
    story.append(Spacer(1, 8))

    # =========================================================================
    # OUTCOME: PRELIMINARY REQUIREMENTS LIST (PRL)
    # =========================================================================
    story.append(Paragraph("<b>OUTCOME: PRELIMINARY REQUIREMENTS LIST (PRL)</b>", sec_hdr))
    story.append(Paragraph(
        "The Preliminary Requirements List synthesizes and prioritizes all elicited requirements using the industry-standard <b>MoSCoW framework</b> (Must Have, Should Have, Could Have, Won't Have).",
        body_txt
    ))
    story.append(Spacer(1, 4))

    prl_data = [
        [
            Paragraph("Req ID", tbl_hdr),
            Paragraph("Requirement Description", tbl_hdr),
            Paragraph("Type", tbl_hdr),
            Paragraph("Key Stakeholder", tbl_hdr),
            Paragraph("MoSCoW Priority", tbl_hdr),
            Paragraph("Complexity", tbl_hdr)
        ],
        [Paragraph("PRL-01", tbl_cell_code), Paragraph("MFA & Biometric App Authentication", tbl_cell), Paragraph("FR", tbl_cell_center), Paragraph("Borrower, Lender", tbl_cell), Paragraph("<b>Must Have</b>", tbl_cell_bold), Paragraph("Low", tbl_cell_center)],
        [Paragraph("PRL-02", tbl_cell_code), Paragraph("DigiLocker & ML Kit Facial Liveness KYC", tbl_cell), Paragraph("FR", tbl_cell_center), Paragraph("Borrower, Admin", tbl_cell), Paragraph("<b>Must Have</b>", tbl_cell_bold), Paragraph("Medium", tbl_cell_center)],
        [Paragraph("PRL-03", tbl_cell_code), Paragraph("AES-256 Encrypted Local Document Vault", tbl_cell), Paragraph("NFR", tbl_cell_center), Paragraph("All Users", tbl_cell), Paragraph("<b>Must Have</b>", tbl_cell_bold), Paragraph("High", tbl_cell_center)],
        [Paragraph("PRL-04", tbl_cell_code), Paragraph("Social P2P Reverse Bidding Marketplace", tbl_cell), Paragraph("FR", tbl_cell_center), Paragraph("Borrower, Lender", tbl_cell), Paragraph("<b>Must Have</b>", tbl_cell_bold), Paragraph("High", tbl_cell_center)],
        [Paragraph("PRL-05", tbl_cell_code), Paragraph("Purpose-Bound Direct Merchant Disbursal", tbl_cell), Paragraph("FR", tbl_cell_center), Paragraph("Borrower, Merchant", tbl_cell), Paragraph("<b>Must Have</b>", tbl_cell_bold), Paragraph("High", tbl_cell_center)],
        [Paragraph("PRL-06", tbl_cell_code), Paragraph("Re 1 Penny-Drop Account Name Validation", tbl_cell), Paragraph("FR", tbl_cell_center), Paragraph("Merchant, Lender", tbl_cell), Paragraph("<b>Must Have</b>", tbl_cell_bold), Paragraph("Medium", tbl_cell_center)],
        [Paragraph("PRL-07", tbl_cell_code), Paragraph("Section 4 NI Act Promissory Note Synthesis", tbl_cell), Paragraph("FR", tbl_cell_center), Paragraph("Borrower, Lender", tbl_cell), Paragraph("<b>Must Have</b>", tbl_cell_bold), Paragraph("Medium", tbl_cell_center)],
        [Paragraph("PRL-08", tbl_cell_code), Paragraph("Aadhaar OTP e-Sign with SHA-256 Audit Seal", tbl_cell), Paragraph("FR", tbl_cell_center), Paragraph("Borrower, Lender", tbl_cell), Paragraph("<b>Must Have</b>", tbl_cell_bold), Paragraph("Medium", tbl_cell_center)],
        [Paragraph("PRL-09", tbl_cell_code), Paragraph("Dynamic UPI QR & Deep-Link Settlements", tbl_cell), Paragraph("FR", tbl_cell_center), Paragraph("Borrower, Lender", tbl_cell), Paragraph("<b>Must Have</b>", tbl_cell_bold), Paragraph("Low", tbl_cell_center)],
        [Paragraph("PRL-10", tbl_cell_code), Paragraph("Non-Compounding 2% Max RBI Penalty Engine", tbl_cell), Paragraph("FR/NFR", tbl_cell_center), Paragraph("Borrower, Regulator", tbl_cell), Paragraph("<b>Must Have</b>", tbl_cell_bold), Paragraph("Medium", tbl_cell_center)],
        [Paragraph("PRL-11", tbl_cell_code), Paragraph("Doorstep Field Agent Dispatch & Audit Desk", tbl_cell), Paragraph("FR", tbl_cell_center), Paragraph("Field Agent, Admin", tbl_cell), Paragraph("<b>Should Have</b>", tbl_cell), Paragraph("High", tbl_cell_center)],
        [Paragraph("PRL-12", tbl_cell_code), Paragraph("Multi-Asset Collateral Escrow (5 Classes)", tbl_cell), Paragraph("FR", tbl_cell_center), Paragraph("Lender, Borrower", tbl_cell), Paragraph("<b>Should Have</b>", tbl_cell), Paragraph("Medium", tbl_cell_center)],
        [Paragraph("PRL-13", tbl_cell_code), Paragraph("Offline-First Room SQLite with WorkManager", tbl_cell), Paragraph("NFR", tbl_cell_center), Paragraph("All Users", tbl_cell), Paragraph("<b>Should Have</b>", tbl_cell), Paragraph("High", tbl_cell_center)],
        [Paragraph("PRL-14", tbl_cell_code), Paragraph("Multi-Model AI Speculative Assistant", tbl_cell), Paragraph("FR", tbl_cell_center), Paragraph("Borrower", tbl_cell), Paragraph("<b>Should Have</b>", tbl_cell), Paragraph("Medium", tbl_cell_center)],
        [Paragraph("PRL-15", tbl_cell_code), Paragraph("Automated Telegram Bot Desk (@Loanzo_bot)", tbl_cell), Paragraph("FR", tbl_cell_center), Paragraph("All Users, Admin", tbl_cell), Paragraph("<b>Should Have</b>", tbl_cell), Paragraph("Low", tbl_cell_center)],
        [Paragraph("PRL-16", tbl_cell_code), Paragraph("21+ Regional Language Runtime Switcher", tbl_cell), Paragraph("NFR", tbl_cell_center), Paragraph("Rural Borrowers", tbl_cell), Paragraph("<b>Should Have</b>", tbl_cell), Paragraph("Medium", tbl_cell_center)],
        [Paragraph("PRL-17", tbl_cell_code), Paragraph("Dual-Agent Consensus for High-Value Assets", tbl_cell), Paragraph("FR", tbl_cell_center), Paragraph("Field Agent, Lender", tbl_cell), Paragraph("<b>Could Have</b>", tbl_cell), Paragraph("Medium", tbl_cell_center)],
        [Paragraph("PRL-18", tbl_cell_code), Paragraph("Prepayment & Foreclosure Savings Simulator", tbl_cell), Paragraph("FR", tbl_cell_center), Paragraph("Borrower", tbl_cell), Paragraph("<b>Could Have</b>", tbl_cell), Paragraph("Low", tbl_cell_center)],
        [Paragraph("PRL-19", tbl_cell_code), Paragraph("Alternative Digital Credit Dossier Ladder", tbl_cell), Paragraph("FR", tbl_cell_center), Paragraph("Borrower, Bank", tbl_cell), Paragraph("<b>Could Have</b>", tbl_cell), Paragraph("Medium", tbl_cell_center)],
        [Paragraph("PRL-20", tbl_cell_code), Paragraph("Automated Blockchain Smart Contract Mirror", tbl_cell), Paragraph("FR", tbl_cell_center), Paragraph("Auditors", tbl_cell), Paragraph("Won't Have (v1)", tbl_cell), Paragraph("High", tbl_cell_center)]
    ]

    prl_table = Table(prl_data, colWidths=[42, 190, 35, 105, 95, 73])
    prl_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_primary),
        ('BOX', (0, 0), (-1, -1), 0.5, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border),
        ('TOPPADDING', (0, 0), (-1, -1), 2),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 2),
        ('LEFTPADDING', (0, 0), (-1, -1), 3.5),
        ('RIGHTPADDING', (0, 0), (-1, -1), 3.5),
        ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, c_light]),
    ]))
    story.append(prl_table)
    story.append(Spacer(1, 8))

    # =========================================================================
    # CONCLUSION & SIGN-OFF
    # =========================================================================
    story.append(Paragraph("<b>CONCLUSION & LAB SIGN-OFF</b>", sec_sub))
    story.append(Paragraph(
        "<b>Summary of Elicitation:</b> In Lab 4, requirement elicitation was successfully conducted for the Loanzo peer-to-peer microfinance platform. Core stakeholders across borrowers, lenders, field operatives, merchants, and regulatory bodies were mapped into functional roles. A total of <b>20 Core Functional Requirements</b> and <b>12 Non-Functional Requirements</b> were elicited, addressing chronic microfinance vulnerabilities including fund diversion, usurious interest rates, and legal recovery backlogs. The prioritized <b>Preliminary Requirements List (PRL)</b> establishes the foundational specification required for <b>Lab 5: Requirement Analysis and Modeling (Use Case Diagrams, Activity Flows & SRS Documentation)</b>.",
        body_txt
    ))
    story.append(Spacer(1, 8))

    # Signoff Block
    sign_data = [
        [
            Paragraph("<b>Student Signature / Roll No:</b><br/><font color='#64748B'>Satyam Kumar • B.Tech CSE (2024-2027)</font>", body_txt),
            Paragraph("<b>Lab Faculty Assessment & Sign-off:</b><br/><font color='#64748B'>Marks: [ &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; / 10 ] &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Signature: ___________________</font>", body_txt)
        ]
    ]
    sign_tbl = Table(sign_data, colWidths=[270, 270])
    sign_tbl.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), c_light),
        ('BOX', (0, 0), (-1, -1), 0.5, c_border),
        ('TOPPADDING', (0, 0), (-1, -1), 4),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 4),
        ('LEFTPADDING', (0, 0), (-1, -1), 6),
        ('RIGHTPADDING', (0, 0), (-1, -1), 6),
    ]))
    story.append(sign_tbl)

    doc.build(story, canvasmaker=NumberedCanvas)
    print(f"PDF successfully built: {filename}")

if __name__ == "__main__":
    out_pdf = "Lab4_Requirement_Elicitation_Loanzo.pdf"
    build_pdf(out_pdf)
    # Also save to docs directory
    if os.path.exists("docs"):
        docs_pdf = os.path.join("docs", "Lab4_Requirement_Elicitation_Loanzo.pdf")
        build_pdf(docs_pdf)
