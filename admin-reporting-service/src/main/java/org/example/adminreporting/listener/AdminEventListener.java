package org.example.adminreporting.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.adminreporting.config.RabbitMQConfig;
import org.example.adminreporting.dto.event.AdminEvents.*;
import org.example.adminreporting.entity.*;
import org.example.adminreporting.repository.*;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminEventListener {

    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;

    private final FarmSummaryRepository farmSummaryRepository;
    private final PlotSummaryRepository plotSummaryRepository;
    private final SeasonSummaryRepository seasonSummaryRepository;
    private final IncidentSummaryRepository incidentSummaryRepository;
    private final TaskSummaryRepository taskSummaryRepository;
    private final AlertSummaryRepository alertSummaryRepository;
    private final InventoryLotSummaryRepository inventoryLotSummaryRepository;
    private final HarvestSummaryRepository harvestSummaryRepository;
    private final ExpenseSummaryRepository expenseSummaryRepository;
    private final MarketplaceOrderSummaryRepository marketplaceOrderSummaryRepository;
    private final MarketplaceOrderItemSummaryRepository marketplaceOrderItemSummaryRepository;

    private final UserSummaryRepository userSummaryRepository;
    private final AuditLogEntryRepository auditLogEntryRepository;
    private final DocumentRepository documentRepository;
    private final MarketplaceProductSummaryRepository marketplaceProductSummaryRepository;

    @Transactional
    @RabbitListener(queues = RabbitMQConfig.ADMIN_REPORTING_EVENTS_QUEUE)
    public void handleEvent(Message message) throws Exception {
        String eventId = message.getMessageProperties().getMessageId();
        if (eventId == null) {
            log.warn("Received event without messageId, skipping");
            return;
        }

        Optional<ProcessedEvent> existing = processedEventRepository.findById(eventId);
        if (existing.isPresent()) {
            log.info("Event {} already processed, skipping", eventId);
            return;
        }

        String eventType = (String) message.getMessageProperties().getHeaders().get("eventType");
        if (eventType == null || eventType.isBlank()) {
            log.warn("Received event {} without eventType header, skipping", eventId);
            return;
        }

        log.info("Admin Reporting Service processing event {} of type {}", eventId, eventType);

        try {
            boolean handled = dispatchEvent(message, eventType);
            if (handled) {
                processedEventRepository.save(ProcessedEvent.builder()
                        .eventId(eventId)
                        .processedAt(LocalDateTime.now())
                        .build());
                log.info("Event {} processed and marked successfully", eventId);
            } else {
                log.warn("Unhandled event type {}, not marking as processed", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing event {}: {}", eventId, e.getMessage(), e);
            throw e;
        }
    }

    private boolean dispatchEvent(Message message, String eventType) throws Exception {
        if ("farm.event.farm.created".equals(eventType) || "farm.event.farm.updated".equals(eventType)) {
            handleFarmChanged(message);
            return true;
        }
        if ("farm.event.farm.deleted".equals(eventType)) {
            handleFarmDeleted(message);
            return true;
        }
        if ("farm.event.plot.created".equals(eventType) || "farm.event.plot.updated".equals(eventType)) {
            handlePlotChanged(message);
            return true;
        }
        if ("farm.event.plot.deleted".equals(eventType)) {
            handlePlotDeleted(message);
            return true;
        }
        if ("season.event.season.created".equals(eventType)
                || "season.event.season.updated".equals(eventType)
                || "season.event.season.status.changed".equals(eventType)
                || "season.event.season.completed".equals(eventType)) {
            handleSeasonChanged(message);
            return true;
        }
        if ("season.event.harvest.recorded".equals(eventType)) {
            handleHarvestRecorded(message);
            return true;
        }
        if ("finance.event.expense.created".equals(eventType) || "finance.event.expense.updated".equals(eventType)) {
            handleExpenseChanged(message);
            return true;
        }
        if ("finance.event.expense.deleted".equals(eventType)) {
            handleExpenseDeleted(message);
            return true;
        }
        if (eventType.startsWith("incident.event.incident.")) {
            handleIncidentChanged(message);
            return true;
        }
        if ("inventory.event.product_warehouse_lot_received".equals(eventType)) {
            handleInventoryLotReceived(message);
            return true;
        }
        if ("order.created".equals(eventType)) {
            handleOrderCreated(message);
            return true;
        }
        if ("payment.submitted".equals(eventType)) {
            handlePaymentSubmitted(message);
            return true;
        }
        if ("payment.verified".equals(eventType)) {
            handlePaymentVerified(message);
            return true;
        }
        if ("order.completed".equals(eventType)) {
            handleOrderCompleted(message);
            return true;
        }
        if ("order.cancelled".equals(eventType)) {
            handleOrderCancelled(message);
            return true;
        }
        if ("identity.event.user.created".equals(eventType) || "identity.event.user.updated".equals(eventType)) {
            handleUserChanged(message);
            return true;
        }
        if ("TASK_ASSIGNED".equals(eventType)) {
            handleTaskAssigned(message);
            return true;
        }
        if ("TASK_COMPLETED".equals(eventType)) {
            handleTaskCompleted(message);
            return true;
        }
        if ("ALERT_CHANGED".equals(eventType)) {
            handleAlertChanged(message);
            return true;
        }
        if ("audit.event.created".equals(eventType)) {
            handleAuditLogCreated(message);
            return true;
        }
        if (eventType.startsWith("document.event.")) {
            handleDocumentEvent(message);
            return true;
        }
        if ("marketplace.product.changed".equals(eventType)) {
            handleMarketplaceProductChanged(message);
            return true;
        }
        return false;
    }

    private void handleFarmChanged(Message message) throws Exception {
        FarmChangedEventDto dto = objectMapper.readValue(message.getBody(), FarmChangedEventDto.class);
        FarmSummary summary = FarmSummary.builder()
                .farmId(dto.getFarmId())
                .farmName(dto.getFarmName())
                .active(dto.getActive())
                .build();
        farmSummaryRepository.save(summary);
    }

    private void handleFarmDeleted(Message message) throws Exception {
        FarmChangedEventDto dto = objectMapper.readValue(message.getBody(), FarmChangedEventDto.class);
        farmSummaryRepository.deleteById(dto.getFarmId());
    }

    private void handlePlotChanged(Message message) throws Exception {
        PlotChangedEventDto dto = objectMapper.readValue(message.getBody(), PlotChangedEventDto.class);
        PlotSummary summary = PlotSummary.builder()
                .plotId(dto.getPlotId())
                .farmId(dto.getFarmId())
                .plotName(dto.getPlotName())
                .build();
        plotSummaryRepository.save(summary);
    }

    private void handlePlotDeleted(Message message) throws Exception {
        PlotChangedEventDto dto = objectMapper.readValue(message.getBody(), PlotChangedEventDto.class);
        plotSummaryRepository.deleteById(dto.getPlotId());
    }

    private void handleSeasonChanged(Message message) throws Exception {
        SeasonChangedEventDto dto = objectMapper.readValue(message.getBody(), SeasonChangedEventDto.class);
        SeasonSummary summary = SeasonSummary.builder()
                .seasonId(dto.getSeasonId())
                .seasonName(dto.getSeasonName())
                .plotId(dto.getPlotId())
                .cropId(dto.getCropId())
                .varietyId(dto.getVarietyId())
                .status(dto.getStatus())
                .startDate(dto.getStartDate())
                .expectedYieldKg(dto.getExpectedYieldKg())
                .actualYieldKg(dto.getActualYieldKg())
                .build();
        seasonSummaryRepository.save(summary);
    }

    private void handleHarvestRecorded(Message message) throws Exception {
        HarvestRecordedEventDto dto = objectMapper.readValue(message.getBody(), HarvestRecordedEventDto.class);
        HarvestSummary summary = HarvestSummary.builder()
                .harvestId(dto.getHarvestId())
                .seasonId(dto.getSeasonId())
                .quantity(dto.getQuantity())
                .unitPrice(BigDecimal.ZERO) // defaults in reports, updated in marketplace/sale if needed
                .build();
        harvestSummaryRepository.save(summary);
    }

    private void handleExpenseChanged(Message message) throws Exception {
        ExpenseChangedEventDto dto = objectMapper.readValue(message.getBody(), ExpenseChangedEventDto.class);
        ExpenseSummary summary = ExpenseSummary.builder()
                .expenseId(dto.getExpenseId())
                .seasonId(dto.getSeasonId())
                .totalCost(dto.getTotalCost() != null ? dto.getTotalCost() : dto.getAmount())
                .category(dto.getCategory())
                .itemName(dto.getItemName())
                .expenseDate(dto.getExpenseDate())
                .build();
        expenseSummaryRepository.save(summary);
    }

    private void handleExpenseDeleted(Message message) throws Exception {
        ExpenseChangedEventDto dto = objectMapper.readValue(message.getBody(), ExpenseChangedEventDto.class);
        expenseSummaryRepository.deleteById(dto.getExpenseId());
    }

    private void handleIncidentChanged(Message message) throws Exception {
        IncidentChangedEventDto dto = objectMapper.readValue(message.getBody(), IncidentChangedEventDto.class);
        IncidentSummary summary = IncidentSummary.builder()
                .incidentId(dto.getIncidentId())
                .seasonId(dto.getSeasonId())
                .status(dto.getStatus())
                .incidentType(dto.getIncidentType())
                .severity(dto.getSeverity())
                .resolvedAt(dto.getResolvedAt())
                .createdAt(dto.getCreatedAt())
                .build();
        incidentSummaryRepository.save(summary);
    }

    private void handleInventoryLotReceived(Message message) throws Exception {
        ProductWarehouseLotReceivedEventDto dto = objectMapper.readValue(message.getBody(), ProductWarehouseLotReceivedEventDto.class);
        String farmName = farmSummaryRepository.findById(dto.getFarmId())
                .map(FarmSummary::getFarmName)
                .orElse("Farm " + dto.getFarmId());

        InventoryLotSummary summary = InventoryLotSummary.builder()
                .lotId(dto.getLotId())
                .farmId(dto.getFarmId())
                .farmName(farmName)
                .warehouseId(dto.getWarehouseId())
                .warehouseName("Warehouse " + dto.getWarehouseId())
                .quantityOnHand(dto.getQuantity())
                .build();
        inventoryLotSummaryRepository.save(summary);
    }

    private void handleOrderCreated(Message message) throws Exception {
        MarketplaceOrderCreatedEventDto dto = objectMapper.readValue(message.getBody(), MarketplaceOrderCreatedEventDto.class);
        MarketplaceOrderSummary summary = MarketplaceOrderSummary.builder()
                .orderId(dto.getPayload().getOrderId())
                .orderCode("ORD-" + dto.getPayload().getOrderId())
                .buyerId(dto.getPayload().getBuyerUserId())
                .buyerName("Buyer " + dto.getPayload().getBuyerUserId())
                .status(dto.getPayload().getStatus())
                .paymentStatus("PENDING")
                .totalAmount(dto.getPayload().getTotalAmount() != null ? dto.getPayload().getTotalAmount() : BigDecimal.ZERO)
                .createdAt(dto.getOccurredAt())
                .build();
        marketplaceOrderSummaryRepository.save(summary);

        if (dto.getPayload().getItems() != null) {
            for (MarketplaceOrderCreatedEventDto.OrderItemPayload item : dto.getPayload().getItems()) {
                MarketplaceOrderItemSummary itemSummary = MarketplaceOrderItemSummary.builder()
                        .itemId(item.getItemId())
                        .orderId(dto.getPayload().getOrderId())
                        .seasonId(item.getSeasonId())
                        .quantity(item.getQuantity() != null ? BigDecimal.valueOf(item.getQuantity()) : BigDecimal.ZERO)
                        .unitPrice(item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO)
                        .lineTotal(item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO)
                        .build();
                marketplaceOrderItemSummaryRepository.save(itemSummary);
            }
        }
    }

    private void handlePaymentSubmitted(Message message) throws Exception {
        MarketplacePaymentSubmittedEventDto dto = objectMapper.readValue(message.getBody(), MarketplacePaymentSubmittedEventDto.class);
        marketplaceOrderSummaryRepository.findById(dto.getPayload().getOrderId())
                .ifPresent(order -> {
                    order.setPaymentStatus("SUBMITTED");
                    order.setPaymentProofUploadedAt(dto.getOccurredAt());
                    marketplaceOrderSummaryRepository.save(order);
                });
    }

    private void handlePaymentVerified(Message message) throws Exception {
        MarketplacePaymentVerifiedEventDto dto = objectMapper.readValue(message.getBody(), MarketplacePaymentVerifiedEventDto.class);
        marketplaceOrderSummaryRepository.findById(dto.getPayload().getOrderId())
                .ifPresent(order -> {
                    order.setPaymentStatus(dto.getPayload().getVerificationStatus());
                    marketplaceOrderSummaryRepository.save(order);
                });
    }

    private void handleOrderCompleted(Message message) throws Exception {
        MarketplaceOrderCompletedEventDto dto = objectMapper.readValue(message.getBody(), MarketplaceOrderCompletedEventDto.class);
        marketplaceOrderSummaryRepository.findById(dto.getPayload().getOrderId())
                .ifPresent(order -> {
                    order.setStatus("COMPLETED");
                    marketplaceOrderSummaryRepository.save(order);
                });
    }

    private void handleOrderCancelled(Message message) throws Exception {
        MarketplaceOrderCancelledEventDto dto = objectMapper.readValue(message.getBody(), MarketplaceOrderCancelledEventDto.class);
        marketplaceOrderSummaryRepository.findById(dto.getPayload().getOrderId())
                .ifPresent(order -> {
                    order.setStatus("CANCELLED");
                    marketplaceOrderSummaryRepository.save(order);
                });
    }

    private void handleUserChanged(Message message) throws Exception {
        UserChangedEventDto dto = objectMapper.readValue(message.getBody(), UserChangedEventDto.class);
        if ("DELETED".equals(dto.getAction())) {
            userSummaryRepository.deleteById(dto.getUserId());
        } else {
            UserSummary summary = UserSummary.builder()
                    .userId(dto.getUserId())
                    .username(dto.getUsername())
                    .email(dto.getEmail())
                    .fullName(dto.getFullName())
                    .status(dto.getStatus())
                    .roleCode(dto.getRoles() != null && !dto.getRoles().isEmpty() ? dto.getRoles().get(0) : "")
                    .build();
            userSummaryRepository.save(summary);
        }
    }

    private void handleTaskAssigned(Message message) throws Exception {
        TaskEventDto dto = objectMapper.readValue(message.getBody(), TaskEventDto.class);
        TaskSummary summary = TaskSummary.builder()
                .taskId(dto.getTaskId())
                .seasonId(dto.getSeasonId())
                .status("ASSIGNED")
                .build();
        taskSummaryRepository.save(summary);
    }

    private void handleTaskCompleted(Message message) throws Exception {
        TaskEventDto dto = objectMapper.readValue(message.getBody(), TaskEventDto.class);
        TaskSummary summary = TaskSummary.builder()
                .taskId(dto.getTaskId())
                .seasonId(dto.getSeasonId())
                .status("COMPLETED")
                .build();
        taskSummaryRepository.save(summary);
    }

    private void handleAlertChanged(Message message) throws Exception {
        AlertChangedEventDto dto = objectMapper.readValue(message.getBody(), AlertChangedEventDto.class);
        AlertSummary summary = AlertSummary.builder()
                .alertId(dto.getAlertId())
                .seasonId(dto.getSeasonId())
                .type(dto.getAlertType())
                .severity(dto.getSeverity())
                .status(dto.getStatus())
                .build();
        alertSummaryRepository.save(summary);
    }

    private void handleAuditLogCreated(Message message) throws Exception {
        AuditLogCreatedEventDto dto = objectMapper.readValue(message.getBody(), AuditLogCreatedEventDto.class);
        AuditLogEntry entry = AuditLogEntry.builder()
                .id(dto.getId())
                .entityType(dto.getModule() != null ? dto.getModule() : "unknown")
                .entityId(0) // default
                .operation(dto.getAction())
                .performedBy(dto.getCreatedBy())
                .performedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : LocalDateTime.now())
                .snapshotDataJson(dto.getDetails())
                .ipAddress(dto.getIpAddress())
                .build();
        auditLogEntryRepository.save(entry);
    }

    private void handleDocumentEvent(Message message) throws Exception {
        DocumentEventDto dto = objectMapper.readValue(message.getBody(), DocumentEventDto.class);
        if ("DELETED".equals(dto.getAction())) {
            documentRepository.deleteById(dto.getId());
        } else {
            Optional<Document> existing = documentRepository.findById(dto.getId());
            Document doc;
            if (existing.isPresent()) {
                doc = existing.get();
                doc.setTitle(dto.getTitle());
                doc.setTopic(dto.getTopic());
                doc.setIsActive(dto.getIsActive());
                if (dto.getUrl() != null) doc.setUrl(dto.getUrl());
                if (dto.getDescription() != null) doc.setDescription(dto.getDescription());
            } else {
                doc = Document.builder()
                        .documentId(dto.getId())
                        .title(dto.getTitle())
                        .topic(dto.getTopic())
                        .isActive(dto.getIsActive())
                        .url(dto.getUrl() != null ? dto.getUrl() : "")
                        .description(dto.getDescription())
                        .isPublic(true)
                        .build();
            }
            documentRepository.save(doc);
        }
    }

    private void handleMarketplaceProductChanged(Message message) throws Exception {
        MarketplaceProductChangedEventDto dto = objectMapper.readValue(message.getBody(), MarketplaceProductChangedEventDto.class);
        MarketplaceProductSummary summary = MarketplaceProductSummary.builder()
                .productId(dto.getPayload().getProductId())
                .productName(dto.getPayload().getProductName())
                .farmId(dto.getPayload().getFarmId())
                .farmName(dto.getPayload().getFarmName())
                .farmerId(dto.getPayload().getFarmerId())
                .farmerName(dto.getPayload().getFarmerName())
                .status(dto.getPayload().getStatus())
                .updatedAt(dto.getPayload().getUpdatedAt() != null ? LocalDateTime.parse(dto.getPayload().getUpdatedAt()) : LocalDateTime.now())
                .build();
        marketplaceProductSummaryRepository.save(summary);
    }
}
