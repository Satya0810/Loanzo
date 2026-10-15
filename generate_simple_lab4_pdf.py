import os
from reportlab.lib import colors
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, HRFlowable
)
from reportlab.pdfgen import canvas

class ProfessionalNumberedCanvas(canvas.Canvas):
    def __init__(self, *args, **kwargs):
        super(ProfessionalNumberedCanvas, self).__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_footer(num_pages)
            super(ProfessionalNumberedCanvas, self).showPage()
        super(ProfessionalNumberedCanvas, self).save()

    def draw_footer(self, page_count):
        self.saveState()
        self.setFont("Helvetica", 8)
        self.setFillColor(colors.HexColor("#4B5563"))
        self.drawString(40, 24, "Lab 4: Requirement Elicitation – I  |  Project: Loanzo Protocol")
        self.drawRightString(572, 24, f"Page {self._pageNumber} of {page_count}")
        self.setStrokeColor(colors.HexColor("#D1D5DB"))
        self.setLineWidth(0.5)
        self.line(40, 34, 572, 34)
        self.restoreState()

def build_professional_pdf(filename="Lab4_Requirement_Elicitation_Loanzo.pdf"):
    doc = SimpleDocTemplate(
        filename,
        pagesize=letter,
        leftMargin=40,
        rightMargin=40,
        topMargin=38,
        bottomMargin=44
    )

    styles = getSampleStyleSheet()

    # Enterprise Monochrome Palette
    c_primary = colors.HexColor("#111827")     # Slate 900
    c_secondary = colors.HexColor("#1F2937")   # Slate 800
    c_muted = colors.HexColor("#4B5563")       # Slate 600
    c_header_bg = colors.HexColor("#F3F4F6")   # Light gray header
    c_border = colors.HexColor("#D1D5DB")      # Gray 300
    c_border_light = colors.HexColor("#E5E7EB")# Gray 200

    # Typography
    title_style = ParagraphStyle('DocTitle', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=15, leading=19, textColor=c_primary)
    subtitle_style = ParagraphStyle('DocSubtitle', parent=styles['Normal'], fontName='Helvetica', fontSize=9, leading=12, textColor=c_muted)
    
    h1_style = ParagraphStyle('H1', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=10.5, leading=13.5, textColor=c_primary, spaceBefore=8, spaceAfter=4)
    
    body_style = ParagraphStyle('Body', parent=styles['Normal'], fontName='Helvetica', fontSize=7.8, leading=10.8, textColor=c_secondary)
    
    th_style = ParagraphStyle('TH', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=7.2, leading=9.2, textColor=c_primary)
    td_style = ParagraphStyle('TD', parent=styles['Normal'], fontName='Helvetica', fontSize=7, leading=9, textColor=c_secondary)
    td_bold = ParagraphStyle('TDB', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=7, leading=9, textColor=c_primary)
    td_code = ParagraphStyle('TDCode', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=6.8, leading=8.8, textColor=c_primary)
    td_center = ParagraphStyle('TDC', parent=styles['Normal'], fontName='Helvetica', fontSize=7, leading=9, textColor=c_secondary, alignment=1)

    story = []

    # =========================================================================
    # DOCUMENT HEADER
    # =========================================================================
    story.append(Paragraph("Lab 4: Requirement Elicitation – I", title_style))
    story.append(Spacer(1, 2))
    story.append(Paragraph("System Specification: LOANZO (Peer-to-Peer Microfinance and Purpose-Bound Social Lending Platform)", subtitle_style))
    story.append(Spacer(1, 4))
    story.append(HRFlowable(width="100%", thickness=1, color=c_primary, spaceBefore=2, spaceAfter=6))

    # Executive Overview
    story.append(Paragraph("<b>Activities Conducted:</b>", h1_style))
    story.append(Paragraph("1. Identification and Classification of Stakeholders<br/>2. Elicitation of Functional Requirements<br/>3. Elicitation of Non-Functional Requirements", body_style))
    story.append(Spacer(1, 4))
    story.append(Paragraph("<b>Document Outcome:</b>", h1_style))
    story.append(Paragraph("Preliminary Requirements Specification and Prioritization Matrix", body_style))
    story.append(Spacer(1, 6))

    # =========================================================================
    # 1. IDENTIFY STAKEHOLDERS
    # =========================================================================
    story.append(Paragraph("1. Identification and Classification of Stakeholders", h1_style))
    story.append(Paragraph(
        "Stakeholders are categorized based on their operational boundaries, system interactions, and governance responsibilities within the lending ecosystem.",
        body_style
    ))
    story.append(Spacer(1, 4))

    sh_data = [
        [
            Paragraph("Classification", th_style),
            Paragraph("Stakeholder Group", th_style),
            Paragraph("Operational Role and System Boundary", th_style),
            Paragraph("Core Governance and Technical Expectations", th_style)
        ],
        [
            Paragraph("Direct End User", td_bold),
            Paragraph("Borrowers (Micro-Enterprises and Consumers)", td_style),
            Paragraph("Submits loan applications, verifies identity, registers collateral, reviews competitive offers, executes legal promissory notes, and conducts installment repayments.", td_style),
            Paragraph("Transparent reducing-balance interest, complete elimination of advance processing fees, flexible tenure alignment, non-coercive collections, and accessible vernacular navigation.", td_style)
        ],
        [
            Paragraph("Direct End User", td_bold),
            Paragraph("Private Capital Providers (Retail Lenders)", td_style),
            Paragraph("Underwrites micro-loans, submits competitive bids within the marketplace, evaluates counterparty risk profiles, and monitors repayment performance.", td_style),
            Paragraph("Verified counterparty identities, legally binding Promissory Notes compliant with Section 4 of the Negotiable Instruments Act, 1881, collateral backing, and default mitigation.", td_style)
        ],
        [
            Paragraph("Field Operations", td_bold),
            Paragraph("Certified Field Verification Officers", td_style),
            Paragraph("Independent ground personnel conducting doorstep applicant verification, residential audits, and physical collateral appraisal.", td_style),
            Paragraph("Strict privilege isolation (prohibited from borrowing or lending), structured digital inspection checklists, verified dispatch radiuses, and automated per-visit compensation.", td_style)
        ],
        [
            Paragraph("Commercial Partner", td_bold),
            Paragraph("Verified Commercial Suppliers and Merchants", td_style),
            Paragraph("Commercial entities (equipment dealers, healthcare facilities, education providers) receiving direct tranche settlements for approved expenditure.", td_style),
            Paragraph("Immediate settlement reconciliation, elimination of chargeback exposure, and automated invoice verification via account validation protocols.", td_style)
        ],
        [
            Paragraph("System Administration", td_bold),
            Paragraph("Platform Operations and Compliance Management", td_style),
            Paragraph("Platform operators supervising operational integrity, reviewing field officer accreditation, monitoring systemic risk, and handling formal dispute resolution.", td_style),
            Paragraph("Immutable audit trails, automated delinquency alerting, real-time risk assessment telemetry, and strict adherence to statutory lending guidelines.", td_style)
        ],
        [
            Paragraph("Regulatory Authority", td_bold),
            Paragraph("Financial and Payment Regulators (RBI, NPCI)", td_style),
            Paragraph("Statutory entities enforcing regulatory frameworks concerning digital lending, payment clearing systems, and consumer data protection.", td_style),
            Paragraph("Non-custodial fund architecture (zero customer capital pooling), non-compounding penal rate caps (RBI/2023-24/53), and full adherence to data localization mandates.", td_style)
        ]
    ]

    sh_table = Table(sh_data, colWidths=[80, 105, 180, 167])
    sh_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 3),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 3),
        ('LEFTPADDING', (0, 0), (-1, -1), 4),
        ('RIGHTPADDING', (0, 0), (-1, -1), 4),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ]))
    story.append(sh_table)
    story.append(Spacer(1, 10))

    # =========================================================================
    # 2. GATHER FUNCTIONAL REQUIREMENTS
    # =========================================================================
    story.append(Paragraph("2. Elicitation of Functional Requirements", h1_style))
    story.append(Paragraph(
        "Functional requirements detail the specific business workflows, automated logic, inputs, and operational outputs governing the system.",
        body_style
    ))
    story.append(Spacer(1, 4))

    fr_data = [
        [
            Paragraph("Identifier", th_style),
            Paragraph("Functional Domain", th_style),
            Paragraph("Requirement Specification and Operational Logic", th_style)
        ],
        [
            Paragraph("FR-01", td_code),
            Paragraph("Identity & Access Management", td_bold),
            Paragraph("The system shall authenticate users via multi-factor credentials, including telephone one-time passwords (OTP), OAuth integrations, and biometric authentication (fingerprint and facial recognition) backed by hardware cryptographic keystores.", td_style)
        ],
        [
            Paragraph("FR-02", td_code),
            Paragraph("Digital Identity Verification", td_bold),
            Paragraph("The system shall interface with DigiLocker to extract verified Aadhaar and Permanent Account Number (PAN) records and execute on-device facial liveness verification via computer vision algorithms.", td_style)
        ],
        [
            Paragraph("FR-03", td_code),
            Paragraph("Cryptographic Data Vault", td_bold),
            Paragraph("The system shall maintain an AES-256 encrypted local repository for sensitive identity documentation, restricting decryption strictly to authenticated sessions verified via biometrics or master credentials.", td_style)
        ],
        [
            Paragraph("FR-04", td_code),
            Paragraph("Role-Based Privilege Isolation", td_bold),
            Paragraph("The system shall enforce strict privilege separation between retail users and field verification officers, explicitly prohibiting verification personnel from engaging in borrowing or capital-lending operations.", td_style)
        ],
        [
            Paragraph("FR-05", td_code),
            Paragraph("Bilateral Loan Origination", td_bold),
            Paragraph("The system shall support bidirectional transaction origination, permitting borrowers to publish structured loan requisitions and lenders to extend direct credit proposals.", td_style)
        ],
        [
            Paragraph("FR-06", td_code),
            Paragraph("Social Marketplace Bidding", td_bold),
            Paragraph("The system shall provide a competitive marketplace allowing verified lenders to submit competing bids on interest rates (Annual Percentage Rate) and repayment tenures.", td_style)
        ],
        [
            Paragraph("FR-07", td_code),
            Paragraph("Amortization Simulator", td_bold),
            Paragraph("The system shall dynamically calculate reducing-balance, simple, and flat interest amortization schedules with visual breakdowns of principal and interest components.", td_style)
        ],
        [
            Paragraph("FR-08", td_code),
            Paragraph("Milestone Tranche Allocation", td_bold),
            Paragraph("The system shall enforce milestone-based capital disbursement, disallowing unmonitored lump-sum disbursements to mitigate capital diversion risks.", td_style)
        ],
        [
            Paragraph("FR-09", td_code),
            Paragraph("Direct Payee Settlement", td_bold),
            Paragraph("The system shall route approved capital disbursements directly to verified commercial vendor accounts via Unified Payments Interface (UPI) upon submission of verified tax invoices.", td_style)
        ],
        [
            Paragraph("FR-10", td_code),
            Paragraph("Penny-Drop Account Verification", td_bold),
            Paragraph("The system shall execute an automated INR 1.00 penny-drop verification routine to ensure beneficiary bank account holder details precisely match the legal vendor identity specified on commercial invoices.", td_style)
        ],
        [
            Paragraph("FR-11", td_code),
            Paragraph("Collateral Asset Registry", td_bold),
            Paragraph("The system shall record, track, and appraise five collateral classes: Precious Bullion (maximum 75% LTV), Real Estate Deeds, Commercial IT Equipment, Motor Vehicles, and Accounts Receivable.", td_style)
        ],
        [
            Paragraph("FR-12", td_code),
            Paragraph("Ground Verification Dispatch", td_bold),
            Paragraph("The system shall dispatch certified field verification officers to capture timestamped, geofenced photographic records of physical assets and business premises.", td_style)
        ],
        [
            Paragraph("FR-13", td_code),
            Paragraph("Dual-Appraisal Consensus", td_bold),
            Paragraph("The system shall enforce independent multi-party inspection protocols for collateral valuations exceeding INR 100,000 to prevent valuation fraud and collusion.", td_style)
        ],
        [
            Paragraph("FR-14", td_code),
            Paragraph("Statutory Contract Synthesis", td_bold),
            Paragraph("The system shall automatically generate legally enforceable Promissory Notes compliant with Section 4 of the Negotiable Instruments Act, 1881, alongside a standardized Key Fact Statement (KFS).", td_style)
        ],
        [
            Paragraph("FR-15", td_code),
            Paragraph("Cryptographic Contract Execution", td_bold),
            Paragraph("The system shall facilitate dual-party digital execution utilizing Aadhaar-based electronic signatures and embed SHA-256 cryptographic audit digests for legal admissibility under Section 65B of the Indian Evidence Act.", td_style)
        ],
        [
            Paragraph("FR-16", td_code),
            Paragraph("Automated UPI Collections", td_bold),
            Paragraph("The system shall generate dynamic UPI QR payloads and deep links (upi://pay) for installment reconciliation and automated ledger updates.", td_style)
        ],
        [
            Paragraph("FR-17", td_code),
            Paragraph("Fair Lending Penal Engine", td_bold),
            Paragraph("The system shall calculate non-compounding daily overdue charges strictly capped at a 2% monthly ceiling in compliance with regulatory fair lending directives.", td_style)
        ],
        [
            Paragraph("FR-18", td_code),
            Paragraph("Distress Restructuring & Settlement", td_bold),
            Paragraph("The system shall record mutually agreed tenure extensions during verified borrower distress and issue digitally signed No Objection Certificates (NOC) upon complete reconciliation.", td_style)
        ],
        [
            Paragraph("FR-19", td_code),
            Paragraph("Conversational AI Advisory", td_bold),
            Paragraph("The system shall incorporate speculative query orchestration across localized language models to deliver vernacular financial explanations.", td_style)
        ],
        [
            Paragraph("FR-20", td_code),
            Paragraph("Automated Event Notification", td_bold),
            Paragraph("The system shall deliver asynchronous lifecycle event notifications, repayment schedules, and dispute notices via secured messaging webhooks and push notification channels.", td_style)
        ]
    ]

    fr_table = Table(fr_data, colWidths=[45, 125, 362])
    fr_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 2.5),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 2.5),
        ('LEFTPADDING', (0, 0), (-1, -1), 4),
        ('RIGHTPADDING', (0, 0), (-1, -1), 4),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ]))
    story.append(fr_table)
    story.append(Spacer(1, 10))

    # =========================================================================
    # 3. GATHER NON-FUNCTIONAL REQUIREMENTS
    # =========================================================================
    story.append(Paragraph("3. Elicitation of Non-Functional Requirements", h1_style))
    story.append(Paragraph(
        "Non-functional requirements quantify quality attributes, performance thresholds, architectural constraints, and statutory boundaries.",
        body_style
    ))
    story.append(Spacer(1, 4))

    nfr_data = [
        [
            Paragraph("Identifier", th_style),
            Paragraph("Classification", th_style),
            Paragraph("Specification and Quantitative Benchmark", th_style),
            Paragraph("Architectural Control Mechanism", th_style)
        ],
        [
            Paragraph("NFR-01", td_code),
            Paragraph("Performance", td_bold),
            Paragraph("Local database query execution shall not exceed 15 milliseconds on baseline Android devices (2GB RAM).", td_style),
            Paragraph("Normalized Room SQLite architecture with compiled index DAOs.", td_style)
        ],
        [
            Paragraph("NFR-02", td_code),
            Paragraph("Performance", td_bold),
            Paragraph("Cold application startup to an interactive dashboard state shall execute in under 1.8 seconds with zero visual flicker.", td_style),
            Paragraph("Pre-warmed session initialization evaluating security gates asynchronously.", td_style)
        ],
        [
            Paragraph("NFR-03", td_code),
            Paragraph("Performance", td_bold),
            Paragraph("Conversational advisory first-token response latency shall remain under 1,200 milliseconds over mobile networks.", td_style),
            Paragraph("Speculative parallel query routing with local heuristic fallback.", td_style)
        ],
        [
            Paragraph("NFR-04", td_code),
            Paragraph("Security", td_bold),
            Paragraph("All stored personal data, KYC documents, and local database records shall be encrypted at rest using AES-256-GCM.", td_style),
            Paragraph("Hardware-backed Android KeyStore / StrongBox implementation.", td_style)
        ],
        [
            Paragraph("NFR-05", td_code),
            Paragraph("Security", td_bold),
            Paragraph("All network communications shall be transmitted over TLS 1.3 with mandatory certificate pinning.", td_style),
            Paragraph("OkHttp network interceptor enforcing SHA-256 public key hashes.", td_style)
        ],
        [
            Paragraph("NFR-06", td_code),
            Paragraph("Privacy", td_bold),
            Paragraph("The application shall refrain from accessing contact registries or device media files, adhering strictly to the DPDP Act, 2023.", td_style),
            Paragraph("Minimal permission architecture with isolated application sandboxing.", td_style)
        ],
        [
            Paragraph("NFR-07", td_code),
            Paragraph("Reliability", td_bold),
            Paragraph("Core informational and calculation features shall remain fully operational in zero-connectivity environments.", td_style),
            Paragraph("Local persistence layer with transactional background delta synchronization.", td_style)
        ],
        [
            Paragraph("NFR-08", td_code),
            Paragraph("Availability", td_bold),
            Paragraph("Cloud synchronization interfaces and webhook endpoints shall maintain 99.9% operational availability.", td_style),
            Paragraph("Geographically distributed serverless infrastructure.", td_style)
        ],
        [
            Paragraph("NFR-09", td_code),
            Paragraph("Usability", td_bold),
            Paragraph("The user interface shall support runtime translation across 21 regional languages with transition latencies below 150 milliseconds.", td_style),
            Paragraph("In-memory LRU translation caching.", td_style)
        ],
        [
            Paragraph("NFR-10", td_code),
            Paragraph("Usability", td_bold),
            Paragraph("Critical financial actions shall incorporate deliberate confirmation controls to eliminate unintended execution.", td_style),
            Paragraph("Bilateral draggable confirmation interfaces with haptic feedback.", td_style)
        ],
        [
            Paragraph("NFR-11", td_code),
            Paragraph("Compliance", td_bold),
            Paragraph("The platform shall maintain zero custodial deposit balances, executing 100% of transactions through direct account settlements.", td_style),
            Paragraph("Peer-to-peer settlement architecture compliant with central banking regulations.", td_style)
        ],
        [
            Paragraph("NFR-12", td_code),
            Paragraph("Compliance", td_bold),
            Paragraph("All synthesized digital contracts shall be structured to support summary recovery proceedings under Order XXXVII CPC.", td_style),
            Paragraph("Statutory promissory note formatting and cryptographic timestamping.", td_style)
        ]
    ]

    nfr_table = Table(nfr_data, colWidths=[45, 75, 232, 180])
    nfr_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 2.5),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 2.5),
        ('LEFTPADDING', (0, 0), (-1, -1), 4),
        ('RIGHTPADDING', (0, 0), (-1, -1), 4),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ]))
    story.append(nfr_table)
    story.append(Spacer(1, 10))

    # =========================================================================
    # OUTCOME: PRELIMINARY REQUIREMENTS LIST
    # =========================================================================
    story.append(Paragraph("Outcome: Preliminary Requirements Specification", h1_style))
    story.append(Paragraph(
        "Consolidated and prioritized baseline requirements cataloged using the formal MoSCoW prioritization model.",
        body_style
    ))
    story.append(Spacer(1, 4))

    prl_data = [
        [
            Paragraph("Identifier", th_style),
            Paragraph("Requirement Summary", th_style),
            Paragraph("Type", th_style),
            Paragraph("Target Stakeholder", th_style),
            Paragraph("Priority (MoSCoW)", th_style)
        ],
        [Paragraph("PRL-01", td_code), Paragraph("Multi-Factor & Biometric User Authentication", td_style), Paragraph("FR", td_center), Paragraph("Borrower, Capital Provider", td_style), Paragraph("Must Have", td_bold)],
        [Paragraph("PRL-02", td_code), Paragraph("DigiLocker KYC & On-Device Facial Liveness Detection", td_style), Paragraph("FR", td_center), Paragraph("Borrower, Operations", td_style), Paragraph("Must Have", td_bold)],
        [Paragraph("PRL-03", td_code), Paragraph("AES-256 Hardware Encrypted Local Document Vault", td_style), Paragraph("NFR", td_center), Paragraph("All Users", td_style), Paragraph("Must Have", td_bold)],
        [Paragraph("PRL-04", td_code), Paragraph("Social P2P Reverse-Auction Bidding Marketplace", td_style), Paragraph("FR", td_center), Paragraph("Borrower, Capital Provider", td_style), Paragraph("Must Have", td_bold)],
        [Paragraph("PRL-05", td_code), Paragraph("Purpose-Bound Direct Merchant Disbursal via UPI", td_style), Paragraph("FR", td_center), Paragraph("Borrower, Commercial Supplier", td_style), Paragraph("Must Have", td_bold)],
        [Paragraph("PRL-06", td_code), Paragraph("INR 1.00 Penny-Drop Account Name Verification", td_style), Paragraph("FR", td_center), Paragraph("Commercial Supplier, Capital Provider", td_style), Paragraph("Must Have", td_bold)],
        [Paragraph("PRL-07", td_code), Paragraph("Section 4 NI Act Digital Promissory Note Synthesis", td_style), Paragraph("FR", td_center), Paragraph("Borrower, Capital Provider", td_style), Paragraph("Must Have", td_bold)],
        [Paragraph("PRL-08", td_code), Paragraph("Aadhaar OTP e-Signing with SHA-256 Tamper Audit Trail", td_style), Paragraph("FR", td_center), Paragraph("Borrower, Capital Provider", td_style), Paragraph("Must Have", td_bold)],
        [Paragraph("PRL-09", td_code), Paragraph("Dynamic UPI QR & Deep-Link Repayment Settlements", td_style), Paragraph("FR", td_center), Paragraph("Borrower, Capital Provider", td_style), Paragraph("Must Have", td_bold)],
        [Paragraph("PRL-10", td_code), Paragraph("Non-Compounding Statutory Penal Engine (2% Cap)", td_style), Paragraph("FR/NFR", td_center), Paragraph("Borrower, Regulatory Authority", td_style), Paragraph("Must Have", td_bold)],
        [Paragraph("PRL-11", td_code), Paragraph("Doorstep Field Officer Dispatch & Physical Verification Desk", td_style), Paragraph("FR", td_center), Paragraph("Field Operations, Operations", td_style), Paragraph("Should Have", td_style)],
        [Paragraph("PRL-12", td_code), Paragraph("Multi-Asset Collateral Escrow Tracking (5 Categories)", td_style), Paragraph("FR", td_center), Paragraph("Capital Provider, Borrower", td_style), Paragraph("Should Have", td_style)],
        [Paragraph("PRL-13", td_code), Paragraph("Offline-First Room SQLite Architecture with WorkManager", td_style), Paragraph("NFR", td_center), Paragraph("All Users", td_style), Paragraph("Should Have", td_style)],
        [Paragraph("PRL-14", td_code), Paragraph("Multi-Model Speculative AI Assistant (Gemini / SambaNova)", td_style), Paragraph("FR", td_center), Paragraph("Borrower", td_style), Paragraph("Should Have", td_style)],
        [Paragraph("PRL-15", td_code), Paragraph("Automated Telegram Alert Desk (@Loanzo_bot) & FCM Push", td_style), Paragraph("FR", td_center), Paragraph("All Users, Operations", td_style), Paragraph("Should Have", td_style)],
        [Paragraph("PRL-16", td_code), Paragraph("Runtime Language Switcher for 21+ Regional Languages", td_style), Paragraph("NFR", td_center), Paragraph("Borrower", td_style), Paragraph("Should Have", td_style)],
        [Paragraph("PRL-17", td_code), Paragraph("Dual-Agent Consensus for Assets Exceeding INR 100,000", td_style), Paragraph("FR", td_center), Paragraph("Field Operations, Capital Provider", td_style), Paragraph("Could Have", td_style)],
        [Paragraph("PRL-18", td_code), Paragraph("Prepayment & Foreclosure Savings Simulation Tool", td_style), Paragraph("FR", td_center), Paragraph("Borrower", td_style), Paragraph("Could Have", td_style)],
        [Paragraph("PRL-19", td_code), Paragraph("Alternative Digital Credit Dossier Generation", td_style), Paragraph("FR", td_center), Paragraph("Borrower, Commercial Bank", td_style), Paragraph("Could Have", td_style)],
        [Paragraph("PRL-20", td_code), Paragraph("Automated Blockchain Smart Contract Mirroring", td_style), Paragraph("FR", td_center), Paragraph("External Auditors", td_style), Paragraph("Deferred (Phase 2)", td_style)]
    ]

    prl_table = Table(prl_data, colWidths=[45, 237, 35, 115, 100])
    prl_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 2),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 2),
        ('LEFTPADDING', (0, 0), (-1, -1), 4),
        ('RIGHTPADDING', (0, 0), (-1, -1), 4),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
    ]))
    story.append(prl_table)

    doc.build(story, canvasmaker=ProfessionalNumberedCanvas)
    print(f"Professional PDF successfully built: {filename}")

if __name__ == "__main__":
    out_pdf = "Lab4_Requirement_Elicitation_Loanzo.pdf"
    build_professional_pdf(out_pdf)
    if os.path.exists("docs"):
        docs_pdf = os.path.join("docs", "Lab4_Requirement_Elicitation_Loanzo.pdf")
        build_professional_pdf(docs_pdf)
