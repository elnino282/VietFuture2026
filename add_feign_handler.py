import os
import re

code_block = """
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException ex, HttpServletRequest request) {
        int upstreamStatus = ex.status();
        log.error("Upstream call failed [{}], status={}, body={}",
                request.getRequestURI(), upstreamStatus, ex.contentUTF8());

        HttpStatus resolved = HttpStatus.resolve(upstreamStatus);
        HttpStatus finalStatus = (resolved == null || resolved.is5xxServerError())
                ? HttpStatus.BAD_GATEWAY   // service con thực sự sập -> 502, không phải lỗi của service này
                : resolved;                // 4xx thật -> giữ nguyên cho FE

        return ResponseEntity.status(finalStatus).body(
            ErrorResponse.builder()
                .status(finalStatus.value())
                .error(finalStatus.getReasonPhrase())
                .message("Dịch vụ phụ thuộc phản hồi lỗi, vui lòng thử lại sau")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build());
    }
}"""

# Services listed by user
target_services = [
    'farm-service', 'season-service', 'identity-service', 'finance-service', 
    'crop-catalog-service', 'inventory-service', 'incident-service', 
    'admin-reporting-service', 'sustainability-service', 'ai-service'
]

def main():
    for root, dirs, files in os.walk('.'):
        for file in files:
            if file == 'GlobalExceptionHandler.java':
                filepath = os.path.join(root, file)
                
                # check if the file belongs to one of the target services
                in_target_service = False
                for srv in target_services:
                    if f'./{srv}/' in filepath:
                        in_target_service = True
                        break
                        
                if not in_target_service:
                    continue
                    
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                    
                if 'handleFeignException' in content:
                    print(f"Skipping {filepath} - already has handleFeignException")
                    continue
                    
                # Add import feign.FeignException;
                if 'import feign.FeignException;' not in content:
                    content = re.sub(
                        r'(import org\.springframework\.)',
                        r'import feign.FeignException;\n\1',
                        content,
                        count=1
                    )
                
                # Replace the last } with the code block
                last_brace_index = content.rfind('}')
                if last_brace_index != -1:
                    content = content[:last_brace_index] + code_block + content[last_brace_index+1:]
                    
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f"Updated {filepath}")

if __name__ == '__main__':
    main()
