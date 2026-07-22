import os

# 1. Remove unused RestTemplate imports
paths_to_clean = [
    r"c:\Users\thong\Desktop\VietFuture2026\season-service\src\main\java\org\example\season\config\AppConfig.java",
    r"c:\Users\thong\Desktop\VietFuture2026\incident-service\src\main\java\org\example\incident\service\ExternalServiceClient.java",
    r"c:\Users\thong\Desktop\VietFuture2026\farm-service\src\main\java\org\example\farm\config\AppConfig.java",
    r"c:\Users\thong\Desktop\VietFuture2026\crop-catalog-service\src\main\java\org\example\cropcatalog\config\AppConfig.java"
]

for p in paths_to_clean:
    if os.path.exists(p):
        with open(p, "r", encoding="utf-8") as f:
            lines = f.readlines()
        with open(p, "w", encoding="utf-8") as f:
            for line in lines:
                if "import org.springframework.web.client.RestTemplate;" not in line:
                    f.write(line)

# 2. Add circuit breaker enabled to all application.yml
import glob

yaml_files = glob.glob(r"c:\Users\thong\Desktop\VietFuture2026\*-service\src\main\resources\application.yml")
for yml in yaml_files:
    with open(yml, "r", encoding="utf-8") as f:
        content = f.read()
    
    if "circuitbreaker.enabled" not in content and "openfeign:" not in content:
        content += "\nspring:\n  cloud:\n    openfeign:\n      circuitbreaker:\n        enabled: true\n"
        with open(yml, "w", encoding="utf-8") as f:
            f.write(content)
    elif "circuitbreaker.enabled" not in content:
        # Just append it, assuming we can just add a new block at the end
        content += "\nspring:\n  cloud:\n    openfeign:\n      circuitbreaker:\n        enabled: true\n"
        with open(yml, "w", encoding="utf-8") as f:
            f.write(content)

print("RestTemplate imports cleaned up and circuitbreaker enabled in yml files")
