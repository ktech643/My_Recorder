#!/usr/bin/env python3

def analyze_braces(filename, start_line, end_line):
    with open(filename, 'r') as f:
        lines = f.readlines()
    
    brace_count = 0
    try_count = 0
    
    for i in range(start_line - 1, min(end_line, len(lines))):
        line = lines[i]
        line_num = i + 1
        
        # Count braces
        open_braces = line.count('{')
        close_braces = line.count('}')
        brace_count += open_braces - close_braces
        
        # Check for try blocks
        if 'try {' in line.strip():
            try_count += 1
            print(f"Line {line_num}: Found 'try {{' - try_count={try_count}, brace_count={brace_count}")
        
        if 'catch' in line or 'finally' in line:
            print(f"Line {line_num}: Found catch/finally - try_count={try_count}, brace_count={brace_count}")
            if 'catch' in line:
                try_count -= 1
        
        if brace_count < 0:
            print(f"Line {line_num}: ERROR - More closing braces than opening!")
        
        # Show important lines
        if any(keyword in line for keyword in ['try', 'catch', 'finally', 'drawFrame', ');']):
            print(f"Line {line_num} (braces={brace_count}): {line.strip()}")

if __name__ == "__main__":
    analyze_braces("/workspace/app/src/main/java/com/checkmate/android/service/SharedEGL/SharedEglManager.java", 956, 1095)