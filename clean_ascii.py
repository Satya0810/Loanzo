import sys
import re

with open('app/src/main/java/com/loanzo/app/ui/loan/LoanScreens.kt', 'rb') as f:
    content = f.read()

# decode ignoring errors
text = content.decode('utf-8', errors='ignore')

# remove all non-ascii characters (this will strip emojis and box drawing characters)
text = re.sub(r'[^\x00-\x7F]+', '', text)

with open('app/src/main/java/com/loanzo/app/ui/loan/LoanScreens.kt', 'w', encoding='utf-8') as f:
    f.write(text)

print('Cleaned LoanScreens.kt of all non-ASCII characters')
