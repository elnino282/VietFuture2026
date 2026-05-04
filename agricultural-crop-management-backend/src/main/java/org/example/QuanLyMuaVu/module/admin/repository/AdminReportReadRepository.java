package org.example.QuanLyMuaVu.module.admin.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.module.admin.dto.request.AdminReportFilter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdminReportReadRepository {

    private static final RowMapper<SeasonFinancialRow> SEASON_FINANCIAL_ROW_MAPPER = (rs, rowNum) -> SeasonFinancialRow.builder()
            .seasonId(getInteger(rs, "seasonId"))
            .seasonName(rs.getString("seasonName"))
            .startDate(getLocalDate(rs, "startDate"))
            .cropId(getInteger(rs, "cropId"))
            .cropName(rs.getString("cropName"))
            .varietyId(getInteger(rs, "varietyId"))
            .varietyName(rs.getString("varietyName"))
            .plotId(getInteger(rs, "plotId"))
            .plotName(rs.getString("plotName"))
            .farmId(getInteger(rs, "farmId"))
            .farmName(rs.getString("farmName"))
            .expectedYieldKg(getBigDecimal(rs, "expectedYieldKg"))
            .actualYieldKg(getBigDecimal(rs, "actualYieldKg"))
            .harvestQuantityKg(getBigDecimal(rs, "harvestQuantityKg"))
            .harvestRevenue(getBigDecimal(rs, "harvestRevenue"))
            .harvestCount(getLong(rs, "harvestCount"))
            .totalExpense(getBigDecimal(rs, "totalExpense"))
            .build();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<SeasonFinancialRow> findSeasonFinancialRows(AdminReportFilter filter) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", filter.getEffectiveFromDate())
                .addValue("to", filter.getEffectiveToDate())
                .addValue("cropId", filter.getCropId())
                .addValue("farmId", filter.getFarmId())
                .addValue("plotId", filter.getPlotId())
                .addValue("varietyId", filter.getVarietyId())
                .addValue("areaMinHa", filter.getAreaMinHa())
                .addValue("areaMaxHa", filter.getAreaMaxHa());

        StringBuilder sql = new StringBuilder("""
                        select s.season_id as seasonId,
                               s.season_name as seasonName,
                               s.start_date as startDate,
                               c.crop_id as cropId,
                               c.crop_name as cropName,
                               v.id as varietyId,
                               v.name as varietyName,
                               p.plot_id as plotId,
                               p.plot_name as plotName,
                               f.farm_id as farmId,
                               f.farm_name as farmName,
                               s.expected_yield_kg as expectedYieldKg,
                               s.actual_yield_kg as actualYieldKg,
                               coalesce(h.totalQuantity, 0) as harvestQuantityKg,
                               coalesce(h.totalRevenue, 0) as harvestRevenue,
                               coalesce(h.harvestCount, 0) as harvestCount,
                               coalesce(e.totalExpense, 0) as totalExpense
                        from seasons s
                        join plots p on p.plot_id = s.plot_id
                        join farms f on f.farm_id = p.farm_id
                        left join crops c on c.crop_id = s.crop_id
                        left join varieties v on v.id = s.variety_id
                        left join (
                            select h.season_id as seasonId,
                                   sum(coalesce(h.quantity, 0)) as totalQuantity,
                                   sum(coalesce(h.quantity, 0) * coalesce(h.unit, 0)) as totalRevenue,
                                   count(*) as harvestCount
                            from harvests h
                            group by h.season_id
                        ) h on h.seasonId = s.season_id
                        left join (
                            select e.season_id as seasonId,
                                   sum(coalesce(e.total_cost, 0)) as totalExpense
                            from expenses e
                            group by e.season_id
                        ) e on e.seasonId = s.season_id
                        where (:from is null or s.start_date >= :from)
                          and (:to is null or s.start_date < :to)
                          and (:cropId is null or s.crop_id = :cropId)
                          and (:farmId is null or p.farm_id = :farmId)
                          and (:plotId is null or p.plot_id = :plotId)
                          and (:areaMinHa is null or p.area >= :areaMinHa)
                          and (:areaMaxHa is null or p.area <= :areaMaxHa)
                          and (:varietyId is null or s.variety_id = :varietyId)
                        order by s.start_date desc, s.season_id desc
                        """);

        if (filter.hasPagination()) {
            params.addValue("limit", filter.getSafeSize());
            params.addValue("offset", filter.getSafeOffset());
            sql.append(" limit :limit offset :offset");
        }

        return jdbcTemplate.query(sql.toString(), params, SEASON_FINANCIAL_ROW_MAPPER);
    }

    private static Integer getInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private static Long getLong(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    private static BigDecimal getBigDecimal(ResultSet rs, String columnName) throws SQLException {
        return rs.getBigDecimal(columnName);
    }

    private static LocalDate getLocalDate(ResultSet rs, String columnName) throws SQLException {
        java.sql.Date value = rs.getDate(columnName);
        return value != null ? value.toLocalDate() : null;
    }

    @lombok.Builder
    @lombok.Value
    public static class SeasonFinancialRow {
        Integer seasonId;
        String seasonName;
        LocalDate startDate;
        Integer cropId;
        String cropName;
        Integer varietyId;
        String varietyName;
        Integer plotId;
        String plotName;
        Integer farmId;
        String farmName;
        BigDecimal expectedYieldKg;
        BigDecimal actualYieldKg;
        BigDecimal harvestQuantityKg;
        BigDecimal harvestRevenue;
        Long harvestCount;
        BigDecimal totalExpense;
    }
}
