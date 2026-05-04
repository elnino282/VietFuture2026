package org.example.QuanLyMuaVu.module.season.dto.request;

import jakarta.validation.constraints.NotNull;
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
public class CompleteTaskRequest {
    /**
     * Required actual end date for the task.
     */
    @NotNull(message = "Actual end date is required")
    LocalDate actualEndDate;

    /**
     * Optional completion notes.
     */
    String completionNotes;
}
