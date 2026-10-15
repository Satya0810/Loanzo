import matplotlib.pyplot as plt
import matplotlib.patches as patches
from matplotlib.patches import Ellipse, Rectangle, FancyArrowPatch

def draw_actor(ax, x, y, name, scale=1.0):
    """Draws a clean standard UML stick figure actor in pure black & white."""
    r = 0.18 * scale
    # Head
    head = Ellipse((x, y + 0.48 * scale), 2 * r, 2 * r, fill=True, facecolor='white', edgecolor='black', linewidth=1.6, zorder=5)
    ax.add_patch(head)
    # Body
    ax.plot([x, x], [y + 0.30 * scale, y - 0.20 * scale], color='black', linewidth=1.6, zorder=5)
    # Arms
    ax.plot([x - 0.28 * scale, x + 0.28 * scale], [y + 0.12 * scale, y + 0.12 * scale], color='black', linewidth=1.6, zorder=5)
    # Legs
    ax.plot([x, x - 0.24 * scale], [y - 0.20 * scale, y - 0.65 * scale], color='black', linewidth=1.6, zorder=5)
    ax.plot([x, x + 0.24 * scale], [y - 0.20 * scale, y - 0.65 * scale], color='black', linewidth=1.6, zorder=5)
    # Name Label
    ax.text(x, y - 0.88 * scale, name, ha='center', va='top', fontsize=9.5, fontweight='bold', color='black', zorder=5)

def draw_use_case(ax, x, y, text, w=2.45, h=0.72):
    """Draws a clean standard UML oval use case."""
    ellipse = Ellipse((x, y), w, h, fill=True, facecolor='white', edgecolor='black', linewidth=1.3, zorder=3)
    ax.add_patch(ellipse)
    ax.text(x, y, text, ha='center', va='center', fontsize=8.0, color='black', zorder=4, multialignment='center')

def draw_system_boundary(ax, x, y, w, h, title):
    """Draws the system boundary rectangle."""
    rect = Rectangle((x, y), w, h, fill=False, edgecolor='black', linewidth=1.8, linestyle='-', zorder=1)
    ax.add_patch(rect)
    ax.text(x + 0.3, y + h - 0.32, title, ha='left', va='top', fontsize=11.5, fontweight='bold', color='black', zorder=2)

def draw_external_system(ax, x, y, name, w=2.0, h=0.72):
    """Draws a secondary external system actor box."""
    rect = Rectangle((x - w/2, y - h/2), w, h, fill=True, facecolor='white', edgecolor='black', linewidth=1.3, zorder=3)
    ax.add_patch(rect)
    ax.text(x, y + 0.13, "<<system>>", ha='center', va='center', fontsize=7.0, fontstyle='italic', color='#444444', zorder=4)
    ax.text(x, y - 0.13, name, ha='center', va='center', fontsize=8.5, fontweight='bold', color='black', zorder=4)

def draw_solid_line(ax, x1, y1, x2, y2):
    """Draws an association line between actor and use case."""
    ax.plot([x1, x2], [y1, y2], color='black', linewidth=1.1, zorder=2)

def draw_dashed_arrow(ax, x1, y1, x2, y2, label, label_offset=(0, 0.12)):
    """Draws an <<include>> or <<extend>> relationship arrow with label."""
    arrow = FancyArrowPatch((x1, y1), (x2, y2),
                             arrowstyle='->,head_width=3.2,head_length=4.8',
                             linestyle='--', color='black', linewidth=1.0, zorder=2)
    ax.add_patch(arrow)
    mid_x = (x1 + x2) / 2 + label_offset[0]
    mid_y = (y1 + y2) / 2 + label_offset[1]
    ax.text(mid_x, mid_y, label, ha='center', va='center', fontsize=7.0, fontstyle='italic',
            bbox=dict(boxstyle='square,pad=0.10', facecolor='white', edgecolor='none'), zorder=4)

def build_diagram():
    fig, ax = plt.subplots(figsize=(15, 11), dpi=300)
    ax.set_xlim(-2.4, 13.0)
    ax.set_ylim(-0.8, 10.6)
    ax.axis('off')

    # 1. System Boundary
    draw_system_boundary(ax, 0.4, -0.4, 9.4, 10.6, "System Boundary: Loanzo Platform")

    # 2. Actors Placement
    # Left Primary Actors
    draw_actor(ax, -1.3, 8.4, "Borrower", scale=0.95)
    draw_actor(ax, -1.3, 4.8, "Lender", scale=0.95)
    draw_actor(ax, -1.3, 1.4, "Field Agent", scale=0.95)

    # Right Actors: Platform Admin & External Systems
    draw_actor(ax, 11.6, 2.0, "Platform Admin", scale=0.95)
    draw_external_system(ax, 11.6, 8.2, "DigiLocker / UIDAI")
    draw_external_system(ax, 11.6, 5.8, "NPCI UPI Switch")
    draw_external_system(ax, 11.6, 3.8, "NeSL Info Utility")

    # 3. Use Cases - Clean 3-Column Balanced Layout
    # Column 1: Primary Interaction Use Cases (x = 2.4)
    uc_auth     = (2.4, 9.0, "UC-01: Authenticate\n(Biometrics / PIN)")
    uc_kyc      = (2.4, 7.5, "UC-02: Complete KYC\nVerification")
    uc_list     = (2.4, 5.8, "UC-03: Create Loan\nListing")
    uc_bid      = (2.4, 4.3, "UC-04: Browse & Bid\non Loans")
    uc_repay    = (2.4, 2.7, "UC-05: Repay Monthly\nEMI")
    uc_appraise = (2.4, 1.2, "UC-06: Appraise Bullion\nCollateral")

    # Column 2: Core Processing & Included Sub-routines (x = 5.3)
    uc_ocr      = (5.3, 8.2, "UC-08: Extract PAN / Aadhaar\nvia OCR")
    uc_cibil    = (5.3, 6.9, "UC-09: Evaluate Credit\nRisk Score")
    uc_ltv      = (5.3, 5.5, "UC-10: Verify Collateral\n75% LTV Cap")
    uc_disburse = (5.3, 4.0, "UC-11: Disburse Loan via\nAutomated Escrow")
    uc_contract = (5.3, 2.6, "UC-12: Execute Promissory Note\n(Sec 4 NI Act)")
    uc_penalty  = (5.3, 1.2, "UC-13: Apply Delinquency\nLate Penalty")

    # Column 3: Statutory Legal & Admin (x = 8.2)
    uc_stamp    = (8.2, 3.8, "UC-14: Affix Digital\nE-Stamp Token")
    uc_order37  = (8.2, 2.4, "UC-15: Invoke Order 37 CPC\nLegal Summary Suit")
    uc_audit    = (8.2, 0.9, "UC-07: Monitor Risk &\nLedger Audits")

    cases = [uc_auth, uc_kyc, uc_list, uc_bid, uc_repay, uc_appraise,
             uc_ocr, uc_cibil, uc_ltv, uc_disburse, uc_contract, uc_penalty,
             uc_stamp, uc_order37, uc_audit]
    for c in cases:
        draw_use_case(ax, c[0], c[1], c[2])

    # 4. Associations: Borrower (x = -1.3, y = 8.4)
    draw_solid_line(ax, -0.9, 8.7, 1.2, 9.0)   # Borrower -> Auth
    draw_solid_line(ax, -0.9, 8.4, 1.2, 7.5)   # Borrower -> KYC
    draw_solid_line(ax, -0.9, 8.1, 1.2, 5.8)   # Borrower -> Create Listing
    draw_solid_line(ax, -0.9, 7.8, 1.2, 2.7)   # Borrower -> Repay EMI

    # Associations: Lender (x = -1.3, y = 4.8)
    draw_solid_line(ax, -0.9, 5.1, 1.2, 9.0)   # Lender -> Auth
    draw_solid_line(ax, -0.9, 4.8, 1.2, 7.5)   # Lender -> KYC
    draw_solid_line(ax, -0.9, 4.5, 1.2, 4.3)   # Lender -> Browse & Bid

    # Associations: Field Agent (x = -1.3, y = 1.4)
    draw_solid_line(ax, -0.9, 1.7, 1.2, 9.0)   # Field Agent -> Auth
    draw_solid_line(ax, -0.9, 1.4, 1.2, 1.2)   # Field Agent -> Appraise Bullion

    # Associations: Platform Admin (x = 11.6, y = 2.0)
    draw_solid_line(ax, 11.2, 1.6, 9.4, 0.9)   # Admin -> Monitor Risk & Audit
    draw_solid_line(ax, 11.2, 2.3, 9.4, 2.4)   # Admin -> Order 37 Suit

    # Associations: Secondary Systems (Right)
    draw_solid_line(ax, 10.6, 8.2, 6.5, 8.2)   # DigiLocker -> OCR
    draw_solid_line(ax, 10.6, 5.8, 6.5, 4.0)   # NPCI -> Disburse
    draw_solid_line(ax, 10.6, 5.6, 3.6, 2.7)   # NPCI -> Repay EMI
    draw_solid_line(ax, 10.6, 3.8, 9.4, 3.8)   # NeSL -> E-Stamp
    draw_solid_line(ax, 10.6, 3.6, 6.5, 2.6)   # NeSL -> Promissory Contract

    # 5. Include & Extend Relationships (Dashed Arrows)
    # KYC <<include>> OCR
    draw_dashed_arrow(ax, 3.6, 7.7, 4.1, 8.0, "<<include>>", (0, 0.12))
    # KYC <<include>> CIBIL
    draw_dashed_arrow(ax, 3.6, 7.3, 4.1, 7.0, "<<include>>", (0, 0.12))
    # Create Listing <<include>> LTV
    draw_dashed_arrow(ax, 3.6, 5.7, 4.1, 5.6, "<<include>>", (0, 0.12))
    # Browse & Bid <<include>> Disburse
    draw_dashed_arrow(ax, 3.6, 4.2, 4.1, 4.1, "<<include>>", (0, 0.12))
    # Disburse <<include>> Promissory Note
    draw_dashed_arrow(ax, 5.3, 3.6, 5.3, 3.0, "<<include>>", (0.35, 0))
    # Promissory Note <<include>> Digital E-Stamp
    draw_dashed_arrow(ax, 6.5, 2.9, 7.0, 3.5, "<<include>>", (-0.1, 0.12))
    # Late Penalty <<extend>> Repay EMI (arrow points from Penalty to Repay EMI)
    draw_dashed_arrow(ax, 4.1, 1.6, 3.4, 2.3, "<<extend>>", (-0.1, -0.12))
    # Order 37 Suit <<extend>> Promissory Note (arrow points from Order 37 to Promissory Note)
    draw_dashed_arrow(ax, 7.0, 2.4, 6.5, 2.5, "<<extend>>", (0, 0.12))

    plt.tight_layout()
    plt.savefig("docs/diagrams/use_case_diagram_clean.png", dpi=300, bbox_inches='tight')
    plt.close()
    print("Refined clean UML diagram generated successfully.")

if __name__ == "__main__":
    build_diagram()
