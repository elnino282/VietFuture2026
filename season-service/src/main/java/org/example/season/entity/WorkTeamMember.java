package org.example.season.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.season.enums.TeamRole;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "work_team_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkTeamMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_team_id", nullable = false)
    @JsonIgnore
    private WorkTeam workTeam;

    @Column(name = "employee_user_id", nullable = false)
    private Long employeeUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private TeamRole role;
}
