package org.example.QuanLyMuaVu.module.admin.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.Enums.TaskStatus;
import org.example.QuanLyMuaVu.module.admin.dto.response.DashboardStatsDTO;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdminDashboardReadRepository {

    private static final RowMapper<DashboardStatsDTO.UserRoleCount> USER_ROLE_COUNT_ROW_MAPPER = (rs, rowNum) ->
            DashboardStatsDTO.UserRoleCount.builder()
                    .role(rs.getString("role"))
                    .total(getLong(rs, "total"))
                    .build();

    private static final RowMapper<DashboardStatsDTO.UserStatusCount> USER_STATUS_COUNT_ROW_MAPPER = (rs, rowNum) ->
            DashboardStatsDTO.UserStatusCount.builder()
                    .status(rs.getString("status"))
                    .total(getLong(rs, "total"))
                    .build();

    private static final RowMapper<DashboardStatsDTO.SeasonStatusCount> SEASON_STATUS_COUNT_ROW_MAPPER = (rs, rowNum) ->
            DashboardStatsDTO.SeasonStatusCount.builder()
                    .status(rs.getString("status"))
                    .total(getLong(rs, "total"))
                    .build();

    private static final RowMapper<DashboardStatsDTO.RiskySeason> RISKY_SEASON_ROW_MAPPER = (rs, rowNum) ->
            DashboardStatsDTO.RiskySeason.builder()
                    .seasonId(rs.getInt("seasonId"))
                    .seasonName(rs.getString("seasonName"))
                    .farmName(rs.getString("farmName"))
                    .plotName(rs.getString("plotName"))
                    .status(rs.getString("status"))
                    .incidentCount(getLong(rs, "incidentCount"))
                    .overdueTaskCount(getLong(rs, "overdueTaskCount"))
                    .riskScore(getLong(rs, "riskScore"))
                    .build();

    private static final RowMapper<DashboardStatsDTO.InventoryHealth> INVENTORY_HEALTH_ROW_MAPPER = (rs, rowNum) ->
            DashboardStatsDTO.InventoryHealth.builder()
                    .farmId(rs.getInt("farmId"))
                    .farmName(rs.getString("farmName"))
                    .expiredCount(getLong(rs, "expiredCount"))
                    .expiringSoonCount(getLong(rs, "expiringSoonCount"))
                    .totalAtRisk(getLong(rs, "totalAtRisk"))
                    .build();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<DashboardStatsDTO.UserRoleCount> countUsersByRole() {
        return jdbcTemplate.query(
                """
                        select r.role_code as role, count(u.user_id) as total
                        from users u
                        join user_roles ur on ur.user_id = u.user_id
                        join roles r on r.role_id = ur.role_id
                        group by r.role_code
                        order by r.role_code
                        """,
                USER_ROLE_COUNT_ROW_MAPPER);
    }

    public List<DashboardStatsDTO.UserStatusCount> countUsersByStatus() {
        return jdbcTemplate.query(
                """
                        select u.status as status, count(u.user_id) as total
                        from users u
                        group by u.status
                        order by u.status
                        """,
                USER_STATUS_COUNT_ROW_MAPPER);
    }

    public List<DashboardStatsDTO.SeasonStatusCount> countSeasonsByStatus() {
        return jdbcTemplate.query(
                """
                        select s.status as status, count(s.season_id) as total
                        from seasons s
                        group by s.status
                        order by s.status
                        """,
                SEASON_STATUS_COUNT_ROW_MAPPER);
    }

    public List<DashboardStatsDTO.RiskySeason> findRiskySeasons(TaskStatus overdueStatus, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("overdueStatus", overdueStatus != null ? overdueStatus.name() : TaskStatus.OVERDUE.name())
                .addValue("limit", limit);
        return jdbcTemplate.query(
                """
                        select s.season_id as seasonId,
                               s.season_name as seasonName,
                               f.farm_name as farmName,
                               p.plot_name as plotName,
                               s.status as status,
                               coalesce(incident_agg.incident_count, 0) as incidentCount,
                               coalesce(task_agg.overdue_task_count, 0) as overdueTaskCount,
                               (coalesce(incident_agg.incident_count, 0) + coalesce(task_agg.overdue_task_count, 0)) as riskScore
                        from seasons s
                        join plots p on p.plot_id = s.plot_id
                        join farms f on f.farm_id = p.farm_id
                        left join (
                            select i.season_id, count(distinct i.id) as incident_count
                            from incidents i
                            group by i.season_id
                        ) incident_agg on incident_agg.season_id = s.season_id
                        left join (
                            select t.season_id, count(distinct t.task_id) as overdue_task_count
                            from tasks t
                            where t.status = :overdueStatus
                            group by t.season_id
                        ) task_agg on task_agg.season_id = s.season_id
                        order by riskScore desc, s.season_id desc
                        limit :limit
                        """,
                params,
                RISKY_SEASON_ROW_MAPPER);
    }

    public List<DashboardStatsDTO.InventoryHealth> findInventoryHealth(LocalDate today, LocalDate cutoff) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("today", today)
                .addValue("cutoff", cutoff);
        return jdbcTemplate.query(
                """
                        select lot_farm.farm_id as farmId,
                               f.farm_name as farmName,
                               sum(case when sl.expiry_date < :today then 1 else 0 end) as expiredCount,
                               sum(case when sl.expiry_date >= :today and sl.expiry_date <= :cutoff then 1 else 0 end) as expiringSoonCount,
                               sum(case when sl.expiry_date <= :cutoff then 1 else 0 end) as totalAtRisk
                        from (
                            select distinct sm.supply_lot_id as supply_lot_id,
                                            w.farm_id as farm_id
                            from stock_movements sm
                            join warehouses w on w.id = sm.warehouse_id
                            where sm.supply_lot_id is not null
                              and w.farm_id is not null
                        ) lot_farm
                        join supply_lots sl on sl.id = lot_farm.supply_lot_id
                        join farms f on f.farm_id = lot_farm.farm_id
                        where sl.expiry_date is not null
                          and sl.expiry_date <= :cutoff
                        group by lot_farm.farm_id, f.farm_name
                        order by expiredCount desc, expiringSoonCount desc, lot_farm.farm_id asc
                        """,
                params,
                INVENTORY_HEALTH_ROW_MAPPER);
    }

    private static Long getLong(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }
}
