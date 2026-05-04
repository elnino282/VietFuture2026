package org.example.QuanLyMuaVu.module.season.dto.request;

import java.time.LocalDate;
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
public class StartSeasonRequest {
    /**
     * Optional actual start date. If not provided, will use current date.
     */
    LocalDate actualStartDate;
}
