import os
import re

file_path = r"c:\Users\thong\Desktop\VietFuture2026\agricultural-crop-management-frontend\orval.config.js"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

new_content = """export default {
  ...[
    { name: 'identity', port: 8081, entity: 'user' },
    { name: 'crop-catalog', port: 8082, entity: 'crop' },
    { name: 'ai', port: 8083, entity: 'ai' },
    { name: 'farm', port: 8084, entity: 'farm' },
    { name: 'season', port: 8085, entity: 'season' },
    { name: 'inventory', port: 8086, entity: 'inventory' },
    { name: 'finance', port: 8087, entity: 'finance' },
    { name: 'incident', port: 8088, entity: 'incident' },
    { name: 'sustainability', port: 8089, entity: 'sustainability' },
    { name: 'marketplace', port: 8090, entity: 'marketplace' },
    { name: 'admin-reporting', port: 8091, entity: 'report' },
  ].reduce((acc, service) => {
    acc[`${service.name}Service`] = {
      input: {
        target: `http://localhost:${service.port}/v3/api-docs`,
      },
      output: {
        mode: 'split',
        target: `src/entities/${service.entity}/api/generated/${service.name}-service.ts`,
        schemas: `src/entities/${service.entity}/api/generated/model`,
        client: 'react-query',
        prettier: true,
        override: {
          mutator: {
            path: 'src/shared/api/axios-client.ts',
            name: 'customInstance',
          },
        },
      },
    };
    return acc;
  }, {})
};
"""

with open(file_path, "w", encoding="utf-8") as f:
    f.write(new_content)

print("Updated orval.config.js")
