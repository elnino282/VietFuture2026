package org.example.adminreporting.dto.response;

import java.time.LocalDateTime;
import lombok.*;
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
