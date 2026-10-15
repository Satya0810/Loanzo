import os
from reportlab.lib import colors
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, HRFlowable, PageBreak
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
        self.drawString(40, 22, "Lab 5: Requirement Elicitation - II  |  Project: Loanzo Protocol")
        self.drawRightString(572, 22, f"Page {self._pageNumber} of {page_count}")
        self.setStrokeColor(colors.HexColor("#D1D5DB"))
        self.setLineWidth(0.5)
        self.line(40, 32, 572, 32)
        self.restoreState()

# =========================================================================
# =========================================================================
# MONOCHROME VECTOR DECISION TREES (PLAIN-ENGLISH, EXPANDED PADDING)
# =========================================================================

def draw_monochrome_tree_1():
    """Decision Tree 1: Safe Loan Payout & Seller Verification (Plain-English & Safe Padding)"""
    d = Drawing(532, 124)
    # Background Box
    d.add(Rect(0, 0, 532, 124, fillColor=colors.white, strokeColor=colors.HexColor("#D1D5DB"), strokeWidth=0.75, rx=3, ry=3))
    
    # Level 0: Trigger Node (Centered at y=63)
    d.add(Rect(6, 49, 76, 28, fillColor=colors.HexColor("#111827"), strokeColor=colors.HexColor("#111827"), rx=2, ry=2))
    d.add(String(44, 65, "Loan Payout Request", fontName="Helvetica-Bold", fontSize=5.6, fillColor=colors.white, textAnchor="middle"))
    d.add(String(44, 55, "Borrower Asks for Funds", fontName="Helvetica", fontSize=4.9, fillColor=colors.HexColor("#9CA3AF"), textAnchor="middle"))
    
    # Arrow: Level 0 -> Level 1
    d.add(Line(82, 63, 96, 63, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([96, 63, 92, 65, 92, 61], fillColor=colors.HexColor("#374151"), strokeColor=None))
    
    # Level 1: Gate 1 (Valid Seller Invoice Uploaded?)
    d.add(Rect(96, 49, 82, 28, fillColor=colors.white, strokeColor=colors.HexColor("#374151"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(137, 65, "Check 1: Seller Bill?", fontName="Helvetica-Bold", fontSize=5.6, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(137, 55, "Valid bill from merchant", fontName="Helvetica", fontSize=4.9, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    # Level 1 Branch NO (Up) -> Action 1A
    d.add(Line(178, 63, 186, 107, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Line(186, 107, 196, 107, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([196, 107, 192, 109, 192, 105], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(String(184, 111, "NO", fontName="Helvetica-Bold", fontSize=5.2, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    
    d.add(Rect(196, 95, 114, 24, fillColor=colors.HexColor("#F3F4F6"), strokeColor=colors.HexColor("#6B7280"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(253, 109.5, "ACTION 1A: REJECT REQUEST", fontName="Helvetica-Bold", fontSize=5.5, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(253, 100.5, "Ask borrower for valid seller bill", fontName="Helvetica", fontSize=4.8, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    # Level 1 Branch YES (Down) -> Gate 2
    d.add(Line(178, 63, 186, 42, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Line(186, 42, 196, 42, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([196, 42, 192, 44, 192, 40], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(String(184, 46, "YES", fontName="Helvetica-Bold", fontSize=5.2, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    
    # Level 2: Gate 2 (Prior Milestone Verified?)
    d.add(Rect(196, 28, 86, 28, fillColor=colors.white, strokeColor=colors.HexColor("#374151"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(239, 44, "Check 2: Prior Milestone?", fontName="Helvetica-Bold", fontSize=5.6, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(239, 34, "Past work verified (or 1st payout)", fontName="Helvetica", fontSize=4.8, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    # Level 2 Branch NO (Middle Up) -> Action 2A
    d.add(Line(282, 42, 290, 78, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Line(290, 78, 300, 78, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([300, 78, 296, 80, 296, 76], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(String(288, 82, "NO", fontName="Helvetica-Bold", fontSize=5.0, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    
    d.add(Rect(300, 66, 114, 24, fillColor=colors.HexColor("#F3F4F6"), strokeColor=colors.HexColor("#6B7280"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(357, 80.5, "ACTION 2A: PAUSE PAYMENT", fontName="Helvetica-Bold", fontSize=5.5, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(357, 71.5, "Send officer to inspect project site", fontName="Helvetica", fontSize=4.8, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    # Level 2 Branch YES (Forward) -> Gate 3
    d.add(Line(282, 42, 290, 28, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Line(290, 28, 300, 28, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([300, 28, 296, 30, 296, 26], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(String(288, 32, "YES", fontName="Helvetica-Bold", fontSize=5.0, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    
    # Level 3: Gate 3 (Penny-Drop Test Match*)
    d.add(Rect(300, 14, 88, 28, fillColor=colors.white, strokeColor=colors.HexColor("#374151"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(344, 30, "Check 3: Bank Account?*", fontName="Helvetica-Bold", fontSize=5.6, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(344, 20, "INR 1.00 test seller name match", fontName="Helvetica", fontSize=4.8, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    # Level 3 Branch 1: <80% -> Action 3A
    d.add(Line(388, 28, 398, 74, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Line(398, 74, 408, 74, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([408, 74, 404, 76, 404, 72], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(String(396, 78, "<80%", fontName="Helvetica-Bold", fontSize=5.0, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    
    d.add(Rect(408, 62, 120, 24, fillColor=colors.HexColor("#F3F4F6"), strokeColor=colors.HexColor("#6B7280"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(468, 76.5, "ACTION 3A: FREEZE TRANSFER", fontName="Helvetica-Bold", fontSize=5.5, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(468, 67.5, "Wrong account name; alert safety team", fontName="Helvetica", fontSize=4.8, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    # Level 3 Branch 2: 80-94% -> Action 3B
    d.add(Line(388, 28, 398, 45, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Line(398, 45, 408, 45, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([408, 45, 404, 47, 404, 43], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(String(396, 49, "80-94%", fontName="Helvetica-Bold", fontSize=4.8, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    
    d.add(Rect(408, 33, 120, 24, fillColor=colors.HexColor("#E5E7EB"), strokeColor=colors.HexColor("#6B7280"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(468, 47.5, "ACTION 3B: MANUAL REVIEW", fontName="Helvetica-Bold", fontSize=5.5, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(468, 38.5, "Officer checks shop business name", fontName="Helvetica", fontSize=4.8, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    # Level 3 Branch 3: >=95% -> Level 4: Action 4 (Settlement)
    d.add(Line(388, 28, 398, 17, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Line(398, 17, 408, 17, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([408, 17, 404, 19, 404, 15], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(String(396, 21, ">=95%", fontName="Helvetica-Bold", fontSize=5.0, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    
    d.add(Rect(408, 5, 120, 24, fillColor=colors.white, strokeColor=colors.HexColor("#111827"), strokeWidth=1.0, rx=2, ry=2))
    d.add(String(468, 19.5, "ACTION 4: PAY SELLER DIRECTLY", fontName="Helvetica-Bold", fontSize=5.5, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(468, 10.5, "Instant payment to merchant (UPI VPA*)", fontName="Helvetica", fontSize=4.8, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    return d

def draw_monochrome_tree_2():
    """Decision Tree 2: Collateral Valuation, Haircuts & Vault Custody (Plain-English & Safe Padding)"""
    d = Drawing(532, 124)
    # Background Box
    d.add(Rect(0, 0, 532, 124, fillColor=colors.white, strokeColor=colors.HexColor("#D1D5DB"), strokeWidth=0.75, rx=3, ry=3))
    
    # Level 0: Trigger Node (Centered at y=63)
    d.add(Rect(6, 49, 76, 28, fillColor=colors.HexColor("#111827"), strokeColor=colors.HexColor("#111827"), rx=2, ry=2))
    d.add(String(44, 65, "Collateral Pledged", fontName="Helvetica-Bold", fontSize=5.6, fillColor=colors.white, textAnchor="middle"))
    d.add(String(44, 55, "Borrower Offers Security", fontName="Helvetica", fontSize=4.9, fillColor=colors.HexColor("#9CA3AF"), textAnchor="middle"))
    
    # Connector: Level 0 -> Vertical Bus
    d.add(Line(82, 63, 94, 63, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Line(94, 107, 94, 20, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    
    # Level 1 & 2: Asset Class 1 (Gold Bullion)
    d.add(Line(94, 107, 104, 107, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([104, 107, 100, 109, 100, 105], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(Rect(104, 95, 88, 24, fillColor=colors.white, strokeColor=colors.HexColor("#374151"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(148, 109.5, "Gold Bullion / Jewelry", fontName="Helvetica-Bold", fontSize=5.5, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(148, 100.5, "Purity & Weight Tested", fontName="Helvetica", fontSize=4.9, fillColor=colors.HexColor("#6B7280"), textAnchor="middle"))
    
    d.add(Line(192, 107, 202, 107, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([202, 107, 198, 109, 198, 105], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(Rect(202, 95, 82, 24, fillColor=colors.HexColor("#F3F4F6"), strokeColor=colors.HexColor("#9CA3AF"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(243, 109.5, "Max 75% Loan (LTV*)", fontName="Helvetica-Bold", fontSize=5.6, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(243, 100.5, "Based on daily gold rate", fontName="Helvetica", fontSize=4.8, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    # Level 1 & 2: Asset Class 2 (Property Title Deeds)
    d.add(Line(94, 78, 104, 78, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([104, 78, 100, 80, 100, 76], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(Rect(104, 66, 88, 24, fillColor=colors.white, strokeColor=colors.HexColor("#374151"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(148, 80.5, "Land & House Deeds", fontName="Helvetica-Bold", fontSize=5.5, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(148, 71.5, "Ownership Papers Clear", fontName="Helvetica", fontSize=4.9, fillColor=colors.HexColor("#6B7280"), textAnchor="middle"))
    
    d.add(Line(192, 78, 202, 78, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([202, 78, 198, 80, 198, 76], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(Rect(202, 66, 82, 24, fillColor=colors.HexColor("#F3F4F6"), strokeColor=colors.HexColor("#9CA3AF"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(243, 80.5, "Max 55% Loan (LTV*)", fontName="Helvetica-Bold", fontSize=5.6, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(243, 71.5, "Based on govt circle rate", fontName="Helvetica", fontSize=4.8, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    # Level 1 & 2: Asset Class 3 (Motor Vehicles)
    d.add(Line(94, 49, 104, 49, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([104, 49, 100, 51, 100, 47], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(Rect(104, 37, 88, 24, fillColor=colors.white, strokeColor=colors.HexColor("#374151"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(148, 51.5, "Cars or Bikes (Fleet)", fontName="Helvetica-Bold", fontSize=5.5, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(148, 42.5, "Loan marked on RC book*", fontName="Helvetica", fontSize=4.9, fillColor=colors.HexColor("#6B7280"), textAnchor="middle"))
    
    d.add(Line(192, 49, 202, 49, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([202, 49, 198, 51, 198, 47], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(Rect(202, 37, 82, 24, fillColor=colors.HexColor("#F3F4F6"), strokeColor=colors.HexColor("#9CA3AF"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(243, 51.5, "Max 60% Loan (LTV*)", fontName="Helvetica-Bold", fontSize=5.6, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(243, 42.5, "Based on vehicle value", fontName="Helvetica", fontSize=4.8, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    # Level 1 & 2: Asset Class 4 (Commercial IT Assets)
    d.add(Line(94, 20, 104, 20, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([104, 20, 100, 22, 100, 18], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(Rect(104, 8, 88, 24, fillColor=colors.white, strokeColor=colors.HexColor("#374151"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(148, 22.5, "Computers & Machinery", fontName="Helvetica-Bold", fontSize=5.5, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(148, 13.5, "Serial numbers verified", fontName="Helvetica", fontSize=4.9, fillColor=colors.HexColor("#6B7280"), textAnchor="middle"))
    
    d.add(Line(192, 20, 202, 20, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([202, 20, 198, 22, 198, 18], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(Rect(202, 8, 82, 24, fillColor=colors.HexColor("#F3F4F6"), strokeColor=colors.HexColor("#9CA3AF"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(243, 22.5, "Max 45% Loan (LTV*)", fontName="Helvetica-Bold", fontSize=5.6, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(243, 13.5, "Based on depreciated value", fontName="Helvetica", fontSize=4.8, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    # Level 3: Valuation Tier Routing Bus
    d.add(Line(284, 63, 296, 63, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Line(296, 96, 296, 26, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    
    # Tier 1 (<= 50k) -> Action 3A
    d.add(Line(296, 96, 306, 96, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([306, 96, 302, 98, 302, 94], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(Rect(306, 84, 98, 24, fillColor=colors.white, strokeColor=colors.HexColor("#6B7280"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(355, 98.5, "Under INR 50,000", fontName="Helvetica-Bold", fontSize=5.6, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(355, 89.5, "Small Asset Value", fontName="Helvetica", fontSize=4.9, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    d.add(Line(404, 96, 416, 96, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([416, 96, 412, 98, 412, 94], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(Rect(416, 84, 110, 24, fillColor=colors.HexColor("#F3F4F6"), strokeColor=colors.HexColor("#6B7280"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(471, 98.5, "ACTION: LIVE VIDEO CHECK", fontName="Helvetica-Bold", fontSize=5.5, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(471, 89.5, "Approve via live video call", fontName="Helvetica", fontSize=4.8, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    # Tier 2 (50k-100k) -> Action 3B
    d.add(Line(296, 61, 306, 61, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([306, 61, 302, 63, 302, 59], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(Rect(306, 49, 98, 24, fillColor=colors.HexColor("#F3F4F6"), strokeColor=colors.HexColor("#6B7280"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(355, 63.5, "INR 50,001 to 1 Lakh", fontName="Helvetica-Bold", fontSize=5.6, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(355, 54.5, "Medium Asset Value", fontName="Helvetica", fontSize=4.9, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    d.add(Line(404, 61, 416, 61, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([416, 61, 412, 63, 412, 59], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(Rect(416, 49, 110, 24, fillColor=colors.HexColor("#F3F4F6"), strokeColor=colors.HexColor("#6B7280"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(471, 63.5, "ACTION: SEND FIELD OFFICER", fontName="Helvetica-Bold", fontSize=5.5, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(471, 54.5, "Officer visits home with scale", fontName="Helvetica", fontSize=4.8, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    # Tier 3 (> 100k) -> Level 4: Action 4B (Vaulting)
    d.add(Line(296, 26, 306, 26, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([306, 26, 302, 28, 302, 24], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(Rect(306, 14, 98, 24, fillColor=colors.HexColor("#E5E7EB"), strokeColor=colors.HexColor("#374151"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(355, 28.5, "Over INR 1 Lakh", fontName="Helvetica-Bold", fontSize=5.6, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(355, 19.5, "Large Asset Value", fontName="Helvetica", fontSize=4.9, fillColor=colors.HexColor("#374151"), textAnchor="middle"))
    
    d.add(Line(404, 26, 416, 26, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([416, 26, 412, 28, 412, 24], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(Rect(416, 14, 110, 24, fillColor=colors.white, strokeColor=colors.HexColor("#111827"), strokeWidth=1.0, rx=2, ry=2))
    d.add(String(471, 28.5, "ACTION: LOCK IN BANK VAULT", fontName="Helvetica-Bold", fontSize=5.5, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(471, 19.5, "Sealed in security bag* & vaulted", fontName="Helvetica", fontSize=4.8, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    return d

def draw_monochrome_tree_3():
    """Decision Tree 3: Overdue Payments, Late Fees & Legal Recovery (Plain-English & Safe Padding)"""
    d = Drawing(532, 124)
    # Background Box
    d.add(Rect(0, 0, 532, 124, fillColor=colors.white, strokeColor=colors.HexColor("#D1D5DB"), strokeWidth=0.75, rx=3, ry=3))
    
    # Level 0: Trigger Node (Centered vertically at y=63)
    d.add(Rect(6, 49, 76, 28, fillColor=colors.HexColor("#111827"), strokeColor=colors.HexColor("#111827"), rx=2, ry=2))
    d.add(String(44, 65, "Payment Overdue", fontName="Helvetica-Bold", fontSize=5.6, fillColor=colors.white, textAnchor="middle"))
    d.add(String(44, 55, "Check Days Late (DPD*)", fontName="Helvetica", fontSize=4.9, fillColor=colors.HexColor("#9CA3AF"), textAnchor="middle"))
    
    # Connector line: Level 0 -> Vertical Bus
    d.add(Line(82, 63, 94, 63, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Line(94, 106, 94, 19, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    
    # Stage 1: DPD 1 to 3 Days (Grace Window)
    d.add(Line(94, 106, 106, 106, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([106, 106, 102, 108, 102, 104], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(Rect(106, 94, 96, 24, fillColor=colors.white, strokeColor=colors.HexColor("#374151"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(154, 108.5, "1 to 3 Days Overdue", fontName="Helvetica-Bold", fontSize=5.5, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(154, 99.5, "Grace Period (No Fee)", fontName="Helvetica", fontSize=4.9, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    d.add(Line(202, 106, 216, 106, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([216, 106, 212, 108, 212, 104], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(Rect(216, 94, 310, 24, fillColor=colors.white, strokeColor=colors.HexColor("#111827"), strokeWidth=1.0, rx=2, ry=2))
    d.add(String(371, 108.5, "ACTION 1: ZERO PENALTY (INR 0.00 FEE)", fontName="Helvetica-Bold", fontSize=5.5, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(371, 99.5, "Send friendly reminder; no penalty fee and zero score impact", fontName="Helvetica", fontSize=4.8, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    # Stage 2: DPD 4 to 30 Days (Delinquency Window)
    d.add(Line(94, 77, 106, 77, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([106, 77, 102, 79, 102, 75], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(Rect(106, 65, 96, 24, fillColor=colors.white, strokeColor=colors.HexColor("#374151"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(154, 79.5, "4 to 30 Days Overdue", fontName="Helvetica-Bold", fontSize=5.5, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(154, 70.5, "Late Payment Alert", fontName="Helvetica", fontSize=4.9, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    d.add(Line(202, 77, 216, 77, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([216, 77, 212, 79, 212, 75], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(Rect(216, 65, 310, 24, fillColor=colors.HexColor("#F9FAFB"), strokeColor=colors.HexColor("#9CA3AF"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(371, 79.5, "ACTION 2: SIMPLE DAILY LATE FEE* (MAX 2% PER MONTH)", fontName="Helvetica-Bold", fontSize=5.5, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(371, 70.5, "Fair fee on late amount only; never added to loan principal", fontName="Helvetica", fontSize=4.8, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    # Stage 3: DPD 31 to 90 Days (Sub-Standard Risk)
    d.add(Line(94, 48, 106, 48, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([106, 48, 102, 50, 102, 46], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(Rect(106, 36, 96, 24, fillColor=colors.white, strokeColor=colors.HexColor("#374151"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(154, 50.5, "31 to 90 Days Overdue", fontName="Helvetica-Bold", fontSize=5.5, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(154, 41.5, "Urgent Warning Sent", fontName="Helvetica", fontSize=4.9, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    d.add(Line(202, 48, 216, 48, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([216, 48, 212, 50, 212, 46], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(Rect(216, 36, 310, 24, fillColor=colors.HexColor("#F3F4F6"), strokeColor=colors.HexColor("#6B7280"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(371, 50.5, "ACTION 3: CONTACT GUARANTOR* & RESTRUCTURE PLAN", fontName="Helvetica-Bold", fontSize=5.5, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(371, 41.5, "Contact loan guarantor; offer smaller monthly EMI or more time", fontName="Helvetica", fontSize=4.8, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    # Stage 4: DPD > 90 Days (Default / NPA)
    d.add(Line(94, 19, 106, 19, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([106, 19, 102, 21, 102, 17], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(Rect(106, 7, 96, 24, fillColor=colors.white, strokeColor=colors.HexColor("#374151"), strokeWidth=0.75, rx=2, ry=2))
    d.add(String(154, 21.5, "Over 90 Days Overdue", fontName="Helvetica-Bold", fontSize=5.5, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(154, 12.5, "Formal Loan Default", fontName="Helvetica", fontSize=4.9, fillColor=colors.HexColor("#4B5563"), textAnchor="middle"))
    
    d.add(Line(202, 19, 216, 19, strokeColor=colors.HexColor("#374151"), strokeWidth=1))
    d.add(Polygon([216, 19, 212, 21, 212, 17], fillColor=colors.HexColor("#374151"), strokeColor=None))
    d.add(Rect(216, 7, 310, 24, fillColor=colors.HexColor("#E5E7EB"), strokeColor=colors.HexColor("#111827"), strokeWidth=1.0, rx=2, ry=2))
    d.add(String(371, 21.5, "ACTION 4: LEGAL NOTICE & FAST-TRACK COURT ACTION*", fontName="Helvetica-Bold", fontSize=5.5, fillColor=colors.HexColor("#111827"), textAnchor="middle"))
    d.add(String(371, 12.5, "Issue 14-day legal notice; recover debt via court order or asset sale", fontName="Helvetica", fontSize=4.8, fillColor=colors.HexColor("#374151"), textAnchor="middle"))
    
    return d

# =========================================================================
# MAIN PDF COMPILER
# =========================================================================

def build_pdf(filename="Lab5_Requirement_Elicitation_Loanzo.pdf"):
    doc = SimpleDocTemplate(
        filename,
        pagesize=letter,
        leftMargin=40,
        rightMargin=40,
        topMargin=36,
        bottomMargin=36
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
    title_style = ParagraphStyle('DocTitle', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=14.5, leading=18, textColor=c_primary)
    subtitle_style = ParagraphStyle('DocSubtitle', parent=styles['Normal'], fontName='Helvetica', fontSize=8.8, leading=11.5, textColor=c_muted)
    
    h1_style = ParagraphStyle('H1', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=10, leading=13, textColor=c_primary, spaceBefore=6, spaceAfter=2.5)
    h2_style = ParagraphStyle('H2', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=8.2, leading=10.5, textColor=c_primary, spaceBefore=3.5, spaceAfter=1.5)
    h2_tree_style = ParagraphStyle('H2Tree', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=7.9, leading=9.8, textColor=c_primary, spaceBefore=2.0, spaceAfter=1.0)
    
    body_style = ParagraphStyle('Body', parent=styles['Normal'], fontName='Helvetica', fontSize=7.4, leading=10.2, textColor=c_secondary)
    
    th_style = ParagraphStyle('TH', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=7, leading=9, textColor=c_primary)
    th_center = ParagraphStyle('THC', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=7, leading=9, textColor=c_primary, alignment=1)
    
    td_style = ParagraphStyle('TD', parent=styles['Normal'], fontName='Helvetica', fontSize=6.7, leading=8.6, textColor=c_secondary)
    td_bold = ParagraphStyle('TDB', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=6.7, leading=8.6, textColor=c_primary)
    td_code = ParagraphStyle('TDCode', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=6.7, leading=8.6, textColor=c_primary)
    td_center = ParagraphStyle('TDC', parent=styles['Normal'], fontName='Helvetica', fontSize=6.7, leading=8.6, textColor=c_secondary, alignment=1)
    td_center_bold = ParagraphStyle('TDCB', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=6.7, leading=8.6, textColor=c_primary, alignment=1)
    
    # Glossary Typography
    gloss_term = ParagraphStyle('GTerm', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=6.5, leading=8.2, textColor=c_primary)
    gloss_def = ParagraphStyle('GDef', parent=styles['Normal'], fontName='Helvetica', fontSize=6.1, leading=7.7, textColor=c_secondary)

    story = []

    # =========================================================================
    # PAGE 1: DOCUMENT HEADER & SECTION 1
    # =========================================================================
    story.append(Paragraph("Lab 5: Requirement Elicitation - II", title_style))
    story.append(Spacer(1, 2))
    story.append(Paragraph("System Specification: LOANZO (Peer-to-Peer Microfinance and Purpose-Bound Social Lending Platform)", subtitle_style))
    story.append(Spacer(1, 3))
    story.append(HRFlowable(width="100%", thickness=1, color=c_primary, spaceBefore=2, spaceAfter=4))

    # Activities Overview
    story.append(Paragraph("<b>Activities Conducted:</b>", h1_style))
    story.append(Paragraph("1. Refinement of Functional and Non-Functional Requirements<br/>2. Representation of Requirements Using Decision Trees<br/>3. Representation of Requirements Using Decision Tables", body_style))
    story.append(Spacer(1, 2))
    story.append(Paragraph("<b>Document Outcome:</b>", h1_style))
    story.append(Paragraph("Structured Requirements Document (SRD)", body_style))
    story.append(Spacer(1, 4))

    # 1. Refinement of Functional and Non-Functional Requirements
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
            Paragraph("Purpose-Bound Tranche* Disbursal", td_bold),
            Paragraph("<b>Trigger:</b> Borrower initiates milestone disbursement request.<br/>"
                      "<b>Logic:</b> The system extracts vendor UPI VPA* from the commercial tax invoice and executes an automated INR 1.00 penny-drop check*. If beneficiary name matches the invoice (match >= 85%), funds transfer directly to the vendor VPA via NPCI rails. Zero capital is released as unmonitored cash.<br/>"
                      "<b>Acceptance Criteria:</b> 100% of disbursed capital must settle directly into verified vendor accounts.", td_style)
        ],
        [
            Paragraph("RFR-02", td_code),
            Paragraph("Multi-Asset Collateral Valuation", td_bold),
            Paragraph("<b>Trigger:</b> Borrower submits asset pledge documentation.<br/>"
                      "<b>Logic:</b> The system categorizes collateral into 5 classes and enforces statutory Loan-to-Value (LTV)* caps: Gold Bullion (75% IBJA benchmark), Property Deeds (55%), Motor Vehicles (60% Form 34 hypothecated*), Commercial IT Equipment (45%), and Accounts Receivable (75%). Asset valuations exceeding INR 100,000 mandate dual-agent independent inspection.<br/>"
                      "<b>Acceptance Criteria:</b> Loan requests exceeding statutory LTV ceilings are programmatically blocked.", td_style)
        ],
        [
            Paragraph("RFR-03", td_code),
            Paragraph("Statutory Promissory Note* Synthesis", td_bold),
            Paragraph("<b>Trigger:</b> Counterparty bid accepted in marketplace.<br/>"
                      "<b>Logic:</b> The system synthesizes an unconditional Promissory Note compliant with Section 4 of the Negotiable Instruments Act, 1881*, accompanied by a Key Fact Statement (KFS). Documents are executed via Aadhaar OTP e-Sign and sealed with a SHA-256 hash, GPS coordinates, and timestamp for legal admissibility under Section 65B Indian Evidence Act.<br/>"
                      "<b>Acceptance Criteria:</b> Contract generation must complete in <= 2.5 seconds on-device with zero tamper vulnerability.", td_style)
        ],
        [
            Paragraph("RFR-04", td_code),
            Paragraph("Non-Compounding Regulatory Penalties*", td_bold),
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
        ('TOPPADDING', (0, 0), (-1, -1), 2),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 2),
        ('LEFTPADDING', (0, 0), (-1, -1), 4),
        ('RIGHTPADDING', (0, 0), (-1, -1), 4),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ]))
    story.append(t_fr_ref)
    story.append(Spacer(1, 4))

    story.append(Paragraph("1.2 Refined Non-Functional Specifications", h2_style))
    nfr_refined_data = [
        [Paragraph("Identifier", th_style), Paragraph("Quality Attribute", th_style), Paragraph("Operational Requirement & Quantitative Threshold", th_style), Paragraph("Verification Method", th_style)],
        [Paragraph("RNFR-01", td_code), Paragraph("Database Performance", td_bold), Paragraph("Local database query execution shall not exceed 15ms (95th percentile) on baseline 2GB RAM devices.", td_style), Paragraph("Automated Profiler Suite", td_style)],
        [Paragraph("RNFR-02", td_code), Paragraph("Application Cold Start", td_bold), Paragraph("Launch to interactive Home Dashboard state shall complete in <= 1.8 seconds with zero UI flicker.", td_style), Paragraph("Macrobenchmark Test", td_style)],
        [Paragraph("RNFR-03", td_code), Paragraph("Data Confidentiality", td_bold), Paragraph("All stored KYC credentials and documents shall be encrypted at rest using AES-256-GCM via hardware KeyStore.", td_style), Paragraph("Static / Memory Heap Audit", td_style)],
        [Paragraph("RNFR-04", td_code), Paragraph("Offline Fault Tolerance", td_bold), Paragraph("Core features (view loans, calculate EMIs, draft agreements) shall execute with 0% network connectivity.", td_style), Paragraph("Signal Disconnection Test", td_style)],
        [Paragraph("RNFR-05", td_code), Paragraph("Atomic Synchronization", td_bold), Paragraph("Offline mutations must reconcile within 30 seconds of network restoration via transactional WorkManager queues.", td_style), Paragraph("Network Recovery Test", td_style)],
        [Paragraph("RNFR-06", td_code), Paragraph("Regulatory Non-Custodial*", td_bold), Paragraph("Platform shall maintain exactly INR 0.00 custodial deposit balance; 100% direct account settlements.", td_style), Paragraph("Financial Ledger Audit", td_style)]
    ]
    t_nfr_ref = Table(nfr_refined_data, colWidths=[45, 95, 272, 120])
    t_nfr_ref.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 2),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 2),
        ('LEFTPADDING', (0, 0), (-1, -1), 4),
        ('RIGHTPADDING', (0, 0), (-1, -1), 4),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ]))
    story.append(t_nfr_ref)

    # =========================================================================
    # PAGE 2: SECTION 2 (STARTS ON FRESH PAGE AS REQUESTED)
    # =========================================================================
    story.append(PageBreak())

    story.append(Paragraph("2. Representation of Requirements Using Decision Trees", h1_style))
    story.append(Paragraph(
        "Decision trees define transparent, automated logic rules for the platform. Every loan disbursement, collateral pledge, or overdue payment follows clear, predictable steps so borrowers, lenders, and administrators always understand the exact system outcome.",
        body_style
    ))
    story.append(Spacer(1, 2))

    # Monochrome Tree 1
    story.append(Paragraph("<b>Decision Tree 1: Safe Loan Payout (Milestone Tranches*) and Seller Verification Policy</b>", h2_tree_style))
    story.append(draw_monochrome_tree_1())
    story.append(Spacer(1, 1.5))

    # Monochrome Tree 2
    story.append(Paragraph("<b>Decision Tree 2: Collateral Security Valuation, Safety Limits (LTV*) and Inspection Dispatch</b>", h2_tree_style))
    story.append(draw_monochrome_tree_2())
    story.append(Spacer(1, 1.5))

    # Monochrome Tree 3
    story.append(Paragraph("<b>Decision Tree 3: Overdue Payments, Late Fees* and Legal Debt Recovery Workflows</b>", h2_tree_style))
    story.append(draw_monochrome_tree_3())
    story.append(Spacer(1, 1.5))

    # Glossary of Asterisked Terms Directly Below Decision Trees
    story.append(Paragraph("<b>* Key Terminology & Plain-English Definitions for General Readers:</b>", h2_tree_style))
    glossary_data = [
        [
            Paragraph("<b>* Term</b>", gloss_term),
            Paragraph("<b>Plain-English Definition & Real-World Meaning</b>", gloss_term)
        ],
        [
            Paragraph("<b>Tranche Disbursement</b>", gloss_term),
            Paragraph("Releasing a loan in smaller portions/milestones (e.g., Milestone 1 for tools, Milestone 2 for materials) instead of giving the entire cash upfront, preventing fund misuse.", gloss_def)
        ],
        [
            Paragraph("<b>Penny-Drop Check</b>", gloss_term),
            Paragraph("Sending a tiny test payment of INR 1.00 to a vendor's bank account to automatically receive and verify their real, registered account name before transferring loan funds.", gloss_def)
        ],
        [
            Paragraph("<b>UPI VPA</b>", gloss_term),
            Paragraph("Virtual Payment Address (e.g., <i>dealer@bank</i>) -- a unique UPI identifier used to route digital money transfers directly to verified merchants without exposing raw bank details.", gloss_def)
        ],
        [
            Paragraph("<b>LTV Ratio (Loan-to-Value)</b>", gloss_term),
            Paragraph("The maximum loan percentage allowed against an asset's market value (e.g., 75% LTV on INR 100,000 gold means the borrower receives a maximum loan of INR 75,000).", gloss_def)
        ],
        [
            Paragraph("<b>Haircut (Collateral Discount)</b>", gloss_term),
            Paragraph("A safety discount deducted from collateral value to protect lenders from market drops (e.g., a 25% safety haircut leaves a 75% loan ceiling).", gloss_def)
        ],
        [
            Paragraph("<b>Form 34 Hypothecation</b>", gloss_term),
            Paragraph("A formal legal endorsement recorded on a vehicle's official registration certificate (RC Book) noting that the vehicle acts as collateral for the loan.", gloss_def)
        ],
        [
            Paragraph("<b>Promissory Note (Sec 4 NI Act)</b>", gloss_term),
            Paragraph("A legally binding document signed by the borrower containing an unconditional written promise to repay a specific sum of money to the lender on a specified date.", gloss_def)
        ],
        [
            Paragraph("<b>Simple Late Fee (Non-Compounding)</b>", gloss_term),
            Paragraph("A flat daily fee on overdue installments that can never be added into loan principal, preventing lenders from charging 'interest-on-interest' (debt traps).", gloss_def)
        ],
        [
            Paragraph("<b>DPD (Days Past Due)</b>", gloss_term),
            Paragraph("The exact count of days that have passed since an EMI payment was due (e.g., DPD = 5 means the payment is 5 days overdue).", gloss_def)
        ],
        [
            Paragraph("<b>Order XXXVII (Order 37 CPC)</b>", gloss_term),
            Paragraph("A special fast-track civil court lawsuit in India for debt recovery based on written promissory notes, resolving cases in ~60 days without long 7-year trials.", gloss_def)
        ],
        [
            Paragraph("<b>Guarantor / Co-Signer</b>", gloss_term),
            Paragraph("A trusted family member or peer who agrees to help repay the loan if the primary borrower faces hardship or default.", gloss_def)
        ],
        [
            Paragraph("<b>Non-Custodial Model</b>", gloss_term),
            Paragraph("The platform holds INR 0.00 customer deposits; all money transfers move directly from user-to-user or user-to-merchant, eliminating banking license burdens.", gloss_def)
        ]
    ]

    t_gloss = Table(glossary_data, colWidths=[125, 407])
    t_gloss.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 0.7),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 0.7),
        ('LEFTPADDING', (0, 0), (-1, -1), 3.5),
        ('RIGHTPADDING', (0, 0), (-1, -1), 3.5),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
    ]))
    story.append(t_gloss)

    # =========================================================================
    # PAGE 3: SECTION 3 (STARTS ON FRESH PAGE AS REQUESTED)
    # =========================================================================
    story.append(PageBreak())

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
        [Paragraph("C2", td_bold), Paragraph("On-Device Facial Liveness Verification Passed?", td_style), Paragraph("Y", td_center), Paragraph("Y", td_center), Paragraph("Y", td_center), Paragraph("N", td_center), Paragraph("--", td_center)],
        [Paragraph("C3", td_bold), Paragraph("Historical Unresolved Default on Record?", td_style), Paragraph("N", td_center), Paragraph("N", td_center), Paragraph("Y", td_center), Paragraph("--", td_center), Paragraph("--", td_center)],
        [Paragraph("C4", td_bold), Paragraph("Requested Borrowing APR Within Statutory Ceiling?", td_style), Paragraph("Y", td_center), Paragraph("N", td_center), Paragraph("--", td_center), Paragraph("--", td_center), Paragraph("--", td_center)],
        [Paragraph("A1", td_code), Paragraph("Publish Loan Requisition to Social Bidding Marketplace", td_bold), Paragraph("X", td_center_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center)],
        [Paragraph("A2", td_code), Paragraph("Prompt Mandatory APR Adjustment to Comply with Cap", td_bold), Paragraph("", td_center), Paragraph("X", td_center_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center)],
        [Paragraph("A3", td_code), Paragraph("Reject Origination Request and Restrict Lending Privileges", td_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("X", td_center_bold), Paragraph("", td_center), Paragraph("", td_center)],
        [Paragraph("A4", td_code), Paragraph("Prompt Secondary Facial Liveness Verification Challenge", td_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center), Paragraph("X", td_center_bold), Paragraph("", td_center)],
        [Paragraph("A5", td_code), Paragraph("Halt Origination Workflow; Redirect to KYC Verification Portal", td_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center), Paragraph("X", td_center_bold)]
    ]
    t_dtab1 = Table(dtab1_data, colWidths=[38, 264, 46, 46, 46, 46, 46])
    t_dtab1.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 2),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 2),
        ('LEFTPADDING', (0, 0), (-1, -1), 3),
        ('RIGHTPADDING', (0, 0), (-1, -1), 3),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
    ]))
    story.append(t_dtab1)
    story.append(Spacer(1, 6))

    # Decision Table 2
    story.append(Paragraph("<b>Decision Table 2: Purpose-Bound Milestone Tranche Disbursement Verification Gate</b>", h2_style))
    dtab2_data = [
        [Paragraph("Element", th_style), Paragraph("Condition / Action Parameter", th_style), Paragraph("R1", th_center), Paragraph("R2", th_center), Paragraph("R3", th_center), Paragraph("R4", th_center)],
        [Paragraph("C1", td_bold), Paragraph("Commercial Tax Invoice Uploaded by Borrower?", td_style), Paragraph("Y", td_center), Paragraph("Y", td_center), Paragraph("Y", td_center), Paragraph("N", td_center)],
        [Paragraph("C2", td_bold), Paragraph("Penny-Drop Beneficiary Name Matches Invoice (>= 85%)?", td_style), Paragraph("Y", td_center), Paragraph("Y", td_center), Paragraph("N", td_center), Paragraph("--", td_center)],
        [Paragraph("C3", td_bold), Paragraph("Prior Milestone Physical Utilization Verified by Field Agent?", td_style), Paragraph("Y", td_center), Paragraph("N", td_center), Paragraph("--", td_center), Paragraph("--", td_center)],
        [Paragraph("A1", td_code), Paragraph("Execute Direct Tranche Settlement to Vendor UPI VPA", td_bold), Paragraph("X", td_center_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center)],
        [Paragraph("A2", td_code), Paragraph("Hold Tranche; Dispatch Field Agent for Physical Audit", td_bold), Paragraph("", td_center), Paragraph("X", td_center_bold), Paragraph("", td_center), Paragraph("", td_center)],
        [Paragraph("A3", td_code), Paragraph("Lock Tranche; Log Security Incident for Compliance Review", td_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("X", td_center_bold), Paragraph("", td_center)],
        [Paragraph("A4", td_code), Paragraph("Prompt User to Submit Commercial Invoice Documentation", td_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center), Paragraph("X", td_center_bold)]
    ]
    t_dtab2 = Table(dtab2_data, colWidths=[38, 306, 47, 47, 47, 47])
    t_dtab2.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 2),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 2),
        ('LEFTPADDING', (0, 0), (-1, -1), 3),
        ('RIGHTPADDING', (0, 0), (-1, -1), 3),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
    ]))
    story.append(t_dtab2)
    story.append(Spacer(1, 6))

    # Decision Table 3
    story.append(Paragraph("<b>Decision Table 3: Collateral Valuation, Haircut Enforcement, and Field Audit Routing</b>", h2_style))
    dtab3_data = [
        [Paragraph("Element", th_style), Paragraph("Condition / Action Parameter", th_style), Paragraph("R1", th_center), Paragraph("R2", th_center), Paragraph("R3", th_center), Paragraph("R4", th_center), Paragraph("R5", th_center)],
        [Paragraph("C1", td_bold), Paragraph("Pledged Collateral Asset Category", td_style), Paragraph("Gold", td_center), Paragraph("Property", td_center), Paragraph("Vehicle", td_center), Paragraph("IT Assets", td_center), Paragraph("Any", td_center)],
        [Paragraph("C2", td_bold), Paragraph("Primary Ownership Documentation Formally Verified?", td_style), Paragraph("Y", td_center), Paragraph("Y", td_center), Paragraph("Y", td_center), Paragraph("Y", td_center), Paragraph("N", td_center)],
        [Paragraph("C3", td_bold), Paragraph("Declared Asset Valuation Exceeds INR 100,000?", td_style), Paragraph("N", td_center), Paragraph("Y", td_center), Paragraph("N", td_center), Paragraph("Y", td_center), Paragraph("--", td_center)],
        [Paragraph("A1", td_code), Paragraph("Approve Collateral with Statutory 75% LTV Cap (IBJA)", td_bold), Paragraph("X", td_center_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center)],
        [Paragraph("A2", td_code), Paragraph("Approve Collateral with 55% LTV Cap (Non-Encumbrance)", td_bold), Paragraph("", td_center), Paragraph("X", td_center_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center)],
        [Paragraph("A3", td_code), Paragraph("Approve Collateral with 60% LTV Cap (Form 34 Hypothecation)", td_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("X", td_center_bold), Paragraph("", td_center), Paragraph("", td_center)],
        [Paragraph("A4", td_code), Paragraph("Approve Collateral with 45% LTV Cap (Serial Checked)", td_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center), Paragraph("X", td_center_bold), Paragraph("", td_center)],
        [Paragraph("A5", td_code), Paragraph("Mandate Dual-Agent Independent Inspection Protocol", td_bold), Paragraph("", td_center), Paragraph("X", td_center_bold), Paragraph("", td_center), Paragraph("X", td_center_bold), Paragraph("", td_center)],
        [Paragraph("A6", td_code), Paragraph("Reject Collateral Pledge; Request Ownership Documents", td_bold), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center), Paragraph("", td_center), Paragraph("X", td_center_bold)]
    ]
    t_dtab3 = Table(dtab3_data, colWidths=[38, 264, 46, 46, 46, 46, 46])
    t_dtab3.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 2),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 2),
        ('LEFTPADDING', (0, 0), (-1, -1), 3),
        ('RIGHTPADDING', (0, 0), (-1, -1), 3),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
    ]))
    story.append(t_dtab3)

    # =========================================================================
    # PAGE 4: SECTION 4 (OUTCOME: SRD)
    # =========================================================================
    story.append(PageBreak())

    story.append(Paragraph("4. Outcome: Structured Requirements Document (SRD)", h1_style))
    story.append(Paragraph(
        "The Structured Requirements Document compiles refined functional and quality specifications into a normalized traceability matrix ready for architectural design and test verification.",
        body_style
    ))
    story.append(Spacer(1, 4))

    td_state = ParagraphStyle('TDState', parent=styles['Normal'], fontName='Helvetica-Bold', fontSize=6.2, leading=8.0, textColor=c_primary)

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
            Paragraph("AUTHENTICATED", td_state), Paragraph("Security Test", td_style)
        ],
        [
            Paragraph("SRD-02", td_code), Paragraph("DigiLocker Identity Ingestion", td_bold),
            Paragraph("Applicant registration triggered", td_style),
            Paragraph("Query DigiLocker for Aadhaar/PAN; run ML Kit facial liveness detection.", td_style),
            Paragraph("VERIFIED_ID", td_state), Paragraph("Integration Test", td_style)
        ],
        [
            Paragraph("SRD-03", td_code), Paragraph("Cryptographic Data Vault", td_bold),
            Paragraph("Sensitive document stored locally", td_style),
            Paragraph("Encrypt artifacts via AES-256-GCM backed by hardware Android KeyStore.", td_style),
            Paragraph("VAULT_SECURED", td_state), Paragraph("Static Code Audit", td_style)
        ],
        [
            Paragraph("SRD-04", td_code), Paragraph("Reverse-Auction Marketplace", td_bold),
            Paragraph("Loan requisition published", td_style),
            Paragraph("Verified retail lenders submit competitive APR and tenure proposals.", td_style),
            Paragraph("BIDS_ACTIVE", td_state), Paragraph("Functional Test", td_style)
        ],
        [
            Paragraph("SRD-05", td_code), Paragraph("Purpose-Bound Tranche Escrow", td_bold),
            Paragraph("Disbursement milestone reached", td_style),
            Paragraph("Route capital directly to verified vendor UPI VPA; bypass borrower.", td_style),
            Paragraph("SETTLED_VENDOR", td_state), Paragraph("End-to-End API", td_style)
        ],
        [
            Paragraph("SRD-06", td_code), Paragraph("Penny-Drop Account Match", td_bold),
            Paragraph("Vendor account submitted", td_style),
            Paragraph("Execute automated INR 1.00 validation matching commercial invoice name.", td_style),
            Paragraph("NAME_VERIFIED", td_state), Paragraph("API Mocking Test", td_style)
        ],
        [
            Paragraph("SRD-07", td_code), Paragraph("Statutory Promissory Note", td_bold),
            Paragraph("Terms accepted by counterparties", td_style),
            Paragraph("Synthesize 4-page Section 4 NI Act promissory note with Key Fact Statement.", td_style),
            Paragraph("NOTE_SYNTHESIZED", td_state), Paragraph("Document Parser", td_style)
        ],
        [
            Paragraph("SRD-08", td_code), Paragraph("Aadhaar Electronic Signature", td_bold),
            Paragraph("Promissory note generated", td_style),
            Paragraph("Execute Aadhaar OTP e-Sign; embed SHA-256 tamper-evident digital digest.", td_style),
            Paragraph("CONTRACT_SIGNED", td_state), Paragraph("API Audit Test", td_style)
        ],
        [
            Paragraph("SRD-09", td_code), Paragraph("Multi-Asset Collateral Escrow", td_bold),
            Paragraph("Asset pledged by borrower", td_style),
            Paragraph("Enforce statutory LTV ceilings across Gold (75%), Property (55%), etc.", td_style),
            Paragraph("COLLATERAL_LOCKED", td_state), Paragraph("Mathematical Unit", td_style)
        ],
        [
            Paragraph("SRD-10", td_code), Paragraph("Dual-Agent Consensus Protocol", td_bold),
            Paragraph("Valuation exceeds INR 100,000", td_style),
            Paragraph("Require independent inspection reports from two certified ground officers.", td_style),
            Paragraph("DUAL_VERIFIED", td_state), Paragraph("Workflow Test", td_style)
        ],
        [
            Paragraph("SRD-11", td_code), Paragraph("Non-Compounding Overdue Fee", td_bold),
            Paragraph("Installment overdue > 3 days", td_style),
            Paragraph("Apply daily fee strictly capped at statutory 2.0% monthly ceiling.", td_style),
            Paragraph("PENALTY_ACCRUED", td_state), Paragraph("Unit Test Suite", td_style)
        ],
        [
            Paragraph("SRD-12", td_code), Paragraph("Distress Restructuring & NOC", td_bold),
            Paragraph("Borrower distress verified", td_style),
            Paragraph("Restructure amortization schedule; issue digital No Objection Certificate.", td_style),
            Paragraph("RESTRUCTURED", td_state), Paragraph("State Regression", td_style)
        ]
    ]

    t_srd = Table(srd_data, colWidths=[42, 98, 90, 142, 95, 65])
    t_srd.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), c_header_bg),
        ('BOX', (0, 0), (-1, -1), 0.75, c_border),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, c_border_light),
        ('TOPPADDING', (0, 0), (-1, -1), 2.2),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 2.2),
        ('LEFTPADDING', (0, 0), (-1, -1), 3),
        ('RIGHTPADDING', (0, 0), (-1, -1), 3),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
    ]))
    story.append(t_srd)

    doc.build(story, canvasmaker=ProfessionalNumberedCanvas)
    print(f"Monochrome Lab 5 PDF successfully compiled: {filename}")

if __name__ == "__main__":
    out_pdf = "Lab5_Requirement_Elicitation_Loanzo.pdf"
    build_pdf(out_pdf)
    if os.path.exists("docs"):
        docs_pdf = os.path.join("docs", "Lab5_Requirement_Elicitation_Loanzo.pdf")
        build_pdf(docs_pdf)

