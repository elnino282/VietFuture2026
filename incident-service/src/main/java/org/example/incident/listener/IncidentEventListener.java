package org.example.incident.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.incident.dto.event.ExpenseChangedEventDto;
import org.example.incident.dto.event.HarvestRecordedEventDto;
import org.example.incident.dto.event.IncidentChangedEventDto;
import org.example.incident.dto.event.TaskAssignedEventDto;
import org.example.incident.dto.event.TaskCompletedEventDto;
import org.example.incident.entity.ProcessedEvent;
import org.example.incident.repository.ProcessedEventRepository;
import org.example.incident.service.NotificationService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class IncidentEventListener {
    ProcessedEventRepository processedEventRepository;
    NotificationService notificationService;
    ObjectMapper objectMapper;

    @Transactional
    @RabbitListener(queues = "incident-service.events")
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

        log.info("Processing event {} of type {}", eventId, eventType);

        try {
            boolean handled = dispatchEvent(message, eventType);
            if (handled) {
                processedEventRepository.save(
                        ProcessedEvent.builder()
                                .eventId(eventId)
                                .processedAt(LocalDateTime.now())
                                .build()
                );
                log.info("Event {} processed successfully", eventId);
            } else {
                log.warn("Unhandled event type {}, not marking as processed", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing event {}: {}", eventId, e.getMessage(), e);
            throw e;
        }
    }

    private boolean dispatchEvent(Message message, String eventType) throws Exception {
        if (eventType.startsWith("season.event.task.")) {
            if ("season.event.task.assigned".equals(eventType)) {
                handleTaskAssigned(message);
                return true;
            }
            if ("season.event.task.completed".equals(eventType)) {
                handleTaskCompleted(message);
                return true;
            }
            return false;
        }
        if (eventType.startsWith("finance.event.expense.")) {
            handleExpenseChanged(message, eventType);
            return true;
        }
        if ("season.event.harvest.recorded".equals(eventType)) {
            handleHarvestRecorded(message);
            return true;
        }
        if (eventType.startsWith("incident.event.incident.")) {
            handleIncidentChanged(message, eventType);
            return true;
        }
        return false;
    }

    private void handleTaskAssigned(Message message) throws Exception {
        TaskAssignedEventDto event = objectMapper.readValue(message.getBody(), TaskAssignedEventDto.class);
        log.info("Handling season.event.task.assigned: {}", event);

        String title = "New Task Assigned";
        String msg = String.format("You have been assigned a new task: %s", event.getTaskTitle());
        String link = String.format("/tasks/%d", event.getTaskId());

        notificationService.createNotificationFromEvent(event.getAssigneeUserId(), title, msg, link);
    }

    private void handleTaskCompleted(Message message) throws Exception {
        TaskCompletedEventDto event = objectMapper.readValue(message.getBody(), TaskCompletedEventDto.class);
        log.info("Handling season.event.task.completed: {}", event);

        String title = "Task Completed";
        String msg = String.format("Task \"%s\" has been completed!", event.getTaskTitle());
        String link = String.format("/tasks/%d", event.getTaskId());

        notificationService.createNotificationFromEvent(event.getAssigneeUserId(), title, msg, link);
    }

    private void handleExpenseChanged(Message message, String eventType) throws Exception {
        ExpenseChangedEventDto event = objectMapper.readValue(message.getBody(), ExpenseChangedEventDto.class);
        log.info("Handling {}: {}", eventType, event);

        String title;
        String msg;
        switch (event.getAction().toLowerCase()) {
            case "created":
                title = "Expense Created";
                msg = String.format("A new expense has been added: %s (%.2f)", event.getCategory(), event.getAmount());
                break;
            case "updated":
                title = "Expense Updated";
                msg = String.format("An expense has been updated: %s (%.2f)", event.getCategory(), event.getAmount());
                break;
            case "deleted":
                title = "Expense Deleted";
                msg = String.format("An expense has been deleted: %s", event.getCategory());
                break;
            default:
                title = "Expense Updated";
                msg = "An expense has been changed";
        }
        String link = event.getExpenseId() != null ? String.format("/expenses/%d", event.getExpenseId()) : null;

        notificationService.createNotificationFromEvent(event.getOwnerUserId(), title, msg, link);
    }

    private void handleHarvestRecorded(Message message) throws Exception {
        HarvestRecordedEventDto event = objectMapper.readValue(message.getBody(), HarvestRecordedEventDto.class);
        log.info("Handling season.event.harvest.recorded: {}", event);

        String title = "Harvest Recorded";
        String msg = String.format("Harvest recorded for %s (%.2f %s)", event.getCropName(), event.getQuantity(), event.getUnit());
        String link = String.format("/harvests/%d", event.getHarvestId());

        notificationService.createNotificationFromEvent(event.getActorUserId(), title, msg, link);
    }

    private void handleIncidentChanged(Message message, String eventType) throws Exception {
        IncidentChangedEventDto event = objectMapper.readValue(message.getBody(), IncidentChangedEventDto.class);
        log.info("Handling {}: {}", eventType, event);

        String title;
        String msg;
        switch (event.getAction().toLowerCase()) {
            case "created":
                title = "Incident Reported";
                msg = String.format("A new incident has been reported on farm %d", event.getFarmId());
                break;
            case "resolved":
                title = "Incident Resolved";
                msg = String.format("Incident #%d has been resolved!", event.getIncidentId());
                break;
            case "cancelled":
                title = "Incident Cancelled";
                msg = String.format("Incident #%d has been cancelled!", event.getIncidentId());
                break;
            default:
                title = "Incident Updated";
                msg = String.format("Incident #%d has been updated!", event.getIncidentId());
        }
        String link = String.format("/incidents/%d", event.getIncidentId());

        notificationService.createNotificationFromEvent(event.getReporterUserId(), title, msg, link);
    }
}
