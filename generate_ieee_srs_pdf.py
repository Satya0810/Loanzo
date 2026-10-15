import os
import sys
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, HRFlowable
)
from reportlab.pdfgen import canvas

# --- Two-Pass Canvas for Precise Page Numbering (Page X of Y) ---
class IEEENumberedCanvas(canvas.Canvas):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_page_decorations(num_pages)
            super().showPage()
        super().save()

    def draw_page_decorations(self, page_count):
        if self._pageNumber == 1:
            return  # Suppress running header/footer on cover page

        self.saveState()
        self.setFont("Helvetica", 7.5)
        self.setFillColor(colors.HexColor("#4B5563"))

        # Running Header
        self.drawString(40, 755, "LOANZO PROTOCOL | Software Requirements Specification (SRS) - IEEE Std 830-1998")
        self.setStrokeColor(colors.HexColor("#D1D5DB"))
        self.setLineWidth(0.5)
        self.line(40, 748, 572, 748)

        # Running Footer
        self.line(40, 36, 572, 36)
        self.drawString(40, 25, "CONFIDENTIAL & PROPRIETARY | SYSTEM SPECIFICATION & REGULATORY AUDIT")
        page_str = f"Page {self._pageNumber} of {page_count}"
        self.drawRightString(572, 25, page_str)
        self.restoreState()

def build_pdf(output_path):
    doc = SimpleDocTemplate(
        output_path,
        pagesize=letter,
        leftMargin=40,
        rightMargin=40,
        topMargin=50,
        bottomMargin=46
    )

    styles = getSampleStyleSheet()

    # Custom Palette
    c_primary = colors.HexColor("#0F172A")    # Deep Slate
    c_accent = colors.HexColor("#1E40AF")     # Corporate Blue
    c_body = colors.HexColor("#1F2937")       # Charcoal
    c_border = colors.HexColor("#CBD5E1")     # Light Slate Border
    c_border_light = colors.HexColor("#E2E8F0")
    c_header_bg = colors.HexColor("#F1F5F9")  # Table Header Tint

    # Typography Styles
    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=20,
        leading=24,
        textColor=c_primary,
        alignment=1,
        spaceAfter=6
    )
    subtitle_style = ParagraphStyle(
        'DocSub',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=10,
        leading=14,
        textColor=colors.HexColor("#374151"),
        alignment=1,
        spaceAfter=4
    )
    std_badge_style = ParagraphStyle(
        'StdBadge',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=8.5,
        leading=12,
        textColor=c_accent,
        alignment=1,
        spaceAfter=15
    )
    h1_style = ParagraphStyle(
        'H1',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=10,
        leading=13,
        textColor=c_primary,
        spaceBefore=4,
        spaceAfter=2
    )
    body_style = ParagraphStyle(
        'Body',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=7.5,
        leading=10.5,
        textColor=c_body
    )
    bullet_style = ParagraphStyle(
        'Bullet',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=7.5,
        leading=10.5,
        textColor=c_body,
        leftIndent=10
    )
    th_style = ParagraphStyle(
        'TH',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=7.5,
        leading=10,
        textColor=c_primary
    )
    th_center = ParagraphStyle(
        'THC',
        parent=th_style,
        alignment=1
    )
    td_style = ParagraphStyle(
        'TD',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=6.8,
        leading=9,
        textColor=c_body
    )
    td_bold = ParagraphStyle(
        'TDBold',
        parent=td_style,
        fontName='Helvetica-Bold',
        textColor=c_primary
    )
    td_code = ParagraphStyle(
        'TDCode',
        parent=td_style,
        fontName='Helvetica-Bold',
        textColor=c_accent
    )
    td_center = ParagraphStyle(
        'TDCenter',
        parent=td_style,
        alignment=1
    )

    story = []

    # =========================================================================
    # PAGE 1: COVER PAGE & DOCUMENT METADATA
    # =========================================================================
    story.append(Spacer(1, 40))
    story.append(Paragraph("LOANZO FINANCIAL TECHNOLOGIES", ParagraphStyle('SubHeader', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=11, leading=14, textColor=colors.HexColor("#4B5563"), alignment=1, spaceAfter=8)))
    story.append(Paragraph("Software Requirements Specification (SRS)", title_style))
    story.append(Paragraph("Peer-to-Peer Microfinance, Purpose-Bound Social Lending & Contract Synthesis Platform", subtitle_style))
    story.append(Paragraph("Compliant with IEEE Std 830-1998 / IEEE/ISO/IEC 29148:2018 Standards", std_badge_style))
    story.append(HRFlowable(width="80%", thickness=1.5, color=c_primary, spaceBefore=4, spaceAfter=20))

    meta_data = [
        [Paragraph("Document Identifier", td_bold), Paragraph("LZ-SRS-IEEE-830-2026-V1.0", td_style)],
        [Paragraph("Project Codename", td_bold), Paragraph("Loanzo Protocol (Core Android & Microfinance Engine)", td_style)],
        [Paragraph("Document Version", td_bold), Paragraph("Version 1.0 (Production Engineering Baseline)", td_style)],
        [Paragraph("Publication Date", td_bold), Paragraph("September 21, 2026", td_style)],
        [Paragraph("Author / Lead Architect", td_bold), Paragraph("Loanzo Engineering & Systems Architecture Team", td_style)],
        [Paragraph("Regulatory Jurisdiction", td_bold), Paragraph("Republic of India (RBI P2P Directions, IT Act 2000, NI Act 1881, Stamp Act 1899)", td_style)],
        [Paragraph("Classification & Security", td_bold), Paragraph("Confidential & Proprietary : Statutory Engineering Record", td_style)],
        [Paragraph("Target Environment", td_bold), Paragraph("Android 8.0+ (API 26-35) / Room SQLite / Cloud KMS CMEK Storage", td_style)]
    ]
    t_meta = Table(meta_data, colWidths=[145, 265])
    t_meta.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), colors.HexColor("#F9FAFB")),
        ('BOX', (0, 0), (-1, -1), 1.0, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 3.5),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 3.5),
        ('LEFTPADDING', (0, 0), (-1, -1), 8),
        ('RIGHTPADDING', (0, 0), (-1, -1), 8),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
    ]))
    story.append(t_meta)

    story.append(Spacer(1, 24))
    exec_summary_text = (
        "<b>Executive Summary:</b> Grounded in empirical data from the <b>NSSO All-India Debt and Investment "
        "Survey (AIDIS, 77th Round)</b> and <b>RBI Financial Stability Reports</b>, informal non-institutional debt "
        "comprises 19% to 25% of household liabilities in India (where household debt has reached 45.5% of GDP). "
        "Traditional informal credit is burdened by predatory usury (24% to 120% APR) and protracted civil "
        "recovery suits (7 to 10 years). The Loanzo Protocol establishes an institutional-grade, zero-float non-custodial "
        "architecture (INR 0.00 customer deposits) operating under RBI NBFC-P2P Master Directions (RBI/2024-25/63) and "
        "RBI Penal Charges Norms (RBI/2023-24/53). It enables direct UPI rails with server-side webhook verification, "
        "mandatory digital e-stamping under Article 49 of the Indian Stamp Act 1899, Section 4 NI Act Promissory Notes "
        "enforceable via Order 37 CPC fast-track summary suits, automated milestone tranching, 100% transit fidelity "
        "collateral insurance, and multi-tenant Maker-Checker administrative governance."
    )
    story.append(Paragraph(exec_summary_text, body_style))
    story.append(PageBreak())

    # =========================================================================
    # PAGE 2: TABLE OF CONTENTS, 1.0 INTRODUCTION & 2.0 OVERALL DESCRIPTION
    # =========================================================================
    story.append(Paragraph("Document Structure & IEEE 830 Content Map", h1_style))
    story.append(HRFlowable(width="100%", thickness=0.75, color=c_border, spaceBefore=1, spaceAfter=5))

    toc_data = [
        [Paragraph("IEEE Section", th_style), Paragraph("Section Title & Major Coverage Areas", th_style), Paragraph("Target Scope", th_style)],
        [Paragraph("1.0", td_code), Paragraph("<b>Introduction:</b> Purpose, Scope, Definitions, Empirical Sources, and Reading Guide", td_style), Paragraph("Foundation", td_style)],
        [Paragraph("2.0", td_code), Paragraph("<b>Overall Description:</b> Product Perspective, User Classes, Environment, and Constraints", td_style), Paragraph("System Context", td_style)],
        [Paragraph("3.0", td_code), Paragraph("<b>External Interface Requirements:</b> User, Hardware, Software, and Communications Interfaces", td_style), Paragraph("Boundary Interfaces", td_style)],
        [Paragraph("4.0", td_code), Paragraph("<b>System Features & Functional Requirements:</b> FR-01 through FR-13 Granular Engineering Specifications", td_style), Paragraph("Core Logic", td_style)],
        [Paragraph("5.0", td_code), Paragraph("<b>Non-Functional Requirements:</b> Performance, Security, Reliability, and Statutory Compliance", td_style), Paragraph("Quality & Law", td_style)],
        [Paragraph("6.0", td_code), Paragraph("<b>Other Architectural Requirements:</b> Database Entities, Room/Firestore Sync, Disaster Recovery", td_style), Paragraph("Data & Storage", td_style)],
        [Paragraph("7.0", td_code), Paragraph("<b>Verification & Traceability:</b> Requirements Traceability Matrix (RTM) & Test Suite Mapping", td_style), Paragraph("Assurance", td_style)],
        [Paragraph("Appendices", td_code), Paragraph("<b>Appendices A to C:</b> Financial Glossary, State Transitions, and Workflow Diagrams", td_style), Paragraph("Reference", td_style)],
    ]
    t_toc = Table(toc_data, colWidths=[55, 400, 77])
    t_toc.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 2.2),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 2.2),
        ('LEFTPADDING', (0, 0), (-1, -1), 4),
        ('RIGHTPADDING', (0, 0), (-1, -1), 4),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
    ]))
    story.append(t_toc)
    story.append(Spacer(1, 5))

    # 1.0 INTRODUCTION
    story.append(Paragraph("1.0 Introduction", h1_style))
    story.append(HRFlowable(width="100%", thickness=0.5, color=c_border, spaceBefore=1, spaceAfter=3))
    story.append(Paragraph("<b>1.1 Purpose:</b> This document specifies all functional and non-functional requirements for the Loanzo Peer-to-Peer Microfinance and Purpose-Bound Social Lending Platform. It serves as the primary technical specification for developers, quality assurance engineers, security auditors, and legal compliance reviewers.", body_style))
    story.append(Spacer(1, 2))
    story.append(Paragraph("<b>1.2 Scope:</b> Loanzo is an Android mobile platform that transforms informal lending into a secure, transparent, and court-enforceable financial architecture. The platform operates on a <b>Non-Custodial Zero-Float Model</b>, maintaining exactly INR 0.00 customer deposits. Funds route directly between verified borrower, lender, and merchant accounts via National Payments Corporation of India (NPCI) Unified Payments Interface (UPI) rails.", body_style))
    story.append(Spacer(1, 2))
    story.append(Paragraph("<b>1.3 Definitions & Acronyms:</b> Key terms conform to standard financial engineering practices in India: <i>LTV</i> (Loan-to-Value), <i>DPD</i> (Days Past Due), <i>VPA</i> (Virtual Payment Address), <i>Penny-Drop</i> (Automated INR 1.00 verification check), <i>Form 34</i> (Vehicle hypothecation endorsement), <i>Order XXXVII</i> (Expedited summary civil lawsuit), <i>Non-Compounding Late Fee</i> (Fair penalty calculated strictly on overdue principal), and <i>Digital E-Stamping</i> (Article 49 revenue stamp procurement).", body_style))
    story.append(Spacer(1, 2))
    story.append(Paragraph("<b>1.4 References & Valid Data Sources:</b> IEEE Std 830-1998; ISO/IEC/IEEE 29148:2018; NSSO MoSPI All-India Debt and Investment Survey (AIDIS, 77th Round); RBI Financial Stability Report (June 2026); RBI Master Direction NBFC-P2P (RBI/2024-25/63 DoR.FIN.REC.35/03.10.124/2024-25); RBI Fair Lending Practice - Penal Charges in Loan Accounts (RBI/2023-24/53 DoR.MCS.REC.28/01.01.001/2023-24); Indian Stamp Act 1899 (Article 49, Section 35); IT Act 2000 Sec 10A/43A/72A; NI Act 1881 Sec 4; CPC 1908 Order XXXVII; BSA 2023 Sec 63; DPDP Act 2023; IBJA / RBI Tiered Gold LTV Framework.", body_style))
    story.append(Spacer(1, 5))

    # 2.0 OVERALL DESCRIPTION
    story.append(Paragraph("2.0 Overall Description", h1_style))
    story.append(HRFlowable(width="100%", thickness=0.5, color=c_border, spaceBefore=1, spaceAfter=3))
    story.append(Paragraph("<b>2.1 Product Perspective:</b> Loanzo operates as an autonomous Android application combining an offline-first Room SQLite persistence layer with Cloud Firestore synchronization. It interfaces with external identity services (DigiLocker), payment networks (NPCI UPI), enterprise cloud storage (Cloud KMS CMEK), digital e-stamping gateways (NeSL), and automated messaging channels (Telegram Bot API).", body_style))
    story.append(Spacer(1, 2))
    story.append(Paragraph("<b>2.2 User Classes and Characteristics:</b>", body_style))
    story.append(Paragraph("<b>* Unified Consumer (Borrower / Lender):</b> End users who originate or fund micro-loans. Can toggle seamlessly between borrowing and lending perspectives.", bullet_style))
    story.append(Paragraph("<b>* Certified Field Verification Agent:</b> Empaneled doorstep verification officers who conduct physical collateral assaying, verify borrower residences, and transfer pledged assets into partner bank lockers.", bullet_style))
    story.append(Paragraph("<b>* System Administrators (Maker-Checker RBAC):</b> Multi-tenant administrative operators enforcing dual-authorization policies for reviewing agent applications, inspecting flagged KYC documents, and arbitrating multi-party grievances.", bullet_style))
    story.append(Spacer(1, 2))
    story.append(Paragraph("<b>2.3 Operating Environment:</b> Android 8.0 (API 26) through Android 15 (API 35); baseline 2 GB RAM; front-facing camera (min 2 MP); biometric sensor (Fingerprint / Face); GPS location provider with mock-location spoofing detection; network connectivity over 2G/3G/4G/5G/Wi-Fi with full offline-first degradation resilience.", body_style))
    story.append(Spacer(1, 2))
    story.append(Paragraph("<b>2.4 Design & Regulatory Constraints:</b> (a) Non-Custodial: Zero float balance holding; (b) Penalty Ceiling: Maximum 2.0% monthly simple non-compounding penalty per RBI/2023-24/53; (c) Mandatory Digital E-Stamping: Required under Article 49 Indian Stamp Act 1899 to cure Section 35 admissibility bar; (d) Data Localization: Local encryption using AES-256-GCM via Android StrongBox KeyStore and remote Cloud KMS CMEK under DPDP Act 2023; (e) Social Lending Cap: Aggregate INR 10 Lakh per lender ceiling.", body_style))
    story.append(PageBreak())

    # =========================================================================
    # PAGE 3: 3.0 EXTERNAL INTERFACES & 4.0 SYSTEM FEATURES (PART 1: FR-01 TO FR-06)
    # =========================================================================
    story.append(Paragraph("3.0 External Interface Requirements", h1_style))
    story.append(HRFlowable(width="100%", thickness=0.5, color=c_border, spaceBefore=1, spaceAfter=3))

    ext_data = [
        [Paragraph("Interface Class", th_style), Paragraph("Protocol / Specification", th_style), Paragraph("Functional Responsibility & Operational Details", th_style)],
        [
            Paragraph("User Interface (UI)", td_code),
            Paragraph("Material Design 3 / Jetpack Compose", td_style),
            Paragraph("Single Activity architecture driving reactive declarative Compose views. Dynamic dark/light theme engine, tactile swipe-to-confirm repayment buttons, and 180-degree credit score speedometer.", td_style)
        ],
        [
            Paragraph("Hardware Interface", td_code),
            Paragraph("BiometricPrompt, CameraX, Mock-GPS Guard", td_style),
            Paragraph("Accesses device biometric sensors for authentication, CameraX + ML Kit for liveness selfie anti-spoofing, and FusedLocationProviderClient with active mock-location spoofing detection.", td_style)
        ],
        [
            Paragraph("Software Interface", td_code),
            Paragraph("Room SQLite, Cloud Firestore, Cloud KMS, NeSL", td_style),
            Paragraph("Room for local offline storage; Firestore for cross-device sync; Cloud Storage with CMEK encryption; NeSL / State E-Stamping API for Article 49 digital stamp certificates; Bank PA webhooks for payment reconciliation.", td_style)
        ],
        [
            Paragraph("Communications", td_code),
            Paragraph("HTTPS TLS 1.3, FCM, SMS Retriever API", td_style),
            Paragraph("Enforces TLS 1.3 with certificate pinning; Firebase Cloud Messaging for urgent alerts; Google Play SMS Retriever API (0 SMS permission) for penny-drop matching; HTTPS Webhooks for Telegram Assistant Bot.", td_style)
        ]
    ]
    t_ext = Table(ext_data, colWidths=[95, 130, 307])
    t_ext.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 2.2),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 2.2),
        ('LEFTPADDING', (0, 0), (-1, -1), 4),
        ('RIGHTPADDING', (0, 0), (-1, -1), 4),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ]))
    story.append(t_ext)
    story.append(Spacer(1, 5))

    story.append(Paragraph("4.0 System Features & Functional Requirements (Part 1)", h1_style))
    story.append(HRFlowable(width="100%", thickness=0.5, color=c_border, spaceBefore=1, spaceAfter=3))
    story.append(Paragraph("Requirements catalog with deterministic triggers, validation logic, outputs, and verifiable acceptance criteria:", body_style))
    story.append(Spacer(1, 3))

    fr_part1_data = [
        [Paragraph("Req ID", th_style), Paragraph("Feature Domain", th_style), Paragraph("Inputs, Detailed Business Logic & Acceptance Criteria", th_style), Paragraph("Priority", th_center)],
        [
            Paragraph("FR-AUTH-01", td_code),
            Paragraph("Universal Identity & Session Security", td_bold),
            Paragraph("<b>Inputs:</b> Username, Email, Phone, Biometrics.<br/>"
                      "<b>Logic:</b> Validates credentials across SHA-256 hashes and BiometricPrompt. Enforces banking session lifecycle: 3-min background grace window, session lock shield, 48-hr hard token purge, and device UID binding.<br/>"
                      "<b>Acceptance Criteria:</b> Launch to interactive state in <= 1.8s; zero session hijacking.", td_style),
            Paragraph("Mandatory", td_center)
        ],
        [
            Paragraph("FR-KYC-02", td_code),
            Paragraph("DigiLocker & Encrypted Vault", td_bold),
            Paragraph("<b>Inputs:</b> Aadhaar number, PAN, camera selfie.<br/>"
                      "<b>Logic:</b> Verifies authentic identity records via DigiLocker. Captures ML Kit liveness selfie. Houses credentials inside an AES-256 encrypted vault gated behind password/biometric challenge. Remote backups protected via Cloud KMS CMEK under DPDP Act.<br/>"
                      "<b>Acceptance Criteria:</b> 100% of KYC credentials encrypted at rest; zero unauthenticated views.", td_style),
            Paragraph("Mandatory", td_center)
        ],
        [
            Paragraph("FR-BANK-03", td_code),
            Paragraph("Penny-Drop & Webhook Reconcile", td_bold),
            Paragraph("<b>Inputs:</b> Recipient Account, IFSC, server PA webhook, SMS Retriever.<br/>"
                      "<b>Logic:</b> Executes automated INR 1.00 test transfer. Reconciles via server-side Payment Aggregator webhooks to prevent client-side intent spoofing. Matches recipient name against KYC record: >=95% auto-verified, 80-94% dual-admin review, <80% freeze. Max 3 retries.<br/>"
                      "<b>Acceptance Criteria:</b> Capital transfer blocked on <80% match; zero dangerous SMS permissions.", td_style),
            Paragraph("Mandatory", td_center)
        ],
        [
            Paragraph("FR-MKT-04", td_code),
            Paragraph("Social Marketplace & Bidding", td_bold),
            Paragraph("<b>Inputs:</b> Loan requests, capital pools, counter-bids.<br/>"
                      "<b>Logic:</b> Categorized public discovery feed (#Medical, #Business, etc.). Community vouches boost trust score. Lenders submit competing interest rate bids; borrowers execute 1-tap agreement finalization.<br/>"
                      "<b>Acceptance Criteria:</b> Competitive bids display side-by-side; real-time Firestore sync.", td_style),
            Paragraph("High", td_center)
        ],
        [
            Paragraph("FR-LOAN-05", td_code),
            Paragraph("Purpose-Bound Tranche Payout", td_bold),
            Paragraph("<b>Inputs:</b> Milestone commercial invoice, seller UPI VPA.<br/>"
                      "<b>Logic:</b> Verifies merchant invoice and past milestone completion. Disburses loan tranches directly to seller UPI VPA. Discrepancies halt release for admin inspection. Zero capital released as unmonitored cash.<br/>"
                      "<b>Acceptance Criteria:</b> 100% of milestone disbursements settle into verified seller VPAs.", td_style),
            Paragraph("Mandatory", td_center)
        ],
        [
            Paragraph("FR-COL-06", td_code),
            Paragraph("Tiered LTV & Insured Custody", td_bold),
            Paragraph("<b>Inputs:</b> Asset category, weight, appraisal proof.<br/>"
                      "<b>Logic:</b> Enforces statutory tiered LTV: Gold (up to 75%-85% on IBJA rates per RBI framework), Property (55%), Vehicles (60%), Equipment (45%). Doorstep agent assays net weight. Pledged transit covered by 100% fidelity insurance with Dual-OTP handover (doorstep to bank vault).<br/>"
                      "<b>Acceptance Criteria:</b> Programmatically blocks loans exceeding LTV; 100% insured transit.", td_style),
            Paragraph("Mandatory", td_center)
        ]
    ]
    t_fr1 = Table(fr_part1_data, colWidths=[52, 100, 320, 60])
    t_fr1.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 2.0),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 2.0),
        ('LEFTPADDING', (0, 0), (-1, -1), 4),
        ('RIGHTPADDING', (0, 0), (-1, -1), 4),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ]))
    story.append(t_fr1)
    story.append(PageBreak())

    # =========================================================================
    # PAGE 4: 4.0 SYSTEM FEATURES (PART 2: FR-07 TO FR-13) & 5.0 NON-FUNCTIONAL
    # =========================================================================
    story.append(Paragraph("4.0 System Features & Functional Requirements (Part 2)", h1_style))
    story.append(HRFlowable(width="100%", thickness=0.5, color=c_border, spaceBefore=1, spaceAfter=3))

    fr_part2_data = [
        [Paragraph("Req ID", th_style), Paragraph("Feature Domain", th_style), Paragraph("Inputs, Detailed Business Logic & Acceptance Criteria", th_style), Paragraph("Priority", th_center)],
        [
            Paragraph("FR-AGR-07", td_code),
            Paragraph("Digital E-Stamping & Sec 4 NI Note", td_bold),
            Paragraph("<b>Inputs:</b> Digital stamp cert, signature canvas, selfie, biometric consent.<br/>"
                      "<b>Logic:</b> Interfaces with NeSL/State e-stamping API to procure digital stamp certificate under Art 49 Indian Stamp Act 1899. Synthesizes Section 4 NI Promissory Note with 3-factor eSign and Section 63 BSA 2023 certificate, curing Section 35 court admissibility bar.<br/>"
                      "<b>Acceptance Criteria:</b> Digital stamp duty affixed; PDF generation in <= 2.5s; SHA-256 verifiable.", td_style),
            Paragraph("Mandatory", td_center)
        ],
        [
            Paragraph("FR-PAY-08", td_code),
            Paragraph("Dynamic QR & Server-Reconciled Pay", td_bold),
            Paragraph("<b>Inputs:</b> Due date, installment amount, prepayment.<br/>"
                      "<b>Logic:</b> Dynamic UPI QR generation (`upi://pay`). Tactile swipe-to-confirm button eliminates accidental debits. Reconciled via server-side Payment Aggregator webhooks before updating loan ledger. Prepayment simulator computes interest saved.<br/>"
                      "<b>Acceptance Criteria:</b> Zero client-side intent spoofing; zero accidental double debits.", td_style),
            Paragraph("Mandatory", td_center)
        ],
        [
            Paragraph("FR-DEL-09", td_code),
            Paragraph("Non-Compounding & Order 37 Dossier", td_bold),
            Paragraph("<b>Inputs:</b> Overdue days count (DPD).<br/>"
                      "<b>Logic:</b> 3-day grace window (INR 0.00 fee). From Day 4, simple daily fee capped at 2.0%/month (RBI/2023-24/53). At DPD > 30, calls guarantor. At DPD > 90, compiles Order 37 CPC summary suit dossier (e-stamped note, BSA 2023 cert) eliminating oral evidence stages.<br/>"
                      "<b>Acceptance Criteria:</b> Late fee never capitalized into principal; Order 37 dossier generated.", td_style),
            Paragraph("Mandatory", td_center)
        ],
        [
            Paragraph("FR-AGT-10", td_code),
            Paragraph("Field Agent Empanelment & Dispatch", td_bold),
            Paragraph("<b>Inputs:</b> Verification experience, Police Clearance (PCC).<br/>"
                      "<b>Logic:</b> Bank-grade empanelment reviewed via Maker-Checker console. Strict role isolation permanently hides consumer borrowing. Geotagged inspections with mock-GPS detection and instant INR 500 to INR 1,500 per-visit compensation crediting.<br/>"
                      "<b>Acceptance Criteria:</b> Unapproved agents blocked from console; 100% geotagged inspections.", td_style),
            Paragraph("High", td_center)
        ],
        [
            Paragraph("FR-ADM-11", td_code),
            Paragraph("Multi-Tenant Maker-Checker Desk", td_bold),
            Paragraph("<b>Inputs:</b> Administrator RBAC credentials.<br/>"
                      "<b>Logic:</b> Multi-tenant admin console with mandatory Maker-Checker dual authorization for agent approvals, KYC overrides, and dispute escrow release, eliminating single points of failure. Virtual dispute mediation calendar with Google Meet links.<br/>"
                      "<b>Acceptance Criteria:</b> Dual-authorization enforced on high-risk actions; real-time dispatch queue.", td_style),
            Paragraph("High", td_center)
        ],
        [
            Paragraph("FR-COM-12", td_code),
            Paragraph("Multilingual & Telegram Assistant", td_bold),
            Paragraph("<b>Inputs:</b> Chat messages, bot commands, language choice.<br/>"
                      "<b>Logic:</b> High-speed LRU translation across 21+ languages (<150ms). 24/7 Telegram Assistant Bot (@Loanzo_bot) dispatches automated EMI reminders and administrative alerts with cryptographic token binding.<br/>"
                      "<b>Acceptance Criteria:</b> Multi-language UI switching in real-time; 100% bot webhook reliability.", td_style),
            Paragraph("High", td_center)
        ],
        [
            Paragraph("FR-AUD-13", td_code),
            Paragraph("Credit Health & Legal NOC Settlement", td_bold),
            Paragraph("<b>Inputs:</b> Repayment history, zero-due balance.<br/>"
                      "<b>Logic:</b> 180-degree speedometer credit gauge (score 0 to 100). Immutable SHA-256 audit trail. Upon INR 0.00 balance, issues official No Objection Certificate (NOC) with digital seal, de-hypothecating collateral within 24 hours.<br/>"
                      "<b>Acceptance Criteria:</b> Official NOC PDF generated with cryptographic verification seal.", td_style),
            Paragraph("Mandatory", td_center)
        ]
    ]
    t_fr2 = Table(fr_part2_data, colWidths=[52, 100, 320, 60])
    t_fr2.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 1.8),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 1.8),
        ('LEFTPADDING', (0, 0), (-1, -1), 4),
        ('RIGHTPADDING', (0, 0), (-1, -1), 4),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ]))
    story.append(t_fr2)
    story.append(Spacer(1, 5))

    # 5.0 NON-FUNCTIONAL REQUIREMENTS
    story.append(Paragraph("5.0 Non-Functional Requirements", h1_style))
    story.append(HRFlowable(width="100%", thickness=0.5, color=c_border, spaceBefore=1, spaceAfter=3))

    nfr_data = [
        [Paragraph("Req ID", th_style), Paragraph("Quality Attribute", th_style), Paragraph("Operational Requirement & Quantitative Threshold", th_style), Paragraph("Verification Standard", th_style)],
        [Paragraph("NFR-PERF-01", td_code), Paragraph("Database Latency", td_bold), Paragraph("Local Room SQLite queries execute within <= 15 ms (95th percentile) on baseline 2 GB RAM Android hardware.", td_style), Paragraph("Macrobenchmark Suite", td_style)],
        [Paragraph("NFR-PERF-02", td_code), Paragraph("Server Concurrency", td_bold), Paragraph("Cloud backend sustains >= 2,500 requests/sec during peak hours with 95th percentile latency < 250 ms.", td_style), Paragraph("Distributed Load Test", td_style)],
        [Paragraph("NFR-SEC-01", td_code), Paragraph("Encryption at Rest", td_bold), Paragraph("Local records encrypted via AES-256-GCM StrongBox; remote storage protected with Cloud KMS CMEK under DPDP Act.", td_style), Paragraph("Key Management Audit", td_style)],
        [Paragraph("NFR-SEC-02", td_code), Paragraph("Encryption in Transit", td_bold), Paragraph("All network communications enforce TLS 1.3 with SHA-256 certificate pinning across all REST endpoints.", td_style), Paragraph("Traffic Interception Test", td_style)],
        [Paragraph("NFR-SAFE-01", td_code), Paragraph("Non-Custodial Float", td_bold), Paragraph("Platform shall maintain exactly INR 0.00 custodial float balance. Zero pooling of funds permitted.", td_style), Paragraph("Ledger Balance Audit", td_style)],
        [Paragraph("NFR-SAFE-02", td_code), Paragraph("Transit Fidelity Cover", td_bold), Paragraph("100% of pledged physical collateral in transit between doorstep and bank vault is covered under fidelity insurance.", td_style), Paragraph("Policy Underwriting Audit", td_style)],
        [Paragraph("NFR-REG-01", td_code), Paragraph("RBI Penalty Compliance", td_bold), Paragraph("Late charges accrue strictly as simple flat fee capped at 2.0%/month; zero capitalization into principal.", td_style), Paragraph("RBI/2023-24/53 Audit", td_style)],
        [Paragraph("NFR-REG-02", td_code), Paragraph("E-Stamping & BSA 2023", td_bold), Paragraph("Digital e-stamping under Art 49 Stamp Act 1899 and electronic certificates under Sec 63 BSA 2023.", td_style), Paragraph("Judicial Admissibility Audit", td_style)]
    ]
    t_nfr = Table(nfr_data, colWidths=[55, 95, 262, 120])
    t_nfr.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 1.6),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 1.6),
        ('LEFTPADDING', (0, 0), (-1, -1), 4),
        ('RIGHTPADDING', (0, 0), (-1, -1), 4),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ]))
    story.append(t_nfr)
    story.append(PageBreak())

    # =========================================================================
    # PAGE 5: 6.0 DATABASE, 7.0 RTM & APPENDIX A (GLOSSARY)
    # =========================================================================
    story.append(Paragraph("6.0 Database Requirements & Operational Architecture", h1_style))
    story.append(HRFlowable(width="100%", thickness=0.5, color=c_border, spaceBefore=1, spaceAfter=3))

    db_data = [
        [Paragraph("Entity Name", th_style), Paragraph("Primary Key & Key Fields", th_style), Paragraph("Relational Role & Operational Purpose", th_style)],
        [Paragraph("UserEntity", td_code), Paragraph("userId, phone, username, kycStatus, registeredDeviceId, telegramChatId", td_style), Paragraph("Stores core profile, role flags (CONSUMER vs AGENT), and biometric registration status.", td_style)],
        [Paragraph("LoanEntity", td_code), Paragraph("loanId, lenderId, borrowerId, guarantorId, principal, interestRate, status", td_style), Paragraph("Tracks the lifecycle of each loan from DRAFT, ESIGNING, ACTIVE, DELINQUENT, to SETTLED.", td_style)],
        [Paragraph("RepaymentEntity", td_code), Paragraph("repaymentId, loanId, installmentNumber, dueDate, paidDate, amount, serverWebhookId", td_style), Paragraph("Maintains EMI schedule, tracking paid installments, due dates, simple late fees, and server webhook IDs.", td_style)],
        [Paragraph("PledgeEntity", td_code), Paragraph("pledgeId, loanId, assetClass, grossWeight, netWeight, insurancePolicyNumber, lockerNumber", td_style), Paragraph("Records physical collateral details, doorstep assaying results, fidelity insurance policy, and bank locker.", td_style)],
        [Paragraph("AgreementEntity", td_code), Paragraph("agreementId, loanId, agreementPdfUrl, eStampCertificateNumber, contractSha256, gpsCoordinates", td_style), Paragraph("Houses digital Promissory Note PDF links, NeSL e-stamp certificate numbers, SHA-256 hashes, and execution geotags.", td_style)],
        [Paragraph("AgentInspectionEntity", td_code), Paragraph("visitId, agentId, loanId, visitType, checkInGps, mockGpsFlag, compensationAmount", td_style), Paragraph("Tracks doorstep verification assignments, GPS check-in proof, mock-GPS flags, and per-visit agent payouts.", td_style)],
        [Paragraph("NocCertificateEntity", td_code), Paragraph("nocId, loanId, zeroDueTimestamp, digitalSignatureHash, certificatePdfPath", td_style), Paragraph("Stores legally binding NOC certificates issued upon INR 0.00 balance, triggering collateral release within 24 hours.", td_style)]
    ]
    t_db = Table(db_data, colWidths=[105, 175, 252])
    t_db.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 1.2),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 1.2),
        ('LEFTPADDING', (0, 0), (-1, -1), 4),
        ('RIGHTPADDING', (0, 0), (-1, -1), 4),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ]))
    story.append(t_db)
    story.append(Spacer(1, 3))

    # 7.0 RTM
    story.append(Paragraph("7.0 Requirements Traceability Matrix (RTM)", h1_style))
    story.append(HRFlowable(width="100%", thickness=0.5, color=c_border, spaceBefore=1, spaceAfter=2))

    rtm_data = [
        [Paragraph("Req ID", th_style), Paragraph("Requirement Summary", th_style), Paragraph("Primary Design Module", th_style), Paragraph("Automated Test ID", th_style), Paragraph("Statutory Basis", th_style)],
        [Paragraph("FR-AUTH-01", td_code), Paragraph("Universal Identifier Login", td_style), Paragraph("AuthViewModel.kt", td_code), Paragraph("TC-AUTH-101", td_code), Paragraph("IT Act Sec. 43A", td_style)],
        [Paragraph("FR-AUTH-04", td_code), Paragraph("Banking Session Lifecycle", td_style), Paragraph("SessionManager.kt", td_code), Paragraph("TC-AUTH-104", td_code), Paragraph("RBI IT Framework", td_style)],
        [Paragraph("FR-KYC-02", td_code), Paragraph("Liveness & Encrypted Vault", td_style), Paragraph("DocumentVaultScreen.kt", td_code), Paragraph("TC-KYC-203", td_code), Paragraph("DPDP Act Sec. 8", td_style)],
        [Paragraph("FR-BANK-03", td_code), Paragraph("Penny-Drop & Webhook Match", td_style), Paragraph("BankVerificationService.kt", td_code), Paragraph("TC-BNK-302", td_code), Paragraph("RBI P2P Directions", td_style)],
        [Paragraph("FR-MKT-04", td_code), Paragraph("Social Competitive Bidding", td_style), Paragraph("MarketplaceViewModel.kt", td_code), Paragraph("TC-MKT-401", td_code), Paragraph("Fair Practices Code", td_style)],
        [Paragraph("FR-LOAN-05", td_code), Paragraph("Milestone Tranche Disbursement", td_style), Paragraph("TrancheDisbursalEngine.kt", td_code), Paragraph("TC-LON-501", td_code), Paragraph("P2P Guidelines", td_style)],
        [Paragraph("FR-COL-06", td_code), Paragraph("Tiered LTV & Insured Custody", td_style), Paragraph("CollateralValuationEngine.kt", td_code), Paragraph("TC-COL-604", td_code), Paragraph("Contract Act Sec. 176", td_style)],
        [Paragraph("FR-AGR-07", td_code), Paragraph("Digital E-Stamping & Promissory", td_style), Paragraph("AgreementGenerator.kt", td_code), Paragraph("TC-AGR-702", td_code), Paragraph("Stamp Act Art. 49", td_style)],
        [Paragraph("FR-PAY-08", td_code), Paragraph("Server Webhook QR Repayment", td_style), Paragraph("PaymentRepository.kt", td_code), Paragraph("TC-PAY-802", td_code), Paragraph("Payment Systems Act", td_style)],
        [Paragraph("FR-DEL-09", td_code), Paragraph("Non-Compounding & Order 37", td_style), Paragraph("PenaltyCalculator.kt", td_code), Paragraph("TC-DEL-902", td_code), Paragraph("RBI/2023-24/53 / CPC", td_style)],
        [Paragraph("FR-AGT-10", td_code), Paragraph("Field Agent Empanelment & PCC", td_style), Paragraph("AgentApplicationScreen.kt", td_code), Paragraph("TC-AGT-1001", td_code), Paragraph("Police Norms", td_style)],
        [Paragraph("FR-ADM-11", td_code), Paragraph("Multi-Tenant Maker-Checker Desk", td_style), Paragraph("AppOwnerVerificationScreen.kt", td_code), Paragraph("TC-ADM-1101", td_code), Paragraph("Corporate Governance", td_style)],
        [Paragraph("FR-AUD-13", td_code), Paragraph("Official NOC & Credit Gauge", td_style), Paragraph("NocGenerator.kt", td_code), Paragraph("TC-AUD-1301", td_code), Paragraph("IT Act 2000 Sec. 10A", td_style)]
    ]
    t_rtm = Table(rtm_data, colWidths=[55, 120, 115, 115, 127])
    t_rtm.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 1.2),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 1.2),
        ('LEFTPADDING', (0, 0), (-1, -1), 4),
        ('RIGHTPADDING', (0, 0), (-1, -1), 4),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ]))
    story.append(t_rtm)
    story.append(Spacer(1, 3))

    # APPENDIX A: GLOSSARY
    story.append(Paragraph("Appendix A: Plain-English Glossary of Starred & Financial Terms", h1_style))
    story.append(HRFlowable(width="100%", thickness=0.5, color=c_border, spaceBefore=1, spaceAfter=2))

    gloss_data = [
        [Paragraph("Term", th_style), Paragraph("Plain-English Definition & Real-World Meaning", th_style)],
        [Paragraph("Tranche Disbursement", td_bold), Paragraph("Releasing a loan in smaller milestone-bound installments (e.g. tools first, materials second) tied to verified seller bills rather than handing over unmonitored cash upfront.", td_style)],
        [Paragraph("Penny-Drop Check", td_bold), Paragraph("Sending a tiny test payment of INR 1.00 to an account to automatically fetch and verify the recipient's real registered bank name before releasing loan capital.", td_style)],
        [Paragraph("Digital E-Stamping", td_bold), Paragraph("Procuring official state revenue stamp certificates electronically (under Article 49 of the Indian Stamp Act 1899) to satisfy Section 35 and ensure court admissibility.", td_style)],
        [Paragraph("Maker-Checker Governance", td_bold), Paragraph("Enterprise dual-authorization rule requiring one administrator to propose and a second independent administrator to approve high-risk actions, preventing single points of failure.", td_style)],
        [Paragraph("UPI VPA", td_bold), Paragraph("Virtual Payment Address (e.g. dealer@bank) -- a unique UPI identifier used to route digital money transfers directly to verified merchants without exposing raw bank details.", td_style)],
        [Paragraph("Tiered LTV Ratio (IBJA)", td_bold), Paragraph("Maximum loan percentage allowed against gold purity closing prices published by IBJA (up to 85% for loans <= INR 2.5L, 80% <= INR 5L, and 75% > INR 5L per RBI framework).", td_style)],
        [Paragraph("Form 34 Hypothecation", td_bold), Paragraph("A formal legal endorsement recorded on a vehicle's official registration certificate (RC Book) noting that the vehicle acts as collateral for the loan.", td_style)],
        [Paragraph("Promissory Note (Sec 4 NI Act)", td_bold), Paragraph("A legally binding document signed by the borrower containing an unconditional written promise to repay a specific sum of money to the lender on a specified date.", td_style)],
        [Paragraph("Simple Late Fee (Non-Compounding)", td_bold), Paragraph("A flat daily fee on overdue installments that can never be added into loan principal, preventing lenders from charging 'interest-on-interest' (debt traps).", td_style)],
        [Paragraph("Order XXXVII (Order 37 CPC)", td_bold), Paragraph("A special fast-track civil court lawsuit in India for debt recovery based on written promissory notes, resolving cases without long 7-year trials.", td_style)],
        [Paragraph("Transit Fidelity Insurance", td_bold), Paragraph("An institutional insurance policy underwriting 100% of pledged physical collateral value during agent transit from the borrower's doorstep to the partner bank locker.", td_style)]
    ]
    t_gloss = Table(gloss_data, colWidths=[130, 402])
    t_gloss.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 1.2),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 1.2),
        ('LEFTPADDING', (0, 0), (-1, -1), 4),
        ('RIGHTPADDING', (0, 0), (-1, -1), 4),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ]))
    story.append(t_gloss)

    doc.build(story, canvasmaker=IEEENumberedCanvas)
    print(f"IEEE SRS PDF compiled successfully: {output_path}")

if __name__ == "__main__":
    out1 = "Loanzo_SRS_IEEE_830.pdf"
    out2 = os.path.join("docs", "Loanzo_SRS_IEEE_830.pdf")
    build_pdf(out1)
    build_pdf(out2)
