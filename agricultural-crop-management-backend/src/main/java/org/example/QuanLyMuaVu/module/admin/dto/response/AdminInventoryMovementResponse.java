package org.example.QuanLyMuaVu.module.admin.dto.response;

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
public class AdminInventoryMovementResponse {
    Integer movementId;
    String movementType;
    Double quantity;
    LocalDateTime movementDate;
    String reference;
    String note;
}
