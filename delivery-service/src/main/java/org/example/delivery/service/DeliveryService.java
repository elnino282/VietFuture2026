package org.example.delivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.delivery.dto.request.CalculateShippingRequest;
import org.example.delivery.dto.request.CreateDeliveryOrderRequest;
import org.example.delivery.dto.response.ShippingOption;
import org.example.delivery.entity.DeliveryOrder;
import org.example.delivery.entity.enums.DeliveryStatus;
import org.example.delivery.repository.DeliveryOrderRepository;
import org.example.delivery.repository.DeliveryProviderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {

    private final ShippingFeeCalculator shippingFeeCalculator;
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final DeliveryProviderRepository deliveryProviderRepository;

    public List<ShippingOption> calculateShippingOptions(CalculateShippingRequest request) {
        return shippingFeeCalculator.calculateOptions(request);
    }

    @Transactional
    public DeliveryOrder createDeliveryOrder(CreateDeliveryOrderRequest request) {
        log.info("Creating delivery order for marketplace order: {}", request.marketplaceOrderId());

        // Validate provider
        deliveryProviderRepository.findById(request.providerId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid delivery provider ID: " + request.providerId()));

        // Generate a random mock tracking number
        String trackingNumber = "VTF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        DeliveryOrder order = DeliveryOrder.builder()
                .marketplaceOrderId(request.marketplaceOrderId())
                .providerId(request.providerId())
                .trackingNumber(trackingNumber)
                .status(DeliveryStatus.PENDING)
                .shippingFeeVnd(request.shippingFeeVnd())
                .isPerishable(request.isPerishable())
                .requiresColdChain(request.requiresColdChain())
                .recipientName(request.recipientName())
                .recipientPhone(request.recipientPhone())
                .recipientAddress(request.recipientAddress())
                .recipientProvince(request.recipientProvince())
                .weightKg(request.weightKg())
                .estimatedDelivery(LocalDateTime.now().plusDays(2)) // Default 2 days estimate
                .requestedDeliveryDate(request.requestedDeliveryDate())
                .deliveryZoneTo(request.deliveryZoneTo())
                .build();

        return deliveryOrderRepository.save(order);
    }

    public org.example.delivery.dto.response.BatchSuggestionResponse getBatchSuggestions(java.time.LocalDate date, String zone) {
        long count = deliveryOrderRepository.countByRequestedDeliveryDateAndDeliveryZoneTo(date, zone);
        boolean batchEligible = count >= 5;
        return new org.example.delivery.dto.response.BatchSuggestionResponse(batchEligible, date, zone, count, 5L);
    }

    public List<DeliveryOrder> getAllDeliveryOrders() {
        return deliveryOrderRepository.findAll();
    }

    public DeliveryOrder getDeliveryOrder(Integer id) {
        return deliveryOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Delivery order not found with ID: " + id));
    }

    public List<DeliveryOrder> getDeliveryOrdersByMarketplaceOrder(Long marketplaceOrderId) {
        return deliveryOrderRepository.findByMarketplaceOrderId(marketplaceOrderId);
    }

    @Transactional
    public DeliveryOrder updateDeliveryStatus(Integer id, DeliveryStatus status) {
        log.info("Updating delivery order {} status to {}", id, status);
        DeliveryOrder order = getDeliveryOrder(id);
        order.setStatus(status);

        if (status == DeliveryStatus.DELIVERED) {
            order.setActualDelivery(LocalDateTime.now());
        }

        return deliveryOrderRepository.save(order);
    }
}
