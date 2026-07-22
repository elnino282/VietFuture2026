import os
import glob
import re

services = {
    'admin-reporting-service': 15,
    'ai-service': 0,
    'crop-catalog-service': 15,
    'delivery-service': 10,
    'farm-service': 20,
    'finance-service': 10,
    'identity-service': 30,
    'incident-service': 10,
    'inventory-service': 15,
    'marketplace-service': 30,
    'season-service': 20,
    'sustainability-service': 10
}

for service, pool_size in services.items():
    yaml_path = os.path.join(service, 'src', 'main', 'resources', 'application.yml')
    if not os.path.exists(yaml_path):
        continue
    
    with open(yaml_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Add compression to server block
    if 'compression:' not in content:
        server_compression = """  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/plain
    min-response-size: 1024
"""
        # Find server: block and append
        content = re.sub(r'(server:\s*\n(?:\s+port:.*\n)?)', r'\1' + server_compression, content, count=1)

    # Add HikariCP to spring:datasource block if pool_size > 0
    if pool_size > 0 and 'hikari:' not in content and 'datasource:' in content:
        hikari_config = f"""    hikari:
      maximum-pool-size: ${{DB_POOL_MAX:{pool_size}}}
      minimum-idle: ${{DB_POOL_MIN_IDLE:{max(5, pool_size // 3)}}}
      connection-timeout: 30000
      idle-timeout: 600000
"""
        content = re.sub(r'(datasource:\s*\n(?:\s+url:.*\n)?(?:\s+username:.*\n)?(?:\s+password:.*\n)?(?:\s+driver-class-name:.*\n)?)', r'\1' + hikari_config, content, count=1)
        
        # fallback if datasource doesn't have the typical structure, just insert under spring: if no datasource:
        if 'hikari:' not in content and 'datasource:' not in content:
            # this shouldn't happen for these services but just in case
            pass

    with open(yaml_path, 'w', encoding='utf-8') as f:
        f.write(content)
        
    print(f"Updated {yaml_path}")
