package org.example.QuanLyMuaVu.module.ai.dto.request;

import jakarta.validation.constraints.Size;
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
public class SeasonCostOptimizationSuggestionRequest {

    @Size(max = 2000, message = "KEY_INVALID")
    String question;

    @Size(max = 4000, message = "KEY_INVALID")
    String additionalNote;

    Boolean includeInventory;

    @Size(max = 32, message = "KEY_INVALID")
    String locale;
}
