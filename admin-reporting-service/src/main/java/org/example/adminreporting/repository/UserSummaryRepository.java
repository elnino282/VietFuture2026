package org.example.adminreporting.repository;

import java.util.List;
import org.example.adminreporting.dto.response.DashboardStatsDTO.UserRoleCount;
import org.example.adminreporting.dto.response.DashboardStatsDTO.UserStatusCount;
import org.example.adminreporting.entity.UserSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSummaryRepository extends JpaRepository<UserSummary, Long> {
    @Query("SELECT new org.example.adminreporting.dto.response.DashboardStatsDTO$UserRoleCount(u.roleCode, COUNT(u)) FROM UserSummary u GROUP BY u.roleCode")
    List<UserRoleCount> countUsersByRole();

    @Query("SELECT new org.example.adminreporting.dto.response.DashboardStatsDTO$UserStatusCount(u.status, COUNT(u)) FROM UserSummary u GROUP BY u.status")
    List<UserStatusCount> countUsersByStatus();

    Page<UserSummary> findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username, String fullName, String email, Pageable pageable);

    @Query("SELECT DISTINCT u.roleCode FROM UserSummary u WHERE u.roleCode IS NOT NULL")
    List<String> findDistinctRoleCodes();

    long countByStatus(String status);
}
