package org.example.QuanLyMuaVu.module.shared.pattern.Observer;

import lombok.Getter;

@Getter
public class DocumentEvent extends DomainEvent {

    public enum Action {
        CREATED,
        UPDATED,
        DELETED
    }

    private final Action action;
    private final Integer id;
    private final String title;
    private final String topic;
    private final Boolean isActive;
    private final String url;
    private final String description;

    public DocumentEvent(Integer id, String title, String topic, Boolean isActive, String url, String description, Action action) {
        super("Document", id != null ? id.toString() : "unknown");
        this.action = action;
        this.id = id;
        this.title = title;
        this.topic = topic;
        this.isActive = isActive;
        this.url = url;
        this.description = description;
    }

    @Override
    public String getEventType() {
        return "document.event." + action.name().toLowerCase();
    }
}
