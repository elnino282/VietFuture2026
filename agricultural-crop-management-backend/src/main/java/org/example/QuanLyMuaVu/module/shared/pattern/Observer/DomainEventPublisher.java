package org.example.QuanLyMuaVu.module.shared.pattern.Observer;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(DomainEvent event) {
        if (event == null) {
            return;
        }
        applicationEventPublisher.publishEvent(event);
    }
}
