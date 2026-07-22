import os
import glob

yaml_files = glob.glob(r"c:\Users\thong\Desktop\VietFuture2026\*-service\src\main\resources\application.yml")

bad_block = "\nspring:\n  cloud:\n    openfeign:\n      circuitbreaker:\n        enabled: true\n"
bad_block_2 = "\nspring:\n  cloud:\n    openfeign:\n      circuitbreaker:\n        enabled: true"

for yml in yaml_files:
    if os.path.exists(yml):
        with open(yml, "r", encoding="utf-8") as f:
            content = f.read()
        
        if content.endswith(bad_block):
            content = content[:-len(bad_block)]
        elif content.endswith(bad_block_2):
            content = content[:-len(bad_block_2)]
            
        with open(yml, "w", encoding="utf-8") as f:
            f.write(content)

print("Fixed duplicate spring block in application.yml")
