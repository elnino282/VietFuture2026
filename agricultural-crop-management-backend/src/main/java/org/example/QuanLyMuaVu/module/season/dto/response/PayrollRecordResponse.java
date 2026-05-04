package org.example.QuanLyMuaVu.module.season.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
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
public class PayrollRecordResponse {
    Integer id;
    Integer seasonId;
    String seasonName;
    Long employeeUserId;
    String employeeName;
    LocalDate periodStart;
    LocalDate periodEnd;
    Integer totalAssignedTasks;
    Integer totalCompletedTasks;
    BigDecimal wagePerTask;
    BigDecimal totalAmount;
    LocalDateTime generatedAt;
    String note;
}

