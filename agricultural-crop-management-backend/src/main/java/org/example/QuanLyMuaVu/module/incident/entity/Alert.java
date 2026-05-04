package org.example.QuanLyMuaVu.module.incident.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "alerts")
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Integer id;

    @Column(name = "type", nullable = false, length = 50)
    String type;

    @Column(name = "severity", nullable = false, length = 20)
    String severity;

    @Column(name = "status", nullable = false, length = 30)
    String status;

    @Column(name = "farm_id")
    Integer farmId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", insertable = false, updatable = false)
    org.example.QuanLyMuaVu.module.farm.entity.Farm farm;

    @Column(name = "season_id")
    Integer seasonId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", insertable = false, updatable = false)
    org.example.QuanLyMuaVu.module.season.entity.Season season;

    @Column(name = "plot_id")
    Integer plotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plot_id", insertable = false, updatable = false)
    org.example.QuanLyMuaVu.module.farm.entity.Plot plot;

    @Column(name = "crop_id")
    Integer cropId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id", insertable = false, updatable = false)
    org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop crop;

    @Column(name = "title", nullable = false, length = 255)
    String title;

    @Column(name = "message", columnDefinition = "TEXT")
    String message;

    @Column(name = "suggested_action_type", length = 50)
    String suggestedActionType;

    @Column(name = "suggested_action_url", length = 500)
    String suggestedActionUrl;

    @Column(name = "recipient_farmer_ids", columnDefinition = "TEXT")
    String recipientFarmerIds;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "sent_at")
    LocalDateTime sentAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
