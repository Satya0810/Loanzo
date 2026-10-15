import os
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH

def create_diagram_only_docx():
    os.makedirs("docs/diagrams", exist_ok=True)
    doc = Document()

    # Page Margins: 0.6 inches for edge-to-edge high-resolution diagram clarity
    for section in doc.sections:
        section.top_margin = Inches(0.6)
        section.bottomMargin = Inches(0.6)
        section.left_margin = Inches(0.6)
        section.right_margin = Inches(0.6)

    # Document Header Title
    title_p = doc.add_paragraph()
    title_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    title_p.paragraph_format.space_before = Pt(4)
    title_p.paragraph_format.space_after = Pt(2)
    run_title = title_p.add_run("LOANZO PROTOCOL: UML USE CASE DIAGRAMS")
    run_title.font.name = 'Calibri'
    run_title.font.size = Pt(16)
    run_title.font.bold = True
    run_title.font.color.rgb = RGBColor(0x11, 0x18, 0x27)

    sub_p = doc.add_paragraph()
    sub_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    sub_p.paragraph_format.space_before = Pt(0)
    sub_p.paragraph_format.space_after = Pt(10)
    run_sub = sub_p.add_run("OMG UML 2.5 Architecture Models (Color-Coded Line Tracing, Clean Stereotypes & Multi-Party Roles)")
    run_sub.font.name = 'Calibri'
    run_sub.font.size = Pt(9.0)
    run_sub.font.italic = True
    run_sub.font.color.rgb = RGBColor(0x4B, 0x55, 0x63)

    diagrams = [
        ("docs/diagrams/diagram_1_master.png", "Figure 1: Master System Boundary UML Use Case Diagram (Full Architecture with Color-Coded Origin-to-Destination Associations for Borrower, Co-Borrower, Guarantor, Nominee, Lender, Field Agent & Admin)", Inches(7.0)),
        ("docs/diagrams/diagram_2_auth_kyc.png", "Figure 2: Subsystem 1 - Identity & Regulatory KYC Verification (Borrower [Blue], Co-Borrower [Teal], Guarantor [Amber], Lender [Purple])", Inches(6.8)),
        ("docs/diagrams/diagram_3_marketplace.png", "Figure 3: Subsystem 2 - Loan Request, Co-Borrower & Nominee Setup (Borrower [Blue], Co-Borrower [Teal], Lender [Purple])", Inches(6.8)),
        ("docs/diagrams/diagram_4_legal_contracting.png", "Figure 4: Subsystem 3 - Digital Contracts & Guarantee Agreements (Promissory Note & Guarantee Deeds)", Inches(6.8)),
        ("docs/diagrams/diagram_5_repayment.png", "Figure 5: Subsystem 4 - Monthly Repayments & Overdue Escalation (Borrower [Blue], Co-Borrower [Teal], Guarantor [Amber], Lender [Purple])", Inches(6.8)),
        ("docs/diagrams/diagram_6_vault_recovery.png", "Figure 6: Subsystem 5 - Gold Vaulting, Nominee Claim & Legal Recovery (Field Agent [Brown], Borrower [Blue], Nominee [Green], Admin [Red])", Inches(6.8))
    ]

    for i, (img_path, caption, img_width) in enumerate(diagrams):
        if not os.path.exists(img_path):
            print(f"Warning: {img_path} not found")
            continue

        # Add Image
        p_img = doc.add_paragraph()
        p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_img.paragraph_format.space_before = Pt(4)
        p_img.paragraph_format.space_after = Pt(4)
        run_img = p_img.add_run()
        run_img.add_picture(img_path, width=img_width)

        # Add Caption
        p_cap = doc.add_paragraph()
        p_cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_cap.paragraph_format.space_before = Pt(2)
        p_cap.paragraph_format.space_after = Pt(14)
        run_cap = p_cap.add_run(caption)
        run_cap.font.name = 'Calibri'
        run_cap.font.size = Pt(9.5)
        run_cap.font.bold = True
        run_cap.font.color.rgb = RGBColor(0x37, 0x41, 0x51)

        # Add page break between diagrams (except last)
        if i < len(diagrams) - 1:
            doc.add_page_break()

    # Save to Loanzo_UML_Use_Case_Specification_Colored.docx (guaranteed unblocked)
    colored_filename = "docs/diagrams/Loanzo_UML_Use_Case_Specification_Colored.docx"
    doc.save(colored_filename)
    print(f"Successfully generated: {colored_filename}")

    # Also try to save to main filename if not locked by Word
    main_filename = "docs/diagrams/Loanzo_UML_Use_Case_Specification.docx"
    try:
        doc.save(main_filename)
        print(f"Successfully updated main: {main_filename}")
    except PermissionError:
        print(f"Note: {main_filename} is currently open in Microsoft Word. Saved copy to {colored_filename}")

if __name__ == "__main__":
    create_diagram_only_docx()
