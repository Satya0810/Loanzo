import os
import matplotlib.pyplot as plt
import matplotlib.patches as patches
from matplotlib.patches import FancyBboxPatch, Polygon, Circle

def create_activity_diagram(output_path="docs/diagrams/activity_diagram_gfg.png"):
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    
    # High resolution canvas (A3 proportions for clear printing and legibility)
    fig_w, fig_h = 24, 34
    fig, ax = plt.subplots(figsize=(fig_w, fig_h), dpi=300)
    ax.set_xlim(0, 100)
    ax.set_ylim(-3, 141)
    ax.axis('off')
    
    # Color Palette - Professional GeeksforGeeks / Modern Fintech Style
    c_bg = "#F8FAFC"          # Light Slate Canvas
    c_swimlane_hdr = "#1E293B" # Dark Slate
    c_swimlane_bg = ["#FFFFFF", "#F1F5F9", "#FFFFFF", "#F8FAFC"]
    c_border = "#94A3B8"
    c_action_bg = "#FFFFFF"
    c_action_edge = "#2563EB"  # Royal Blue
    c_decision_bg = "#FEF3C7"  # Warm Amber Cream
    c_decision_edge = "#D97706"# Amber 600
    c_fork_join = "#0F172A"    # Deep Charcoal/Black
    c_text = "#0F172A"
    c_subtext = "#475569"
    c_emerald = "#059669"
    c_red = "#DC2626"
    
    # Background Canvas Fill
    fig.patch.set_facecolor(c_bg)
    ax.set_facecolor(c_bg)
    
    # Title Block (Header)
    ax.text(50, 137.5, "LOANZO PROTOCOL — UML ACTIVITY DIAGRAM", 
            fontsize=24, fontweight='bold', ha='center', va='center', color="#0F172A", fontfamily='sans-serif')
    ax.text(50, 135.5, "Standard GeeksforGeeks (GFG) Compliant Specification | End-to-End P2P Lending, Assaying & Debt Servicing Lifecycle", 
            fontsize=12, fontweight='medium', ha='center', va='center', color="#475569", fontfamily='sans-serif')
    
    # 4 Swimlanes Configuration
    lanes = [
        {"name": "1. BORROWER\n(Member / Loan Applicant)", "x1": 2, "x2": 26, "color": "#FFFFFF"},
        {"name": "2. LOANZO APP & CORE ENGINE\n(Automated Protocol & AI Engine)", "x1": 26, "x2": 52, "color": "#F8FAFC"},
        {"name": "3. FIELD AGENT / VALUER\n(Doorstep Assayer & Inspection)", "x1": 52, "x2": 75, "color": "#FFFFFF"},
        {"name": "4. LENDER / INVESTOR\n(Peer Capital Provider)", "x1": 75, "x2": 98, "color": "#F8FAFC"}
    ]
    
    lane_top = 133
    lane_bottom = -1.5
    
    # Draw Swimlane Backgrounds and Column Headers
    for i, lane in enumerate(lanes):
        # Header Box
        hdr_rect = FancyBboxPatch((lane["x1"], lane_top - 4.5), lane["x2"] - lane["x1"], 4.5,
                                  boxstyle="round,pad=0.0,rounding_size=0.8",
                                  facecolor=c_swimlane_hdr, edgecolor="#334155", linewidth=1.5)
        ax.add_patch(hdr_rect)
        ax.text((lane["x1"] + lane["x2"]) / 2, lane_top - 2.25, lane["name"],
                fontsize=11, fontweight='bold', ha='center', va='center', color="#FFFFFF", fontfamily='sans-serif')
        
        # Column Body extending all the way down to lane_bottom
        col_rect = FancyBboxPatch((lane["x1"], lane_bottom), lane["x2"] - lane["x1"], lane_top - 4.5 - lane_bottom,
                                  boxstyle="square,pad=0.0",
                                  facecolor=lane["color"], edgecolor=c_border, linewidth=1.2, linestyle='-')
        ax.add_patch(col_rect)
        
    # Helper Functions for Standard GFG UML Elements
    def draw_action(x, y, w, h, title, subtitle="", color=c_action_bg, border_color=c_action_edge, text_color=c_text):
        rect = FancyBboxPatch((x - w/2, y - h/2), w, h,
                              boxstyle="round,pad=0.2,rounding_size=1.0",
                              facecolor=color, edgecolor=border_color, linewidth=1.8, zorder=3)
        ax.add_patch(rect)
        title_font = 8.5 if w < 12 else 9.5
        sub_font = 7.2 if w < 12 else 8.0
        if subtitle:
            ax.text(x, y + 0.6, title, fontsize=title_font, fontweight='bold', ha='center', va='center', color=text_color, zorder=4)
            ax.text(x, y - 0.7, subtitle, fontsize=sub_font, fontweight='normal', ha='center', va='center', color=c_subtext, zorder=4)
        else:
            ax.text(x, y, title, fontsize=title_font, fontweight='bold', ha='center', va='center', color=text_color, zorder=4)
            
    def draw_decision(x, y, w, h, text, guard_text=""):
        # Diamond shape
        pts = [[x, y + h/2], [x + w/2, y], [x, y - h/2], [x - w/2, y]]
        poly = Polygon(pts, closed=True, facecolor=c_decision_bg, edgecolor=c_decision_edge, linewidth=1.8, zorder=3)
        ax.add_patch(poly)
        ax.text(x, y, text, fontsize=9, fontweight='bold', ha='center', va='center', color="#78350F", zorder=4)
        if guard_text:
            ax.text(x, y - h/2 - 0.7, guard_text, fontsize=8, fontweight='bold', ha='center', va='top', color="#B45309", zorder=4)

    def draw_fork_join(x1, x2, y, h=0.7, label=""):
        rect = FancyBboxPatch((x1, y - h/2), x2 - x1, h,
                              boxstyle="round,pad=0.0,rounding_size=0.3",
                              facecolor=c_fork_join, edgecolor=c_fork_join, zorder=5)
        ax.add_patch(rect)
        if label:
            ax.text((x1 + x2)/2, y + 0.8, label, fontsize=8, fontweight='bold', ha='center', va='bottom', color="#0F172A", zorder=5)

    def draw_arrow(x1, y1, x2, y2, label="", label_pos=(0.5, 0.5), color="#334155", lw=1.6, ls='-'):
        ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                    arrowprops=dict(arrowstyle="->,head_width=0.35,head_length=0.45",
                                    color=color, lw=lw, linestyle=ls,
                                    shrinkA=1, shrinkB=1), zorder=2)
        if label:
            lx = x1 + (x2 - x1) * label_pos[0]
            ly = y1 + (y2 - y1) * label_pos[1]
            ax.text(lx, ly, label, fontsize=8, fontweight='bold', ha='center', va='center',
                    bbox=dict(boxstyle="round,pad=0.2", facecolor="#FFFFFF", edgecolor=color, lw=0.8), zorder=6)

    # 1. INITIAL STATE (Start Node)
    # Circle with solid fill in Borrower Lane
    start_c = Circle((14, 126), 1.2, facecolor="#0F172A", edgecolor="#0F172A", zorder=4)
    ax.add_patch(start_c)
    ax.text(14, 127.8, "START", fontsize=9, fontweight='bold', ha='center', va='center', color="#0F172A")
    
    # Flow 1 -> 2: Open App & Sign In
    draw_arrow(14, 124.8, 14, 121.5)
    draw_action(14, 120, 18, 3.0, "Sign In to Loanzo", "Enter Mobile & Role (Member)")
    
    # Flow 2 -> 3: To Loanzo App (KYC & Trust Score)
    draw_arrow(14, 118.5, 39, 118.5)
    draw_arrow(39, 118.5, 39, 116)
    draw_action(39, 114.5, 20, 3.2, "Verify Identity (KYC)", "Aadhaar & PAN via DigiLocker")
    
    draw_arrow(39, 112.9, 39, 110.5)
    draw_action(39, 109, 21, 3.2, "Calculate Trust Score", "4 Points (KYC, Repay, Bank, Vouches)")
    
    # Flow to Borrower: Configure Simulator
    draw_arrow(39, 107.4, 14, 107.4)
    draw_arrow(14, 107.4, 14, 105)
    draw_action(14, 103.5, 19, 3.0, "Configure Loan Simulator", "Select Amount, Rate & Tenure (Months)")
    
    # Decision: Application Type (Direct vs Community Marketplace)
    draw_arrow(14, 102.0, 14, 99.5)
    draw_decision(14, 97.5, 13, 4.0, "Application\nMethod?")
    
    # Path A: Direct Deal (Cleanly within Lane 1: x from 3 to 15)
    draw_arrow(7.5, 97.5, 4.0, 97.5)
    draw_arrow(4.0, 97.5, 4.0, 90.0, label="[Direct Deal]", label_pos=(0.5, 0.5))
    draw_arrow(4.0, 90.0, 3.0, 90.0)
    draw_action(9, 90, 12, 2.8, "Select Contact", "Known Peer / Relative")
    
    # Path B: Community Marketplace Post
    draw_arrow(20.5, 97.5, 39, 97.5, label="[Community Wall]", label_pos=(0.4, 0.6))
    draw_arrow(39, 97.5, 39, 93)
    draw_action(39, 91.5, 20, 3.0, "Multi-AI Pitch Enhancement", "Rewrites Pitch in Clear Vernacular")
    
    draw_arrow(39, 90.0, 39, 86.5)
    draw_action(39, 85, 20, 3.0, "Publish to Marketplace", "Open for Peer Proposals & Bids")
    
    # Connect to Lender Lane (Bidding & Negotiation)
    draw_arrow(49, 85, 86.5, 85)
    draw_arrow(86.5, 85, 86.5, 82)
    draw_action(86.5, 80.5, 18, 3.0, "Review Requests & Bid", "Lender Proposes Interest & Terms")
    
    # Merge Node for Loan Structuring
    draw_arrow(86.5, 79.0, 45.5, 79.0)
    draw_arrow(9, 88.6, 9, 79.0)
    draw_arrow(9, 79.0, 32.5, 79.0)
    
    draw_decision(39, 79.0, 13, 3.8, "Terms\nAgreed?", "[Both Confirm]")
    
    # Decision: Collateral Offered?
    draw_arrow(39, 77.1, 39, 73.5)
    draw_decision(39, 71.5, 15, 4.0, "Is Collateral\nOffered?")
    
    # ── BRANCH 1: SECURED LOAN (COLLATERALIZED) ──
    draw_arrow(46.5, 71.5, 63.5, 71.5, label="[Yes: Gold/Asset]", label_pos=(0.4, 0.6))
    draw_arrow(63.5, 71.5, 63.5, 67.5)
    
    # FORK BAR (Parallel Execution: Agent Doorstep Inspection + Co-Borrower Verification)
    draw_fork_join(54, 73, 67, h=0.7, label="FORK (Parallel Verification)")
    
    # Thread A: Field Agent (Doorstep Assay) - Perfectly sized w=9.6 at x=57.6 (Span 52.8 - 62.4)
    draw_arrow(57.6, 66.65, 57.6, 63.3)
    draw_action(57.6, 61.5, 9.6, 3.5, "Doorstep Assay", "Physical Purity Test")
    
    # Thread B: Co-Signer / Guarantor - Perfectly sized w=9.6 at x=69.4 (Span 64.6 - 74.2)
    # Completely separated from Thread A with a 2.2 unit clear gap (ZERO OVERLAP!)
    draw_arrow(69.4, 66.65, 69.4, 63.3)
    draw_action(69.4, 61.5, 9.6, 3.5, "Co-Signer Check", "Guarantor Consent")
    
    # JOIN BAR (Synchronization)
    draw_arrow(57.6, 59.75, 57.6, 56.85)
    draw_arrow(69.4, 59.75, 69.4, 56.85)
    draw_fork_join(54, 73, 56.5, h=0.7, label="JOIN (Evidence Synchronized)")
    
    # Secure Vaulting
    draw_arrow(63.5, 56.15, 63.5, 53.0)
    draw_action(63.5, 51.5, 18, 3.0, "Bank Locker Vaulting", "Barcode Bag Seal (#G408459)")
    
    # ── BRANCH 2: UNSECURED LOAN (TRUST SCORE & VOUCHES) ──
    draw_arrow(31.5, 71.5, 14, 71.5, label="[No: Unsecured]", label_pos=(0.4, 0.6))
    draw_arrow(14, 71.5, 14, 63.5)
    draw_action(14, 61.5, 18, 3.5, "Evaluate Social Vouches", "Requires Trust Score >= 75/100")
    
    # MERGE NODE: Contract Generation
    draw_arrow(14, 59.7, 14, 51.5)
    draw_arrow(14, 51.5, 32.5, 47)
    draw_arrow(54.5, 51.5, 45.5, 47)
    
    draw_action(39, 45.5, 20, 3.2, "Generate Loan Agreement", "Digital Legal Promissory Note")
    
    # Both Parties Sign (eSign)
    draw_arrow(39, 43.9, 14, 43.9)
    draw_arrow(14, 43.9, 14, 41.5)
    draw_action(14, 40, 18, 2.8, "Borrower e-Signs", "OTP / Touch Authentication")
    
    draw_arrow(39, 43.9, 86.5, 43.9)
    draw_arrow(86.5, 43.9, 86.5, 41.5)
    draw_action(86.5, 40, 18, 2.8, "Lender e-Signs", "Confirms Capital Disbursement")
    
    # Decision: Money Destination
    draw_arrow(14, 38.6, 39, 38.6)
    draw_arrow(86.5, 38.6, 39, 38.6)
    draw_arrow(39, 38.6, 39, 36)
    draw_decision(39, 34, 15, 3.8, "Disbursement\nRoute?")
    
    # Route 1: Purpose-Bound Tranche (Direct to Merchant / College)
    draw_arrow(46.5, 34, 63.5, 34, label="[To School/Shop]", label_pos=(0.4, 0.6))
    draw_arrow(63.5, 34, 63.5, 30.5)
    draw_action(63.5, 29, 18, 3.0, "Verified Vendor Transfer", "Paid Directly for Tuition/Goods")
    
    # Route 2: Direct to Borrower Account
    draw_arrow(31.5, 34, 14, 34, label="[Direct UPI]", label_pos=(0.4, 0.6))
    draw_arrow(14, 34, 14, 30.5)
    draw_action(14, 29, 18, 3.0, "Direct UPI Transfer", "Transferred to Borrower Bank")
    
    # MERGE & START REPAYMENT CYCLE
    draw_arrow(14, 27.5, 26, 24)
    draw_arrow(63.5, 27.5, 52, 24)
    draw_action(39, 23.5, 22, 3.2, "Monthly Repayment Cycle", "Automated Amortization Schedule")
    
    # Decision: Payment Regularity
    draw_arrow(39, 21.9, 39, 19.5)
    draw_decision(39, 18.0, 14, 3.5, "Paid On Time?")
    
    # Path: Payment On Time
    draw_arrow(46, 18.0, 63.5, 18.0, label="[Yes: Paid]", label_pos=(0.4, 0.6))
    draw_arrow(63.5, 18.0, 63.5, 15)
    draw_decision(63.5, 13.5, 14, 3.2, "Loan Fully\nCleared?")
    
    # Path: Still remaining months (Loop)
    draw_arrow(70.5, 13.5, 73.5, 13.5)
    draw_arrow(73.5, 13.5, 73.5, 23.5)
    draw_arrow(73.5, 23.5, 50, 23.5, label="[No: Next Month EMI]", label_pos=(0.5, 0.8))
    
    # Path: Fully Paid -> Release Collateral & Issue NOC -> FINAL STATE
    draw_arrow(63.5, 11.9, 63.5, 8.5)
    draw_action(63.5, 7.0, 19, 3.0, "Issue Digital NOC & Return Collateral", "Zero Debt Clearance Certificate", border_color=c_emerald)
    
    # Bullseye Final State (End Node)
    draw_arrow(63.5, 5.5, 63.5, 3.5)
    final_outer = Circle((63.5, 2.5), 1.5, facecolor="#FFFFFF", edgecolor=c_emerald, linewidth=2, zorder=4)
    final_inner = Circle((63.5, 2.5), 0.9, facecolor=c_emerald, edgecolor=c_emerald, zorder=5)
    ax.add_patch(final_outer)
    ax.add_patch(final_inner)
    ax.text(63.5, 0.4, "SUCCESSFUL COMPLETION", fontsize=9, fontweight='bold', ha='center', va='center', color=c_emerald)
    
    # Path: Delinquency / Default
    draw_arrow(32, 18.0, 14, 18.0, label="[No: Overdue]", label_pos=(0.4, 0.6))
    draw_arrow(14, 18.0, 14, 15)
    draw_action(14, 13.5, 18, 3.0, "3-Day Grace & Penalty", "Friendly Reminder & Mediation Notice", border_color=c_red)
    
    draw_arrow(14, 12.0, 14, 8.5)
    draw_action(14, 7.0, 18, 3.0, "Mediation & Legal Suit", "Order XXXVII CPC Recovery Process", border_color=c_red)
    
    # Flow Final (Terminated with dispute recovery)
    draw_arrow(14, 5.5, 14, 3.5)
    dispute_circle = Circle((14, 2.5), 1.4, facecolor="#FEE2E2", edgecolor=c_red, linewidth=2, zorder=4)
    ax.add_patch(dispute_circle)
    ax.text(14, 2.5, "X", fontsize=14, fontweight='bold', ha='center', va='center', color=c_red, zorder=5)
    ax.text(14, 0.4, "DISPUTE / SETTLED", fontsize=9, fontweight='bold', ha='center', va='center', color=c_red)

    # Save high-resolution PNG
    plt.savefig(output_path, dpi=300, bbox_inches='tight', facecolor=fig.get_facecolor(), edgecolor='none')
    plt.close()
    print(f"Successfully generated GFG Activity Diagram at: {output_path}")

if __name__ == "__main__":
    create_activity_diagram()
