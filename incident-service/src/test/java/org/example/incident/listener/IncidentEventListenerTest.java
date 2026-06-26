package org.example.incident.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.example.incident.dto.event.TaskAssignedEventDto;
import org.example.incident.entity.ProcessedEvent;
import org.example.incident.repository.ProcessedEventRepository;
import org.example.incident.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

@ExtendWith(MockitoExtension.class)
class IncidentEventListenerTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private NotificationService notificationService;

    private ObjectMapper objectMapper;
    private IncidentEventListener incidentEventListener;

    private MessageProperties messageProperties;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        incidentEventListener = new IncidentEventListener(processedEventRepository, notificationService, objectMapper);
        messageProperties = new MessageProperties();
        messageProperties.setMessageId("test-event-id");
    }

    @Test
    void handleEvent_TaskAssigned_ShouldCreateNotificationAndMarkProcessed() throws Exception {
        TaskAssignedEventDto event = TaskAssignedEventDto.builder()
                .taskId(42)
                .taskTitle("Water crops")
                .seasonId(10)
                .assigneeUserId(99L)
                .build();
        messageProperties.setHeader("eventType", "season.event.task.assigned");
        Message message = new Message(objectMapper.writeValueAsBytes(event), messageProperties);

        when(processedEventRepository.findById("test-event-id")).thenReturn(Optional.empty());

        incidentEventListener.handleEvent(message);

        verify(notificationService).createNotificationFromEvent(
                eq(99L),
                eq("New Task Assigned"),
                eq("You have been assigned a new task: Water crops"),
                eq("/tasks/42")
        );
        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
    }

    @Test
    void handleEvent_UnknownEventType_ShouldNotMarkProcessed() throws Exception {
        messageProperties.setHeader("eventType", "TASK_ASSIGNED");
        Message message = new Message("test-payload".getBytes(), messageProperties);

        when(processedEventRepository.findById("test-event-id")).thenReturn(Optional.empty());

        incidentEventListener.handleEvent(message);

        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
        verify(notificationService, never()).createNotificationFromEvent(any(), any(), any(), any());
    }

    @Test
    void handleEvent_AlreadyProcessedEvent_ShouldSkip() throws Exception {
        messageProperties.setHeader("eventType", "season.event.task.assigned");
        Message message = new Message("test-payload".getBytes(), messageProperties);

        when(processedEventRepository.findById("test-event-id")).thenReturn(Optional.of(new ProcessedEvent()));

        incidentEventListener.handleEvent(message);

        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
        verify(notificationService, never()).createNotificationFromEvent(any(), any(), any(), any());
    }

    @Test
    void handleEvent_NoMessageId_ShouldSkip() throws Exception {
        messageProperties.setMessageId(null);
        messageProperties.setHeader("eventType", "season.event.task.assigned");
        Message message = new Message("test-payload".getBytes(), messageProperties);

        incidentEventListener.handleEvent(message);

        verify(processedEventRepository, never()).findById(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void handleEvent_NoEventTypeHeader_ShouldSkip() throws Exception {
        Message message = new Message("test-payload".getBytes(), messageProperties);

        when(processedEventRepository.findById("test-event-id")).thenReturn(Optional.empty());

        incidentEventListener.handleEvent(message);

        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
    }
}
