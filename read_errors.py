with open('app/src/main/java/com/loanzo/app/ui/loan/LoanScreens.kt', 'rb') as f:
    lines = f.readlines()
for i in [432, 464, 527, 594, 596, 1038]:
    start = max(0, i - 4)
    end = min(len(lines), i + 3)
    print(f'--- Around line {i} ---')
    for j in range(start, end):
        try:
            print(f'{j+1}: {lines[j].decode("utf-8", errors="replace").rstrip()}')
        except Exception as e:
            print(f'{j+1}: {lines[j]} (decode error: {e})')
