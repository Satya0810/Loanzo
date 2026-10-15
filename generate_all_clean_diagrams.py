import os
import matplotlib.pyplot as plt
import matplotlib.patches as patches
from matplotlib.patches import Ellipse, Rectangle, FancyArrowPatch

os.makedirs("docs/diagrams", exist_ok=True)

# 100% Unique Color Palette across all actors, systems, and relationships
COLORS = {
    'Borrower': '#0D47A1',      # 1. Royal Navy Blue
    'Co-Borrower': '#00897B',   # 2. Pure Teal
    'Guarantor': '#FF6F00',     # 3. Vivid Amber Orange
    'Nominee': '#2E7D32',       # 4. Vibrant Forest Green
    'Lender': '#9C27B0',        # 5. Rich Violet / Purple
    'Field Agent': '#6D4C41',   # 6. Chocolate / Sienna Brown
    'Admin': '#D50000',         # 7. Bright Crimson Red
    'NPCI': '#0284C7',          # 8. Sky / Cerulean Blue
    'DigiLocker': '#607D8B',    # 9. Cool Slate / Steel Blue-Grey
    'NeSL': '#303F9F',          # 10. Deep Indigo
    'IBJA': '#F57F17',          # 11. Antique Gold / Mustard
    'Treasury': '#FF5722',      # 12. Deep Terracotta / Coral
    'Court': '#263238',         # 13. Dark Charcoal / Jet
    'CIBIL': '#00B4D8',         # 14. Vivid Electric Cyan
    'include': '#E91E63',       # 15. Hot Pink / Magenta dashed
    'extend': '#880E4F'         # 16. Deep Wine Maroon dashed
}

def draw_actor(ax, x, y, name, color='#111827', subtitle=None, scale=1.0):
    """Draws a standard UML stick figure actor with matching signature color."""
    r = 0.17 * scale
    # Head
    head = Ellipse((x, y + 0.46 * scale), 2 * r, 2 * r, fill=True, facecolor='white', edgecolor=color, linewidth=2.0, zorder=5)
    ax.add_patch(head)
    # Body
    ax.plot([x, x], [y + 0.29 * scale, y - 0.18 * scale], color=color, linewidth=2.0, zorder=5)
    # Arms
    ax.plot([x - 0.26 * scale, x + 0.26 * scale], [y + 0.12 * scale, y + 0.12 * scale], color=color, linewidth=2.0, zorder=5)
    # Legs
    ax.plot([x, x - 0.22 * scale], [y - 0.18 * scale, y - 0.60 * scale], color=color, linewidth=2.0, zorder=5)
    ax.plot([x, x + 0.22 * scale], [y - 0.18 * scale, y - 0.60 * scale], color=color, linewidth=2.0, zorder=5)
    
    # Label with subtitle if present
    label_text = name if not subtitle else f"{name}\n({subtitle})"
    ax.text(x, y - 0.82 * scale, label_text, ha='center', va='top', fontsize=8.2, fontweight='bold', color=color,
            bbox=dict(boxstyle='round,pad=0.22', facecolor='white', edgecolor=color, linewidth=1.1), zorder=5,
            multialignment='center')

def draw_use_case(ax, x, y, text, w=2.6, h=0.74, border_color='#1E293B'):
    """Draws a standard UML oval use case."""
    ellipse = Ellipse((x, y), w, h, fill=True, facecolor='white', edgecolor=border_color, linewidth=1.4, zorder=3)
    ax.add_patch(ellipse)
    ax.text(x, y, text, ha='center', va='center', fontsize=7.6, color='#0F172A', zorder=4, multialignment='center')

def draw_system_boundary(ax, x, y, w, h, title):
    """Draws the system boundary rectangle."""
    rect = Rectangle((x, y), w, h, fill=False, edgecolor='#1E293B', linewidth=1.8, linestyle='-', zorder=1)
    ax.add_patch(rect)
    ax.text(x + 0.25, y + h - 0.30, title, ha='left', va='top', fontsize=10.8, fontweight='bold', color='#0F172A', zorder=2)

def draw_external_system(ax, x, y, name, color='#607D8B', w=2.2, h=0.72):
    """Draws a secondary external system actor box with system unique color."""
    rect = Rectangle((x - w/2, y - h/2), w, h, fill=True, facecolor='white', edgecolor=color, linewidth=1.6, zorder=3)
    ax.add_patch(rect)
    ax.text(x, y + 0.13, "<<system>>", ha='center', va='center', fontsize=7.0, fontstyle='italic', color=color, zorder=4)
    ax.text(x, y - 0.13, name, ha='center', va='center', fontsize=8.0, fontweight='bold', color=color, zorder=4)

def draw_solid_line(ax, x1, y1, x2, y2, color='#111827', lw=1.6):
    """Draws a colored association line between actor and use case with endpoint anchor dots."""
    ax.plot([x1, x2], [y1, y2], color=color, linewidth=lw, solid_capstyle='round', zorder=2)
    ax.plot(x1, y1, marker='o', markersize=3.8, color=color, zorder=4)
    ax.plot(x2, y2, marker='o', markersize=3.8, color=color, zorder=4)

def draw_clean_relationship(ax, x1, y1, x2, y2, rel_type="include", color='#E91E63', is_vertical=False):
    """
    Draws an <<include>> or <<extend>> relationship arrow with pristine positioning.
    - Arrow runs completely clean without text slicing it in half.
    - Text sits cleanly above horizontal lines or to the right of vertical lines.
    - Zero collisions with use case ovals.
    """
    arrow = FancyArrowPatch((x1, y1), (x2, y2),
                             arrowstyle='->,head_width=3.6,head_length=5.2',
                             linestyle='--', color=color, linewidth=1.3, zorder=2)
    ax.add_patch(arrow)
    
    label = f"<<{rel_type}>>"
    
    if is_vertical:
        # Place to the right of the vertical line
        mid_x = (x1 + x2) / 2 + 0.22
        mid_y = (y1 + y2) / 2
        ax.text(mid_x, mid_y, label, ha='left', va='center', fontsize=7.0, fontstyle='italic',
                bbox=dict(boxstyle='round,pad=0.15', facecolor='white', edgecolor='#E2E8F0', linewidth=0.8),
                color=color, zorder=4)
    else:
        # Place cleanly above the line
        mid_x = (x1 + x2) / 2
        mid_y = (y1 + y2) / 2 + 0.16
        ax.text(mid_x, mid_y, label, ha='center', va='bottom', fontsize=7.0, fontstyle='italic',
                bbox=dict(boxstyle='round,pad=0.15', facecolor='white', edgecolor='#E2E8F0', linewidth=0.8),
                color=color, zorder=4)

def draw_legend(ax, items, x, y, w, h=0.48):
    """Draws a clean horizontal color legend box showing line origins and stereotypes."""
    rect = Rectangle((x, y), w, h, fill=True, facecolor='#F8FAFC', edgecolor='#94A3B8', linewidth=1.0, zorder=2)
    ax.add_patch(rect)
    n = len(items)
    step = w / n
    for idx, (label, col, style) in enumerate(items):
        item_x = x + idx * step + 0.10
        item_y = y + h / 2
        if style == 'solid':
            ax.plot([item_x, item_x + 0.26], [item_y, item_y], color=col, linewidth=2.0, solid_capstyle='round', zorder=3)
            ax.plot(item_x, item_y, marker='o', markersize=3.0, color=col, zorder=4)
            ax.plot(item_x + 0.26, item_y, marker='o', markersize=3.0, color=col, zorder=4)
            ax.text(item_x + 0.33, item_y, label, ha='left', va='center', fontsize=6.5, fontweight='bold', color=col, zorder=3)
        elif style == 'dashed':
            ax.plot([item_x, item_x + 0.26], [item_y, item_y], color=col, linewidth=1.5, linestyle='--', zorder=3)
            ax.text(item_x + 0.33, item_y, label, ha='left', va='center', fontsize=6.5, fontstyle='italic', color=col, zorder=3)

# ==========================================
# DIAGRAM 1: MASTER PLATFORM USE CASE DIAGRAM
# Complete Platform Architecture with Easy-Language & Clean Spacing
# ==========================================
def generate_master_diagram():
    fig, ax = plt.subplots(figsize=(18.5, 12.5), dpi=300)
    ax.set_xlim(-2.8, 15.2)
    ax.set_ylim(-1.5, 11.4)
    ax.axis('off')

    draw_system_boundary(ax, 0.2, -0.7, 11.8, 11.9, "System Boundary: Loanzo Platform (Master Architecture)")

    # Left Primary Actors
    draw_actor(ax, -1.6, 9.6, "Borrower", color=COLORS['Borrower'], scale=0.92)
    draw_actor(ax, -1.6, 6.8, "Co-Borrower", color=COLORS['Co-Borrower'], subtitle="Joint Debtor", scale=0.92)
    draw_actor(ax, -1.6, 4.0, "Guarantor", color=COLORS['Guarantor'], subtitle="Surety", scale=0.92)
    draw_actor(ax, -1.6, 1.2, "Nominee", color=COLORS['Nominee'], subtitle="If Borrower Dies", scale=0.92)

    # Right Actors & Systems
    draw_actor(ax, 13.6, 9.6, "Lender", color=COLORS['Lender'], scale=0.92)
    draw_actor(ax, 13.6, 7.4, "Field Agent", color=COLORS['Field Agent'], subtitle="Gold Valuer", scale=0.92)
    draw_external_system(ax, 13.6, 5.4, "NPCI UPI Switch", color=COLORS['NPCI'])
    draw_external_system(ax, 13.6, 3.8, "DigiLocker / UIDAI", color=COLORS['DigiLocker'])
    draw_external_system(ax, 13.6, 2.4, "NeSL Info Utility", color=COLORS['NeSL'])
    draw_actor(ax, 13.6, 0.9, "Platform Admin", color=COLORS['Admin'], scale=0.92)

    # Column 1 Use Cases (x = 2.0, w = 2.6 -> left: 0.7, right: 3.3)
    draw_use_case(ax, 2.0, 9.8, "UC-01: Log In\n(PIN or Biometrics)")
    draw_use_case(ax, 2.0, 8.4, "UC-02: Complete KYC\n(Aadhaar & PAN)")
    draw_use_case(ax, 2.0, 7.0, "UC-03: Create Loan Request\n& Enter Nominee Details")
    draw_use_case(ax, 2.0, 5.6, "UC-04: Accept Joint Loan\nLiability via Biometric")
    draw_use_case(ax, 2.0, 4.2, "UC-05: Sign Formal\nGuarantee Agreement")
    draw_use_case(ax, 2.0, 2.8, "UC-06: Pay Monthly\nEMI via UPI")
    draw_use_case(ax, 2.0, 1.4, "UC-07: Pay Overdue EMI\non Borrower's Behalf")
    draw_use_case(ax, 2.0, 0.0, "UC-08: Return Gold to Borrower\nupon Full Payoff")

    # Column 2 Use Cases (x = 6.0, w = 2.6 -> left: 4.7, right: 7.3)
    draw_use_case(ax, 6.0, 8.4, "UC-09: Verify Identity\nvia DigiLocker OCR")
    draw_use_case(ax, 6.0, 7.0, "UC-10: Check 75% Gold\nLoan Value Cap")
    draw_use_case(ax, 6.0, 4.9, "UC-11: Disburse Loan Funds\nvia Secure Escrow")
    draw_use_case(ax, 6.0, 3.5, "UC-12: Sign Digital Loan\nContract & Promissory Note")
    draw_use_case(ax, 6.0, 2.1, "UC-13: Apply Late Fee\n(After 3-Day Grace)")
    draw_use_case(ax, 6.0, 0.7, "UC-14: Send Legal Notice\n(30+ Days Overdue)")

    # Column 3 Use Cases (x = 10.0, w = 2.6 -> left: 8.7, right: 11.3)
    draw_use_case(ax, 10.0, 9.6, "UC-15: Browse Loan Requests\n& Place Bids")
    draw_use_case(ax, 10.0, 7.4, "UC-16: Check Gold Purity\n& Lock in Bank Vault")
    draw_use_case(ax, 10.0, 4.9, "UC-17: Attach Official\nDigital Stamp (NeSL)")
    draw_use_case(ax, 10.0, 3.5, "UC-18: Record Legal Debt\nRegistry with NeSL")
    draw_use_case(ax, 10.0, 2.1, "UC-19: File Court Recovery\nSuit (Order 37 CPC)")
    draw_use_case(ax, 10.0, 0.7, "UC-20: Audit Repayment\nLedger & Security Logs")
    draw_use_case(ax, 10.0, -0.2, "UC-21: Claim Gold or Surplus\nif Borrower Dies (§50 CPC)")

    # Associations: Borrower (Royal Blue lines)
    c_bor = COLORS['Borrower']
    draw_solid_line(ax, -1.2, 9.9, 0.7, 9.8, color=c_bor)   # UC-01
    draw_solid_line(ax, -1.2, 9.6, 0.7, 8.4, color=c_bor)   # UC-02
    draw_solid_line(ax, -1.2, 9.3, 0.7, 7.0, color=c_bor)   # UC-03
    draw_solid_line(ax, -1.2, 9.0, 0.7, 2.8, color=c_bor)   # UC-06 Pay EMI
    draw_solid_line(ax, -1.2, 8.7, 0.7, 0.0, color=c_bor)   # UC-08 Get Gold Back

    # Associations: Co-Borrower (Teal lines)
    c_cob = COLORS['Co-Borrower']
    draw_solid_line(ax, -1.2, 7.1, 0.7, 9.8, color=c_cob)   # UC-01
    draw_solid_line(ax, -1.2, 6.9, 0.7, 8.4, color=c_cob)   # UC-02
    draw_solid_line(ax, -1.2, 6.7, 0.7, 5.6, color=c_cob)   # UC-04 Joint Obligation
    draw_solid_line(ax, -1.2, 6.4, 0.7, 1.4, color=c_cob)   # UC-07 Pay Overdue EMI

    # Associations: Guarantor (Amber lines)
    c_gua = COLORS['Guarantor']
    draw_solid_line(ax, -1.2, 4.3, 0.7, 9.8, color=c_gua)   # UC-01
    draw_solid_line(ax, -1.2, 4.1, 0.7, 8.4, color=c_gua)   # UC-02
    draw_solid_line(ax, -1.2, 3.9, 0.7, 4.2, color=c_gua)   # UC-05 Guarantee Deed
    draw_solid_line(ax, -1.2, 3.6, 0.7, 1.4, color=c_gua)   # UC-07 Settle Overdue EMI

    # Associations: Nominee (Forest Green lines) - Contingent upon demise
    c_nom = COLORS['Nominee']
    draw_solid_line(ax, -1.2, 1.2, 8.7, -0.2, color=c_nom)  # UC-21 Claim Gold/Surplus on Death

    # Associations: Lender (Violet / Purple lines)
    c_len = COLORS['Lender']
    draw_solid_line(ax, 13.2, 9.8, 3.3, 9.8, color=c_len)   # UC-01 Auth
    draw_solid_line(ax, 13.2, 9.6, 11.3, 9.6, color=c_len) # UC-15 Browse & Bid
    draw_solid_line(ax, 13.2, 9.3, 7.3, 4.9, color=c_len)  # UC-11 Disburse Escrow

    # Associations: Field Agent (Chocolate Brown lines)
    c_fld = COLORS['Field Agent']
    draw_solid_line(ax, 13.2, 7.4, 11.3, 7.4, color=c_fld) # UC-16 Check Gold & Vault

    # Associations: NPCI UPI Switch (Sky Blue lines)
    c_npci = COLORS['NPCI']
    draw_solid_line(ax, 12.5, 5.4, 7.3, 4.9, color=c_npci)  # UC-11 Disburse
    draw_solid_line(ax, 12.5, 5.2, 3.3, 2.8, color=c_npci)  # UC-06 Pay EMI

    # Associations: DigiLocker / UIDAI (Slate Blue-Grey lines)
    c_digi = COLORS['DigiLocker']
    draw_solid_line(ax, 12.5, 3.8, 7.3, 8.4, color=c_digi)  # UC-09 OCR Verification

    # Associations: NeSL Info Utility (Deep Indigo lines)
    c_nesl = COLORS['NeSL']
    draw_solid_line(ax, 12.5, 2.6, 11.3, 4.9, color=c_nesl) # UC-17 E-Stamp
    draw_solid_line(ax, 12.5, 2.4, 11.3, 3.5, color=c_nesl) # UC-18 Debt Registry

    # Associations: Platform Admin (Bright Crimson lines)
    c_adm = COLORS['Admin']
    draw_solid_line(ax, 13.2, 1.1, 11.3, 2.1, color=c_adm)  # UC-19 Order 37 Suit
    draw_solid_line(ax, 13.2, 0.8, 11.3, 0.7, color=c_adm)  # UC-20 Risk & Audit

    # Clean Relationships (Generous 1.4-unit gaps with zero label collisions)
    c_inc = COLORS['include']
    c_ext = COLORS['extend']
    
    # Column 1 to Column 2 includes (horizontal gap from 3.3 to 4.7)
    draw_clean_relationship(ax, 3.3, 8.4, 4.7, 8.4, rel_type="include", color=c_inc)                  # UC-02 -> UC-09
    draw_clean_relationship(ax, 3.3, 7.0, 4.7, 7.0, rel_type="include", color=c_inc)                  # UC-03 -> UC-10
    draw_clean_relationship(ax, 3.3, 5.6, 4.7, 4.2, rel_type="include", color=c_inc)                  # UC-04 -> UC-12
    draw_clean_relationship(ax, 3.3, 4.2, 4.7, 3.7, rel_type="include", color=c_inc)                  # UC-05 -> UC-12

    # Column 2 vertical include
    draw_clean_relationship(ax, 6.0, 6.6, 6.0, 5.3, rel_type="include", color=c_inc, is_vertical=True) # UC-10 -> UC-11

    # Column 2 to Column 3 includes (horizontal gap from 7.3 to 8.7)
    draw_clean_relationship(ax, 7.3, 3.7, 8.7, 4.7, rel_type="include", color=c_inc)                  # UC-12 -> UC-17
    draw_clean_relationship(ax, 7.3, 3.5, 8.7, 3.5, rel_type="include", color=c_inc)                  # UC-12 -> UC-18
    draw_clean_relationship(ax, 8.7, 7.4, 7.3, 7.0, rel_type="include", color=c_inc)                  # UC-16 -> UC-10
    draw_clean_relationship(ax, 8.7, 9.4, 7.3, 5.2, rel_type="include", color=c_inc)                  # UC-15 -> UC-11

    # Extensions
    draw_clean_relationship(ax, 4.7, 2.1, 3.3, 2.6, rel_type="extend", color=c_ext)                   # UC-13 extends UC-06
    draw_clean_relationship(ax, 4.7, 0.9, 3.3, 1.4, rel_type="extend", color=c_ext)                   # UC-14 extends UC-07
    draw_clean_relationship(ax, 8.7, 2.1, 7.3, 3.2, rel_type="extend", color=c_ext)                   # UC-19 extends UC-12
    draw_clean_relationship(ax, 8.7, -0.2, 3.3, 0.0, rel_type="extend", color=c_ext)                  # UC-21 extends UC-08

    # Comprehensive Color Legend Box with 100% Unique Colors
    legend_items = [
        ("Borrower", COLORS['Borrower'], 'solid'),
        ("Co-Borrower", COLORS['Co-Borrower'], 'solid'),
        ("Guarantor", COLORS['Guarantor'], 'solid'),
        ("Nominee", COLORS['Nominee'], 'solid'),
        ("Lender", COLORS['Lender'], 'solid'),
        ("Field Agent", COLORS['Field Agent'], 'solid'),
        ("Admin", COLORS['Admin'], 'solid'),
        ("NPCI UPI", COLORS['NPCI'], 'solid'),
        ("DigiLocker", COLORS['DigiLocker'], 'solid'),
        ("NeSL", COLORS['NeSL'], 'solid'),
        ("<<include>>", COLORS['include'], 'dashed'),
        ("<<extend>>", COLORS['extend'], 'dashed')
    ]
    draw_legend(ax, legend_items, x=0.2, y=-1.38, w=11.8, h=0.48)

    plt.tight_layout()
    path = "docs/diagrams/diagram_1_master.png"
    plt.savefig(path, dpi=300, bbox_inches='tight')
    plt.close()
    print(f"Generated {path}")

# ==========================================
# DIAGRAM 2: AUTHENTICATION & MULTI-PARTY KYC
# Borrower, Co-Borrower, Guarantor, Lender (Nominee properly excluded from upfront KYC)
# ==========================================
def generate_auth_kyc_diagram():
    fig, ax = plt.subplots(figsize=(15.0, 9.5), dpi=300)
    ax.set_xlim(-2.8, 12.8)
    ax.set_ylim(-1.4, 9.0)
    ax.axis('off')

    draw_system_boundary(ax, 0.3, -0.6, 9.6, 9.4, "Subsystem 1: Identity & Regulatory KYC Verification")

    draw_actor(ax, -1.6, 7.2, "Borrower", color=COLORS['Borrower'], scale=0.92)
    draw_actor(ax, -1.6, 5.0, "Co-Borrower", color=COLORS['Co-Borrower'], subtitle="Joint Debtor", scale=0.92)
    draw_actor(ax, -1.6, 2.8, "Guarantor", color=COLORS['Guarantor'], subtitle="Surety", scale=0.92)
    draw_actor(ax, -1.6, 0.6, "Lender", color=COLORS['Lender'], scale=0.92)

    draw_external_system(ax, 11.4, 6.2, "DigiLocker / UIDAI", color=COLORS['DigiLocker'])
    draw_external_system(ax, 11.4, 2.6, "Credit Bureau (CIBIL)", color=COLORS['CIBIL'])

    # Column 1 (x = 2.4, w = 2.6 -> right = 3.7)
    draw_use_case(ax, 2.4, 7.0, "UC-01: Log In\n(PIN or Biometrics)")
    draw_use_case(ax, 2.4, 4.8, "UC-02: Submit Aadhaar\n& PAN Card Details")
    draw_use_case(ax, 2.4, 2.2, "UC-03: Register Biometric Key\non Phone StrongBox")

    # Column 2 (x = 7.4, w = 2.6 -> left = 6.1)
    draw_use_case(ax, 7.4, 6.2, "UC-04: Verify Identity\nvia DigiLocker OCR")
    draw_use_case(ax, 7.4, 2.6, "UC-05: Check Credit Score\nfrom CIBIL Bureau")

    # Associations
    for y_pos in [7.4, 5.2, 3.0, 0.8]:
        col = [COLORS['Borrower'], COLORS['Co-Borrower'], COLORS['Guarantor'], COLORS['Lender']][int((7.4 - y_pos)/2.2)]
        draw_solid_line(ax, -1.2, y_pos, 1.1, 7.0, color=col)
        draw_solid_line(ax, -1.2, y_pos - 0.2, 1.1, 4.8, color=col)
    draw_solid_line(ax, -1.2, 7.0, 1.1, 2.2, color=COLORS['Borrower'])

    # External systems
    draw_solid_line(ax, 10.3, 6.2, 8.7, 6.2, color=COLORS['DigiLocker'])
    draw_solid_line(ax, 10.3, 2.6, 8.7, 2.6, color=COLORS['CIBIL'])

    # Relationships (Generous 2.4-unit gap from 3.7 to 6.1)
    draw_clean_relationship(ax, 3.7, 5.0, 6.1, 6.0, rel_type="include", color=COLORS['include'])
    draw_clean_relationship(ax, 3.7, 4.6, 6.1, 3.0, rel_type="include", color=COLORS['include'])

    # Legend
    legend_items = [
        ("Borrower", COLORS['Borrower'], 'solid'),
        ("Co-Borrower", COLORS['Co-Borrower'], 'solid'),
        ("Guarantor", COLORS['Guarantor'], 'solid'),
        ("Lender", COLORS['Lender'], 'solid'),
        ("DigiLocker", COLORS['DigiLocker'], 'solid'),
        ("CIBIL Bureau", COLORS['CIBIL'], 'solid'),
        ("<<include>>", COLORS['include'], 'dashed')
    ]
    draw_legend(ax, legend_items, x=0.3, y=-1.25, w=9.6, h=0.48)

    plt.tight_layout()
    path = "docs/diagrams/diagram_2_auth_kyc.png"
    plt.savefig(path, dpi=300, bbox_inches='tight')
    plt.close()
    print(f"Generated {path}")

# ==========================================
# DIAGRAM 3: MARKETPLACE, CO-BORROWER & NOMINEE
# Borrower declares Nominee, Co-Borrower accepts, Lender bids
# ==========================================
def generate_marketplace_diagram():
    fig, ax = plt.subplots(figsize=(15.0, 9.5), dpi=300)
    ax.set_xlim(-2.8, 12.8)
    ax.set_ylim(-1.4, 9.0)
    ax.axis('off')

    draw_system_boundary(ax, 0.3, -0.6, 9.6, 9.4, "Subsystem 2: Loan Request, Co-Borrower & Nominee Setup")

    draw_actor(ax, -1.6, 7.2, "Borrower", color=COLORS['Borrower'], scale=0.92)
    draw_actor(ax, -1.6, 4.5, "Co-Borrower", color=COLORS['Co-Borrower'], subtitle="Joint Debtor", scale=0.92)
    draw_actor(ax, 11.4, 6.8, "Lender", color=COLORS['Lender'], scale=0.92)
    draw_external_system(ax, 11.4, 2.8, "IBJA Gold Feed", color=COLORS['IBJA'])

    # Column 1 (x = 2.4, w = 2.6 -> right: 3.7)
    draw_use_case(ax, 2.4, 7.2, "UC-06: Create Loan Listing\n(Amount, Tenure, Purpose)")
    draw_use_case(ax, 2.4, 5.2, "UC-07: Select Co-Borrower\n& Guarantor")
    draw_use_case(ax, 2.4, 3.2, "UC-08: Enter Nominee\nBeneficiary Details")
    draw_use_case(ax, 2.4, 1.2, "UC-09: Confirm Joint Loan\nResponsibility via Biometric")

    # Column 2 (x = 7.4, w = 2.6 -> left: 6.1)
    draw_use_case(ax, 7.4, 6.8, "UC-10: Browse Loan Listings\n& Place Interest Bids")
    draw_use_case(ax, 7.4, 4.4, "UC-11: Enforce 75% Gold\nLoan Value Cap")
    draw_use_case(ax, 7.4, 2.0, "UC-12: Accept Winning Bid\n& Reserve Escrow Funds")

    # Borrower associations (Blue)
    draw_solid_line(ax, -1.2, 7.4, 1.1, 7.2, color=COLORS['Borrower'])
    draw_solid_line(ax, -1.2, 7.1, 1.1, 5.2, color=COLORS['Borrower'])
    draw_solid_line(ax, -1.2, 6.8, 1.1, 3.2, color=COLORS['Borrower'])

    # Co-Borrower association (Teal)
    draw_solid_line(ax, -1.2, 4.5, 1.1, 1.2, color=COLORS['Co-Borrower'])

    # Lender associations (Purple)
    draw_solid_line(ax, 11.0, 7.0, 8.7, 6.8, color=COLORS['Lender'])
    draw_solid_line(ax, 11.0, 6.6, 8.7, 2.0, color=COLORS['Lender'])

    # IBJA Feed (Antique Gold)
    draw_solid_line(ax, 10.3, 2.8, 8.7, 4.4, color=COLORS['IBJA'])

    # Relationships (Generous 2.4-unit gap from 3.7 to 6.1)
    draw_clean_relationship(ax, 2.4, 4.8, 2.4, 1.6, rel_type="include", color=COLORS['include'], is_vertical=True) # Select obligor -> Confirm liability
    draw_clean_relationship(ax, 3.7, 7.2, 6.1, 4.8, rel_type="include", color=COLORS['include'])                  # Create listing -> 75% LTV Cap
    draw_clean_relationship(ax, 7.4, 6.4, 7.4, 2.4, rel_type="include", color=COLORS['include'], is_vertical=True) # Browse/Bid -> Reserve Escrow

    # Legend
    legend_items = [
        ("Borrower", COLORS['Borrower'], 'solid'),
        ("Co-Borrower", COLORS['Co-Borrower'], 'solid'),
        ("Lender", COLORS['Lender'], 'solid'),
        ("IBJA Gold Feed", COLORS['IBJA'], 'solid'),
        ("<<include>>", COLORS['include'], 'dashed')
    ]
    draw_legend(ax, legend_items, x=0.3, y=-1.25, w=9.6, h=0.48)

    plt.tight_layout()
    path = "docs/diagrams/diagram_3_marketplace.png"
    plt.savefig(path, dpi=300, bbox_inches='tight')
    plt.close()
    print(f"Generated {path}")

# ==========================================
# DIAGRAM 4: MULTI-PARTY STATUTORY CONTRACTING
# Borrower, Co-Borrower, Guarantor, Lender, State Treasury, NeSL
# ==========================================
def generate_legal_contracting_diagram():
    fig, ax = plt.subplots(figsize=(15.0, 9.5), dpi=300)
    ax.set_xlim(-2.8, 12.8)
    ax.set_ylim(-1.4, 9.0)
    ax.axis('off')

    draw_system_boundary(ax, 0.3, -0.6, 9.6, 9.4, "Subsystem 3: Digital Contracts & Guarantee Agreements")

    draw_actor(ax, -1.6, 7.0, "Borrower", color=COLORS['Borrower'], scale=0.92)
    draw_actor(ax, -1.6, 4.6, "Co-Borrower", color=COLORS['Co-Borrower'], subtitle="Joint Debtor", scale=0.92)
    draw_actor(ax, -1.6, 2.2, "Guarantor", color=COLORS['Guarantor'], subtitle="Surety", scale=0.92)
    draw_actor(ax, 11.4, 6.8, "Lender", color=COLORS['Lender'], scale=0.92)

    draw_external_system(ax, 11.4, 4.2, "State Stamp Treasury", color=COLORS['Treasury'])
    draw_external_system(ax, 11.4, 1.8, "NeSL Info Utility", color=COLORS['NeSL'])

    # Column 1 (x = 2.4, w = 2.6 -> right: 3.7)
    draw_use_case(ax, 2.4, 6.6, "UC-13: Sign Promissory Note\n(Borrower & Lender)")
    draw_use_case(ax, 2.4, 4.4, "UC-14: Sign Joint Obligation\nContract (Co-Borrower)")
    draw_use_case(ax, 2.4, 2.0, "UC-15: Sign Formal\nGuarantee Deed (Guarantor)")

    # Column 2 (x = 7.4, w = 2.6 -> left: 6.1)
    draw_use_case(ax, 7.4, 6.4, "UC-16: Attach Official\nDigital E-Stamp Token")
    draw_use_case(ax, 7.4, 4.0, "UC-17: Record Legal Debt\nRegistry with NeSL")
    draw_use_case(ax, 7.4, 1.8, "UC-18: Download Court-Ready\nTamper-Proof Legal PDF")

    # Associations
    draw_solid_line(ax, -1.2, 7.1, 1.1, 6.6, color=COLORS['Borrower'])
    draw_solid_line(ax, -1.2, 4.6, 1.1, 4.4, color=COLORS['Co-Borrower'])
    draw_solid_line(ax, -1.2, 2.2, 1.1, 2.0, color=COLORS['Guarantor'])
    draw_solid_line(ax, 11.0, 6.8, 3.7, 6.6, color=COLORS['Lender'])

    # External systems
    draw_solid_line(ax, 10.3, 4.2, 8.7, 6.4, color=COLORS['Treasury'])
    draw_solid_line(ax, 10.3, 1.8, 8.7, 4.0, color=COLORS['NeSL'])

    # Relationships (Generous 2.4-unit gap from 3.7 to 6.1)
    draw_clean_relationship(ax, 3.7, 6.6, 6.1, 6.5, rel_type="include", color=COLORS['include'])
    draw_clean_relationship(ax, 3.7, 4.4, 6.1, 4.2, rel_type="include", color=COLORS['include'])
    draw_clean_relationship(ax, 3.7, 2.2, 6.1, 3.8, rel_type="include", color=COLORS['include'])
    draw_clean_relationship(ax, 7.4, 3.6, 7.4, 2.2, rel_type="include", color=COLORS['include'], is_vertical=True)

    # Legend
    legend_items = [
        ("Borrower", COLORS['Borrower'], 'solid'),
        ("Co-Borrower", COLORS['Co-Borrower'], 'solid'),
        ("Guarantor", COLORS['Guarantor'], 'solid'),
        ("Lender", COLORS['Lender'], 'solid'),
        ("Stamp Treasury", COLORS['Treasury'], 'solid'),
        ("NeSL Utility", COLORS['NeSL'], 'solid'),
        ("<<include>>", COLORS['include'], 'dashed')
    ]
    draw_legend(ax, legend_items, x=0.3, y=-1.25, w=9.6, h=0.48)

    plt.tight_layout()
    path = "docs/diagrams/diagram_4_legal_contracting.png"
    plt.savefig(path, dpi=300, bbox_inches='tight')
    plt.close()
    print(f"Generated {path}")

# ==========================================
# DIAGRAM 5: SERVICING & DELINQUENCY ESCALATION
# Borrower, Co-Borrower, Guarantor, Lender, NPCI UPI
# ==========================================
def generate_repayment_diagram():
    fig, ax = plt.subplots(figsize=(15.0, 9.5), dpi=300)
    ax.set_xlim(-2.8, 12.8)
    ax.set_ylim(-1.4, 9.0)
    ax.axis('off')

    draw_system_boundary(ax, 0.3, -0.6, 9.6, 9.4, "Subsystem 4: Monthly Repayments & Overdue Escalation")

    draw_actor(ax, -1.6, 7.0, "Borrower", color=COLORS['Borrower'], scale=0.92)
    draw_actor(ax, -1.6, 4.6, "Co-Borrower", color=COLORS['Co-Borrower'], subtitle="Joint Debtor", scale=0.92)
    draw_actor(ax, -1.6, 2.2, "Guarantor", color=COLORS['Guarantor'], subtitle="Surety", scale=0.92)
    draw_actor(ax, 11.4, 7.0, "Lender", color=COLORS['Lender'], scale=0.92)
    draw_external_system(ax, 11.4, 3.2, "NPCI UPI Switch", color=COLORS['NPCI'])

    # Column 1 (x = 2.4, w = 2.6 -> right: 3.7)
    draw_use_case(ax, 2.4, 6.8, "UC-19: Pay Monthly EMI\nvia UPI")
    draw_use_case(ax, 2.4, 4.6, "UC-20: Set Up UPI AutoPay\nRecurring Mandate")
    draw_use_case(ax, 2.4, 2.0, "UC-21: Pay Overdue EMI\non Borrower's Behalf")

    # Column 2 (x = 7.4, w = 2.6 -> left: 6.1)
    draw_use_case(ax, 7.4, 6.8, "UC-22: Charge Late Fee\n(Overdue > 3 Days)")
    draw_use_case(ax, 7.4, 4.6, "UC-23: Send Urgent Alert to\nCo-Borrower (Days 4-30)")
    draw_use_case(ax, 7.4, 2.6, "UC-24: Send Legal Notice to\nGuarantor (30+ Days Overdue)")
    draw_use_case(ax, 7.4, 0.8, "UC-25: Restructure Loan &\nRevise EMI Schedule")

    # Associations
    draw_solid_line(ax, -1.2, 7.2, 1.1, 6.8, color=COLORS['Borrower'])
    draw_solid_line(ax, -1.2, 6.8, 1.1, 4.6, color=COLORS['Borrower'])

    draw_solid_line(ax, -1.2, 4.6, 1.1, 2.0, color=COLORS['Co-Borrower'])
    draw_solid_line(ax, -1.2, 2.2, 1.1, 2.0, color=COLORS['Guarantor'])

    draw_solid_line(ax, 11.0, 7.0, 3.7, 6.8, color=COLORS['Lender'])

    # NPCI associations (Sky Blue)
    draw_solid_line(ax, 10.3, 3.4, 3.7, 6.8, color=COLORS['NPCI'])
    draw_solid_line(ax, 10.3, 3.0, 3.7, 4.6, color=COLORS['NPCI'])

    # Clean Relationships (Generous 2.4-unit gap from 3.7 to 6.1)
    c_ext = COLORS['extend']
    draw_clean_relationship(ax, 6.1, 6.8, 3.7, 6.8, rel_type="extend", color=c_ext)
    draw_clean_relationship(ax, 6.1, 4.9, 3.7, 6.4, rel_type="extend", color=c_ext)
    draw_clean_relationship(ax, 6.1, 2.8, 3.7, 6.0, rel_type="extend", color=c_ext)
    draw_clean_relationship(ax, 7.4, 1.7, 7.4, 1.2, rel_type="extend", color=c_ext, is_vertical=True)

    # Legend
    legend_items = [
        ("Borrower", COLORS['Borrower'], 'solid'),
        ("Co-Borrower", COLORS['Co-Borrower'], 'solid'),
        ("Guarantor", COLORS['Guarantor'], 'solid'),
        ("Lender", COLORS['Lender'], 'solid'),
        ("NPCI UPI", COLORS['NPCI'], 'solid'),
        ("<<extend>>", COLORS['extend'], 'dashed')
    ]
    draw_legend(ax, legend_items, x=0.3, y=-1.25, w=9.6, h=0.48)

    plt.tight_layout()
    path = "docs/diagrams/diagram_5_repayment.png"
    plt.savefig(path, dpi=300, bbox_inches='tight')
    plt.close()
    print(f"Generated {path}")

# ==========================================
# DIAGRAM 6: VAULTING, NOMINEE & RECOVERY
# Field Agent, Borrower, Nominee (Contingent), Platform Admin, Civil Court
# ==========================================
def generate_vault_recovery_diagram():
    fig, ax = plt.subplots(figsize=(15.0, 9.5), dpi=300)
    ax.set_xlim(-2.8, 12.8)
    ax.set_ylim(-1.4, 9.0)
    ax.axis('off')

    draw_system_boundary(ax, 0.3, -0.6, 9.6, 9.4, "Subsystem 5: Gold Vaulting, Nominee Claim & Legal Recovery")

    draw_actor(ax, -1.6, 7.2, "Field Agent", color=COLORS['Field Agent'], subtitle="Gold Valuer", scale=0.92)
    draw_actor(ax, -1.6, 4.6, "Borrower", color=COLORS['Borrower'], subtitle="Primary Debtor", scale=0.92)
    draw_actor(ax, -1.6, 1.8, "Nominee", color=COLORS['Nominee'], subtitle="If Borrower Dies", scale=0.92)
    draw_actor(ax, 11.4, 6.6, "Platform Admin", color=COLORS['Admin'], scale=0.92)
    draw_external_system(ax, 11.4, 3.0, "Civil District Court", color=COLORS['Court'])

    # Column 1 (x = 2.4, w = 2.6 -> right: 3.7)
    draw_use_case(ax, 2.4, 7.2, "UC-26: Test Gold Purity\n& Weigh Net Grams")
    draw_use_case(ax, 2.4, 5.0, "UC-27: Seal Gold in Barcode\nPouch & Deposit in Vault")
    draw_use_case(ax, 2.4, 2.8, "UC-28: Return Gold to Borrower\n(Full Payoff & NOC)")
    draw_use_case(ax, 2.4, 0.8, "UC-29: Submit Death Proof &\nClaim Gold / Surplus (§50 CPC)")

    # Column 2 (x = 7.4, w = 2.6 -> left: 6.1)
    draw_use_case(ax, 7.4, 6.8, "UC-30: Audit Repayment Ledger\n& Security Logs")
    draw_use_case(ax, 7.4, 4.4, "UC-31: Auction Gold Collateral\n(If Loan Defaults at 90+ Days)")
    draw_use_case(ax, 7.4, 2.0, "UC-32: File Order 37 Fast-Track\nCourt Recovery Suit")

    # Associations
    draw_solid_line(ax, -1.2, 7.4, 1.1, 7.2, color=COLORS['Field Agent'])
    draw_solid_line(ax, -1.2, 7.0, 1.1, 5.0, color=COLORS['Field Agent'])

    draw_solid_line(ax, -1.2, 4.6, 1.1, 2.8, color=COLORS['Borrower'])
    draw_solid_line(ax, -1.2, 1.8, 1.1, 0.8, color=COLORS['Nominee'])

    draw_solid_line(ax, 11.0, 6.8, 8.7, 6.8, color=COLORS['Admin'])
    draw_solid_line(ax, 11.0, 6.5, 8.7, 4.4, color=COLORS['Admin'])
    draw_solid_line(ax, 11.0, 6.2, 8.7, 2.0, color=COLORS['Admin'])

    # Civil Court association (Charcoal)
    draw_solid_line(ax, 10.3, 3.0, 8.7, 2.0, color=COLORS['Court'])

    # Clean Relationships (Generous 2.4-unit gap from 3.7 to 6.1)
    draw_clean_relationship(ax, 2.4, 6.8, 2.4, 5.4, rel_type="include", color=COLORS['include'], is_vertical=True)
    draw_clean_relationship(ax, 6.1, 4.4, 3.7, 3.2, rel_type="extend", color=COLORS['extend'])
    draw_clean_relationship(ax, 6.1, 2.0, 3.7, 2.6, rel_type="extend", color=COLORS['extend'])
    draw_clean_relationship(ax, 2.4, 1.2, 2.4, 2.4, rel_type="extend", color=COLORS['extend'], is_vertical=True)

    # Legend
    legend_items = [
        ("Field Agent", COLORS['Field Agent'], 'solid'),
        ("Borrower", COLORS['Borrower'], 'solid'),
        ("Nominee", COLORS['Nominee'], 'solid'),
        ("Admin", COLORS['Admin'], 'solid'),
        ("Civil Court", COLORS['Court'], 'solid'),
        ("<<include>>", COLORS['include'], 'dashed'),
        ("<<extend>>", COLORS['extend'], 'dashed')
    ]
    draw_legend(ax, legend_items, x=0.3, y=-1.25, w=9.6, h=0.48)

    plt.tight_layout()
    path = "docs/diagrams/diagram_6_vault_recovery.png"
    plt.savefig(path, dpi=300, bbox_inches='tight')
    plt.close()
    print(f"Generated {path}")

if __name__ == "__main__":
    generate_master_diagram()
    generate_auth_kyc_diagram()
    generate_marketplace_diagram()
    generate_legal_contracting_diagram()
    generate_repayment_diagram()
    generate_vault_recovery_diagram()
    print("All diagrams generated with wide column clearance, clean above-the-line labels, and easy language.")
