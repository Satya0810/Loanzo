import os
import sys
from reportlab.lib import colors
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, HRFlowable, Image, KeepTogether, PageBreak
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
            self.draw_header_footer(num_pages)
            super(ProfessionalNumberedCanvas, self).showPage()
        super(ProfessionalNumberedCanvas, self).save()

    def draw_header_footer(self, page_count):
        self.saveState()
        self.setFont("Helvetica-Bold", 8)
        self.setFillColor(colors.HexColor("#1E3A8A")) # Deep Navy
        
        # Header (pages > 1)
        if self._pageNumber > 1:
            self.drawString(40, 755, "LOANZO PROTOCOL  |  FORMAL UML USE CASE SPECIFICATION & TESTING SUITE")
            self.drawRightString(572, 755, "IEEE Std 830-1998 & OMG UML 2.5")
            self.setStrokeColor(colors.HexColor("#CBD5E1"))
            self.setLineWidth(0.6)
            self.line(40, 748, 572, 748)

        # Footer
        self.setFont("Helvetica", 8)
        self.setFillColor(colors.HexColor("#475569"))
        self.drawString(40, 25, "Confidential  |  Software Engineering, Architecture & Quality Assurance Reference")
        self.drawRightString(572, 25, f"Page {self._pageNumber} of {page_count}")
        self.setStrokeColor(colors.HexColor("#CBD5E1"))
        self.setLineWidth(0.6)
        self.line(40, 35, 572, 35)
        self.restoreState()

def build_pdf(filename="docs/diagrams/Loanzo_UML_Use_Case_Specification_Report.pdf"):
    os.makedirs(os.path.dirname(filename), exist_ok=True)
    doc = SimpleDocTemplate(
        filename,
        pagesize=letter,
        leftMargin=40,
        rightMargin=40,
        topMargin=46,
        bottomMargin=46
    )

    styles = getSampleStyleSheet()

    # Premium Corporate Palette
    c_primary = colors.HexColor("#0F172A")    # Slate 900
    c_brand = colors.HexColor("#1E3A8A")      # Navy 900
    c_teal = colors.HexColor("#0D9488")       # Teal 600
    c_secondary = colors.HexColor("#334155")  # Slate 700
    c_muted = colors.HexColor("#64748B")      # Slate 500
    c_bg_light = colors.HexColor("#F8FAFC")   # Slate 50
    c_header_bg = colors.HexColor("#0F172A")  # Slate 900 for table header
    c_border = colors.HexColor("#E2E8F0")

    # Typography
    h1 = ParagraphStyle('DocH1', fontName='Helvetica-Bold', fontSize=18, leading=22, textColor=c_primary, spaceAfter=4)
    h2 = ParagraphStyle('DocH2', fontName='Helvetica-Bold', fontSize=12, leading=16, textColor=c_brand, spaceBefore=12, spaceAfter=6)
    h3 = ParagraphStyle('DocH3', fontName='Helvetica-Bold', fontSize=10, leading=14, textColor=c_teal, spaceBefore=8, spaceAfter=4)
    body = ParagraphStyle('DocBody', fontName='Helvetica', fontSize=8.5, leading=12, textColor=c_secondary, spaceAfter=4)
    body_bold = ParagraphStyle('DocBodyBold', fontName='Helvetica-Bold', fontSize=8.5, leading=12, textColor=c_primary)
    caption = ParagraphStyle('DocCaption', fontName='Helvetica-Oblique', fontSize=7.5, leading=10, textColor=c_muted, alignment=1)
    
    table_cell = ParagraphStyle('DocCell', fontName='Helvetica', fontSize=7.5, leading=10.5, textColor=c_secondary)
    table_cell_bold = ParagraphStyle('DocCellBold', fontName='Helvetica-Bold', fontSize=7.5, leading=10.5, textColor=c_primary)
    table_cell_head = ParagraphStyle('DocCellHead', fontName='Helvetica-Bold', fontSize=7.5, leading=10.5, textColor=colors.white)
    
    code_text = ParagraphStyle('DocCode', fontName='Courier', fontSize=7, leading=9.5, textColor=colors.HexColor("#0F766E"))

    elements = []

    # Title & Metadata
    elements.append(Paragraph("LOANZO PROTOCOL: FORMAL UML USE CASE SPECIFICATION", h1))
    elements.append(Paragraph("System Architecture, OMG UML 2.5 Models, GeeksforGeeks Formal Specifications & QA Verification Matrix", ParagraphStyle('Sub', fontName='Helvetica', fontSize=10, leading=14, textColor=c_muted)))
    elements.append(Spacer(1, 8))
    elements.append(HRFlowable(width="100%", thickness=1.5, color=c_brand, spaceBefore=2, spaceAfter=10))

    meta_data = [
        [Paragraph("<b>Standard Compliance:</b> IEEE Std 830-1998, OMG UML 2.5", table_cell), Paragraph("<b>Software Domain:</b> FinTech / P2P Micro-lending", table_cell)],
        [Paragraph("<b>Design Methodology:</b> Use-Case-Driven Development (UCDD)", table_cell), Paragraph("<b>QA Framework:</b> Equivalence Partitioning & BVA", table_cell)],
        [Paragraph("<b>Statutory Reference:</b> NI Act 1881 (Sec 4), Order 37 CPC", table_cell), Paragraph("<b>Platform Version:</b> v2.4 Release Candidate", table_cell)]
    ]
    t_meta = Table(meta_data, colWidths=[266, 266])
    t_meta.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), c_bg_light),
        ('BOX', (0,0), (-1,-1), 0.5, c_border),
        ('INNERGRID', (0,0), (-1,-1), 0.5, c_border),
        ('TOPPADDING', (0,0), (-1,-1), 4),
        ('BOTTOMPADDING', (0,0), (-1,-1), 4),
        ('LEFTPADDING', (0,0), (-1,-1), 6),
        ('RIGHTPADDING', (0,0), (-1,-1), 6),
    ]))
    elements.append(t_meta)
    elements.append(Spacer(1, 12))

    # Section 1: Executive Summary & Actor Taxonomy
    elements.append(Paragraph("1. Executive Summary & OMG UML Actor Hierarchy", h2))
    elements.append(Paragraph(
        "This specification establishes the formal software engineering UML Use Case modeling foundation for the Loanzo Peer-to-Peer Micro-lending Platform. "
        "In alignment with <b>GeeksforGeeks (GFG)</b> software development standards, Pressman Software Engineering principles, and IEEE Std 830-1998, "
        "all functional capabilities are partitioned across well-defined system boundaries, strict actor specializations, and OMG UML relationship semantics.",
        body
    ))
    elements.append(Spacer(1, 4))

    actor_table_data = [
        [Paragraph("Actor Name", table_cell_head), Paragraph("Type & Multiplicity", table_cell_head), Paragraph("Role Description", table_cell_head), Paragraph("Interactions", table_cell_head)],
        [
            Paragraph("PlatformMember", table_cell_bold),
            Paragraph("Abstract Primary", table_cell),
            Paragraph("Base entity for authenticated users with registered cryptographic keys.", table_cell),
            Paragraph("Authenticate, Manage Profile, Push Alerts", table_cell)
        ],
        [
            Paragraph("Borrower", table_cell_bold),
            Paragraph("Primary (1..*)", table_cell),
            Paragraph("Specializes PlatformMember. Applies for micro-credit, pledges gold/vehicle collateral.", table_cell),
            Paragraph("Request Loan, Pledge Collateral, e-Sign Note, Pay EMI", table_cell)
        ],
        [
            Paragraph("Lender", table_cell_bold),
            Paragraph("Primary (1..*)", table_cell),
            Paragraph("Specializes PlatformMember. Underwrites bids, provides capital, manages yields.", table_cell),
            Paragraph("Browse Marketplace, Submit Loan Bid, Disburse Funds", table_cell)
        ],
        [
            Paragraph("FieldAgent", table_cell_bold),
            Paragraph("Primary / Custodian", table_cell),
            Paragraph("Verifies physical collateral, issues geo-tagged vault deposit receipt tokens.", table_cell),
            Paragraph("Appraise Bullion, Store Collateral, Register Vault Token", table_cell)
        ],
        [
            Paragraph("PlatformAdmin", table_cell_bold),
            Paragraph("Primary Admin", table_cell),
            Paragraph("Oversees platform liquidity, audit compliance, KYC sanctions, dispute resolution.", table_cell),
            Paragraph("Audit Ledgers, Freeze Delinquent Accounts, Sanction Check", table_cell)
        ],
        [
            Paragraph("NPCI UPI Switch", table_cell_bold),
            Paragraph("External Secondary", table_cell),
            Paragraph("Unified Payments Interface rail for instant P2P disbursements & autopay e-mandates.", table_cell),
            Paragraph("Disburse Capital, Settle Repayments, Handle Chargebacks", table_cell)
        ],
        [
            Paragraph("DigiLocker / NeSL", table_cell_bold),
            Paragraph("External Regulatory", table_cell),
            Paragraph("Government-approved identity repository & National E-Governance Services Information Utility.", table_cell),
            Paragraph("Aadhaar XML, PAN OCR, Legal Debt Registration", table_cell)
        ]
    ]
    t_actors = Table(actor_table_data, colWidths=[90, 85, 205, 152])
    t_actors.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), c_header_bg),
        ('BOX', (0,0), (-1,-1), 0.5, c_border),
        ('INNERGRID', (0,0), (-1,-1), 0.5, c_border),
        ('TOPPADDING', (0,0), (-1,-1), 4),
        ('BOTTOMPADDING', (0,0), (-1,-1), 4),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, c_bg_light]),
    ]))
    elements.append(t_actors)
    elements.append(Spacer(1, 10))

    # Section 2: Visual UML Diagram
    elements.append(Paragraph("2. Master UML Use Case Architecture Diagram", h2))
    img_path = "c:/AndroidProjects/loanzo/docs/diagrams/uml_use_case_diagram.jpg"
    if os.path.exists(img_path):
        elements.append(Image(img_path, width=530, height=298))
        elements.append(Paragraph("Figure 1: OMG UML 2.5 Use Case Diagram for the Loanzo Digital Micro-Lending Platform (Boundary, Actors, Includes, and Extends)", caption))
    else:
        elements.append(Paragraph("[Visual diagram asset located at docs/diagrams/uml_use_case_diagram.jpg]", body))
    elements.append(Spacer(1, 12))

    # Section 3: UML Semantic Relationships
    elements.append(Paragraph("3. Rigorous UML Relationship Semantics (OMG UML 2.5 vs GFG Practice)", h2))
    elements.append(Paragraph(
        "In professional software engineering, misuse of <code>&lt;&lt;include&gt;&gt;</code> and <code>&lt;&lt;extend&gt;&gt;</code> is the #1 design flaw. "
        "Loanzo enforces strict OMG UML 2.5 semantic rules:",
        body
    ))
    
    rules_data = [
        [Paragraph("Relationship Type", table_cell_head), Paragraph("Arrow Direction", table_cell_head), Paragraph("Execution Condition", table_cell_head), Paragraph("Concrete Loanzo Implementation", table_cell_head)],
        [
            Paragraph("<b>&lt;&lt;include&gt;&gt;</b>", table_cell_bold),
            Paragraph("Base Use Case --&gt; Included UC", table_cell),
            Paragraph("<b>Mandatory</b>: Base use case cannot complete without executing included UC.", table_cell),
            Paragraph("<code>UC-05 (Create Listing)</code> unconditionally includes <code>UC-02 (Verify KYC)</code> and <code>UC-03 (Calculate CIBIL)</code>.", table_cell)
        ],
        [
            Paragraph("<b>&lt;&lt;extend&gt;&gt;</b>", table_cell_bold),
            Paragraph("Extension UC --&gt; Base Use Case", table_cell),
            Paragraph("<b>Conditional</b>: Executes only when specific Extension Point condition is met.", table_cell),
            Paragraph("<code>UC-15 (Levy Penalty)</code> extends <code>UC-14 (Pay EMI)</code> at Extension Point: <i>Past Due Date &gt; 3 Business Days</i>.", table_cell)
        ],
        [
            Paragraph("<b>Actor Generalization</b>", table_cell_bold),
            Paragraph("Child Actor --|&gt; Parent Actor", table_cell),
            Paragraph("<b>Inheritance</b>: Child actor inherits all association lines of parent actor.", table_cell),
            Paragraph("<code>Borrower</code> and <code>Lender</code> inherit base authentication and session capabilities from <code>PlatformMember</code>.", table_cell)
        ]
    ]
    t_rules = Table(rules_data, colWidths=[90, 95, 160, 187])
    t_rules.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), c_header_bg),
        ('BOX', (0,0), (-1,-1), 0.5, c_border),
        ('INNERGRID', (0,0), (-1,-1), 0.5, c_border),
        ('TOPPADDING', (0,0), (-1,-1), 4),
        ('BOTTOMPADDING', (0,0), (-1,-1), 4),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, c_bg_light]),
    ]))
    elements.append(t_rules)
    elements.append(Spacer(1, 12))

    # Page Break for Formal Specifications
    elements.append(PageBreak())

    # Section 4: GFG Formal Use Case Specifications
    elements.append(Paragraph("4. Formal Use Case Specifications (GeeksforGeeks & Cockburn Style)", h2))
    elements.append(Paragraph("Representative mission-critical use cases detailed with comprehensive exception and alternative handling.", body))
    elements.append(Spacer(1, 6))

    def make_use_case_table(uc_id, name, actor, desc, pre, trigger, success_steps, alt_steps, post):
        table_content = [
            [Paragraph(f"<b>Use Case ID:</b> {uc_id}", table_cell_bold), Paragraph(f"<b>Use Case Name:</b> {name}", table_cell_bold)],
            [Paragraph(f"<b>Primary Actor:</b> {actor}", table_cell), Paragraph("<b>Priority / Risk:</b> High / Mission Critical", table_cell)],
            [Paragraph(f"<b>Brief Description:</b> {desc}", table_cell), ""],
            [Paragraph(f"<b>Pre-Conditions:</b> {pre}", table_cell), ""],
            [Paragraph(f"<b>Trigger:</b> {trigger}", table_cell), ""],
            [Paragraph("<b>Main Success Scenario:</b><br/>" + "<br/>".join([f"{i+1}. {s}" for i, s in enumerate(success_steps)]), table_cell), ""],
            [Paragraph("<b>Alternative / Exception Flows:</b><br/>" + "<br/>".join([f"&bull; {a}" for a in alt_steps]), table_cell), ""],
            [Paragraph(f"<b>Post-Conditions:</b> {post}", table_cell), ""]
        ]
        t = Table(table_content, colWidths=[150, 382])
        t.setStyle(TableStyle([
            ('SPAN', (0,2), (1,2)),
            ('SPAN', (0,3), (1,3)),
            ('SPAN', (0,4), (1,4)),
            ('SPAN', (0,5), (1,5)),
            ('SPAN', (0,6), (1,6)),
            ('SPAN', (0,7), (1,7)),
            ('BACKGROUND', (0,0), (-1,0), c_bg_light),
            ('BOX', (0,0), (-1,-1), 0.5, c_border),
            ('INNERGRID', (0,0), (-1,-1), 0.5, c_border),
            ('TOPPADDING', (0,0), (-1,-1), 3),
            ('BOTTOMPADDING', (0,0), (-1,-1), 3),
            ('LEFTPADDING', (0,0), (-1,-1), 5),
            ('RIGHTPADDING', (0,0), (-1,-1), 5),
        ]))
        return t

    # UC-05
    t_uc05 = make_use_case_table(
        "UC-05", "Create Peer-to-Peer Micro-Loan Listing", "Borrower",
        "Borrower compiles loan requirements (principal, tenor, interest rate) and publishes a listing to the marketplace.",
        "Borrower is authenticated via Android StrongBox biometric session; Tier-1 KYC status is ACTIVE; CIBIL &gt;= 500.",
        "Borrower taps 'Create Loan Request' on the Marketplace dashboard.",
        [
            "System loads Borrower KYC profile and computes borrowing eligibility ceiling.",
            "Borrower inputs desired loan amount (INR 1,000 - 500,000) and tenor (1 - 36 months).",
            "Borrower selects collateral type (Unsecured, 24K Bullion Gold, or Vehicle Hypothecation).",
            "System invokes &lt;&lt;include&gt;&gt; UC-07 (Calculate Collateral LTV) if collateral is pledged.",
            "Borrower submits listing; System generates cryptographic listing SHA-256 fingerprint.",
            "Listing transitions to STATE: LISTED_PUBLIC."
        ],
        [
            "<b>4a. LTV Exceeded:</b> If pledged collateral value is &lt; 133% of principal (LTV &gt; 75%), system blocks creation and prompts for additional collateral.",
            "<b>2a. Max Active Loans Exceeded:</b> If borrower has &gt;= 3 open loans, system aborts with error code ERR_BORROW_CAP_EXCEEDED."
        ],
        "Listing is persisted in Room SQLite DB, broadcast to active lenders via WebSocket, and timer starts."
    )
    elements.append(t_uc05)
    elements.append(Spacer(1, 10))

    # UC-11
    t_uc11 = make_use_case_table(
        "UC-11", "Execute Statutory Promissory Note & Agreement", "Borrower & Lender",
        "Generates legally enforceable Section 4 Negotiable Instruments Act 1881 promissory note with digital stamp & signature.",
        "Listing is 100% funded by Lender(s); State is ESCROW_RESERVED.",
        "Borrower taps 'Review & e-Sign Loan Contract'.",
        [
            "System invokes LoanzoPdfEngine to render dual-party Section 4 NI Act legal contract with precise amortization schedule.",
            "System invokes &lt;&lt;include&gt;&gt; UC-12 (Affix Digital E-Stamp Token) via NeSL / State Treasury API.",
            "Borrower reviews terms and provides StrongBox Biometric Signature (ECDSA P-256 with user-presence gate).",
            "Lender provides matching cryptographic countersignature.",
            "System generates immutable SHA-256 digest of final PDF and timestamps it.",
            "Loan state transitions to STATE: EXECUTED_DISBURSAL_READY."
        ],
        [
            "<b>3a. Biometric Mismatch / Lockout:</b> StrongBox returns BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT; user must authenticate via hardware device PIN.",
            "<b>2a. E-Stamping Gateway Timeout:</b> System retries via fallback state treasury buffer; queues transaction for offline signing."
        ],
        "Tamper-proof PDF stored in app scoped storage; contract SHA-256 hash registered in NeSL Information Utility."
    )
    elements.append(t_uc11)
    elements.append(Spacer(1, 12))

    # Section 5: Software Testing & Boundary Value Analysis (BVA)
    elements.append(Paragraph("5. Software Testing Derivation & Verification Framework", h2))
    elements.append(Paragraph(
        "In accordance with software engineering testing methodologies (Pressman Ch. 19, GeeksforGeeks BVA Guide), "
        "Use Cases are directly translated into Black-Box Test Cases using <b>Equivalence Partitioning (EP)</b> and <b>Boundary Value Analysis (BVA)</b>.",
        body
    ))
    elements.append(Spacer(1, 4))

    bva_data = [
        [Paragraph("Parameter Under Test", table_cell_head), Paragraph("Valid Range", table_cell_head), Paragraph("Boundaries Evaluated", table_cell_head), Paragraph("Expected System Behavior", table_cell_head), Paragraph("Kotlin Test Case", table_cell_head)],
        [
            Paragraph("Loan Principal (INR)", table_cell_bold),
            Paragraph("1,000 - 500,000", table_cell),
            Paragraph("Min-1: 999<br/>Min: 1,000<br/>Max: 500,000<br/>Max+1: 500,001", table_cell),
            Paragraph("Reject (&lt;1000)<br/>Accept<br/>Accept<br/>Reject (&gt;500k)", table_cell),
            Paragraph("<code>MarketplaceViewModelTest.kt</code><br/>testLoanAmountBoundaries()", code_text)
        ],
        [
            Paragraph("Annual Interest Rate (%)", table_cell_bold),
            Paragraph("6.00% - 36.00%", table_cell),
            Paragraph("Min-1: 5.99%<br/>Min: 6.00%<br/>Max: 36.00%<br/>Max+1: 36.01%", table_cell),
            Paragraph("Reject (Sub-market)<br/>Accept<br/>Accept<br/>Reject (Usury Cap)", table_cell),
            Paragraph("<code>MarketplaceViewModelTest.kt</code><br/>testInterestRateBoundaries()", code_text)
        ],
        [
            Paragraph("Collateral LTV (%)", table_cell_bold),
            Paragraph("&lt;= 75.00%", table_cell),
            Paragraph("Norm: 65.0%<br/>Max: 75.0%<br/>Max+1: 75.1%", table_cell),
            Paragraph("Accept (Normal)<br/>Accept (RBI limit)<br/>Reject (Over-leveraged)", table_cell),
            Paragraph("<code>CollateralVaultTest.kt</code><br/>testLtvCeilingEnforcement()", code_text)
        ],
        [
            Paragraph("Delinquency Grace Period", table_cell_bold),
            Paragraph("3 Calendar Days", table_cell),
            Paragraph("Day 1: Grace<br/>Day 3: Grace End<br/>Day 4: Penalty", table_cell),
            Paragraph("No penalty fee<br/>No penalty fee<br/>Trigger &lt;&lt;extend&gt;&gt; UC-15", table_cell),
            Paragraph("<code>RepaymentScheduleTest.kt</code><br/>testGracePeriodPenalty()", code_text)
        ]
    ]
    t_bva = Table(bva_data, colWidths=[95, 75, 105, 125, 132])
    t_bva.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), c_header_bg),
        ('BOX', (0,0), (-1,-1), 0.5, c_border),
        ('INNERGRID', (0,0), (-1,-1), 0.5, c_border),
        ('TOPPADDING', (0,0), (-1,-1), 3.5),
        ('BOTTOMPADDING', (0,0), (-1,-1), 3.5),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, c_bg_light]),
    ]))
    elements.append(t_bva)
    elements.append(Spacer(1, 10))

    # Section 6: Requirements Traceability Matrix (RTM)
    elements.append(Paragraph("6. Requirements Traceability Matrix (RTM)", h2))
    elements.append(Paragraph("Direct mapping from IEEE Std 830 Functional Requirements to Implementation & Verification assets:", body))
    elements.append(Spacer(1, 4))

    rtm_data = [
        [Paragraph("Req ID", table_cell_head), Paragraph("Functional Requirement", table_cell_head), Paragraph("Use Case", table_cell_head), Paragraph("Code Implementation Artifact", table_cell_head), Paragraph("Verification Test Case", table_cell_head)],
        [
            Paragraph("<b>FR-AUTH</b>", table_cell_bold),
            Paragraph("StrongBox Biometric Authentication", table_cell),
            Paragraph("UC-01", table_cell),
            Paragraph("<code>BiometricAuthManager.kt</code>", code_text),
            Paragraph("<code>AuthViewModelTest.kt</code>", code_text)
        ],
        [
            Paragraph("<b>FR-KYC</b>", table_cell_bold),
            Paragraph("Aadhaar XML & PAN OCR Verification", table_cell),
            Paragraph("UC-02", table_cell),
            Paragraph("<code>KycVerificationEngine.kt</code>", code_text),
            Paragraph("<code>KycEngineTest.kt</code>", code_text)
        ],
        [
            Paragraph("<b>FR-LOAN</b>", table_cell_bold),
            Paragraph("P2P Loan Creation & Auction Bidding", table_cell),
            Paragraph("UC-05, UC-08", table_cell),
            Paragraph("<code>MarketplaceRepository.kt</code>", code_text),
            Paragraph("<code>MarketplaceViewModelTest.kt</code>", code_text)
        ],
        [
            Paragraph("<b>FR-LEGAL</b>", table_cell_bold),
            Paragraph("Section 4 NI Act Promissory PDF & e-Stamp", table_cell),
            Paragraph("UC-11, UC-12", table_cell),
            Paragraph("<code>LoanzoPdfEngine.kt</code>", code_text),
            Paragraph("<code>LoanzoPdfEngineTest.kt</code>", code_text)
        ],
        [
            Paragraph("<b>FR-REPAY</b>", table_cell_bold),
            Paragraph("UPI Autopay & Delinquency Servicing", table_cell),
            Paragraph("UC-14, UC-15", table_cell),
            Paragraph("<code>RepaymentRepository.kt</code>", code_text),
            Paragraph("<code>RepaymentScheduleTest.kt</code>", code_text)
        ],
        [
            Paragraph("<b>FR-RECOVERY</b>", table_cell_bold),
            Paragraph("Order 37 CPC Summary Legal Recovery", table_cell),
            Paragraph("UC-18, UC-19", table_cell),
            Paragraph("<code>DisputeRecoveryEngine.kt</code>", code_text),
            Paragraph("<code>DisputeRecoveryTest.kt</code>", code_text)
        ]
    ]
    t_rtm = Table(rtm_data, colWidths=[55, 140, 50, 145, 142])
    t_rtm.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), c_header_bg),
        ('BOX', (0,0), (-1,-1), 0.5, c_border),
        ('INNERGRID', (0,0), (-1,-1), 0.5, c_border),
        ('TOPPADDING', (0,0), (-1,-1), 3),
        ('BOTTOMPADDING', (0,0), (-1,-1), 3),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, c_bg_light]),
    ]))
    elements.append(t_rtm)

    # Build Document
    doc.build(elements, canvasmaker=ProfessionalNumberedCanvas)
    print(f"Successfully generated {filename}")

if __name__ == "__main__":
    build_pdf()
