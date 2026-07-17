package org.example.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubStandardDispositionRequest {

    @NotNull(message = "KEY_INVALID")
    @DecimalMin(value = "0.0", inclusive = false, message = "KEY_INVALID")
    BigDecimal quantity;

    /**
     * Hướng xử lý: SELL_LIVESTOCK_FEED, COMPOSTING, PROCESSING, DISCARDED, SELL_DISCOUNT
     */
    @NotBlank(message = "KEY_INVALID")
    String disposition;

    /** Tên người mua (nếu bán cho chăn nuôi/chiết khấu) */
    String buyerName;

    /** Liên hệ người mua */
    String buyerContact;

    /** Giá bán mỗi đơn vị (VND/kg) — nullable nếu loại bỏ hoặc ủ phân */
    BigDecimal pricePerUnit;

    /** Ghi chú */
    String note;
}
