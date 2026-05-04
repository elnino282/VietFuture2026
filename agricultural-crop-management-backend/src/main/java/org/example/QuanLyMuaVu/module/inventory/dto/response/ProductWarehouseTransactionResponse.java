package org.example.QuanLyMuaVu.module.inventory.dto.response;

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
public class ProductWarehouseTransactionResponse {

    Integer id;
    Integer lotId;
    String lotCode;
    String transactionType;
    BigDecimal quantity;
    String unit;
    BigDecimal resultingOnHand;
    String referenceType;
    String referenceId;
    String note;
    Long createdBy;
    String createdByName;
    LocalDateTime createdAt;
}

