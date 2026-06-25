package org.example.farm.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
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
@Table(name = "provinces")
public class Province {

    @Id
    @Column(name = "Id")
    Integer id;

    @Column(name = "Name", nullable = false, length = 128)
    String name;

    @Column(name = "Slug", nullable = false, length = 128)
    String slug;

    @Column(name = "Type", nullable = false, length = 32)
    String type;

    @Column(name = "NameWithType", nullable = false, length = 256)
    String nameWithType;

    @JsonIgnore
    @OneToMany(mappedBy = "province")
    List<Ward> wards;
}
