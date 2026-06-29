package org.example.adminreporting.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.example.adminreporting.dto.event.AdminEvents.*;
import org.example.adminreporting.entity.*;
import org.example.adminreporting.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

@ExtendWith(MockitoExtension.class)
class AdminEventListenerTest {

    @Mock private ProcessedEventRepository processedEventRepository;
    @Mock private FarmSummaryRepository farmSummaryRepository;
    @Mock private PlotSummaryRepository plotSummaryRepository;
    @Mock private SeasonSummaryRepository seasonSummaryRepository;
    @Mock private IncidentSummaryRepository incidentSummaryRepository;
    @Mock private TaskSummaryRepository taskSummaryRepository;
    @Mock private AlertSummaryRepository alertSummaryRepository;
    @Mock private InventoryLotSummaryRepository inventoryLotSummaryRepository;
    @Mock private HarvestSummaryRepository harvestSummaryRepository;
    @Mock private ExpenseSummaryRepository expenseSummaryRepository;
    @Mock private MarketplaceOrderSummaryRepository marketplaceOrderSummaryRepository;
    @Mock private MarketplaceOrderItemSummaryRepository marketplaceOrderItemSummaryRepository;
    @Mock private UserSummaryRepository userSummaryRepository;
    @Mock private AuditLogEntryRepository auditLogEntryRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private MarketplaceProductSummaryRepository marketplaceProductSummaryRepository;

    private ObjectMapper objectMapper;
    private AdminEventListener listener;
    private MessageProperties messageProperties;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        listener = new AdminEventListener(
                objectMapper,
                processedEventRepository,
                farmSummaryRepository,
                plotSummaryRepository,
                seasonSummaryRepository,
                incidentSummaryRepository,
                taskSummaryRepository,
                alertSummaryRepository,
                inventoryLotSummaryRepository,
                harvestSummaryRepository,
                expenseSummaryRepository,
                marketplaceOrderSummaryRepository,
                marketplaceOrderItemSummaryRepository,
                userSummaryRepository,
                auditLogEntryRepository,
                documentRepository,
                marketplaceProductSummaryRepository
        );
        messageProperties = new MessageProperties();
        messageProperties.setMessageId("evt-123");
    }

    @Test
    void handleEvent_FarmCreated_ShouldSaveFarmSummary() throws Exception {
        FarmChangedEventDto dto = FarmChangedEventDto.builder()
                .farmId(1)
                .farmName("Happy Farm")
                .active(true)
                .build();
        messageProperties.setHeader("eventType", "farm.event.farm.created");
        Message message = new Message(objectMapper.writeValueAsBytes(dto), messageProperties);

        when(processedEventRepository.findById("evt-123")).thenReturn(Optional.empty());

        listener.handleEvent(message);

        ArgumentCaptor<FarmSummary> captor = ArgumentCaptor.forClass(FarmSummary.class);
        verify(farmSummaryRepository).save(captor.capture());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
        assertEquals(1, captor.getValue().getFarmId());
        assertEquals("Happy Farm", captor.getValue().getFarmName());
    }

    @Test
    void handleEvent_SeasonCreated_ShouldSaveSeasonSummary() throws Exception {
        SeasonChangedEventDto dto = SeasonChangedEventDto.builder()
                .seasonId(10)
                .seasonName("Vụ Mùa Đông Xuân")
                .plotId(5)
                .cropId(1)
                .status("ACTIVE")
                .startDate(LocalDate.of(2026, 1, 1))
                .expectedYieldKg(BigDecimal.valueOf(5000.0))
                .build();
        messageProperties.setHeader("eventType", "season.event.season.created");
        Message message = new Message(objectMapper.writeValueAsBytes(dto), messageProperties);

        when(processedEventRepository.findById("evt-123")).thenReturn(Optional.empty());

        listener.handleEvent(message);

        ArgumentCaptor<SeasonSummary> captor = ArgumentCaptor.forClass(SeasonSummary.class);
        verify(seasonSummaryRepository).save(captor.capture());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
        assertEquals(10, captor.getValue().getSeasonId());
        assertEquals("Vụ Mùa Đông Xuân", captor.getValue().getSeasonName());
        assertEquals("ACTIVE", captor.getValue().getStatus());
    }

    @Test
    void handleEvent_OrderCreated_ShouldSaveOrderSummary() throws Exception {
        MarketplaceOrderCreatedEventDto dto = MarketplaceOrderCreatedEventDto.builder()
                .eventId("evt-order-1")
                .occurredAt(LocalDateTime.now())
                .payload(MarketplaceOrderCreatedEventDto.Payload.builder()
                        .orderId(99L)
                        .buyerUserId(42L)
                        .status("PENDING")
                        .build())
                .build();
        messageProperties.setHeader("eventType", "order.created");
        Message message = new Message(objectMapper.writeValueAsBytes(dto), messageProperties);

        when(processedEventRepository.findById("evt-123")).thenReturn(Optional.empty());

        listener.handleEvent(message);

        ArgumentCaptor<MarketplaceOrderSummary> captor = ArgumentCaptor.forClass(MarketplaceOrderSummary.class);
        verify(marketplaceOrderSummaryRepository).save(captor.capture());
        assertEquals(99L, captor.getValue().getOrderId());
        assertEquals("PENDING", captor.getValue().getStatus());
    }

    @Test
    void handleEvent_TaskAssigned_ShouldSaveTaskSummary() throws Exception {
        TaskEventDto dto = TaskEventDto.builder()
                .taskId(7)
                .seasonId(3)
                .build();
        messageProperties.setHeader("eventType", "season.event.task.assigned");
        Message message = new Message(objectMapper.writeValueAsBytes(dto), messageProperties);

        when(processedEventRepository.findById("evt-123")).thenReturn(Optional.empty());

        listener.handleEvent(message);

        ArgumentCaptor<TaskSummary> captor = ArgumentCaptor.forClass(TaskSummary.class);
        verify(taskSummaryRepository).save(captor.capture());
        assertEquals(7, captor.getValue().getTaskId());
        assertEquals("ASSIGNED", captor.getValue().getStatus());
    }

    @Test
    void handleEvent_AlertCreated_ShouldSaveAlertSummary() throws Exception {
        AlertChangedEventDto dto = AlertChangedEventDto.builder()
                .alertId(12)
                .seasonId(5)
                .alertType("SEASON_RISK")
                .severity("HIGH")
                .status("ACTIVE")
                .build();
        messageProperties.setHeader("eventType", "incident.event.alert.created");
        Message message = new Message(objectMapper.writeValueAsBytes(dto), messageProperties);

        when(processedEventRepository.findById("evt-123")).thenReturn(Optional.empty());

        listener.handleEvent(message);

        ArgumentCaptor<AlertSummary> captor = ArgumentCaptor.forClass(AlertSummary.class);
        verify(alertSummaryRepository).save(captor.capture());
        assertEquals(12, captor.getValue().getAlertId());
        assertEquals("HIGH", captor.getValue().getSeverity());
    }

    @Test
    void handleEvent_StockAdjusted_ShouldUpdateLotQuantity() throws Exception {
        StockAdjustedEventDto dto = StockAdjustedEventDto.builder()
                .productWarehouseLotId(20)
                .newQuantity(BigDecimal.valueOf(75))
                .build();
        messageProperties.setHeader("eventType", "inventory.event.stock_adjusted");
        Message message = new Message(objectMapper.writeValueAsBytes(dto), messageProperties);

        InventoryLotSummary existingLot = InventoryLotSummary.builder()
                .lotId(20)
                .quantityOnHand(BigDecimal.valueOf(100))
                .build();
        when(processedEventRepository.findById("evt-123")).thenReturn(Optional.empty());
        when(inventoryLotSummaryRepository.findById(20)).thenReturn(Optional.of(existingLot));

        listener.handleEvent(message);

        ArgumentCaptor<InventoryLotSummary> captor = ArgumentCaptor.forClass(InventoryLotSummary.class);
        verify(inventoryLotSummaryRepository).save(captor.capture());
        assertEquals(BigDecimal.valueOf(75), captor.getValue().getQuantityOnHand());
    }
}
