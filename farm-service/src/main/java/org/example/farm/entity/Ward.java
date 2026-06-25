package org.example.farm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "wards")
public class Ward {

    @Id
    @Column(name = "Id")
    Integer id;

    @Column(name = "Name", nullable = false, length = 255)
    String name;

    @Column(name = "Slug", nullable = false, length = 255)
    String slug;

    @Column(name = "Type", nullable = false, length = 64)
    String type;

    @Column(name = "NameWithType", nullable = false, length = 512)
    String nameWithType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProvinceId", nullable = false)
    Province province;
}
