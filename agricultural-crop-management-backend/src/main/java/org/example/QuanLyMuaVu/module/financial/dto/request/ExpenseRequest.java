package org.example.QuanLyMuaVu.module.financial.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseRequest {
    Long userId;
    Integer seasonId;
    String itemName;
    BigDecimal unitPrice;
    Integer quantity;
    LocalDate expenseDate;
}
