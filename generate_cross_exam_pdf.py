import os
import sys
from reportlab.lib import colors
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether, HRFlowable
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
            self.draw_page_decorations(num_pages)
            super(NumberedCanvas, self).showPage()
        super(NumberedCanvas, self).save()

    def draw_page_decorations(self, page_count):
        if self._pageNumber == 1:
            return  # Suppress on cover page

        self.saveState()
        self.setFont("Helvetica", 8)
        self.setFillColor(colors.HexColor("#64748B"))

        # Running Header
        self.drawString(40, 755, "LOANZO | Legal, Regulatory, Forensic & Compliance Dossier (Plain-English Edition)")
        self.setStrokeColor(colors.HexColor("#CBD5E1"))
        self.setLineWidth(0.5)
        self.line(40, 748, 572, 748)

        # Running Footer
        self.line(40, 42, 572, 42)
        self.drawString(40, 30, "PUBLIC & REGULATORY EDITION — WITH INLINE PLAIN-ENGLISH DEFINITIONS")
        page_str = f"Page {self._pageNumber} of {page_count}"
        self.drawRightString(572, 30, page_str)
        self.restoreState()

def build_pdf(filename):
    doc = SimpleDocTemplate(
        filename,
        pagesize=letter,
        leftMargin=40,
        rightMargin=40,
        topMargin=54,
        bottomMargin=54
    )

    styles = getSampleStyleSheet()

    c_navy = colors.HexColor("#0A1628")
    c_slate = colors.HexColor("#1E293B")
    c_gold = colors.HexColor("#D97706")
    c_red = colors.HexColor("#B91C1C")
    c_blue = colors.HexColor("#1D4ED8")
    c_green = colors.HexColor("#047857")

    title_style = ParagraphStyle(
        'CoverTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=23,
        leading=29,
        textColor=c_navy,
        spaceAfter=10
    )

    subtitle_style = ParagraphStyle(
        'CoverSubtitle',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=10.5,
        leading=15,
        textColor=colors.HexColor("#475569"),
        spaceAfter=16
    )

    meta_style = ParagraphStyle(
        'CoverMeta',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=8.5,
        leading=12,
        textColor=colors.HexColor("#0284C7")
    )

    cat_title_style = ParagraphStyle(
        'CatTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=12,
        leading=16,
        textColor=colors.white
    )

    q_title_style = ParagraphStyle(
        'QuestionTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=9.5,
        leading=13,
        textColor=c_navy,
        spaceBefore=7,
        spaceAfter=3,
        keepWithNext=True
    )

    challenge_style = ParagraphStyle(
        'ChallengeText',
        parent=styles['Normal'],
        fontName='Helvetica-Oblique',
        fontSize=8.2,
        leading=11.2,
        textColor=c_red,
        spaceAfter=3,
        keepWithNext=True
    )

    bullet_style = ParagraphStyle(
        'BulletText',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=8.2,
        leading=11.5,
        textColor=c_slate,
        leftIndent=10,
        spaceAfter=2.5
    )

    gloss_term = ParagraphStyle(
        'GlossTerm',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=7.5,
        leading=9.5,
        textColor=c_navy
    )

    gloss_def = ParagraphStyle(
        'GlossDef',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=7,
        leading=9,
        textColor=colors.HexColor("#334155")
    )

    table_header_style = ParagraphStyle(
        'TH',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=7.5,
        leading=9.5,
        textColor=colors.white
    )

    table_cell_style = ParagraphStyle(
        'TC',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=7,
        leading=9,
        textColor=c_slate
    )

    story = []

    # ─────────────────────────────────────────────────────────────
    # COVER PAGE
    # ─────────────────────────────────────────────────────────────
    story.append(Spacer(1, 10))
    tag_data = [[
        Paragraph("<b>EXECUTIVE REGULATORY &amp; PUBLIC COMPREHENSION DOSSIER</b>", ParagraphStyle('Tag', fontName='Helvetica-Bold', fontSize=8, textColor=c_gold)),
        Paragraph("<b>PLAIN-ENGLISH EXPLAINER EDITION (75 Q&amp;As)</b>", ParagraphStyle('TagR', fontName='Helvetica-Bold', fontSize=8, textColor=colors.HexColor("#64748B"), alignment=2))
    ]]
    tag_table = Table(tag_data, colWidths=[280, 252])
    tag_table.setStyle(TableStyle([
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
        ('BOTTOMPADDING', (0,0), (-1,-1), 0),
        ('TOPPADDING', (0,0), (-1,-1), 0),
    ]))
    story.append(tag_table)
    story.append(Spacer(1, 12))

    story.append(Paragraph("LOANZO MASTER LEGAL &amp; REGULATORY COMPLIANCE DOSSIER", title_style))
    story.append(Paragraph(
        "75 In-Depth Forensic, Statutory, Criminal &amp; Architectural Inquiries with "
        "<b>Inline Plain-English Explanations</b> for General Public, Borrowers, Lenders, Regulators &amp; Judicial Courts.",
        subtitle_style
    ))
    story.append(HRFlowable(width="100%", thickness=2, color=c_gold, spaceBefore=2, spaceAfter=10))

    # Executive Overview Box
    exec_text = (
        "<b>PURPOSE OF THIS PLAIN-ENGLISH EDITION:</b> Financial laws and tech architectures use dense jargon. "
        "To ensure complete transparency and accessibility for the general public, this edition embeds <b>inline explanations "
        "in simple, everyday language</b> for every technical concept—such as <i>Escrow, FLDG, Benami, Usury, Bailment, "
        "Sec 138 Cheque Bounce, Moratorium, Pari Passu, and DigiLocker XML</i>. It proves that Loanzo is not just legally bulletproof, "
        "but also honest, transparent, and easy to understand for every Indian citizen."
    )
    exec_box = Table([[Paragraph(exec_text, ParagraphStyle('Exec', fontName='Helvetica', fontSize=8, leading=11.5, textColor=c_slate))]], colWidths=[532])
    exec_box.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor("#F1F5F9")),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor("#CBD5E1")),
        ('LEFTPADDING', (0,0), (-1,-1), 10),
        ('RIGHTPADDING', (0,0), (-1,-1), 10),
        ('TOPPADDING', (0,0), (-1,-1), 8),
        ('BOTTOMPADDING', (0,0), (-1,-1), 8),
    ]))
    story.append(exec_box)
    story.append(Spacer(1, 10))

    # Quick Glossary Preview Table on Page 1
    story.append(Paragraph("<b>QUICK-START PLAIN-ENGLISH DECODER (KEY TERMS EXPLAINED):</b>", ParagraphStyle('DecTitle', fontName='Helvetica-Bold', fontSize=8.5, leading=11, textColor=c_navy)))
    story.append(Spacer(1, 4))
    
    gloss_data = [
        [
            Paragraph("<b>Term</b>", table_header_style),
            Paragraph("<b>What It Actually Means in Plain English</b>", table_header_style),
            Paragraph("<b>Term</b>", table_header_style),
            Paragraph("<b>What It Actually Means in Plain English</b>", table_header_style),
        ],
        [
            Paragraph("<b>NBFC-P2P</b>", gloss_term),
            Paragraph("An RBI-licensed company that lets individuals lend directly to other individuals.", gloss_def),
            Paragraph("<b>FLDG</b>", gloss_term),
            Paragraph("First-Loss Default Guarantee: An illegal promise where a platform covers losses if a borrower defaults.", gloss_def),
        ],
        [
            Paragraph("<b>Escrow</b>", gloss_term),
            Paragraph("A neutral, bank-locked account where funds wait safely until both sides fulfill their promises.", gloss_def),
            Paragraph("<b>Bailment</b>", gloss_term),
            Paragraph("A legal contract where you give your gold/asset to a vault for safekeeping; they must return it unharmed.", gloss_def),
        ],
        [
            Paragraph("<b>Usury</b>", gloss_term),
            Paragraph("Charging illegally high or predatory interest rates (loan sharking).", gloss_def),
            Paragraph("<b>Benami</b>", gloss_term),
            Paragraph("Hiding black money by lending or holding property under someone else's name (like a driver or maid).", gloss_def),
        ],
        [
            Paragraph("<b>Penny Drop</b>", gloss_term),
            Paragraph("Sending ₹1 to a bank account to instantly confirm the account is alive and verify the real owner's name.", gloss_def),
            Paragraph("<b>eNACH</b>", gloss_term),
            Paragraph("Automated digital bank mandate that pulls monthly EMI payments directly from the borrower's account.", gloss_def),
        ],
        [
            Paragraph("<b>Pari Passu</b>", gloss_term),
            Paragraph("A Latin phrase meaning 'on equal footing'—all creditors share remaining recovered money equally.", gloss_def),
            Paragraph("<b>Sec 138 / 25 PSS</b>", gloss_term),
            Paragraph("Criminal laws where bouncing a cheque or digital auto-debit can lead to up to 2 years in prison.", gloss_def),
        ],
        [
            Paragraph("<b>Sec 269SS/T</b>", gloss_term),
            Paragraph("Income tax laws that strictly ban giving or repaying loans of ₹20,000+ in cash (100% fine).", gloss_def),
            Paragraph("<b>WORM Storage</b>", gloss_term),
            Paragraph("Write-Once-Read-Many: Digital files locked so neither hackers nor administrators can ever edit or delete them.", gloss_def),
        ]
    ]
    gloss_table = Table(gloss_data, colWidths=[65, 201, 65, 201])
    gloss_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), c_navy),
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor("#CBD5E1")),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, colors.HexColor("#F8FAFC")]),
        ('TOPPADDING', (0,0), (-1,-1), 3),
        ('BOTTOMPADDING', (0,0), (-1,-1), 3),
        ('LEFTPADDING', (0,0), (-1,-1), 4),
        ('RIGHTPADDING', (0,0), (-1,-1), 4),
    ]))
    story.append(gloss_table)

    story.append(PageBreak())

    # ─────────────────────────────────────────────────────────────
    # DATA SECTION: ALL 75 QUESTIONS WITH INLINE EXPLANATIONS
    # ─────────────────────────────────────────────────────────────
    def add_category(cat_num, cat_title, cat_statute):
        banner_data = [[
            Paragraph(f"<b>CATEGORY {cat_num}: {cat_title.upper()}</b>", cat_title_style),
            Paragraph(f"<font size=7 color='#CBD5E1'>{cat_statute}</font>", ParagraphStyle('CatStat', fontName='Helvetica', fontSize=7, leading=9, textColor=colors.white, alignment=2))
        ]]
        banner_table = Table(banner_data, colWidths=[350, 182])
        banner_table.setStyle(TableStyle([
            ('BACKGROUND', (0,0), (-1,-1), c_navy),
            ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
            ('TOPPADDING', (0,0), (-1,-1), 5),
            ('BOTTOMPADDING', (0,0), (-1,-1), 5),
            ('LEFTPADDING', (0,0), (-1,-1), 8),
            ('RIGHTPADDING', (0,0), (-1,-1), 8),
        ]))
        story.append(Spacer(1, 8))
        story.append(banner_table)
        story.append(Spacer(1, 4))

    def add_qa(q_num, question, challenge, answers):
        items = []
        items.append(Paragraph(f"<b>Q{q_num}: {question}</b>", q_title_style))
        items.append(Paragraph(f"<b>The Legal / Regulatory Risk:</b> {challenge}", challenge_style))
        for ans in answers:
            items.append(Paragraph(f"• {ans}", bullet_style))
        items.append(HRFlowable(width="100%", thickness=0.5, color=colors.HexColor("#E2E8F0"), spaceBefore=3, spaceAfter=5))
        story.append(KeepTogether(items))

    # 1. RBI Licensing
    add_category("I", "Regulatory Framework & RBI Licensing", "RBI NBFC-P2P Master Directions 2017 & Aug 2024")
    add_qa(1, "Are you operating an illegal, unlicensed NBFC-P2P lending business?",
           "The Reserve Bank of India (RBI) rules state that no entity other than a specially licensed NBFC-P2P (a Non-Banking Financial Company licensed for Peer-to-Peer lending) can facilitate peer loans. Does Loanzo violate Section 45-IA of the RBI Act?",
           [
               "<b>Dual-Mode Architecture:</b> Loanzo operates Mode 1 as a private digital ledger under the Indian Contract Act, 1872 for direct loans between friends and family. For open public borrowing (Mode 2 Marketplace), it routes transactions strictly through an <b>RBI-registered NBFC-P2P partner</b> via secure APIs.",
               "<b>Zero Balance-Sheet Risk:</b> Loanzo <i>never holds or touches user money on its own balance sheet</i>. It functions as a pure technology enabler and record-keeper, eliminating any risk of running an illegal shadow bank."
           ])
    add_qa(2, "How do you comply with RBI's August 2024 rule banning credit enhancement and guaranteed returns?",
           "RBI recently penalized fintech platforms that promised lenders '100% guaranteed returns' or provided an FLDG (First-Loss Default Guarantee — an illegal promise where the platform agrees to pay the lender if the borrower runs away).",
           [
               "<b>Strict Prohibition of FLDG:</b> Loanzo enforces <b>0% credit enhancement</b>. The app clearly informs lenders that all investments carry credit risk and that Loanzo provides no artificial safety nets or guaranteed yields.",
               "<b>No Liquidity Pooling:</b> Every loan is an independent bilateral contract directly between the named lender and borrower; funds are never pooled into a common investment pot to pay off earlier investors."
           ])
    add_qa(3, "How is the aggregate ₹50 Lakh lender limit enforced across all P2P platforms?",
           "RBI rules state an individual cannot lend more than ₹50 Lakhs across all P2P platforms combined, and anyone lending more than ₹10 Lakhs must provide a Chartered Accountant (CA) certificate proving a net worth of at least ₹50 Lakhs.",
           [
               "<b>Automated Exposure Tracker:</b> Loanzo's database tracks every lender's cumulative active loans in real time.",
               "<b>Mandatory CA Net-Worth Gate:</b> Once a lender crosses ₹10,00,000 in total active lending, the app automatically locks new loan creation until the user uploads an official <b>CA Net-Worth Certificate verified with an ICAI UDIN number</b> (Unique Document Identification Number)."
           ])
    add_qa(4, "How do you enforce the ₹50,000 single-borrower exposure limit?",
           "To prevent ordinary citizens from losing large sums to a single stranger, RBI limits an individual lender from giving more than ₹50,000 to the same borrower on regulated P2P networks.",
           [
               "<b>Syndicated Loan Tranches:</b> In marketplace loans, loan requests exceeding ₹50,000 are split into multiple smaller shares called <i>Tranches</i> (e.g., a ₹1 Lakh loan is split between at least two separate lenders).",
               "The app automatically caps each individual lender's bid to a maximum of ₹50,000 per borrower."
           ])
    add_qa(5, "How are loan funds disbursed without violating RBI's Escrow rules?",
           "Fintechs cannot take money into their own company bank accounts. RBI mandates that funds must flow through two independent Escrow accounts (neutral third-party bank lockers) managed by a bank-appointed trustee.",
           [
               "<b>Trustee-Managed Escrow:</b> For marketplace loans, Loanzo integrates directly with <b>Trustee Escrow APIs</b> (operated by major banks like ICICI and Axis Bank). Loanzo's founders and employees cannot touch or divert this money.",
               "<b>Direct Bank-to-Bank for Friends/Family:</b> In private friendly loans, money travels directly from the lender's bank account to the borrower's bank account via UPI or IMPS, and is verified using official banking UTR numbers (Unique Transaction Reference)."
           ])

    # 2. Money Lending Acts
    add_category("II", "State Money Lenders Acts & Usury Regulations", "State Money Lending Acts & Usurious Loans Act 1918")
    add_qa(6, "How do you prevent users from running an illegal, unlicensed moneylending business?",
           "Under State Money Lenders Acts (like in Maharashtra, Karnataka, and Gujarat), anyone carrying on the business of lending money for interest without a government moneylender's license can face criminal charges and cannot recover debts in court.",
           [
               "<b>Friendly Loan Exemption:</b> Indian courts have long held that occasional, friendly loans between acquaintances do not constitute a systematic commercial 'business of moneylending'.",
               "<b>Lender Activity Frequency Gate:</b> If an unlicensed individual creates more than <b>3 active interest-bearing loans</b> within a single year, Loanzo automatically freezes their account and requires them to either upload a State Moneylender License or route future loans through the regulated NBFC pipeline."
           ])
    add_qa(7, "How does Loanzo prevent usurious interest rates (predatory 50% - 100% interest)?",
           "The Usurious Loans Act, 1918 and state laws prohibit extortionate interest rates—commonly called <i>Kanduvatti</i> or <i>Meter Vatti</i> (predatory daily/hourly interest loans that trap poor families in debt).",
           [
               "<b>Hard-Coded Interest Ceilings:</b> In the loan creation screen (CreateLoanScreen), interest rates are hard-clamped: unsecured personal loans cannot exceed <b>18% - 24% per year</b>, and collateral-backed loans cannot exceed <b>12% - 15% per year</b>.",
               "Typing in an extortionate interest rate throws an immediate validation error, making predatory loans technically impossible to generate."
           ])
    add_qa(8, "Are late fees and penalty charges legal and non-compounding?",
           "Under Section 74 of the Contract Act and RBI's 2024 Fair Lending circular, lenders cannot charge compounding interest on late fees or charge unfair penalty interest.",
           [
               "<b>Simple Penal Charges:</b> Penalties are calculated solely as simple daily fees; they are <i>never compounded (interest on interest is strictly banned)</i>.",
               "<b>Statutory 3-Day Grace Period:</b> Borrowers get a mandatory 3-day grace window before any late charge begins, and total accumulated penalties are strictly capped at 100% of the loan interest."
           ])

    # 3. KYC & AML
    add_category("III", "Identity Verification, KYC & Anti-Money Laundering (PMLA)", "PMLA 2002 & RBI Master Directions on KYC")
    add_qa(9, "How do you stop scammers from using fake or photoshopped Aadhaar cards?",
           "Online fraudsters often download sample Aadhaar cards, edit the name and photo in Photoshop, and take loans without any intention of paying.",
           [
               "<b>Direct Government DigiLocker Integration:</b> Loanzo does not rely on user-uploaded photo scans. It connects directly to the <b>MeitY DigiLocker government database</b> (DigiLockerVerificationService.kt) to fetch digitally signed Aadhaar XML data.",
               "<b>Live Anti-Spoof Selfie:</b> The user must take a live camera selfie with blinking and head-turning tests that matches their government Aadhaar picture, stopping static photos or printed paper face masks."
           ])
    add_qa(10, "How do you ensure the bank account belongs to the verified person?",
           "A scammer might use their real Aadhaar for KYC, but enter a stolen bank account or mule account (an account rented from a poor student) to receive the disbursed money.",
           [
               "<b>Penny Drop Name Matching:</b> Before accepting any bank account, Loanzo sends ₹1 via NPCI IMPS. The recipient bank returns the registered account holder's full legal name.",
               "The returned name must match the DigiLocker Aadhaar/PAN name with over 85% accuracy. Any discrepancy immediately blocks loan disbursal."
           ])
    add_qa(11, "How do you detect and stop money laundering under PMLA?",
           "Criminals use peer lending networks for <i>Smurfing</i> (breaking large sums of black money into dozens of tiny loans to hide illegal origin).",
           [
               "<b>Automated Suspicious Activity Detection:</b> The system monitors unusual patterns—such as rapid loan creation right below legal thresholds or frequent counterparty switching with no clear purpose.",
               "When triggered, the platform generates a <b>Suspicious Transaction Report (STR)</b> and alerts the Financial Intelligence Unit (FIU-IND) in compliance with the Prevention of Money Laundering Act (PMLA)."
           ])

    # 4. DPDP Act
    add_category("IV", "Data Privacy & The DPDP Act 2023", "Digital Personal Data Protection Act 2023")
    add_qa(12, "Does Loanzo comply with India's new Data Protection Law (DPDP 2023)?",
           "India's DPDP Act, 2023 imposes penalties up to ₹250 Crores if an app misuses personal data, forces users to agree to vague terms, or shares private info without clear permission.",
           [
               "<b>Granular Consent (No Bundling):</b> Users grant separate, individual permissions for KYC, bank verification, and notifications. There are zero pre-ticked checkboxes.",
               "<b>Multi-Language Privacy Notices:</b> Plain-language notices clearly explain what data is collected and provide the direct contact info of the Data Protection Officer (DPO)."
           ])
    add_qa(13, "Does Loanzo steal phone contacts or photo galleries like illegal Chinese loan apps?",
           "Illegal predatory lending apps secretly scrape a user's entire phonebook, family photos, and call logs, and then threaten to leak private photos to relatives when a payment is late.",
           [
               "<b>Zero Contact or Storage Permissions:</b> Loanzo's app code (AndroidManifest.xml) <i>does not request or contain permissions to read contacts, call history, SMS messages, or device storage</i>.",
               "Borrowers and lenders connect strictly via usernames, phone numbers they manually share, or QR codes. The app physically cannot access your family contacts or private media."
           ])
    add_qa(14, "How do you handle a user's 'Right to Delete Data' when tax laws require records to be kept?",
           "The DPDP Act lets users demand their personal data be deleted, but anti-money laundering laws (PMLA) mandate that financial transaction records must be kept for at least 5 years.",
           [
               "<b>Smart Two-Tier Data Separation:</b> When a user requests account deletion, marketing tokens, profile pictures, and community wall posts are wiped immediately.",
               "Financial contracts, bank payment proofs, and KYC audit records are locked in a read-only secure compliance vault accessible only to law enforcement, automatically purging after exactly 5 years."
           ])

    # 5. Contracts & Signatures
    add_category("V", "Digital Contracts, eSign & Legal Enforceability", "IT Act 2000 (Sec 10A, 65B) & Bharatiya Sakshya Adhiniyam 2023 (Sec 63)")
    add_qa(15, "Is an electronic loan agreement generated on Loanzo valid in a real court?",
           "Defaulters frequently argue in court that digital PDF agreements are fake, photoshopped, or inadmissible under the rules of evidence.",
           [
               "<b>Sec 10A IT Act 2000 Protection:</b> Indian law explicitly states that electronic agreements formed through digital consent are fully binding contracts.",
               "<b>Automated Evidence Certificate (Sec 63 BSA 2023 / 65B Evidence Act):</b> Every exported loan contract comes with a certified digital evidence certificate detailing the exact server timestamp, IP address, device serial number, and a tamper-proof SHA-256 digital fingerprint."
           ])
    add_qa(16, "How is Stamp Duty handled on digital agreements across different Indian states?",
           "An unstamped or improperly stamped loan agreement cannot be presented as evidence in an Indian court until severe penalties (up to 10 times the stamp value) are paid under Section 35 of the Stamp Act.",
           [
               "<b>Dynamic State Stamp Calculator:</b> The system calculates the exact stamp duty required based on the borrower's home state and loan amount (e.g., Karnataka Stamp Act vs. Maharashtra Stamp Act).",
               "<b>Digital e-Stamping:</b> Loanzo integrates with government e-stamping agencies (NeSL and SHCIL) to affix official government stamp certificate numbers directly onto the agreement header."
           ])
    add_qa(17, "What if a borrower claims 'My friend or child used my phone to sign the loan'?",
           "Borrowers frequently attempt the <i>Non Est Factum</i> legal defense (a Latin phrase meaning 'It is not my deed / I didn't sign it').",
           [
               "<b>Three-Layer Signature Proof:</b> Signing a loan requires: (1) Device hardware binding, (2) An OTP sent directly to the borrower's government Aadhaar-linked phone, and (3) A real-time camera selfie taken at the exact second of signing.",
               "This combined biometric and hardware trail makes it legally impossible to claim that a child or stranger signed the contract."
           ])

    # 6. Physical Collateral & Vault
    add_category("VI", "Physical Collateral, Dual-Agent Dispatch & Vault Custody", "Indian Contract Act 1872 (Sec 148 Bailment & Sec 172 Pledge)")
    add_qa(18, "What is the legal status of Loanzo holding physical gold, vehicle RCs, or land deeds?",
           "If an app takes physical custody of valuables without proper legal classification, it can be accused of running an illegal pawn shop or facing theft charges under Section 378 IPC (Section 316 BNS).",
           [
               "<b>Contract of Bailment & Pledge:</b> Custody is structured under Section 148 (<i>Bailment</i> — leaving an asset in custody for safekeeping) and Section 172 (<i>Pledge</i> — depositing an asset as security for a loan) of the Indian Contract Act, 1872.",
               "Loanzo acts as a neutral Escrow Bailee holding the pledged asset in trust for the lender until the debt is cleared, protected by a legally binding Bailment Agreement."
           ])
    add_qa(19, "How do you ensure field agents do not steal or swap real gold with fake brass jewelry?",
           "Collateral replacement by corrupt field staff is one of the oldest and most widespread frauds in informal lending.",
           [
               "<b>Dual-Blind Independent Consensus:</b> Two separate, independent field agents are dispatched simultaneously. Neither agent knows the other's identity or appraisal score.",
               "<b>Tamper-Evident Security Seal:</b> In the borrower's direct presence, the collateral is placed in a high-security bag sealed with a barcode and RFID security tape. The bag cannot be opened without entering secret OTP codes sent to both the borrower and lender."
           ])
    add_qa(20, "What happens if pledged gold or documents in the vault are destroyed by fire or stolen?",
           "Under Section 151 of the Contract Act, a custodian is legally responsible for taking reasonable care of deposited goods.",
           [
               "<b>Bank-Grade Locker Vaults:</b> Assets are stored exclusively in physical safe deposit lockers with regulated commercial banks, never in private startup offices.",
               "<b>Bankers Blanket Indemnity Insurance:</b> Every stored item is backed by a full replacement insurance policy covering burglary, robbery, fire, and employee fraud."
           ])

    # 7. Recovery & Anti-Harassment
    add_category("VII", "Debt Collection, Fair Practices & Anti-Harassment", "RBI Fair Practices Code & Supreme Court Guidelines")
    add_qa(21, "How does Loanzo stop abusive collection agents, midnight calls, and threats?",
           "Supreme Court rulings (such as <i>ICICI Bank v. Shanti Devi Sharma</i>) declare that harassing, humiliating, or threatening borrowers over unpaid debts is a punishable criminal offense.",
           [
               "<b>Strict Contact Hours (08:00 AM - 07:00 PM):</b> The system programmatically blocks any automated reminder, push notification, or message from being sent outside legal daytime hours.",
               "<b>Zero Public Shaming:</b> Reminders are sent only to the borrower's registered device. No calls or messages are ever sent to workplace colleagues, neighbors, or relatives."
           ])
    add_qa(22, "Are field verification and collection agents background-checked?",
           "Using unverified recovery musclemen leads to police FIRs and regulatory bans.",
           [
               "<b>Mandatory Police Verification:</b> Every field agent must upload an official <b>Police Clearance Certificate (PCC)</b> confirming zero criminal history.",
               "<b>IIBF Certification:</b> Agents must be certified by the Indian Institute of Banking & Finance (IIBF) as trained Debt Recovery Agents (DRA), ensuring professional conduct."
           ])

    # 8. Taxation
    add_category("VIII", "Taxation, TDS on Interest & GST Compliance", "Income Tax Act 1961 (Sec 194A) & CGST Act 2017")
    add_qa(23, "Does a borrower have to deduct 10% TDS (tax) when paying interest to a friend?",
           "Under Section 194A of the Income Tax Act, interest payments over certain limits require TDS (Tax Deducted at Source — withholding tax and depositing it with the government).",
           [
               "<b>Individual Exemption:</b> Indian tax law explicitly exempts ordinary individuals and HUFs (families not subject to commercial tax audits) from deducting TDS on private loans.",
               "<b>Annual Tax Summary:</b> At the end of every financial year, Loanzo generates a certified tax statement detailing all interest received, allowing lenders to report it cleanly under 'Income from Other Sources' in their tax return."
           ])
    add_qa(24, "Does Loanzo charge GST on loan money or interest?",
           "Many people mistakenly believe that Goods and Services Tax (GST) applies to borrowed money.",
           [
               "<b>Loan Interest is 100% GST-Free:</b> Under government notification No. 12/2017, loan disbursements, principal repayments, and interest charges are completely exempt from GST.",
               "<b>GST on Platform Convenience Fees:</b> Standard 18% GST applies strictly to Loanzo's software convenience fees (such as verification and legal e-stamping fees), backed by a valid digital tax invoice."
           ])

    # 9. Dispute Resolution
    add_category("IX", "Dispute Resolution, Arbitration & Bounced Repayments", "Arbitration & Conciliation Act 1996 & Sec 138 NI Act")
    add_qa(25, "How are loan defaults resolved without waiting 10 years in civil courts?",
           "Ordinary civil suits in Indian courts take between 5 to 12 years to resolve, making recovery of small and medium loans practically impossible.",
           [
               "<b>Online Dispute Resolution (ODR):</b> Every agreement includes a mandatory arbitration clause under the Arbitration and Conciliation Act, 1996.",
               "Disputes are submitted to digital dispute resolution platforms (such as Sama or Presolv360), where an independent, certified arbitrator conducts virtual hearings and issues an enforceable legal award within 60 to 90 days."
           ])
    add_qa(26, "Can a lender file a criminal case if a digital payment (eNACH) bounces?",
           "In modern digital lending, paper cheques are rarely used. How does criminal deterrence work?",
           [
               "<b>Section 25 of the PSS Act:</b> Indian law (Payment and Settlement Systems Act, 2007) gives bounced electronic bank mandates (eNACH / UPI AutoPay) the exact same criminal power as bouncing a physical paper cheque under <b>Section 138 of the Negotiable Instruments Act</b>.",
               "When an auto-debit bounces due to insufficient funds, Loanzo automatically compiles the official NPCI bounce memo and generates a <b>30-Day Statutory Legal Demand Notice</b> ready for court filing."
           ])

    # 10. Co-Borrowers & Succession
    add_category("X", "Co-Borrowers, Guarantees & What Happens Upon Death", "Indian Contract Act (Sec 128) & CPC (Sec 50)")
    add_qa(27, "What is the legal liability of a co-borrower or guarantor?",
           "Guarantors often argue in court: 'I only signed as a character witness; you cannot demand money from me.'",
           [
               "<b>Co-Extensive Liability:</b> Section 128 of the Indian Contract Act states that a guarantor's liability is <i>co-extensive with the borrower</i> (meaning the lender has the full legal right to recover the entire unpaid balance directly from the guarantor).",
               "Co-borrowers must complete full DigiLocker KYC, biometric face verification, and execute a formal Guarantee Deed."
           ])
    add_qa(28, "Does a debt disappear if the borrower passes away before completing repayment?",
           "What happens to the loan balance if a borrower dies unexpectedly?",
           [
               "<b>Heirs Liable up to Inherited Wealth:</b> Under Section 50 of the Code of Civil Procedure (CPC), legal heirs must pay off the deceased borrower's debt, <i>strictly up to the value of the property or wealth they inherited</i> from the deceased person.",
               "If the loan was backed by gold or collateral in the Loanzo vault, the lender holds first right (Section 173 Contract Act) to recover dues from the collateral before releasing remaining proceeds to the heirs."
           ])

    # 11. Security & Cryptography
    add_category("XI", "Cybersecurity, Hardware Security & Anti-Tampering", "IT Act Sec 43 & 66, CERT-In Guidelines")
    add_qa(29, "How do you prevent hackers from intercepting network calls and altering loan amounts?",
           "A hacker using a network proxy could attempt to modify a loan amount from ₹10,000 to ₹10,00,000 or change the receiving bank account number.",
           [
               "<b>Cryptographic Payload Signing:</b> Every API request is digitally signed with an HMAC-SHA256 signature bound to the device's hardware chip, rendering any tampered data invalid.",
               "<b>SSL Certificate Pinning:</b> The app communicates strictly with verified Loanzo server certificates over TLS 1.3, blocking man-in-the-middle network interception tools."
           ])
    add_qa(30, "What stops an internal database administrator from altering a user's loan balance?",
           "A corrupt database administrator could secretly run SQL commands to reduce their friend's debt or erase repayments.",
           [
               "<b>Immutable Merkle Audit Trail:</b> Every payment is chained cryptographically to the previous payment hash: <i>Hash = SHA-256(PreviousHash + Amount + UTR + Timestamp)</i>.",
               "If an administrator edits a single database row directly, the cryptographic chain breaks instantly, alerting the security desk and flagging the ledger as tampered."
           ])

    # 12. Continuity
    add_category("XII", "What Happens if Loanzo Shuts Down Permanently?", "RBI IT Directives & Resolution Planning")
    add_qa(31, "If Loanzo goes bankrupt or ceases operations, do borrowers get a free pass?",
           "If a fintech company goes out of business, lenders fear they will lose all their money with no legal recourse.",
           [
               "<b>Direct Independent Contracts:</b> Loan agreements are direct bilateral legal contracts between the Borrower and Lender. Loanzo is merely the digital facilitator.",
               "<b>Self-Sustaining Legal Dossiers:</b> Users can download a self-contained PDF dossier containing the full signed contract, electronic evidence certificates, and bank UTR receipts, which remain 100% enforceable in court even if Loanzo's servers are completely offline."
           ])

    # 13. Edge Cases & Wall
    add_category("XIII", "Community Wall Governance & Anti-Fraud Logic", "Article 21 Privacy & Defamation Laws")
    add_qa(32, "How does the Community Wall protect borrowers from being publicly shamed?",
           "Publicly posting a person's financial troubles on a social wall violates privacy rights under the Indian Constitution (Article 21) and constitutes criminal defamation under Section 499 IPC (Section 356 BNS).",
           [
               "<b>Complete Personal Anonymity:</b> Marketplace listings <i>never display a borrower's full name, phone number, PAN, Aadhaar, or exact home address</i>.",
               "<b>Zero Delinquency Shaming:</b> Active loans, late repayments, or defaulted accounts can <b>never be posted to the Community Wall</b>. The wall is strictly for prospective proposals, never for collection shaming."
           ])
    add_qa(33, "How does Loanzo stop circular lending and Ponzi schemes?",
           "Three colluding friends borrow from each other in a circle (A lends to B, B lends to C, C lends to A) to artificially boost their credit scores.",
           [
               "<b>Graph Network Cycle Detection:</b> The credit evaluation engine uses Tarjan's strongly connected components algorithm to detect circular borrowing rings.",
               "Any detected loop instantly freezes loan creation for all involved parties and flags the accounts for forensic audit."
           ])
    add_qa(34, "What prevents a lender from falsely claiming they never received a cash repayment?",
           "In informal lending, counterparties constantly fight over undocumented hand-to-hand cash payments.",
           [
               "<b>Cash Repayments Banned:</b> The platform requires digital bank repayments through UPI, IMPS, or bank transfers with traceable reference numbers.",
               "If an offline cash handover is recorded, it requires a <b>Two-Way Biometric Handshake</b>: the balance does not clear until the lender explicitly logs into their phone and confirms receipt with their fingerprint."
           ])

    # 14. Grievance & Ops
    add_category("XIV", "Customer Support, Grievance Redressal & Scalability", "RBI Ombudsman Scheme & Consumer Protection Act 2019")
    add_qa(35, "How does Loanzo comply with the mandatory RBI Ombudsman scheme?",
           "Fintech platforms that ignore user complaints face immediate license suspension by regulators.",
           [
               "<b>Three-Tier Resolution Process:</b> Built-in support ticketing (SupportTicketScreens.kt) with strict deadlines: 48-hour Level 1 response, 7-day Grievance Officer formal virtual hearing (HearingsTab).",
               "If a complaint remains unresolved after 30 days, the user is provided a direct link to escalate to the RBI Ombudsman or the Consumer Dispute Redressal Commission."
           ])
    add_qa(36, "What happens if a user's phone is stolen or SIM-swapped?",
           "A scammer steals an unlocked phone or duplicates a SIM card to redirect loan disbursements to a criminal account.",
           [
               "<b>24-Hour Financial Cool-Off Window:</b> Logging in from an unfamiliar new device hardware ID triggers an automatic 24-hour security freeze.",
               "During this cool-off period, taking new loans, changing bank account numbers, and disbursing funds are strictly blocked."
           ])
    add_qa(37, "How do you prevent a borrower from bribing a field agent to overvalue worthless jewelry?",
           "A borrower offers a ₹10,000 bribe to an agent to write down a fake ₹5,00,000 value for cheap costume jewelry.",
           [
               "<b>Dual-Blind Inspection Protocol:</b> Two independent agents from different agencies are dispatched separately. Neither knows who the second inspector is.",
               "If Agent A evaluates an item at ₹1 Lakh and Agent B evaluates it at ₹5 Lakhs, the variance triggers an immediate consensus failure, disqualifying the loan."
           ])
    add_qa(38, "How does the app handle small ₹5,000 emergency loans vs. large ₹25 Lakh business loans?",
           "A ₹5,000 loan cannot afford days of physical visits, while a ₹25 Lakh loan requires deep collateral due diligence.",
           [
               "<b>Tiered Adaptive Risk Engine:</b> Micro loans (<₹25k) are 100% digital with instant DigiLocker e-KYC and UPI transfer.",
               "High-ticket facilities (>₹2 Lakhs) automatically require Account Aggregator bank statement analysis, physical dual-agent home verification, and vault collateral deposit."
           ])
    add_qa(39, "Can Non-Resident Indians (NRIs) lend or borrow on Loanzo under foreign exchange laws?",
           "India's Foreign Exchange Management Act (FEMA 1999) strictly regulates foreign money entering India for lending.",
           [
               "<b>Rupee NRO Accounts Only:</b> NRIs can participate strictly through Indian Rupee <b>Non-Resident Ordinary (NRO) bank accounts</b> on a non-repatriation basis under RBI guidelines.",
               "Foreign currency remittances from offshore accounts (NRE / FCNR) for peer lending are automatically blocked."
           ])
    add_qa(40, "What is Loanzo's core advantage over traditional banks?",
           "Why would everyday citizens use Loanzo instead of applying for a personal bank loan?",
           [
               "<b>Formalizing India's Informal Lending Economy:</b> Over 80% of personal borrowing in India happens between friends, relatives, and merchants with zero legal safety, broken relationships, and no recovery recourse.",
               "Loanzo removes awkwardness from informal lending by providing a formal, respectful, automated platform with legally binding agreements, automated payment tracking, and physical vault safety."
           ])

    # 15. Financial Crimes
    add_category("XV", "Financial Crimes, Benami Lending & Hawala (PMLA)", "Benami Transactions Act 2016 & PMLA Sec 3/4")
    add_qa(41, "How does Loanzo stop Benami lending (wealthy people lending black money through dummy accounts)?",
           "Under the Benami Transactions Act, lending unaccounted black money through a dummy account (like using a domestic worker's identity) carries up to 7 years in prison.",
           [
               "<b>Source-of-Funds Bank Matching:</b> Money disbursed must come from a verified bank account carrying the exact same legal name as the DigiLocker-verified PAN and Aadhaar.",
               "Lenders investing over ₹5 Lakhs must undergo automated income tier verification via Account Aggregator or ITR-V, and must sign a statutory declaration affirming they are the true beneficial owner."
           ])
    add_qa(42, "How do you stop criminal cartels from using fake loans for Hawala money washing?",
           "Criminals use sham loans to create fake paper capital losses while settling the real money offshore through illegal cash <i>Hawala</i> networks.",
           [
               "<b>Hardware and Network Graphing:</b> The system detects if two parties share the same device hardware fingerprint, IP subnet, or signing geo-location, blocking collusive loans.",
               "Every payment must carry an NPCI bank UTR reference; suspicious loan structuring triggers automated STR filing with the Financial Intelligence Unit (FIU-IND)."
           ])
    add_qa(43, "What happens if a vault custodian steals pledged gold (Criminal Breach of Trust)?",
           "Stealing pledged collateral constitutes Criminal Breach of Trust under Section 405/406 IPC (Section 316 BNS).",
           [
               "Collateral is kept inside institutional Safe Deposit Lockers with regulated commercial banks, never in private startup premises.",
               "Tamper-evident RFID bags with dual-OTP authorization prevent unilateral access, backed by a comprehensive Bankers Blanket Indemnity Insurance policy."
           ])
    add_qa(44, "How do you prevent angry lenders from sending abusive, threatening messages?",
           "Bitter lenders use messaging channels to harass, threaten, or extort borrowers in violation of Section 383 IPC (Section 308 BNS).",
           [
               "<b>NLP Real-Time Content Filtering:</b> All loan notes and grievance descriptions pass through an automated language filter that blocks abusive, threatening, or extortionate words.",
               "Communication is restricted to formal, audited state actions ('Counter-Offer', 'Request Extension', 'Schedule Hearing'); terms warn that abuse results in immediate police cyber cell referral."
           ])
    add_qa(45, "How does Loanzo stop AI Deepfakes from fooling the live face verification?",
           "Scammers use generative AI software to stream moving 3D face masks to pass camera liveness tests.",
           [
               "<b>Hardware Camera Attestation:</b> Camera2 API checks verify that video streams originate from physical camera hardware sensors, blocking virtual camera injection drivers.",
               "<b>Challenge-Response Liveness:</b> The screen flashes randomized color sequences while measuring blood volume pulse fluctuations on the user's skin (rPPG technology), confirming living human tissue."
           ])

    # 16. Mobile Threat
    add_category("XVI", "Mobile Security, Root Detection & Notification Scraping", "Google Play Developer Policies & Android TEE Security")
    add_qa(46, "Will Google Play ban Loanzo for reading notifications (LoanzoNotificationListener.kt)?",
           "Google Play strictly forbids apps from reading notifications unless it is their core declared functionality.",
           [
               "<b>Strict Keyword Filtering:</b> LoanzoNotificationListener.kt filters messages strictly in-memory for specific 'Loanzo' and 'Token' keywords, instantly discarding all personal chats.",
               "<b>Dual-Release Build Architecture:</b> The public Google Play release uses official Google Play SMS Consent APIs (zero sensitive permissions), while the background notification listener is retained only in specialized enterprise builds."
           ])
    add_qa(47, "Can a scammer send a fake notification on their phone to mark an unpaid loan as paid?",
           "A hacker creates a malicious app on their phone that generates a fake notification: 'WhatsApp: Loanzo Token 123456'.",
           [
               "<b>Cryptographic Backend Token Verification:</b> The phone's notification listener never makes an independent decision. It sends the token to the backend, which verifies it against a server-issued token with a 3-minute expiration window.",
               "Canonical loan balance updates occur only when the banking partner's webhook confirms the real UTR bank transfer."
           ])
    add_qa(48, "Can a borrower on a rooted phone hack the app's memory to mark agreements as signed?",
           "A tech-savvy borrower uses tools like Frida or Magisk to edit local app memory and pretend they signed the contract.",
           [
               "<b>Google Play Integrity Hardware Checks:</b> The app checks hardware attestation; devices failing MEETS_STRONG_INTEGRITY (rooted or bootloader-unlocked) are blocked from signing.",
               "<b>Zero-Client-Trust Backend:</b> The mobile phone is treated as an untrusted display terminal; all binding contract states and disbursements are processed and validated on secure server ledgers."
           ])
    add_qa(49, "How do you stop Sybil Attacks (cloning the app 50 times on one phone)?",
           "A fraudster runs 50 virtual copies of Loanzo using app cloner tools to fake 50 community bids and artificially inflate trust ratings.",
           [
               "<b>Hardware Root-of-Trust Fingerprinting:</b> Loanzo queries non-virtualizable hardware registers (DRM Widevine Hardware ID and Keymaster Attestation) that cannot be duplicated by app cloners.",
               "A single physical phone can support only one verified user identity; multiple accounts sharing the same hardware chip are locked."
           ])
    add_qa(50, "How are digital signing keys protected if a user's phone is stolen?",
           "A thief steals an unlocked phone and attempts to extract private signing keys to authorize sham loans.",
           [
               "<b>Hardware Secure Enclave Storage:</b> Master cryptographic keys are stored in the phone's physical hardware <b>Trusted Execution Environment (TEE)</b> via Android Keystore.",
               "Keys cannot be extracted even with physical memory dumps, and authorizing any contract requires fresh biometric fingerprint authentication."
           ])

    # 17. IBC
    add_category("XVII", "Insolvency, Personal Bankruptcy & What Happens in Defaults", "Insolvency & Bankruptcy Code 2016 (Part III)")
    add_qa(51, "What happens if a borrower files for personal bankruptcy under the IBC 2016?",
           "Under Part III of the Insolvency and Bankruptcy Code (IBC 2016), when an individual debtor files for insolvency, a court moratorium takes effect.",
           [
               "<b>Statutory Stay of Proceedings:</b> Under Section 96 of the IBC, all collection notices and legal recovery proceedings are paused automatically by law.",
               "<b>Automated Proof-of-Claim Filing:</b> Loanzo compiles <b>Form B (Proof of Claim by Financial Creditor)</b> containing full contract copies, bank statements, and electronic evidence certificates for direct submission to the court-appointed Resolution Professional."
           ])
    add_qa(52, "Can an indigent borrower use a 'Fresh Start Order' (IBC Sec 80) to cancel their debt?",
           "Low-income debtors with annual income under ₹60,000 can apply to the court for a Fresh Start Order wiping out unsecured debts up to ₹35,000.",
           [
               "<b>Secured Loans are Protected:</b> Section 79(14)(e) of the IBC explicitly excludes secured debts from being canceled; lenders retain the full legal right to recover from physical vault collateral.",
               "Canceled unsecured micro-debts transition to a court-discharged status, granting the lender an official certificate to claim a clean tax write-off."
           ])
    add_qa(53, "Does giving a borrower extra time to pay (restructuring) hurt their credit score?",
           "RBI guidelines state that changing loan terms due to financial distress must be reported accurately to credit bureaus.",
           [
               "<b>Transparent Restructuring Tags:</b> Loan modifications (tenure extensions, moratoriums) are recorded openly in LoanEntity (isRestructured = true).",
               "Credit bureau reporting (CIBIL, Experian) via the partner NBFC tags the account accurately as 'Restructured Due to Financial Stress', maintaining full regulatory compliance."
           ])
    add_qa(54, "Who gets paid first if a borrower goes bankrupt: Loanzo lenders or big commercial banks?",
           "In asset liquidation, the legal priority order (waterfall) determines which creditors get repaid first.",
           [
               "<b>Priority of Pledged Assets:</b> Pledged collateral held in the Loanzo vault establishes a specific possessory lien under Section 173 of the Contract Act, ranking <b>ahead of general unsecured bank overdrafts or credit cards</b>.",
               "Unsecured peer loans share remaining liquidated general assets <i>Pari Passu</i> (on an equal percentage footing) with other general creditors."
           ])
    add_qa(55, "How does Loanzo stop a borrower from pledging the same property deed to multiple lenders?",
           "A scammer uploads the same land deed to 3 different lenders to borrow 3 times the asset's value.",
           [
               "<b>Cryptographic Image Fingerprinting:</b> Uploaded deeds and vehicle documents are indexed using perceptual hashing and SHA-256 checksums, immediately blocking duplicate uploads.",
               "High-value secured loans mandate physical deposit of original title deeds in the partner bank locker, making multi-pledging physically impossible."
           ])

    # 18. Tax Forensics
    add_category("XVIII", "Tax Laws, Cash Loan Bans & Source of Funds", "Income Tax Act 1961 (Sec 68, 69, 269SS, 269T)")
    add_qa(56, "Can anyone give or accept a peer loan of ₹20,000 or more in cash?",
           "Section 269SS of the Income Tax Act strictly bans giving or taking any loan of ₹20,000 or more in cash. Violating this attracts a <b>100% fine equal to the entire loan amount under Section 271D</b>.",
           [
               "<b>100% Cashless Architecture:</b> Loanzo physically contains no features or buttons to record cash disbursements.",
               "All transactions require digital banking UTR trails (UPI, IMPS, NEFT), fully protecting users from devastating Section 269SS penalties."
           ])
    add_qa(57, "Can a borrower repay a loan of ₹20,000 or more in cash?",
           "Section 269T of the Income Tax Act bans repaying loans of ₹20,000 or more in cash, carrying a <b>100% penalty under Section 271E</b>.",
           [
               "Repayments occur strictly via UPI Dynamic QR Codes, eNACH auto-debits, or direct bank transfers.",
               "A borrower cannot mark a loan as repaid by claiming 'I gave cash' without an official bank transaction reference verified by the lender."
           ])
    add_qa(58, "How does a lender prove where they got the money to lend if the tax department investigates?",
           "The Income Tax Department can issue scrutiny notices under Section 68/69 treating unexplained investments as taxable black money taxed at 60% plus heavy surcharges.",
           [
               "<b>Automated Forensic Tax Dossier:</b> ReportExporter.kt generates an instant tax compliance pack detailing bank account debits, UTR numbers, counterparty verified PAN/Aadhaar data, and stamped digital contracts.",
               "This completely fulfills the three mandatory legal tests: proving the counterparty's identity, the genuineness of the loan, and the official banking trail."
           ])
    add_qa(59, "Are high-value peer transactions on Loanzo reported to the tax department?",
           "Rule 114E of the Income Tax Rules requires reporting entities to submit Annual SFT (Statement of Financial Transactions) reports for large banking movements.",
           [
               "Partner escrow banks automatically report cumulative transfers exceeding ₹10 Lakhs in a financial year under existing SFT-004 banking reporting pipelines.",
               "Loanzo exports clean, audit-ready financial statements for effortless tax filing."
           ])
    add_qa(60, "Does GST apply to late payment fees or early loan closure charges?",
           "Tax authorities clarified in Circular No. 102/2019 that late payment fees can attract 18% GST.",
           [
               "Private loans between unregistered individuals do not attract GST.",
               "Where Loanzo charges an administrative late fee or platform facilitation fee, 18% GST is added and backed by an official digital GST invoice."
           ])

    # 19. Real Estate & Mortgages
    add_category("XIX", "Real Estate Mortgages, Gold Purity & Vehicle Loans", "Transfer of Property Act 1882 & Motor Vehicles Act 1988")
    add_qa(61, "Does depositing property deeds create an enforceable mortgage under Indian law?",
           "An equitable mortgage by deposit of title deeds (Section 58(f) Transfer of Property Act) is strictly regulated, and states like Maharashtra mandate compulsory registration.",
           [
               "<b>State-Specific Mortgage Rules:</b> In states where registering an Agreement Relating to Deposit of Title Deeds (ARDTD) is mandatory, the app generates standard deeds and requires Sub-Registrar registration receipts before disbursement.",
               "Original parent title documents are stored securely in bank lockers, accompanied by stamped Memorandums of Deposit."
           ])
    add_qa(62, "Can a private lender seize and auction a mortgaged house without going to court?",
           "Only banks and big NBFCs have special powers under the SARFAESI Act to seize and sell houses without a court order. Private individuals do not have SARFAESI powers.",
           [
               "<b>Honest Disclosure:</b> Loanzo clearly informs lenders that private mortgage enforcement requires a Mortgage Suit for Sale under Order XXXIV of the CPC or execution of an arbitral decree.",
               "Agreements include an irrevocable Power of Attorney to auction pledged assets upon confirmed arbitral award, drastically reducing litigation time."
           ])
    add_qa(63, "Who pays if a field agent certifies 22K gold that turns out to be copper-plated fake jewelry?",
           "A bad gold evaluation leaves the lender with worthless collateral when a borrower defaults.",
           [
               "<b>Electronic Testing Equipment:</b> Agents must use electronic Specific Gravity Densimeters and portable X-Ray Fluorescence (XRF) scanners, not subjective acid touchstones.",
               "<b>Professional Indemnity Insurance:</b> Inspection agencies must maintain professional insurance policies indemnifying the lender against appraisal mistakes up to ₹25 Lakhs."
           ])
    add_qa(64, "Can a borrower sell their car to someone else while the physical RC book is in the vault?",
           "A borrower could visit the transport office (RTO), claim they 'lost' their paper RC book, get a duplicate, and sell the car.",
           [
               "<b>Official Vahan Portal Lien (Form 34):</b> High-value vehicle loans require a formal digital hypothecation entry endorsed directly on the Ministry of Road Transport's Vahan registry.",
               "The RTO cannot issue a duplicate RC book or transfer vehicle ownership without an electronic NOC issued directly by the lender through the app."
           ])
    add_qa(65, "Can a lender or agent physically break into a borrower's house to seize collateral?",
           "The Supreme Court has ruled in multiple cases (such as <i>ICICI Bank v. Prakash Kaur</i>) that using musclemen to forcefully repossess vehicles or collateral is criminal trespass and robbery.",
           [
               "<b>Strict Ban on Forceful Repossession:</b> Loanzo strictly prohibits self-help or aggressive recovery tactics.",
               "Asset recovery follows formal voluntary surrender protocols or execution through authorized court bailiffs."
           ])

    # 20. Contract Capacity & Cross-Border
    add_category("XX", "Minors, Force Majeure & Borrowers Fleeing Abroad", "Indian Contract Act 1872 & Limitation Act 1963")
    add_qa(66, "What happens if a 17-year-old student lies about their age and takes a loan?",
           "Under Section 11 of the Indian Contract Act (<i>Mohori Bibee v. Dharmodas Ghose</i>), a contract with a minor is <b>void ab initio</b> (completely invalid from the beginning); money lent to a minor can never be recovered by law.",
           [
               "<b>Cryptographic Date-of-Birth Verification:</b> The user's birthdate is pulled directly from UIDAI/DigiLocker government XML records, not from manual user typing.",
               "If the calculated age is under 18.00 years, onboarding terminates instantly."
           ])
    add_qa(67, "Can a borrower refuse to pay by claiming 'Force Majeure' (Act of God) due to job loss?",
           "Defaulters often cite Section 56 of the Contract Act (Frustration of Contract) claiming economic hardship relieves them from repaying debts.",
           [
               "<b>Settled Supreme Court Law:</b> In <i>Energy Watchdog v. CERC (2017)</i>, the Supreme Court ruled that commercial hardship, job loss, or financial difficulty <i>never excuses debt repayment</i>.",
               "Loanzo contracts explicitly affirm that repayment of principal and interest is an unconditional personal obligation."
           ])
    add_qa(68, "Can a lender unilaterally increase interest rates or fees mid-way through a loan?",
           "A lender sees market rates rising and tries to raise interest from 12% to 18% without the borrower's agreement.",
           [
               "<b>Cryptographically Locked Contracts:</b> Once signed by both parties, loan terms are permanently frozen in the database and PDF.",
               "Any term change requires a mutual <b>Restructuring Handshake</b> creating a signed Addendum requiring OTP consent from both sides."
           ])
    add_qa(69, "What legal remedy exists if a defaulting borrower flees abroad to the UAE, UK, or Canada?",
           "Borrowers move overseas to escape Indian court summons and debt collection.",
           [
               "<b>Look Out Circulars (LOC):</b> Criminal complaints under Section 138 NI Act / Sec 25 PSS Act allow magistrate courts to issue warrants that trigger airport detention through the Bureau of Immigration upon return.",
               "<b>Reciprocating Country Enforcement:</b> Under Section 44A of the CPC, civil decrees from Indian courts can be directly executed against assets in reciprocating countries (including the UAE, UK, and Singapore)."
           ])
    add_qa(70, "How does Loanzo stop loans from becoming time-barred under the Limitation Act?",
           "Under the Limitation Act, 1963, a lender must initiate recovery within <b>exactly 3 years</b> from the date of default, or the debt is legally extinguished forever.",
           [
               "<b>Section 18 Electronic Debt Acknowledgements:</b> Every time a borrower logs into the app, views their dashboard, clicks 'Request Extension', or makes an EMI payment, an official electronic <b>Acknowledgement of Debt</b> is recorded.",
               "Under Section 18 of the Limitation Act, every written acknowledgement resets the 3-year recovery clock afresh."
           ])

    # 21. AI Governance & Fraud
    add_category("XXI", "Algorithmic Fairness, Admin Corruption & Chargeback Scams", "Constitution of India (Art 14/15) & NPCI Dispute Rules")
    add_qa(71, "How does Loanzo ensure its credit scoring does not discriminate against poor or minority groups?",
           "Artificial intelligence algorithms can inadvertently practice <i>Digital Redlining</i> (unfairly rejecting borrowers based on caste, religion, gender, or postal pin code).",
           [
               "<b>Protected Demographic Exclusion:</b> Loanzo's scoring algorithms exclude religion, caste, gender, and social category entirely from model calculations.",
               "Credit appraisal is based solely on objective financial data: repayment punctuality, banking velocity, and verified collateral."
           ])
    add_qa(72, "What stops a corrupt internal employee from approving fake KYC documents for bribes?",
           "A rogue company administrator approves forged documents or releases vault collateral in exchange for private kickbacks.",
           [
               "<b>Four-Eyes Principle:</b> Critical administrative actions (such as overriding KYC rejections or releasing vault assets) require dual approval: one admin stages the action, and an independent Super-Admin from a different IP must approve it.",
               "All administrator actions are logged in immutable, append-only cloud audit logs that internal staff cannot alter or delete."
           ])
    add_qa(73, "How does the system defend against fraudulent UPI payment chargebacks?",
           "A borrower pays an EMI via UPI and then dishonestly contacts their bank claiming 'Unauthorized transaction / I was hacked!' to reverse the money.",
           [
               "<b>NPCI Dispute Defense Package:</b> When a bank dispute is initiated, Loanzo automatically compiles a defense dossier with device telemetry, 2FA logs, and the underlying signed loan contract.",
               "Banks reject chargeback claims when presented with cryptographically verified contractual consideration."
           ])
    add_qa(74, "How do you stop predatory loan sharks from targeting desperate people on the Community Wall?",
           "Predatory lenders monitor new community loan requests and offer extortionate offline side deals to vulnerable borrowers.",
           [
               "<b>Rate Guardrails & Contact Masking:</b> Prospective lenders cannot see a borrower's phone number, email, or street address.",
               "All bidding occurs through verified in-app counters clamped to statutory interest ceilings."
           ])
    add_qa(75, "Does charging a platform fee make Loanzo an illegal broker under moneylending laws?",
           "State moneylending acts strictly regulate loan brokers and cap brokerage fees.",
           [
               "<b>Pure Software (SaaS) Classification:</b> Loanzo's fee is structured not as a percentage loan brokerage commission, but as a pure <b>Software & Technology Facilitation Fee</b> (SAC Code 998313 / 998314) backed by 18% GST invoices.",
               "Consideration is paid for identity verification APIs, digital e-stamping, and encrypted cloud storage, firmly establishing it as technology infrastructure."
           ])

    # ─────────────────────────────────────────────────────────────
    # STATUTORY SUMMARY MATRIX TABLE
    # ─────────────────────────────────────────────────────────────
    story.append(PageBreak())
    story.append(Paragraph("<b>MASTER STATUTORY COMPLIANCE MATRIX (WITH PLAIN-ENGLISH SUMMARIES)</b>", ParagraphStyle('MTitle', fontName='Helvetica-Bold', fontSize=13, leading=17, textColor=c_navy)))
    story.append(Paragraph("Summary of Core Indian Statutes, Legal Risks, and Loanzo Technical &amp; Operational Mitigations.", subtitle_style))
    story.append(HRFlowable(width="100%", thickness=1.5, color=c_gold, spaceBefore=2, spaceAfter=8))

    matrix_headers = [
        Paragraph("<b>#</b>", table_header_style),
        Paragraph("<b>Statute / Regulator</b>", table_header_style),
        Paragraph("<b>Key Legal Mandate (Plain English)</b>", table_header_style),
        Paragraph("<b>Loanzo Technical &amp; Architectural Defense</b>", table_header_style)
    ]

    matrix_rows = [
        matrix_headers,
        [Paragraph("1", table_cell_style), Paragraph("RBI P2P Directions", table_cell_style), Paragraph("No unlicensed lending, no guaranteed returns (FLDG), no fund pooling.", table_cell_style), Paragraph("Dual-mode architecture; 0% balance sheet risk; API partner NBFC.", table_cell_style)],
        [Paragraph("2", table_cell_style), Paragraph("State Money Lenders Acts", table_cell_style), Paragraph("Bans carrying on moneylending business without a license.", table_cell_style), Paragraph("Lender frequency gate (max 3 loans/yr); statutory interest caps.", table_cell_style)],
        [Paragraph("3", table_cell_style), Paragraph("Usurious Loans Act 1918", table_cell_style), Paragraph("Criminalizes predatory, extortionate interest rates (Kanduvatti).", table_cell_style), Paragraph("Clamped at 18-24% unsecured, 12-15% secured in CreateLoanScreen.", table_cell_style)],
        [Paragraph("4", table_cell_style), Paragraph("PMLA 2002 / FIU-IND", table_cell_style), Paragraph("Bans money laundering, black money layering, and fake KYC.", table_cell_style), Paragraph("DigiLocker direct XML + Penny drop bank match + STR auto-filing.", table_cell_style)],
        [Paragraph("5", table_cell_style), Paragraph("DPDP Act 2023", table_cell_style), Paragraph("Requires clear consent; strictly bans phonebook &amp; SMS scraping.", table_cell_style), Paragraph("Granular consent; zero contact/SMS permissions; 5-yr PMLA vault.", table_cell_style)],
        [Paragraph("6", table_cell_style), Paragraph("Sec 63 BSA / 65B Evidence", table_cell_style), Paragraph("Mandates certificates of authenticity for computer contracts.", table_cell_style), Paragraph("Automated Sec 63 BSA evidence certificates with SHA-256 seals.", table_cell_style)],
        [Paragraph("7", table_cell_style), Paragraph("Indian Stamp Act 1899", table_cell_style), Paragraph("Unstamped agreements cannot be used as evidence in court.", table_cell_style), Paragraph("NeSL / SHCIL dynamic e-stamping with unique GRN certificates.", table_cell_style)],
        [Paragraph("8", table_cell_style), Paragraph("Contract Act (Sec 148/172)", table_cell_style), Paragraph("Safeguards pledged goods in custody (Bailment &amp; Pledge).", table_cell_style), Paragraph("Bailment deed; Dual-Agent consensus dispatch; bank locker custody.", table_cell_style)],
        [Paragraph("9", table_cell_style), Paragraph("Sec 25 PSS / 138 NI Act", table_cell_style), Paragraph("Bounced digital EMI mandates carry up to 2 years prison.", table_cell_style), Paragraph("NPCI return memo auto-compilation + statutory 30-day legal notice.", table_cell_style)],
        [Paragraph("10", table_cell_style), Paragraph("Income Tax Sec 269SS/T", table_cell_style), Paragraph("Bans cash loans/repayments of ₹20,000+ (100% fine).", table_cell_style), Paragraph("Systemic 100% ban on cash; banking UTRs only; statutory warnings.", table_cell_style)],
        [Paragraph("11", table_cell_style), Paragraph("Part III IBC 2016", table_cell_style), Paragraph("Governs personal bankruptcy and court-ordered debt pauses.", table_cell_style), Paragraph("Sec 96 stay compliance; Form B claim filing generation.", table_cell_style)],
        [Paragraph("12", table_cell_style), Paragraph("IT Act 2000 (Sec 43/66)", table_cell_style), Paragraph("Punishes phone hacking, memory editing, and app tampering.", table_cell_style), Paragraph("Play Integrity hardware attestation; Android Keystore TEE.", table_cell_style)],
        [Paragraph("13", table_cell_style), Paragraph("NPCI Dispute Guidelines", table_cell_style), Paragraph("Defends against false 'unauthorized payment' refund claims.", table_cell_style), Paragraph("Automated 24hr contractual defense package sent to issuer bank.", table_cell_style)],
        [Paragraph("14", table_cell_style), Paragraph("Benami Transactions Act", table_cell_style), Paragraph("Bans hiding wealth by lending through dummy proxy accounts.", table_cell_style), Paragraph("Closed-loop bank account matching; Account Aggregator ITR checks.", table_cell_style)],
        [Paragraph("15", table_cell_style), Paragraph("Arbitration Act 1996", table_cell_style), Paragraph("Avoids 10-year court delays through fast-track digital ODR.", table_cell_style), Paragraph("Fast-track digital ODR institutional arbitration within 60-90 days.", table_cell_style)]
    ]

    matrix_table = Table(matrix_rows, colWidths=[18, 105, 185, 224])
    matrix_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), c_navy),
        ('VALIGN', (0,0), (-1,-1), 'TOP'),
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor("#CBD5E1")),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, colors.HexColor("#F8FAFC")]),
        ('TOPPADDING', (0,0), (-1,-1), 3.5),
        ('BOTTOMPADDING', (0,0), (-1,-1), 3.5),
        ('LEFTPADDING', (0,0), (-1,-1), 4),
        ('RIGHTPADDING', (0,0), (-1,-1), 4),
    ]))
    story.append(matrix_table)

    # Build document
    doc.build(story, canvasmaker=NumberedCanvas)
    print(f"Successfully generated plain-English master PDF dossier: {filename}")

if __name__ == '__main__':
    output_path = sys.argv[1] if len(sys.argv) > 1 else "Loanzo_Legal_Compliance_Cross_Examination_75_QnA.pdf"
    build_pdf(output_path)
