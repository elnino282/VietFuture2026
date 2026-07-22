import os
import glob
import sys

def find_files(root_dir, extensions):
    matched_files = []
    for ext in extensions:
        pattern = os.path.join(root_dir, '**', f'*.{ext}')
        matched_files.extend(glob.glob(pattern, recursive=True))
    return matched_files

def main():
    root_dir = '.'
    extensions = ['java', 'sql', 'yml', 'yaml', 'properties', 'xml', 'ts', 'tsx', 'md']
    all_files = find_files(root_dir, extensions)
    
    non_utf8_files = []
    fixed_files = []
    needs_review = []
    
    for filepath in all_files:
        if 'node_modules' in filepath.replace('\\', '/') or '/target/' in filepath.replace('\\', '/') or '/.git/' in filepath.replace('\\', '/'):
            continue
            
        with open(filepath, 'rb') as f:
            raw_bytes = f.read()
            
        try:
            raw_bytes.decode('utf-8', errors='strict')
            continue
        except UnicodeDecodeError:
            non_utf8_files.append(filepath)
            
            decoded_text = None
            used_encoding = None
            for enc in ['windows-1258', 'windows-1252', 'cp1258', 'cp1252']:
                try:
                    decoded_text = raw_bytes.decode(enc, errors='strict')
                    used_encoding = enc
                    break
                except UnicodeDecodeError:
                    continue
                    
            if decoded_text is not None:
                lines = decoded_text.splitlines(keepends=True)
                suspicious_lines = []
                for i, line in enumerate(lines):
                    if '?' in line:
                        if '"' in line and '?' in line:
                            suspicious_lines.append((i+1, line.strip()))
                        elif any(ord(c) > 127 for c in line) and '?' in line:
                            suspicious_lines.append((i+1, line.strip()))
                            
                if suspicious_lines:
                    needs_review.append((filepath, suspicious_lines))
                
                with open(filepath, 'wb') as out_f:
                    out_f.write(decoded_text.encode('utf-8'))
                    
                fixed_files.append((filepath, used_encoding))
            else:
                needs_review.append((filepath, [(-1, "Could not decode with any fallback encoding")]))

    with open('encoding_report.txt', 'w', encoding='utf-8') as f:
        f.write(f"Total non-UTF-8 files found: {len(non_utf8_files)}\n")
        f.write(f"Fixed files:\n")
        for filepath, enc in fixed_files:
            f.write(f" - {filepath} ({enc})\n")
            
        f.write(f"Needs review:\n")
        for filepath, lines in needs_review:
            f.write(f" - {filepath}\n")
            for lnum, ltext in lines:
                f.write(f"   Line {lnum}: {ltext}\n")

if __name__ == '__main__':
    main()
