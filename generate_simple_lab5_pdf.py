import os
from reportlab.lib import colors
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, HRFlowable
)
from reportlab.graphics.shapes import Drawing, Rect, String, Line, Polygon
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
        self.drawString(40, 24, "Lab 5: Requirement Elicitation – II  |  Project: Loanzo Protocol")
        self.drawRightString(572, 24, f"Page {self._pageNumber} of {page_count}")
        self.setStrokeColor(colors.HexColor("#D1D5DB"))
        self.setLineWidth(0.5)
        self.line(40, 34, 572, 34)
        self.restoreState()

# =========================================================================
# VECTOR DECISION TREES GENERATION (CRISP GRAPHICAL DIAGRAMS)
# =========================================================================

def draw_tree_1():
    """Decision Tree 1: Purpose-Bound Milestone Tranche Disbursement Policy"""
    d = Drawing(532, 98)
    # Background card
    d.add(Rect(0, 0, 532, 98, fillColor=colors.HexColor("#F9FAFB"), strokeColor=colors.HexColor("#D1D5DB"), strokeWidth=0.75, rx=3, ry=3))
    
    # Root: Trigger
    d.add(Rect(10, 36, 92, 26, fillColor=colors.HexColor("#111827"), strokeColor=colors.HexColor("#111827"), rx=2, ry=2))
    d.add(String(56, 50, "Disbursement Request", fontName="Helvetica-Bold", fontSize=6.2, fillColor=colors.white, textAnchor="middle"))
    d.add(String(56, 42, "Milestone Trigger", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#9CA3AF"), textAnchor="middle"))
    
    # Arrow to C1
    d.add(Line(102, 49, 122, 49, strokeColor=colors.HexColor("#4B5563"), strokeWidth=1))
    d.add(Polygon([122, 49, 118, 51, 118, 47], fillColor=colors.HexColor("#4B5563"), strokeColor=None))
    
    # Node C1: Commercial Tax Invoice?
    d.add(Rect(122, 33, 106, 32, fillColor=colors.white, strokeColor=colors.HexColor("#374151"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(175, 54, "Condition 1: Invoice Valid?", fontName="Helvetica-Bold", fontSize=6.2, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(175, 45, "Commercial Tax Invoice", fontName="Helvetica", fontSize=5.8, fillColor=colors.HexColor("#374151"), textAnchor="middle"))
    d.add(String(175, 37, "Uploaded & Verified", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#6B7280"), textAnchor="middle"))
    
    # C1 Branch NO (Up)
    d.add(Line(228, 49, 245, 75, strokeColor=colors.HexColor("#DC2626"), strokeWidth=1))
    d.add(Line(245, 75, 262, 75, strokeColor=colors.HexColor("#DC2626"), strokeWidth=1))
    d.add(Polygon([262, 75, 258, 77, 258, 73], fillColor=colors.HexColor("#DC2626"), strokeColor=None))
    d.add(String(240, 78, "NO", fontName="Helvetica-Bold", fontSize=6, fillColor=colors.HexColor("#DC2626"), textAnchor="middle"))
    
    d.add(Rect(262, 64, 130, 22, fillColor=colors.HexColor("#FEE2E2"), strokeColor=colors.HexColor("#FCA5A5"), strokeWidth=0.5, rx=2, ry=2))
    d.add(String(327, 76, "ACTION: REJECT REQUEST", fontName="Helvetica-Bold", fontSize=6.2, fillColor=colors.HexColor("#991B1B"), textAnchor="middle"))
    d.add(String(327, 68, "Error: Tax Invoice Mandatory", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#B91C1C"), textAnchor="middle"))
    
    # C1 Branch YES (Down)
    d.add(Line(228, 49, 245, 30, strokeColor=colors.HexColor("#059669"), strokeWidth=1))
    d.add(Line(245, 30, 262, 30, strokeColor=colors.HexColor("#059669"), strokeWidth=1))
    d.add(Polygon([262, 30, 258, 32, 258, 28], fillColor=colors.HexColor("#059669"), strokeColor=None))
    d.add(String(240, 34, "YES", fontName="Helvetica-Bold", fontSize=6, fillColor=colors.HexColor("#059669"), textAnchor="middle"))
    
    # Node C2: Penny Drop Name Match?
    d.add(Rect(262, 14, 108, 32, fillColor=colors.white, strokeColor=colors.HexColor("#374151"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(316, 35, "Condition 2: Payee Match?", fontName="Helvetica-Bold", fontSize=6.2, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(316, 26, "Penny-Drop Verification", fontName="Helvetica", fontSize=5.8, fillColor=colors.HexColor("#374151"), textAnchor="middle"))
    d.add(String(316, 18, "Beneficiary Match >= 85%", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#6B7280"), textAnchor="middle"))
    
    # C2 Branch NO (Upper middle)
    d.add(Line(370, 30, 385, 44, strokeColor=colors.HexColor("#D97706"), strokeWidth=1))
    d.add(Line(385, 44, 398, 44, strokeColor=colors.HexColor("#D97706"), strokeWidth=1))
    d.add(Polygon([398, 44, 394, 46, 394, 42], fillColor=colors.HexColor("#D97706"), strokeColor=None))
    d.add(String(382, 48, "NO", fontName="Helvetica-Bold", fontSize=6, fillColor=colors.HexColor("#D97706"), textAnchor="middle"))
    
    d.add(Rect(398, 33, 124, 22, fillColor=colors.HexColor("#FEF3C7"), strokeColor=colors.HexColor("#FCD34D"), strokeWidth=0.5, rx=2, ry=2))
    d.add(String(460, 45, "ACTION: LOCK TRANCHE", fontName="Helvetica-Bold", fontSize=6.2, fillColor=colors.HexColor("#92400E"), textAnchor="middle"))
    d.add(String(460, 37, "Flag Incident & Compliance Audit", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#B45309"), textAnchor="middle"))
    
    # C2 Branch YES (Bottom)
    d.add(Line(370, 30, 385, 16, strokeColor=colors.HexColor("#059669"), strokeWidth=1))
    d.add(Line(385, 16, 398, 16, strokeColor=colors.HexColor("#059669"), strokeWidth=1))
    d.add(Polygon([398, 16, 394, 18, 394, 14], fillColor=colors.HexColor("#059669"), strokeColor=None))
    d.add(String(382, 20, "YES", fontName="Helvetica-Bold", fontSize=6, fillColor=colors.HexColor("#059669"), textAnchor="middle"))
    
    d.add(Rect(398, 5, 124, 22, fillColor=colors.HexColor("#D1FAE5"), strokeColor=colors.HexColor("#6EE7B7"), strokeWidth=0.5, rx=2, ry=2))
    d.add(String(460, 17, "ACTION: EXECUTE SETTLEMENT", fontName="Helvetica-Bold", fontSize=6.2, fillColor=colors.HexColor("#065F46"), textAnchor="middle"))
    d.add(String(460, 9, "Direct UPI Transfer to Vendor VPA", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#047857"), textAnchor="middle"))
    
    return d

def draw_tree_2():
    """Decision Tree 2: Collateral Valuation, LTV Ceiling, and Inspection Dispatch"""
    d = Drawing(532, 102)
    # Background card
    d.add(Rect(0, 0, 532, 102, fillColor=colors.HexColor("#F9FAFB"), strokeColor=colors.HexColor("#D1D5DB"), strokeWidth=0.75, rx=3, ry=3))
    
    # Root Node
    d.add(Rect(10, 38, 92, 26, fillColor=colors.HexColor("#111827"), strokeColor=colors.HexColor("#111827"), rx=2, ry=2))
    d.add(String(56, 52, "Collateral Asset Pledged", fontName="Helvetica-Bold", fontSize=6.2, fillColor=colors.white, textAnchor="middle"))
    d.add(String(56, 44, "Valuation Initiation", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#9CA3AF"), textAnchor="middle"))
    
    # Connector line to Asset Class Column
    d.add(Line(102, 51, 120, 51, strokeColor=colors.HexColor("#4B5563"), strokeWidth=1))
    
    # Fork to 4 Asset Classes
    d.add(Line(120, 85, 120, 17, strokeColor=colors.HexColor("#4B5563"), strokeWidth=1))
    
    # Class 1: Gold
    d.add(Line(120, 85, 136, 85, strokeColor=colors.HexColor("#4B5563"), strokeWidth=1))
    d.add(Rect(136, 75, 105, 20, fillColor=colors.white, strokeColor=colors.HexColor("#374151"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(188, 87, "Precious Bullion (Gold)", fontName="Helvetica-Bold", fontSize=6, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(188, 79, "Density Assay Verified", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#6B7280"), textAnchor="middle"))
    
    d.add(Line(241, 85, 258, 85, strokeColor=colors.HexColor("#059669"), strokeWidth=1))
    d.add(Rect(258, 75, 105, 20, fillColor=colors.HexColor("#EFF6FF"), strokeColor=colors.HexColor("#93C5FD"), strokeWidth=0.5, rx=2, ry=2))
    d.add(String(310, 85, "Cap LTV at 75%", fontName="Helvetica-Bold", fontSize=6.2, fillColor=colors.HexColor("#1E40AF"), textAnchor="middle"))
    d.add(String(310, 78, "IBJA 30-Day Moving Avg", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#3B82F6"), textAnchor="middle"))
    
    # Class 2: Property
    d.add(Line(120, 62, 136, 62, strokeColor=colors.HexColor("#4B5563"), strokeWidth=1))
    d.add(Rect(136, 52, 105, 20, fillColor=colors.white, strokeColor=colors.HexColor("#374151"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(188, 64, "Property Title Deeds", fontName="Helvetica-Bold", fontSize=6, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(188, 56, "Non-Encumbrance Verified", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#6B7280"), textAnchor="middle"))
    
    d.add(Line(241, 62, 258, 62, strokeColor=colors.HexColor("#059669"), strokeWidth=1))
    d.add(Rect(258, 52, 105, 20, fillColor=colors.HexColor("#EFF6FF"), strokeColor=colors.HexColor("#93C5FD"), strokeWidth=0.5, rx=2, ry=2))
    d.add(String(310, 62, "Cap LTV at 55%", fontName="Helvetica-Bold", fontSize=6.2, fillColor=colors.HexColor("#1E40AF"), textAnchor="middle"))
    d.add(String(310, 55, "Circle Rate Valuation", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#3B82F6"), textAnchor="middle"))
    
    # Class 3: Vehicles
    d.add(Line(120, 39, 136, 39, strokeColor=colors.HexColor("#4B5563"), strokeWidth=1))
    d.add(Rect(136, 29, 105, 20, fillColor=colors.white, strokeColor=colors.HexColor("#374151"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(188, 41, "Motor Vehicles (Fleet)", fontName="Helvetica-Bold", fontSize=6, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(188, 33, "Form 34 Hypothecation", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#6B7280"), textAnchor="middle"))
    
    d.add(Line(241, 39, 258, 39, strokeColor=colors.HexColor("#059669"), strokeWidth=1))
    d.add(Rect(258, 29, 105, 20, fillColor=colors.HexColor("#EFF6FF"), strokeColor=colors.HexColor("#93C5FD"), strokeWidth=0.5, rx=2, ry=2))
    d.add(String(310, 39, "Cap LTV at 60%", fontName="Helvetica-Bold", fontSize=6.2, fillColor=colors.HexColor("#1E40AF"), textAnchor="middle"))
    d.add(String(310, 32, "Vahan RC Book Verification", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#3B82F6"), textAnchor="middle"))
    
    # Class 4: IT Equipment
    d.add(Line(120, 16, 136, 16, strokeColor=colors.HexColor("#4B5563"), strokeWidth=1))
    d.add(Rect(136, 6, 105, 20, fillColor=colors.white, strokeColor=colors.HexColor("#374151"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(188, 18, "Commercial IT Assets", fontName="Helvetica-Bold", fontSize=6, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(188, 10, "Serial & MDM Inspected", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#6B7280"), textAnchor="middle"))
    
    d.add(Line(241, 16, 258, 16, strokeColor=colors.HexColor("#059669"), strokeWidth=1))
    d.add(Rect(258, 6, 105, 20, fillColor=colors.HexColor("#EFF6FF"), strokeColor=colors.HexColor("#93C5FD"), strokeWidth=0.5, rx=2, ry=2))
    d.add(String(310, 16, "Cap LTV at 45%", fontName="Helvetica-Bold", fontSize=6.2, fillColor=colors.HexColor("#1E40AF"), textAnchor="middle"))
    d.add(String(310, 9, "Depreciated Invoice Value", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#3B82F6"), textAnchor="middle"))
    
    # Valuation Threshold Routing
    d.add(Line(363, 51, 380, 51, strokeColor=colors.HexColor("#4B5563"), strokeWidth=1))
    d.add(Line(380, 72, 380, 30, strokeColor=colors.HexColor("#4B5563"), strokeWidth=1))
    
    # Threshold <= 100k (Upper)
    d.add(Line(380, 72, 396, 72, strokeColor=colors.HexColor("#4B5563"), strokeWidth=1))
    d.add(Polygon([396, 72, 392, 74, 392, 70], fillColor=colors.HexColor("#4B5563"), strokeColor=None))
    d.add(Rect(396, 61, 126, 22, fillColor=colors.HexColor("#F3F4F6"), strokeColor=colors.HexColor("#D1D5DB"), strokeWidth=0.5, rx=2, ry=2))
    d.add(String(459, 73, "Valuation <= INR 100,000", fontName="Helvetica-Bold", fontSize=6.2, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(459, 65, "ACTION: Dispatch Single Field Agent", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    # Threshold > 100k (Lower)
    d.add(Line(380, 30, 396, 30, strokeColor=colors.HexColor("#D97706"), strokeWidth=1))
    d.add(Polygon([396, 30, 392, 32, 392, 28], fillColor=colors.HexColor("#D97706"), strokeColor=None))
    d.add(Rect(396, 19, 126, 22, fillColor=colors.HexColor("#FEF3C7"), strokeColor=colors.HexColor("#FCD34D"), strokeWidth=0.5, rx=2, ry=2))
    d.add(String(459, 31, "Valuation > INR 100,000", fontName="Helvetica-Bold", fontSize=6.2, fillColor=colors.HexColor("#92400E"), textAnchor="middle"))
    d.add(String(459, 23, "ACTION: Mandate Dual-Agent Consensus", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#B45309"), textAnchor="middle"))
    
    return d

def draw_tree_3():
    """Decision Tree 3: Delinquency Management, Penal Accrual, and Restructuring"""
    d = Drawing(532, 98)
    # Background card
    d.add(Rect(0, 0, 532, 98, fillColor=colors.HexColor("#F9FAFB"), strokeColor=colors.HexColor("#D1D5DB"), strokeWidth=0.75, rx=3, ry=3))
    
    # Root: Installment Overdue
    d.add(Rect(10, 36, 92, 26, fillColor=colors.HexColor("#111827"), strokeColor=colors.HexColor("#111827"), rx=2, ry=2))
    d.add(String(56, 50, "Installment Overdue", fontName="Helvetica-Bold", fontSize=6.2, fillColor=colors.white, textAnchor="middle"))
    d.add(String(56, 42, "Repayment Status Check", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#9CA3AF"), textAnchor="middle"))
    
    # Connector line
    d.add(Line(102, 49, 120, 49, strokeColor=colors.HexColor("#4B5563"), strokeWidth=1))
    d.add(Line(120, 78, 120, 20, strokeColor=colors.HexColor("#4B5563"), strokeWidth=1))
    
    # Branch 1: DPD <= 3 Days (Grace)
    d.add(Line(120, 78, 138, 78, strokeColor=colors.HexColor("#059669"), strokeWidth=1))
    d.add(Polygon([138, 78, 134, 80, 134, 76], fillColor=colors.HexColor("#059669"), strokeColor=None))
    d.add(Rect(138, 67, 108, 22, fillColor=colors.white, strokeColor=colors.HexColor("#374151"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(192, 79, "Days Past Due <= 3 Days", fontName="Helvetica-Bold", fontSize=6.2, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(192, 71, "Statutory Grace Period", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#059669"), textAnchor="middle"))
    
    d.add(Line(246, 78, 268, 78, strokeColor=colors.HexColor("#059669"), strokeWidth=1))
    d.add(Polygon([268, 78, 264, 80, 264, 76], fillColor=colors.HexColor("#059669"), strokeColor=None))
    d.add(Rect(268, 67, 252, 22, fillColor=colors.HexColor("#D1FAE5"), strokeColor=colors.HexColor("#6EE7B7"), strokeWidth=0.5, rx=2, ry=2))
    d.add(String(394, 79, "ACTION: ZERO PENAL CHARGE ACCRUED (INR 0.00)", fontName="Helvetica-Bold", fontSize=6.2, fillColor=colors.HexColor("#065F46"), textAnchor="middle"))
    d.add(String(394, 71, "Dispatch Non-Coercive In-App & Messaging Reminder", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#047857"), textAnchor="middle"))
    
    # Branch 2: 4 <= DPD <= 30 Days (Delinquency)
    d.add(Line(120, 49, 138, 49, strokeColor=colors.HexColor("#D97706"), strokeWidth=1))
    d.add(Polygon([138, 49, 134, 51, 134, 47], fillColor=colors.HexColor("#D97706"), strokeColor=None))
    d.add(Rect(138, 38, 108, 22, fillColor=colors.white, strokeColor=colors.HexColor("#374151"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(192, 50, "4 <= Days Past Due <= 30", fontName="Helvetica-Bold", fontSize=6.2, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(192, 42, "Standard Delinquency Window", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#D97706"), textAnchor="middle"))
    
    d.add(Line(246, 49, 268, 49, strokeColor=colors.HexColor("#D97706"), strokeWidth=1))
    d.add(Polygon([268, 49, 264, 51, 264, 47], fillColor=colors.HexColor("#D97706"), strokeColor=None))
    d.add(Rect(268, 38, 252, 22, fillColor=colors.HexColor("#FEF3C7"), strokeColor=colors.HexColor("#FCD34D"), strokeWidth=0.5, rx=2, ry=2))
    d.add(String(394, 50, "ACTION: APPLY NON-COMPOUNDING PENAL FEE (MAX 2%/MO)", fontName="Helvetica-Bold", fontSize=6.2, fillColor=colors.HexColor("#92400E"), textAnchor="middle"))
    d.add(String(394, 42, "Fees Tracked in Auxiliary Ledger; Prohibited from Capitalizing into Principal", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#B45309"), textAnchor="middle"))
    
    # Branch 3: DPD > 30 Days (Critical Default)
    d.add(Line(120, 20, 138, 20, strokeColor=colors.HexColor("#DC2626"), strokeWidth=1))
    d.add(Polygon([138, 20, 134, 22, 134, 18], fillColor=colors.HexColor("#DC2626"), strokeColor=None))
    d.add(Rect(138, 9, 108, 22, fillColor=colors.white, strokeColor=colors.HexColor("#374151"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(192, 21, "Days Past Due > 30 Days", fontName="Helvetica-Bold", fontSize=6.2, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(192, 13, "Critical Delinquency Stage", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#DC2626"), textAnchor="middle"))
    
    d.add(Line(246, 20, 268, 20, strokeColor=colors.HexColor("#DC2626"), strokeWidth=1))
    d.add(Polygon([268, 20, 264, 22, 264, 18], fillColor=colors.HexColor("#DC2626"), strokeColor=None))
    d.add(Rect(268, 9, 252, 22, fillColor=colors.HexColor("#FEE2E2"), strokeColor=colors.HexColor("#FCA5A5"), strokeWidth=0.5, rx=2, ry=2))
    d.add(String(394, 21, "ACTION: FREEZE BORROWING PRIVILEGES & INITIATE LEGAL NOTICE", fontName="Helvetica-Bold", fontSize=6.2, fillColor=colors.HexColor("#991B1B"), textAnchor="middle"))
    d.add(String(394, 13, "Prepare Section 4 NI Act Summary Recovery Dossier (Order XXXVII CPC)", fontName="Helvetica", fontSize=5.5, fillColor=colors.HexColor("#B91C1C"), textAnchor="middle"))
    
    return d

# =========================================================================
# MAIN PDF BUILDER
# =========================================================================

def build_pdf(filename="Lab5_Requirement_Elicitation_Loanzo.pdf"):
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
    
    h1_style = ParagraphStyle('H1', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=10.5, leading=13.5, textColor=c_primary, spaceBefore=7, spaceAfter=3)
    h2_style = ParagraphStyle('H2', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=8.5, leading=11, textColor=c_primary, spaceBefore=4, spaceAfter=2)
    
    body_style = ParagraphStyle('Body', parent=styles['Normal'], fontName='Helvetica', fontSize=7.6, leading=10.5, textColor=c_secondary)
    
    th_style = ParagraphStyle('TH', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=7.2, leading=9.2, textColor=c_primary)
    th_center = ParagraphStyle('THC', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=7.2, leading=9.2, textColor=c_primary, alignment=1)
    
    td_style = ParagraphStyle('TD', parent=styles['Normal'], fontName='Helvetica', fontSize=6.8, leading=8.8, textColor=c_secondary)
    td_bold = ParagraphStyle('TDB', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=6.8, leading=8.8, textColor=c_primary)
    td_code = ParagraphStyle('TDCode', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=6.8, leading=8.8, textColor=c_primary)
    td_center = ParagraphStyle('TDC', parent=styles['Normal'], fontName='Helvetica', fontSize=6.8, leading=8.8, textColor=c_secondary, alignment=1)
    td_center_bold = ParagraphStyle('TDCB', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=6.8, leading=8.8, textColor=c_primary, alignment=1)

    story = []

    # =========================================================================
    # DOCUMENT HEADER
    # =========================================================================
    story.append(Paragraph("Lab 5: Requirement Elicitation – II", title_style))
    story.append(Spacer(1, 2))
    story.append(Paragraph("System Specification: LOANZO (Peer-to-Peer Microfinance and Purpose-Bound Social Lending Platform)", subtitle_style))
    story.append(Spacer(1, 3))
    story.append(HRFlowable(width="100%", thickness=1, color=c_primary, spaceBefore=2, spaceAfter=5))

    # Activities Overview
    story.append(Paragraph("<b>Activities Conducted:</b>", h1_style))
    story.append(Paragraph("1. Refinement of Functional and Non-Functional Requirements<br/>2. Representation of Requirements Using Decision Trees<br/>3. Representation of Requirements Using Decision Tables", body_style))
    story.append(Spacer(1, 2))
    story.append(Paragraph("<b>Document Outcome:</b>", h1_style))
    story.append(Paragraph("Structured Requirements Document (SRD)", body_style))
    story.append(Spacer(1, 5))

    # =========================================================================
    # 1. REFINE FUNCTIONAL AND NON-FUNCTIONAL REQUIREMENTS
    # =========================================================================
    story.append(Paragraph("1. Refinement of Functional and Non-Functional Requirements", h1_style))
    story.append(Paragraph(
        "High-level requirements elicited in Lab 4 are refined into granular engineering specifications with explicit triggers, business rules, operational constraints, and measurable quality thresholds.",
        body_style
    ))
    story.append(Spacer(1, 3))

    story.append(Paragraph("1.1 Refined Functional Specifications", h2_style))
    fr_refined_data = [
        [Paragraph("Identifier", th_style), Paragraph("Refined Requirement Domain", th_style), Paragraph("Pre-Condition, Detailed Business Logic & Acceptance Criteria", th_style)],
        [
            Paragraph("RFR-01", td_code),
            Paragraph("Purpose-Bound Tranche Disbursal", td_bold),
            Paragraph("<b>Trigger:</b> Borrower initiates milestone disbursement request.<br/>"
                      "<b>Logic:</b> The system extracts vendor UPI VPA from the commercial tax invoice and executes an automated INR 1.00 penny-drop routine. If the beneficiary account name matches the invoice name (match >= 85%), funds transfer directly to the vendor VPA via NPCI rails. Zero capital is released as unmonitored cash.<br/>"
                      "<b>Acceptance Criteria:</b> 100% of disbursed capital must settle directly into verified vendor accounts.", td_style)
        ],
        [
            Paragraph("RFR-02", td_code),
            Paragraph("Multi-Asset Collateral Valuation", td_bold),
            Paragraph("<b>Trigger:</b> Borrower submits asset pledge documentation.<br/>"
                      "<b>Logic:</b> The system categorizes collateral into 5 classes and enforces statutory Loan-to-Value (LTV) caps: Gold Bullion (75% IBJA benchmark), Property Deeds (55%), Motor Vehicles (60% Form 34 hypothecated), Commercial IT Equipment (45%), and Accounts Receivable (75%). Asset valuations exceeding INR 100,000 mandate dual-agent independent inspection.<br/>"
                      "<b>Acceptance Criteria:</b> Loan requests exceeding statutory LTV ceilings are programmatically blocked.", td_style)
        ],
        [
            Paragraph("RFR-03", td_code),
            Paragraph("Statutory Promissory Note Synthesis", td_bold),
            Paragraph("<b>Trigger:</b> Counterparty bid accepted in marketplace.<br/>"
                      "<b>Logic:</b> The system synthesizes an unconditional Promissory Note compliant with Section 4 of the Negotiable Instruments Act, 1881, accompanied by a Key Fact Statement (KFS). Documents are executed via Aadhaar OTP e-Sign and sealed with a SHA-256 hash, GPS coordinates, and timestamp for legal admissibility under Section 65B Indian Evidence Act.<br/>"
                      "<b>Acceptance Criteria:</b> Contract generation must complete in <= 2.5 seconds on-device with zero tamper vulnerability.", td_style)
        ],
        [
            Paragraph("RFR-04", td_code),
            Paragraph("Non-Compounding Regulatory Penalties", td_bold),
            Paragraph("<b>Trigger:</b> Installment overdue status detected.<br/>"
                      "<b>Logic:</b> The system provides a 3-day grace period with zero penalty. From Day 4, daily penal charges accrue strictly on overdue principal, capped at a maximum 2.0% monthly ceiling per regulatory guidelines (RBI/2023-24/53). Penal charges are tracked separately and never capitalized into loan principal.<br/>"
                      "<b>Acceptance Criteria:</b> Cumulative monthly penalty must never exceed 2% of overdue principal balance.", td_style)
        ]
    ]
    t_fr_ref = Table(fr_refined_data, colWidths=[45, 125, 362])
    t_fr_ref.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 2.5),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 2.5),
        ('LEFTPADDING', (0, 0), (-1, -1), 4),
        ('RIGHTPADDING', (0, 0), (-1, -1), 4),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ]))
    story.append(t_fr_ref)
    story.append(Spacer(1, 5))

    story.append(Paragraph("1.2 Refined Non-Functional Specifications", h2_style))
    nfr_refined_data = [
        [Paragraph("Identifier", th_style), Paragraph("Quality Attribute", th_style), Paragraph("Operational Requirement & Quantitative Threshold", th_style), Paragraph("Verification Method", th_style)],
        [Paragraph("RNFR-01", td_code), Paragraph("Database Performance", td_bold), Paragraph("Local database query execution shall not exceed 15ms (95th percentile) on baseline 2GB RAM devices.", td_style), Paragraph("Automated Profiler Suite", td_style)],
        [Paragraph("RNFR-02", td_code), Paragraph("Application Cold Start", td_bold), Paragraph("Launch to interactive Home Dashboard state shall complete in <= 1.8 seconds with zero UI flicker.", td_style), Paragraph("Macrobenchmark Test", td_style)],
        [Paragraph("RNFR-03", td_code), Paragraph("Data Confidentiality", td_bold), Paragraph("All stored KYC credentials and documents shall be encrypted at rest using AES-256-GCM via hardware KeyStore.", td_style), Paragraph("Static / Memory Heap Audit", td_style)],
        [Paragraph("RNFR-04", td_code), Paragraph("Offline Fault Tolerance", td_bold), Paragraph("Core features (view loans, calculate EMIs, draft agreements) shall execute with 0% network connectivity.", td_style), Paragraph("Signal Disconnection Test", td_style)],
        [Paragraph("RNFR-05", td_code), Paragraph("Atomic Synchronization", td_bold), Paragraph("Offline mutations must reconcile within 30 seconds of network restoration via transactional WorkManager queues.", td_style), Paragraph("Network Recovery Test", td_style)],
        [Paragraph("RNFR-06", td_code), Paragraph("Regulatory Non-Custodial", td_bold), Paragraph("Platform shall maintain exactly INR 0.00 custodial deposit balance; 100% direct account settlements.", td_style), Paragraph("Financial Ledger Audit", td_style)]
    ]
    t_nfr_ref = Table(nfr_refined_data, colWidths=[45, 95, 272, 120])
    t_nfr_ref.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 2.5),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 2.5),
        ('LEFTPADDING', (0, 0), (-1, -1), 4),
        ('RIGHTPADDING', (0, 0), (-1, -1), 4),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ]))
    story.append(t_nfr_ref)
    story.append(Spacer(1, 8))

    # =========================================================================
    # 2. REPRESENT REQUIREMENTS USING DECISION TREES (VECTOR GRAPHICS)
    # =========================================================================
    story.append(Paragraph("2. Representation of Requirements Using Decision Trees", h1_style))
    story.append(Paragraph(
        "Decision trees model complex branching logic, ensuring every conditional pathway results in a deterministic and verifiable system action.",
        body_style
    ))
    story.append(Spacer(1, 3))

    # Visual Tree 1
    story.append(Paragraph("<b>Decision Tree 1: Purpose-Bound Milestone Tranche Disbursement Policy</b>", h2_style))
    story.append(draw_tree_1())
    story.append(Spacer(1, 5))

    # Visual Tree 2
    story.append(Paragraph("<b>Decision Tree 2: Collateral Valuation, LTV Ceiling, and Inspection Dispatch Protocol</b>", h2_style))
    story.append(draw_tree_2())
    story.append(Spacer(1, 5))

    # Visual Tree 3
    story.append(Paragraph("<b>Decision Tree 3: Delinquency Management, Penal Accrual, and Restructuring Workflow</b>", h2_style))
    story.append(draw_tree_3())
    story.append(Spacer(1, 8))

    # =========================================================================
    # 3. REPRESENT REQUIREMENTS USING DECISION TABLES
    # =========================================================================
    story.append(Paragraph("3. Representation of Requirements Using Decision Tables", h1_style))
    story.append(Paragraph(
        "Decision tables provide exhaustive verification of multivariate logic, mapping combinations of input conditions to discrete system actions with zero ambiguity.",
        body_style
    ))
    story.append(Spacer(1, 3))

    # Decision Table 1
    story.append(Paragraph("<b>Decision Table 1: Loan Application Underwriting and Marketplace Bidding Gate</b>", h2_style))
    dtab1_data = [
        [Paragraph("Element", th_style), Paragraph("Condition / Action Parameter", th_style), Paragraph("R1", th_center), Paragraph("R2", th_center), Paragraph("R3", th_center), Paragraph("R4", th_center), Paragraph("R5", th_center)],
        [Paragraph("C1", td_bold), Paragraph("DigiLocker Identity Verified (Aadhaar and PAN)?", td_style), Paragraph("Y", td_center), Paragraph("Y", td_center), Paragraph("Y", td_center), Paragraph("Y", td_center), Paragraph("N", td_center)],
        [Paragraph("C2", td_bold), Paragraph("On-Device Facial Liveness Verification Passed?", td_style), Paragraph("Y", td_center), Paragraph("Y", td_center), Paragraph("Y", td_center), Paragraph("N", td_center), Paragraph("—", td_center)],
        [Paragraph("C3", td_bold), Paragraph("Historical Unresolved Default on Record?", td_style), Paragraph("N", td_center), Paragraph("N", td_center), Paragraph("Y", td_center), Paragraph("—", td_center), Paragraph("—", td_center)],
        [Paragraph("C4", td_bold), Paragraph("Requested Borrowing APR Within Statutory Ceiling?", td_style), Paragraph("Y", td_center), Paragraph("N", td_center), Paragraph("—", td_center), Paragraph("—", td_center), Paragraph("—", td_center)],
        [Paragraph("A1", td_code), Paragraph("Publish Loan Requisition to Social Bidding Marketplace", td_bold), Paragraph("X", td_center_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center)],
        [Paragraph("A2", td_code), Paragraph("Prompt Mandatory APR Adjustment to Comply with Cap", td_bold), Paragraph("", td_center), Paragraph("X", td_center_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center)],
        [Paragraph("A3", td_code), Paragraph("Reject Origination Request and Restrict Lending Privileges", td_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("X", td_center_bold), Paragraph("", td_center), Paragraph("", td_center)],
        [Paragraph("A4", td_code), Paragraph("Prompt Secondary Facial Liveness Verification Challenge", td_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center), Paragraph("X", td_center_bold), Paragraph("", td_center)],
        [Paragraph("A5", td_code), Paragraph("Halt Origination Workflow; Redirect to KYC Verification Portal", td_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center), Paragraph("X", td_center_bold)]
    ]
    t_dtab1 = Table(dtab1_data, colWidths=[30, 272, 46, 46, 46, 46, 46])
    t_dtab1.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 1.8),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 1.8),
        ('LEFTPADDING', (0, 0), (-1, -1), 3),
        ('RIGHTPADDING', (0, 0), (-1, -1), 3),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
    ]))
    story.append(t_dtab1)
    story.append(Spacer(1, 5))

    # Decision Table 2
    story.append(Paragraph("<b>Decision Table 2: Purpose-Bound Milestone Tranche Disbursement Verification Gate</b>", h2_style))
    dtab2_data = [
        [Paragraph("Element", th_style), Paragraph("Condition / Action Parameter", th_style), Paragraph("R1", th_center), Paragraph("R2", th_center), Paragraph("R3", th_center), Paragraph("R4", th_center)],
        [Paragraph("C1", td_bold), Paragraph("Commercial Tax Invoice Uploaded by Borrower?", td_style), Paragraph("Y", td_center), Paragraph("Y", td_center), Paragraph("Y", td_center), Paragraph("N", td_center)],
        [Paragraph("C2", td_bold), Paragraph("Penny-Drop Beneficiary Name Matches Invoice (>= 85%)?", td_style), Paragraph("Y", td_center), Paragraph("Y", td_center), Paragraph("N", td_center), Paragraph("—", td_center)],
        [Paragraph("C3", td_bold), Paragraph("Prior Milestone Physical Utilization Verified by Field Agent?", td_style), Paragraph("Y", td_center), Paragraph("N", td_center), Paragraph("—", td_center), Paragraph("—", td_center)],
        [Paragraph("A1", td_code), Paragraph("Execute Direct Tranche Settlement to Vendor UPI VPA", td_bold), Paragraph("X", td_center_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center)],
        [Paragraph("A2", td_code), Paragraph("Hold Tranche; Dispatch Field Agent for Physical Audit", td_bold), Paragraph("", td_center), Paragraph("X", td_center_bold), Paragraph("", td_center), Paragraph("", td_center)],
        [Paragraph("A3", td_code), Paragraph("Lock Tranche; Log Security Incident for Compliance Review", td_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("X", td_center_bold), Paragraph("", td_center)],
        [Paragraph("A4", td_code), Paragraph("Prompt User to Submit Commercial Invoice Documentation", td_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center), Paragraph("X", td_center_bold)]
    ]
    t_dtab2 = Table(dtab2_data, colWidths=[30, 312, 47, 47, 47, 47])
    t_dtab2.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 1.8),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 1.8),
        ('LEFTPADDING', (0, 0), (-1, -1), 3),
        ('RIGHTPADDING', (0, 0), (-1, -1), 3),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
    ]))
    story.append(t_dtab2)
    story.append(Spacer(1, 5))

    # Decision Table 3
    story.append(Paragraph("<b>Decision Table 3: Collateral Valuation, Haircut Enforcement, and Field Audit Routing</b>", h2_style))
    dtab3_data = [
        [Paragraph("Element", th_style), Paragraph("Condition / Action Parameter", th_style), Paragraph("R1", th_center), Paragraph("R2", th_center), Paragraph("R3", th_center), Paragraph("R4", th_center), Paragraph("R5", th_center)],
        [Paragraph("C1", td_bold), Paragraph("Pledged Collateral Asset Category", td_style), Paragraph("Gold", td_center), Paragraph("Property", td_center), Paragraph("Vehicle", td_center), Paragraph("IT Assets", td_center), Paragraph("Any", td_center)],
        [Paragraph("C2", td_bold), Paragraph("Primary Ownership Documentation Formally Verified?", td_style), Paragraph("Y", td_center), Paragraph("Y", td_center), Paragraph("Y", td_center), Paragraph("Y", td_center), Paragraph("N", td_center)],
        [Paragraph("C3", td_bold), Paragraph("Declared Asset Valuation Exceeds INR 100,000?", td_style), Paragraph("N", td_center), Paragraph("Y", td_center), Paragraph("N", td_center), Paragraph("Y", td_center), Paragraph("—", td_center)],
        [Paragraph("A1", td_code), Paragraph("Approve Collateral with Statutory 75% LTV Cap (IBJA)", td_bold), Paragraph("X", td_center_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center)],
        [Paragraph("A2", td_code), Paragraph("Approve Collateral with 55% LTV Cap (Non-Encumbrance)", td_bold), Paragraph("", td_center), Paragraph("X", td_center_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center)],
        [Paragraph("A3", td_code), Paragraph("Approve Collateral with 60% LTV Cap (Form 34 Hypothecation)", td_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("X", td_center_bold), Paragraph("", td_center), Paragraph("", td_center)],
        [Paragraph("A4", td_code), Paragraph("Approve Collateral with 45% LTV Cap (Serial Checked)", td_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center), Paragraph("X", td_center_bold), Paragraph("", td_center)],
        [Paragraph("A5", td_code), Paragraph("Mandate Dual-Agent Independent Inspection Protocol", td_bold), Paragraph("", td_center), Paragraph("X", td_center_bold), Paragraph("", td_center), Paragraph("X", td_center_bold), Paragraph("", td_center)],
        [Paragraph("A6", td_code), Paragraph("Reject Collateral Pledge; Request Ownership Documents", td_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center), Paragraph("X", td_center_bold)]
    ]
    t_dtab3 = Table(dtab3_data, colWidths=[30, 272, 46, 46, 46, 46, 46])
    t_dtab3.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 1.8),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 1.8),
        ('LEFTPADDING', (0, 0), (-1, -1), 3),
        ('RIGHTPADDING', (0, 0), (-1, -1), 3),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
    ]))
    story.append(t_dtab3)
    story.append(Spacer(1, 8))

    # =========================================================================
    # 4. OUTCOME: STRUCTURED REQUIREMENTS DOCUMENT
    # =========================================================================
    story.append(Paragraph("4. Outcome: Structured Requirements Document (SRD)", h1_style))
    story.append(Paragraph(
        "The Structured Requirements Document compiles refined functional and quality specifications into a normalized traceability matrix ready for engineering design and test verification.",
        body_style
    ))
    story.append(Spacer(1, 3))

    srd_data = [
        [
            Paragraph("Identifier", th_style),
            Paragraph("Requirement Title", th_style),
            Paragraph("Pre-Condition / Trigger", th_style),
            Paragraph("Operational Specification", th_style),
            Paragraph("System State", th_style),
            Paragraph("Verification", th_style)
        ],
        [
            Paragraph("SRD-01", td_code), Paragraph("Multi-Factor Authentication", td_bold),
            Paragraph("User initiates application launch", td_style),
            Paragraph("Enforce OTP, password credentials, and BiometricPrompt hardware check.", td_style),
            Paragraph("AUTHENTICATED", td_style), Paragraph("Security Test", td_style)
        ],
        [
            Paragraph("SRD-02", td_code), Paragraph("DigiLocker Identity Ingestion", td_bold),
            Paragraph("Applicant registration triggered", td_style),
            Paragraph("Query DigiLocker for Aadhaar/PAN; run ML Kit facial liveness detection.", td_style),
            Paragraph("VERIFIED_ID", td_style), Paragraph("Integration Test", td_style)
        ],
        [
            Paragraph("SRD-03", td_code), Paragraph("Cryptographic Data Vault", td_bold),
            Paragraph("Sensitive document stored locally", td_style),
            Paragraph("Encrypt artifacts via AES-256-GCM backed by hardware Android KeyStore.", td_style),
            Paragraph("VAULT_SECURED", td_style), Paragraph("Static Code Audit", td_style)
        ],
        [
            Paragraph("SRD-04", td_code), Paragraph("Reverse-Auction Marketplace", td_bold),
            Paragraph("Loan requisition published", td_style),
            Paragraph("Verified retail lenders submit competitive APR and tenure proposals.", td_style),
            Paragraph("BIDS_ACTIVE", td_style), Paragraph("Functional Test", td_style)
        ],
        [
            Paragraph("SRD-05", td_code), Paragraph("Purpose-Bound Tranche Escrow", td_bold),
            Paragraph("Disbursement milestone reached", td_style),
            Paragraph("Route capital directly to verified vendor UPI VPA; bypass borrower.", td_style),
            Paragraph("SETTLED_VENDOR", td_style), Paragraph("End-to-End API", td_style)
        ],
        [
            Paragraph("SRD-06", td_code), Paragraph("Penny-Drop Account Match", td_bold),
            Paragraph("Vendor account submitted", td_style),
            Paragraph("Execute automated INR 1.00 validation matching commercial invoice name.", td_style),
            Paragraph("NAME_VERIFIED", td_style), Paragraph("API Mocking Test", td_style)
        ],
        [
            Paragraph("SRD-07", td_code), Paragraph("Statutory Promissory Note", td_bold),
            Paragraph("Terms accepted by counterparties", td_style),
            Paragraph("Synthesize 4-page Section 4 NI Act promissory note with Key Fact Statement.", td_style),
            Paragraph("NOTE_SYNTHESIZED", td_style), Paragraph("Document Parser", td_style)
        ],
        [
            Paragraph("SRD-08", td_code), Paragraph("Aadhaar Electronic Signature", td_bold),
            Paragraph("Promissory note generated", td_style),
            Paragraph("Execute Aadhaar OTP e-Sign; embed SHA-256 tamper-evident digital digest.", td_style),
            Paragraph("CONTRACT_SIGNED", td_style), Paragraph("API Audit Test", td_style)
        ],
        [
            Paragraph("SRD-09", td_code), Paragraph("Multi-Asset Collateral Escrow", td_bold),
            Paragraph("Asset pledged by borrower", td_style),
            Paragraph("Enforce statutory LTV ceilings across Gold (75%), Property (55%), etc.", td_style),
            Paragraph("COLLATERAL_LOCKED", td_style), Paragraph("Mathematical Unit", td_style)
        ],
        [
            Paragraph("SRD-10", td_code), Paragraph("Dual-Agent Consensus Protocol", td_bold),
            Paragraph("Valuation exceeds INR 100,000", td_style),
            Paragraph("Require independent inspection reports from two certified ground officers.", td_style),
            Paragraph("DUAL_VERIFIED", td_style), Paragraph("Workflow Test", td_style)
        ],
        [
            Paragraph("SRD-11", td_code), Paragraph("Non-Compounding Overdue Fee", td_bold),
            Paragraph("Installment overdue > 3 days", td_style),
            Paragraph("Apply daily fee strictly capped at statutory 2.0% monthly ceiling.", td_style),
            Paragraph("PENALTY_ACCRUED", td_style), Paragraph("Unit Test Suite", td_style)
        ],
        [
            Paragraph("SRD-12", td_code), Paragraph("Distress Restructuring & NOC", td_bold),
            Paragraph("Borrower distress verified", td_style),
            Paragraph("Restructure amortization schedule; issue digital No Objection Certificate.", td_style),
            Paragraph("RESTRUCTURED", td_style), Paragraph("State Regression", td_style)
        ]
    ]

    t_srd = Table(srd_data, colWidths=[40, 105, 95, 152, 75, 65])
    t_srd.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 1.8),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 1.8),
        ('LEFTPADDING', (0, 0), (-1, -1), 3),
        ('RIGHTPADDING', (0, 0), (-1, -1), 3),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
    ]))
    story.append(t_srd)

    doc.build(story, canvasmaker=ProfessionalNumberedCanvas)
    print(f"Professional Lab 5 PDF with Vector Trees built: {filename}")

if __name__ == "__main__":
    out_pdf = "Lab5_Requirement_Elicitation_Loanzo.pdf"
    build_pdf(out_pdf)
    if os.path.exists("docs"):
        docs_pdf = os.path.join("docs", "Lab5_Requirement_Elicitation_Loanzo.pdf")
        build_pdf(docs_pdf)
