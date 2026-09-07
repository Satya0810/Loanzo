import sys

with open('app/src/main/java/com/loanzo/app/ui/loan/LoanScreens.kt', 'rb') as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    # Detect start of corruption
    if b'if (repayment.paidDate != null)' in line:
        new_lines.append(line)
        new_lines.append(b'                                        else "Due ${repayment.dueDate.toDateString()}",\n')
        new_lines.append(b'                                        style = MaterialTheme.typography.bodySmall,\n')
        new_lines.append(b'                                        color = MaterialTheme.colorScheme.onSurfaceVariant\n')
        new_lines.append(b'                                    )\n')
        new_lines.append(b'                                }\n')
        new_lines.append(b'                            }\n')
        new_lines.append(b'                            StatusBadge(repayment.status, color = when (repayment.status) {\n')
        new_lines.append(b'                                "PAID" -> Emerald400; "OVERDUE" -> Red400; else -> Gold500\n')
        new_lines.append(b'                            })\n')
        new_lines.append(b'                        }\n')
        new_lines.append(b'                    }\n')
        new_lines.append(b'                }\n')
        new_lines.append(b'            }\n')
        new_lines.append(b'\n')
        new_lines.append(b'            // \xe2\x94\x80\xe2\x94\x80\xe2\x94\x80 7. COLLATERAL & PLEDGES OVERVIEW \xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\xe2\x94\x80\n')
        new_lines.append(b'            item {\n')
        new_lines.append(b'                SectionHeader(title = "\xf0\x9f\x9b\xa1\xef\xb8\x8f Collateral & Pledges")\n')
        new_lines.append(b'            }\n')
        new_lines.append(b'\n')
        
        skip = True
        continue
    
    if skip and b'if (state.pledges.isEmpty()) {' in line and b'item' not in line:
        skip = False
        new_lines.append(line)
        continue
        
    if not skip:
        new_lines.append(line)

with open('app/src/main/java/com/loanzo/app/ui/loan/LoanScreens.kt', 'wb') as f:
    f.writelines(new_lines)
print('Fixed file.')
