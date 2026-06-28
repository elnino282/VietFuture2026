package org.example.marketplace;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.marketplace.config.TestSecurityConfig;
import org.example.marketplace.entity.MarketplaceOrder;
import org.example.marketplace.entity.OutboxEvent;
import org.example.marketplace.model.MarketplaceOrderStatus;
import org.example.marketplace.model.MarketplacePaymentMethod;
import org.example.marketplace.model.MarketplacePaymentVerificationStatus;
import org.example.marketplace.repository.MarketplaceOrderRepository;
import org.example.marketplace.repository.OutboxEventRepository;
import org.example.marketplace.service.MarketplaceOutboxPublisher;
import org.example.marketplace.shared.security.CurrentUserService;
import org.example.marketplace.client.FarmClient;
import org.example.marketplace.client.SeasonClient;
import org.example.marketplace.client.InventoryClient;
import org.example.marketplace.client.IdentityClient;
import org.example.marketplace.service.MarketplaceStorageService;
import org.example.marketplace.dto.request.MarketplaceUpdateOrderStatusRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, MarketplaceOrderIntegrationTest.TestConfig.class})
public class MarketplaceOrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MarketplaceOrderRepository marketplaceOrderRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private MarketplaceOutboxPublisher marketplaceOutboxPublisher;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private FarmClient farmClient;

    @MockitoBean
    private SeasonClient seasonClient;

    @MockitoBean
    private InventoryClient inventoryClient;

    @MockitoBean
    private IdentityClient identityClient;

    @MockitoBean
    private MarketplaceStorageService storageService;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public MarketplaceOutboxPublisher marketplaceOutboxPublisher(
                OutboxEventRepository outboxEventRepository,
                RabbitTemplate rabbitTemplate) {
            return new MarketplaceOutboxPublisher(outboxEventRepository, rabbitTemplate);
        }
    }

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        marketplaceOrderRepository.deleteAll();

        // Standard stubbing for inventory releases and confirmations
        when(inventoryClient.releaseReservation(any(), any()))
                .thenReturn(new InventoryClient.ReservationResult(true, "Mock release success", List.of()));
        when(inventoryClient.confirmStockOut(any(), any()))
                .thenReturn(new InventoryClient.ReservationResult(true, "Mock stock-out success", List.of()));
    }

    @Test
    void testBuyerCancelOrderAndVerifyOutboxPublication() throws Exception {
        // 1. Arrange: Create an order that belongs to buyer user 100
        Long buyerUserId = 100L;
        Long farmerUserId = 200L;

        MarketplaceOrder order = MarketplaceOrder.builder()
                .orderGroupId(10L)
                .orderCode("ORD-CANCEL-123")
                .buyerUserId(buyerUserId)
                .farmerUserId(farmerUserId)
                .status(MarketplaceOrderStatus.PENDING_PAYMENT)
                .paymentMethod(MarketplacePaymentMethod.BANK_TRANSFER)
                .paymentVerificationStatus(MarketplacePaymentVerificationStatus.AWAITING_PROOF)
                .shippingRecipientName("John Doe")
                .shippingPhone("123456789")
                .shippingAddressLine("123 Farm Road")
                .subtotal(new BigDecimal("100.00"))
                .shippingFee(new BigDecimal("10.00"))
                .totalAmount(new BigDecimal("110.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        order = marketplaceOrderRepository.save(order);
        Long orderId = order.getId();

        // Mock current user as the buyer
        when(currentUserService.getCurrentUserId()).thenReturn(buyerUserId);

        // 2. Act: Buyer requests cancellation
        mockMvc.perform(put("/api/v1/marketplace/orders/{orderId}/cancel", orderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 3. Assert status in DB
        Optional<MarketplaceOrder> updatedOrderOpt = marketplaceOrderRepository.findById(orderId);
        assertTrue(updatedOrderOpt.isPresent());
        assertEquals(MarketplaceOrderStatus.CANCELLED, updatedOrderOpt.get().getStatus());

        // 4. Assert outbox entry
        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        assertEquals(1, outboxEvents.size());
        OutboxEvent cancelEvent = outboxEvents.get(0);
        assertEquals("MarketplaceOrderCancelledEvent", cancelEvent.getEventType());
        assertEquals("MarketplaceOrder", cancelEvent.getAggregateType());
        assertEquals(orderId.toString(), cancelEvent.getAggregateId());
        assertFalse(cancelEvent.getProcessed());
        assertTrue(cancelEvent.getPayload().contains("ORD-CANCEL-123") || cancelEvent.getPayload().contains("\"orderId\":" + orderId));

        // 5. Act: Trigger outbox publisher to "publish live"
        marketplaceOutboxPublisher.publishPendingEvents();

        // 6. Assert published to RabbitMQ
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate, times(1)).send(
                eq("marketplace-exchange"),
                eq("order.cancelled"),
                messageCaptor.capture()
        );

        // Verify outbox entry is updated to processed
        List<OutboxEvent> outboxEventsAfter = outboxEventRepository.findAll();
        assertEquals(1, outboxEventsAfter.size());
        assertTrue(outboxEventsAfter.get(0).getProcessed());
    }

    @Test
    void testFarmerCompleteOrderAndVerifyOutboxPublication() throws Exception {
        // 1. Arrange: Create an order that belongs to farmer user 200
        Long buyerUserId = 100L;
        Long farmerUserId = 200L;

        MarketplaceOrder order = MarketplaceOrder.builder()
                .orderGroupId(11L)
                .orderCode("ORD-COMPLETE-123")
                .buyerUserId(buyerUserId)
                .farmerUserId(farmerUserId)
                .status(MarketplaceOrderStatus.SHIPPED)
                .paymentMethod(MarketplacePaymentMethod.BANK_TRANSFER)
                .paymentVerificationStatus(MarketplacePaymentVerificationStatus.VERIFIED)
                .shippingRecipientName("John Doe")
                .shippingPhone("123456789")
                .shippingAddressLine("123 Farm Road")
                .subtotal(new BigDecimal("200.00"))
                .shippingFee(new BigDecimal("15.00"))
                .totalAmount(new BigDecimal("215.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        order = marketplaceOrderRepository.save(order);
        Long orderId = order.getId();

        // Mock current user as the farmer
        when(currentUserService.getCurrentUserId()).thenReturn(farmerUserId);

        // Request body to update status to COMPLETED
        MarketplaceUpdateOrderStatusRequest request = new MarketplaceUpdateOrderStatusRequest(MarketplaceOrderStatus.COMPLETED);

        // 2. Act: Farmer completes order
        mockMvc.perform(patch("/api/v1/farmer/orders/{orderId}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 3. Assert status in DB
        Optional<MarketplaceOrder> updatedOrderOpt = marketplaceOrderRepository.findById(orderId);
        assertTrue(updatedOrderOpt.isPresent());
        assertEquals(MarketplaceOrderStatus.COMPLETED, updatedOrderOpt.get().getStatus());

        // 4. Assert outbox entry
        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        assertEquals(1, outboxEvents.size());
        OutboxEvent completeEvent = outboxEvents.get(0);
        assertEquals("MarketplaceOrderCompletedEvent", completeEvent.getEventType());
        assertEquals("MarketplaceOrder", completeEvent.getAggregateType());
        assertEquals(orderId.toString(), completeEvent.getAggregateId());
        assertFalse(completeEvent.getProcessed());

        // 5. Act: Trigger outbox publisher to "publish live"
        marketplaceOutboxPublisher.publishPendingEvents();

        // 6. Assert published to RabbitMQ
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate, times(1)).send(
                eq("marketplace-exchange"),
                eq("order.completed"),
                messageCaptor.capture()
        );

        // Verify outbox entry is updated to processed
        List<OutboxEvent> outboxEventsAfter = outboxEventRepository.findAll();
        assertEquals(1, outboxEventsAfter.size());
        assertTrue(outboxEventsAfter.get(0).getProcessed());
    }

    @Test
    void testAdminCompleteOrderAndVerifyOutboxPublication() throws Exception {
        // 1. Arrange: Create an order that belongs to buyer user 100
        Long buyerUserId = 100L;
        Long farmerUserId = 200L;

        MarketplaceOrder order = MarketplaceOrder.builder()
                .orderGroupId(12L)
                .orderCode("ORD-ADMIN-COMPLETE-123")
                .buyerUserId(buyerUserId)
                .farmerUserId(farmerUserId)
                .status(MarketplaceOrderStatus.DELIVERED)
                .paymentMethod(MarketplacePaymentMethod.BANK_TRANSFER)
                .paymentVerificationStatus(MarketplacePaymentVerificationStatus.VERIFIED)
                .shippingRecipientName("John Doe")
                .shippingPhone("123456789")
                .shippingAddressLine("123 Farm Road")
                .subtotal(new BigDecimal("300.00"))
                .shippingFee(new BigDecimal("20.00"))
                .totalAmount(new BigDecimal("320.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        order = marketplaceOrderRepository.save(order);
        Long orderId = order.getId();

        // Mock current user as admin
        when(currentUserService.getCurrentUserId()).thenReturn(999L);
        when(currentUserService.isAdmin()).thenReturn(true);

        // Request body to update status to COMPLETED
        MarketplaceUpdateOrderStatusRequest request = new MarketplaceUpdateOrderStatusRequest(MarketplaceOrderStatus.COMPLETED);

        // 2. Act: Admin completes order
        mockMvc.perform(patch("/api/v1/marketplace/admin/orders/{orderId}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 3. Assert status in DB
        Optional<MarketplaceOrder> updatedOrderOpt = marketplaceOrderRepository.findById(orderId);
        assertTrue(updatedOrderOpt.isPresent());
        assertEquals(MarketplaceOrderStatus.COMPLETED, updatedOrderOpt.get().getStatus());

        // 4. Assert outbox entry
        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        assertEquals(1, outboxEvents.size());
        OutboxEvent completeEvent = outboxEvents.get(0);
        assertEquals("MarketplaceOrderCompletedEvent", completeEvent.getEventType());
        assertFalse(completeEvent.getProcessed());

        // 5. Act: Trigger outbox publisher to "publish live"
        marketplaceOutboxPublisher.publishPendingEvents();

        // 6. Assert published to RabbitMQ
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate, times(1)).send(
                eq("marketplace-exchange"),
                eq("order.completed"),
                messageCaptor.capture()
        );

        // Verify outbox entry is updated to processed
        List<OutboxEvent> outboxEventsAfter = outboxEventRepository.findAll();
        assertEquals(1, outboxEventsAfter.size());
        assertTrue(outboxEventsAfter.get(0).getProcessed());
    }
}
