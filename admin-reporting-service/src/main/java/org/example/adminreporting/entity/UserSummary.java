package org.example.adminreporting.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admin_user_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummary {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email")
    private String email;

    @Column(name = "status")
    private String status;

    @Column(name = "role_code")
    private String roleCode;
}
