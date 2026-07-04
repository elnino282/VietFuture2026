package org.example.season.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamProgressSummaryResponse {
    private Long workTeamId;
    private String teamName;
    private Long plotId;
    private Long totalTasks;
    private Long completedTasks;
    private Long inProgressTasks;
    private Double completionRate;

    // Custom constructor cho JPQL (vì JPQL không thể tự map field nếu không có constructor chính xác)
    public TeamProgressSummaryResponse(Long workTeamId, String teamName, Long plotId, Long totalTasks, Long completedTasks, Long inProgressTasks) {
        this.workTeamId = workTeamId;
        this.teamName = teamName;
        this.plotId = plotId;
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
        this.inProgressTasks = inProgressTasks;
        
        // Tính tỷ lệ % hoàn thành
        if (totalTasks != null && totalTasks > 0) {
            long comp = completedTasks != null ? completedTasks : 0;
            this.completionRate = (double) comp / totalTasks * 100.0;
        } else {
            this.completionRate = 0.0;
        }
    }
}
