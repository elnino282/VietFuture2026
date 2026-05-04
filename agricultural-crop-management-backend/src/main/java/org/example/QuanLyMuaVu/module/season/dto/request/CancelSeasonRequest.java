package org.example.QuanLyMuaVu.module.season.dto.request;

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
public class CancelSeasonRequest {
    /**
     * Set to true to force cancellation even if there are harvests.
     * Default is false.
     */
    @Builder.Default
    Boolean forceCancel = false;

    /**
     * Optional reason for cancellation.
     */
    String reason;
}
