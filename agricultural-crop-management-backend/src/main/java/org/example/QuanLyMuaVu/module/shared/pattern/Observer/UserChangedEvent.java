package org.example.QuanLyMuaVu.module.shared.pattern.Observer;

import java.util.List;
import lombok.Getter;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.module.identity.entity.Role;

@Getter
public class UserChangedEvent extends DomainEvent {

    public enum Action {
        CREATED,
        UPDATED,
        DELETED
    }

    private final Action action;
    private final Long userId;
    private final String username;
    private final String email;
    private final String fullName;
    private final String phone;
    private final String status;
    private final List<String> roles;

    public UserChangedEvent(User user, Action action) {
        super("User",
              user != null && user.getId() != null ? user.getId().toString() : "unknown");
        this.action = action;
        this.userId = user != null ? user.getId() : null;
        this.username = user != null ? user.getUsername() : null;
        this.email = user != null ? user.getEmail() : null;
        this.fullName = user != null ? user.getFullName() : null;
        this.phone = user != null ? user.getPhone() : null;
        this.status = user != null && user.getStatus() != null ? user.getStatus().name() : null;
        this.roles = user != null && user.getRoles() != null
                ? user.getRoles().stream().map(Role::getCode).toList()
                : List.of();
    }

    @Override
    public String getEventType() {
        return "identity.event.user." + action.name().toLowerCase();
    }
}
