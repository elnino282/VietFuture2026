import os
import shutil
import glob
import re

base_dir = r"c:\Users\thong\Desktop\VietFuture2026\agricultural-crop-management-frontend"

# Mappings of old paths to new paths (relative to base_dir)
file_moves = {
    "src/api/auth.ts": "src/features/auth/api/auth.ts",
    "src/api/auth.test.ts": "src/features/auth/api/auth.test.ts",
    "src/api/axios-client.ts": "src/shared/api/axios-client.ts",
    "src/api/catalogApi.ts": "src/entities/crop/api/catalogApi.ts",
    "src/api/certificationApi.ts": "src/entities/farm/api/certificationApi.ts",
    "src/api/farmsApi.ts": "src/entities/farm/api/farmsApi.ts",
    "src/api/seasonsApi.ts": "src/entities/season/api/seasonsApi.ts",
    "src/api/tasksApi.ts": "src/entities/task/api/tasksApi.ts",
    "src/services/aiChatService.ts": "src/entities/ai/api/aiChatService.ts",
    "src/services/aiChatService.test.ts": "src/entities/ai/api/aiChatService.test.ts",
    "src/services/api.admin.tsx": "src/entities/user/api/api.admin.tsx",
    "src/services/api.buyer.tsx": "src/entities/user/api/api.buyer.tsx",
    "src/services/api.farmer.ts": "src/entities/user/api/api.farmer.ts",
    "src/services/api.admin.pending-approvals.schema.test.ts": "src/entities/user/api/api.admin.pending-approvals.schema.test.ts",
}

# Add generated files mapping
generated_season = "src/api/generated/season-service.ts"
generated_season_dest = "src/entities/season/api/generated/season-service.ts"
if os.path.exists(os.path.join(base_dir, generated_season)):
    file_moves[generated_season] = generated_season_dest

# Create destination dirs and move files
for src, dst in file_moves.items():
    src_path = os.path.join(base_dir, src)
    dst_path = os.path.join(base_dir, dst)
    if os.path.exists(src_path):
        os.makedirs(os.path.dirname(dst_path), exist_ok=True)
        shutil.move(src_path, dst_path)

# Move generated models for season
models_src = os.path.join(base_dir, "src/api/generated/model/season")
models_dst = os.path.join(base_dir, "src/entities/season/api/generated/model/season")
if os.path.exists(models_src):
    os.makedirs(os.path.dirname(models_dst), exist_ok=True)
    shutil.move(models_src, models_dst)

# Update imports
import_replacements = {
    "@/api/auth": "@/features/auth/api/auth",
    "@/api/axios-client": "@/shared/api/axios-client",
    "@/api/catalogApi": "@/entities/crop/api/catalogApi",
    "@/api/certificationApi": "@/entities/farm/api/certificationApi",
    "@/api/farmsApi": "@/entities/farm/api/farmsApi",
    "@/api/seasonsApi": "@/entities/season/api/seasonsApi",
    "@/api/tasksApi": "@/entities/task/api/tasksApi",
    "@/services/aiChatService": "@/entities/ai/api/aiChatService",
    "@/services/api.admin": "@/entities/user/api/api.admin",
    "@/services/api.buyer": "@/entities/user/api/api.buyer",
    "@/services/api.farmer": "@/entities/user/api/api.farmer",
    "@/api/generated/season-service": "@/entities/season/api/generated/season-service",
    "@/api/generated/model/season": "@/entities/season/api/generated/model/season",
}

# Regex to handle replacing import paths
# E.g. from "@/api/auth" to from "@/features/auth/api/auth"
def replace_imports(content):
    for old, new in import_replacements.items():
        # Handle exact quotes
        content = content.replace(f'"{old}"', f'"{new}"')
        content = content.replace(f"'{old}'", f"'{new}'")
        # Handle nested imports from model e.g. "@/api/generated/model/season/seasonDto"
        content = content.replace(f'"{old}/', f'"{new}/')
        content = content.replace(f"'{old}/", f"'{new}/")
    
    # Orval sometimes generates relative imports for mutator, we updated orval.config.js to output to shared/api
    # so we also need to fix any relative imports pointing to old mutator, but Orval will regenerate anyway.
    
    return content

src_dir = os.path.join(base_dir, "src")
for root, dirs, files in os.walk(src_dir):
    for file in files:
        if file.endswith((".ts", ".tsx", ".js", ".jsx")):
            file_path = os.path.join(root, file)
            with open(file_path, "r", encoding="utf-8") as f:
                content = f.read()
            new_content = replace_imports(content)
            if content != new_content:
                with open(file_path, "w", encoding="utf-8") as f:
                    f.write(new_content)

print("Migration complete.")
