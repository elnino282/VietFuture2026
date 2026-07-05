import os
import re

base_dir = '/home/thong/Projects/VietFuture2026/marketplace-service/src/main/java/org/example/marketplace'

# 1. Delete old fallback files
files_to_delete = [
    f'{base_dir}/client/impl/FarmClientFallback.java',
    f'{base_dir}/client/fallback/IdentityClientFallback.java',
    f'{base_dir}/client/fallback/SeasonClientFallback.java',
    f'{base_dir}/client/fallback/InventoryFeignClientFallback.java',
]
for f in files_to_delete:
    if os.path.exists(f):
        os.remove(f)

# 2. Create FallbackFactory files
farm_factory = """package org.example.marketplace.client.impl;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.example.marketplace.client.FarmClient;
import org.example.marketplace.dto.client.FarmDetailDto;
import org.example.marketplace.dto.client.FarmSummaryDto;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class FarmClientFallbackFactory implements FallbackFactory<FarmClient> {

    @Override
    public FarmClient create(Throwable cause) {
        return new FarmClient() {
            @Override
            public List<FarmSummaryDto> getFarmsByIds(List<Integer> farmIds) {
                log.error("Fallback getFarmsByIds farmIds={} cause={}", farmIds, cause.toString());
                return List.of();
            }

            @Override
            public FarmDetailDto getFarmDetail(Integer farmId) {
                if (cause instanceof FeignException.NotFound) {
                    log.warn("Farm {} không tồn tại trên farm-service (404 thật)", farmId);
                    return null;
                }
                log.error("farm-service lỗi khi lấy farmId={}: {}", farmId, cause.toString());
                throw new IllegalStateException("Dịch vụ farm-service đang gặp sự cố, vui lòng thử lại sau");
            }

            @Override
            public List<Integer> getFarmIdsByUserId(Long userId) {
                log.error("Fallback getFarmIdsByUserId userId={} cause={}", userId, cause.toString());
                return List.of();
            }
        };
    }
}
"""
with open(f'{base_dir}/client/impl/FarmClientFallbackFactory.java', 'w') as f:
    f.write(farm_factory)

identity_factory = """package org.example.marketplace.client.fallback;

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
"""
with open(f'{base_dir}/client/fallback/IdentityClientFallbackFactory.java', 'w') as f:
    f.write(identity_factory)


season_factory = """package org.example.marketplace.client.fallback;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.example.marketplace.client.SeasonClient;
import org.example.marketplace.dto.client.SeasonDetailDto;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class SeasonClientFallbackFactory implements FallbackFactory<SeasonClient> {

    @Override
    public SeasonClient create(Throwable cause) {
        return new SeasonClient() {
            @Override
            public List<SeasonDetailDto> getSeasonsByIds(List<Integer> seasonIds) {
                log.error("season-service lỗi khi lấy seasonsByIds: {}", cause.toString());
                return Collections.emptyList();
            }

            @Override
            public SeasonDetailDto getSeasonDetail(Integer seasonId) {
                if (cause instanceof FeignException.NotFound) {
                    return null;
                }
                log.error("season-service lỗi khi lấy seasonId={}: {}", seasonId, cause.toString());
                throw new IllegalStateException("Dịch vụ season-service đang gặp sự cố, vui lòng thử lại sau");
            }

            @Override
            public List<Integer> getSeasonIdsByOwnerId(Long ownerId) {
                log.error("season-service lỗi khi lấy season IDs cho ownerId={}: {}", ownerId, cause.toString());
                return Collections.emptyList();
            }
        };
    }
}
"""
with open(f'{base_dir}/client/fallback/SeasonClientFallbackFactory.java', 'w') as f:
    f.write(season_factory)


inventory_factory = """package org.example.marketplace.client.fallback;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.example.marketplace.client.InventoryFeignClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class InventoryFeignClientFallbackFactory implements FallbackFactory<InventoryFeignClient> {

    @Override
    public InventoryFeignClient create(Throwable cause) {
        return new InventoryFeignClient() {
            @Override
            public Map<String, Object> getLotDetail(Integer lotId, String token) {
                if (cause instanceof FeignException.NotFound) {
                    return null;
                }
                log.error("inventory-service lỗi khi lấy lotDetail lotId={}: {}", lotId, cause.toString());
                throw new IllegalStateException("Dịch vụ inventory-service đang gặp sự cố, vui lòng thử lại sau");
            }

            @Override
            public Map<String, Object> reserveStock(String idempotencyKey, String token, Map<String, Object> requestBody) {
                log.error("inventory-service lỗi khi reserveStock: {}", cause.toString());
                throw new IllegalStateException("Dịch vụ inventory-service đang gặp sự cố, vui lòng thử lại sau");
            }

            @Override
            public Map<String, Object> releaseReservation(String token, Map<String, Object> requestBody) {
                log.error("inventory-service lỗi khi releaseReservation: {}", cause.toString());
                throw new IllegalStateException("Dịch vụ inventory-service đang gặp sự cố, vui lòng thử lại sau");
            }

            @Override
            public Map<String, Object> confirmStockOut(String token, Map<String, Object> requestBody) {
                log.error("inventory-service lỗi khi confirmStockOut: {}", cause.toString());
                throw new IllegalStateException("Dịch vụ inventory-service đang gặp sự cố, vui lòng thử lại sau");
            }

            @Override
            public List<Map<String, Object>> getAvailableStock(List<Integer> lotIds, String token) {
                log.error("inventory-service lỗi khi getAvailableStock: {}", cause.toString());
                return Collections.emptyList();
            }

            @Override
            public List<Map<String, Object>> getLotsBySeasonIds(List<Integer> seasonIds, String token) {
                log.error("inventory-service lỗi khi getLotsBySeasonIds: {}", cause.toString());
                return Collections.emptyList();
            }
        };
    }
}
"""
with open(f'{base_dir}/client/fallback/InventoryFeignClientFallbackFactory.java', 'w') as f:
    f.write(inventory_factory)


# 3. Update Feign Client annotations
clients = [
    (f'{base_dir}/client/FarmClient.java', 'FarmClientFallback', 'org.example.marketplace.client.impl.FarmClientFallbackFactory'),
    (f'{base_dir}/client/IdentityClient.java', 'IdentityClientFallback', 'org.example.marketplace.client.fallback.IdentityClientFallbackFactory'),
    (f'{base_dir}/client/SeasonClient.java', 'SeasonClientFallback', 'org.example.marketplace.client.fallback.SeasonClientFallbackFactory'),
    (f'{base_dir}/client/InventoryFeignClient.java', 'InventoryFeignClientFallback', 'org.example.marketplace.client.fallback.InventoryFeignClientFallbackFactory'),
]

for file_path, old_class, new_class in clients:
    with open(file_path, 'r') as f:
        content = f.read()
    
    # Replace fallback = X.class with fallbackFactory = Y.class
    content = re.sub(r'fallback\s*=\s*(.*?)\.class', f'fallbackFactory = {new_class}.class', content)
    
    with open(file_path, 'w') as f:
        f.write(content)

# 4. Add @JsonIgnore to entities to prevent infinite recursion
entities = [
    'MarketplaceCart.java',
    'MarketplaceCartItem.java',
    'MarketplaceOrder.java',
    'MarketplaceOrderItem.java'
]

def add_json_ignore(file_path, pattern_field):
    with open(file_path, 'r') as f:
        content = f.read()
    
    # Add import if needed
    if 'import com.fasterxml.jackson.annotation.JsonIgnore;' not in content:
        content = re.sub(r'(package .*?;)', r'\1\n\nimport com.fasterxml.jackson.annotation.JsonIgnore;', content, count=1)
    
    # Find the field and add @JsonIgnore above it
    # We look for something like:
    # @OneToMany(...)
    # private List<MarketplaceCartItem> items;
    # Or
    # @ManyToOne
    # private MarketplaceCart cart;
    
    lines = content.split('\n')
    for i, line in enumerate(lines):
        if pattern_field in line and not '@JsonIgnore' in lines[i-1]:
            # we need to insert @JsonIgnore before this field. Usually there are annotations above it.
            # let's just insert it right above the field definition.
            lines.insert(i, '    @JsonIgnore')
            break
            
    with open(file_path, 'w') as f:
        f.write('\n'.join(lines))

# For Cart and Order, we ignore the List of items.
# For CartItem and OrderItem, we ignore the parent back-reference.
# Actually, ignoring the back-reference (CartItem -> Cart) is the standard way to prevent recursion.
# I will add it to the back-reference fields.

add_json_ignore(f'{base_dir}/entity/MarketplaceCartItem.java', 'MarketplaceCart cart;')
add_json_ignore(f'{base_dir}/entity/MarketplaceOrderItem.java', 'MarketplaceOrder order;')
# It's also safe to put it on the collection side or both, but usually back-reference is enough.
add_json_ignore(f'{base_dir}/entity/MarketplaceCart.java', 'List<MarketplaceCartItem> items;')
add_json_ignore(f'{base_dir}/entity/MarketplaceOrder.java', 'List<MarketplaceOrderItem> items;')
add_json_ignore(f'{base_dir}/entity/MarketplaceOrderGroup.java', 'List<MarketplaceOrder> orders;')

print("Done")
