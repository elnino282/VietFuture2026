package org.example.marketplace.client.fallback;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.example.marketplace.client.IdentityClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IdentityClientFallbackFactory implements FallbackFactory<IdentityClient> {

    @Override
    public IdentityClient create(Throwable cause) {
        return new IdentityClient() {
            @Override
            public String getUserDisplayName(Long userId) {
                log.error("identity-service lỗi khi lấy display name cho userId={}: {}", userId, cause.toString());
                if (cause instanceof FeignException.NotFound) {
                    return null;
                }
                throw new IllegalStateException("Dịch vụ identity-service đang gặp sự cố, vui lòng thử lại sau");
            }
        };
    }
}
