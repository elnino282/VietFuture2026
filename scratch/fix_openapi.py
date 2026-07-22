import os
import glob
import re

# 1. Fix OpenApiConfig.java
configs = glob.glob(r"c:\Users\thong\Desktop\VietFuture2026\*-service\src\main\java\org\example\*\config\OpenApiConfig.java")
# Also the template
configs.append(r"c:\Users\thong\Desktop\VietFuture2026\service-template\src\main\java\org\example\template\config\OpenApiConfig.java")

for c in configs:
    if os.path.exists(c):
        with open(c, "r", encoding="utf-8") as f:
            content = f.read()
        
        # Remove `.name(securitySchemeName)`
        new_content = content.replace(".name(securitySchemeName)\n", "")
        new_content = new_content.replace(".name(securitySchemeName)\r\n", "")
        new_content = new_content.replace(".name(securitySchemeName)", "")
        
        if content != new_content:
            with open(c, "w", encoding="utf-8") as f:
                f.write(new_content)

# 2. Fix docker-compose.yml ai-service port
compose_file = r"c:\Users\thong\Desktop\VietFuture2026\docker-compose.yml"
with open(compose_file, "r", encoding="utf-8") as f:
    compose = f.read()

# Find ai-service and add ports:
if "8083:8083" not in compose:
    ai_service_block = """  ai-service:
    build:
      context: ./ai-service
      dockerfile: Dockerfile
    container_name: ai_service_quanlymuavu
    restart: unless-stopped
    ports:
      - "8083:8083"
    environment:"""
    
    compose = re.sub(
        r"  ai-service:\s+build:\s+context: ./ai-service\s+dockerfile: Dockerfile\s+container_name: ai_service_quanlymuavu\s+restart: unless-stopped\s+environment:",
        ai_service_block,
        compose
    )
    with open(compose_file, "w", encoding="utf-8") as f:
        f.write(compose)

print("Fixed OpenAPI configs and docker-compose.yml")
