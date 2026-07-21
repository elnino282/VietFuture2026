import os
import glob

dirs_to_check = [
    'farm-service/src/**/*.java',
    'season-service/src/**/*.java',
    'marketplace-service/src/**/*.java',
    'delivery-service/src/**/*.java',
    'crop-catalog-service/src/**/*.java'
]

files_changed = 0
for pattern in dirs_to_check:
    for filepath in glob.glob(pattern, recursive=True):
        encoding_used = 'utf-8'
        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
        except UnicodeDecodeError:
            with open(filepath, 'r', encoding='cp1252') as f:
                content = f.read()
            encoding_used = 'cp1252'
            
        if '\\"' in content:
            new_content = content.replace('\\"', '"')
            with open(filepath, 'w', encoding=encoding_used) as f:
                f.write(new_content)
            files_changed += 1
            print(f"Fixed: {filepath} ({encoding_used})")

print(f"Total files fixed this run: {files_changed}")
