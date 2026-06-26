package org.example.sustainability.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Optional;
import org.example.sustainability.dto.event.FarmChangedEventDto;
import org.example.sustainability.dto.event.SeasonChangedEventDto;
import org.example.sustainability.entity.ProcessedEvent;
import org.example.sustainability.repository.ProcessedEventRepository;
import org.example.sustainability.snapshot.entity.FarmSnapshot;
import org.example.sustainability.snapshot.entity.SeasonSnapshot;
import org.example.sustainability.snapshot.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

@ExtendWith(MockitoExtension.class)
class SustainabilityEventListenerTest {

    @Mock private ProcessedEventRepository processedEventRepository;
    @Mock private FarmSnapshotRepository farmSnapshotRepository;
    @Mock private PlotSnapshotRepository plotSnapshotRepository;
    @Mock private SeasonSnapshotRepository seasonSnapshotRepository;
    @Mock private CropSnapshotRepository cropSnapshotRepository;
    @Mock private HarvestSnapshotRepository harvestSnapshotRepository;
    @Mock private ExpenseSnapshotRepository expenseSnapshotRepository;
    @Mock private IncidentSnapshotRepository incidentSnapshotRepository;

    private ObjectMapper objectMapper;
    private SustainabilityEventListener listener;
    private MessageProperties messageProperties;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        listener = new SustainabilityEventListener(
                processedEventRepository,
                objectMapper,
                farmSnapshotRepository,
                plotSnapshotRepository,
                seasonSnapshotRepository,
                cropSnapshotRepository,
                harvestSnapshotRepository,
                expenseSnapshotRepository,
                incidentSnapshotRepository
        );
        messageProperties = new MessageProperties();
        messageProperties.setMessageId("evt-1");
    }

    @Test
    void handleEvent_FarmCreated_ShouldPersistSnapshot() throws Exception {
        FarmChangedEventDto dto = FarmChangedEventDto.builder()
                .farmId(1)
                .userId(10L)
                .farmName("Green Farm")
                .build();
        messageProperties.setHeader("eventType", "farm.event.farm.created");
        Message message = new Message(objectMapper.writeValueAsBytes(dto), messageProperties);

        when(processedEventRepository.findById("evt-1")).thenReturn(Optional.empty());

        listener.handleEvent(message);

        ArgumentCaptor<FarmSnapshot> captor = ArgumentCaptor.forClass(FarmSnapshot.class);
        verify(farmSnapshotRepository).save(captor.capture());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
        assertEquals(1, captor.getValue().getFarmId());
        assertEquals("Green Farm", captor.getValue().getFarmName());
    }

    @Test
    void handleEvent_SeasonCreated_ShouldPersistSnapshotWithFarmId() throws Exception {
        SeasonChangedEventDto dto = SeasonChangedEventDto.builder()
                .seasonId(5)
                .seasonName("Spring Rice")
                .plotId(2)
                .farmId(1)
                .cropId(3)
                .startDate(LocalDate.of(2025, 3, 1))
                .status("PLANNED")
                .build();
        messageProperties.setHeader("eventType", "season.event.season.created");
        Message message = new Message(objectMapper.writeValueAsBytes(dto), messageProperties);

        when(processedEventRepository.findById("evt-1")).thenReturn(Optional.empty());

        listener.handleEvent(message);

        ArgumentCaptor<SeasonSnapshot> captor = ArgumentCaptor.forClass(SeasonSnapshot.class);
        verify(seasonSnapshotRepository).save(captor.capture());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
        assertEquals(5, captor.getValue().getSeasonId());
        assertEquals(1, captor.getValue().getFarmId());
        assertEquals("Spring Rice", captor.getValue().getSeasonName());
    }

    @Test
    void handleEvent_UnknownType_ShouldNotMarkProcessed() throws Exception {
        messageProperties.setHeader("eventType", "unknown.event.type");
        Message message = new Message("{}".getBytes(), messageProperties);

        when(processedEventRepository.findById("evt-1")).thenReturn(Optional.empty());

        listener.handleEvent(message);

        verify(processedEventRepository, never()).save(any());
        verify(farmSnapshotRepository, never()).save(any());
    }

    @Test
    void handleEvent_NoEventTypeHeader_ShouldSkip() throws Exception {
        Message message = new Message("{}".getBytes(), messageProperties);

        when(processedEventRepository.findById("evt-1")).thenReturn(Optional.empty());

        listener.handleEvent(message);

        verify(processedEventRepository, never()).save(any());
    }
}
