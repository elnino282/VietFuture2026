package org.example.QuanLyMuaVu.module.admin.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Request body for Admin to update a Task.
 * Used for admin intervention features like task reassignment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminTaskUpdateRequest {

    /**
     * New status for the task.
     * Valid values: PENDING, IN_PROGRESS, DONE, CANCELLED
     */
    String status;

    /**
     * New user ID to reassign the task to.
     * Must be the farm owner.
     */
    Long userId;

    /**
     * Optional notes for the update.
     */
    String notes;
}
