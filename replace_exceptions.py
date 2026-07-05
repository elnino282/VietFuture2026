import re

file_path = '/home/thong/Projects/VietFuture2026/marketplace-service/src/main/java/org/example/marketplace/service/MarketplaceServiceImpl.java'

with open(file_path, 'r') as f:
    content = f.read()

def replace_exception(match):
    msg = match.group(1)
    if "not found" in msg.lower() or "not exist" in msg.lower():
        ex = "ResourceNotFoundException"
    elif "empty" in msg.lower() or "required" in msg.lower() or "can only" in msg.lower():
        ex = "BadRequestException"
    elif "forbidden" in msg.lower():
        ex = "ForbiddenException"
    elif "already" in msg.lower() or "failed to reserve" in msg.lower():
        ex = "ConflictException"
    else:
        # Fallback to BadRequestException for any other validation errors
        ex = "BadRequestException"
    return f'new {ex}("{msg}")'

# For exact string matches like "Failed to reserve stock: " + reservationFailureMessage
# We need to handle this specially because it's not a simple literal string
content = re.sub(r'new RuntimeException\("Failed to reserve stock: " \+ reservationFailureMessage\)',
                 r'new ConflictException("Failed to reserve stock: " + reservationFailureMessage)', content)

# For literal strings
content = re.sub(r'new RuntimeException\("([^"]+)"\)', replace_exception, content)

imports = """import org.example.marketplace.exception.ResourceNotFoundException;
import org.example.marketplace.exception.BadRequestException;
import org.example.marketplace.exception.ForbiddenException;
import org.example.marketplace.exception.ConflictException;
"""

if "import org.example.marketplace.exception.ResourceNotFoundException;" not in content:
    content = re.sub(r'(import .*?;)', r'\1\n' + imports, content, count=1)

with open(file_path, 'w') as f:
    f.write(content)

print("Replacements done.")
