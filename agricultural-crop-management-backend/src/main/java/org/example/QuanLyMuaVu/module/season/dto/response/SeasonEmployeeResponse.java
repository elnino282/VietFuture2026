package org.example.QuanLyMuaVu.module.season.dto.response;

import java.math.BigDecimal;
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
public class SeasonEmployeeResponse {
    Integer id;
    Integer seasonId;
    String seasonName;
    Long employeeUserId;
    String employeeUsername;
    String employeeName;
    String employeeEmail;
    BigDecimal wagePerTask;
    Boolean active;
    LocalDateTime createdAt;
}

