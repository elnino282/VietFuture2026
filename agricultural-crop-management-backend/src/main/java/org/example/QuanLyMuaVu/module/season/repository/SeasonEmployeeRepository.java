package org.example.QuanLyMuaVu.module.season.repository;


import java.util.List;
import java.util.Optional;
import org.example.QuanLyMuaVu.module.season.entity.SeasonEmployee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SeasonEmployeeRepository extends JpaRepository<SeasonEmployee, Integer> {

    Optional<SeasonEmployee> findBySeason_IdAndEmployee_Id(Integer seasonId, Long employeeId);

    boolean existsBySeason_IdAndEmployee_Id(Integer seasonId, Long employeeId);

    List<SeasonEmployee> findAllBySeason_IdAndActiveTrue(Integer seasonId);

    @Query("SELECT se FROM SeasonEmployee se " +
            "WHERE se.season.id = :seasonId " +
            "AND (:keyword IS NULL OR :keyword = '' " +
            "OR LOWER(se.employee.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(se.employee.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(se.employee.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<SeasonEmployee> searchBySeasonAndKeyword(@Param("seasonId") Integer seasonId,
            @Param("keyword") String keyword,
            Pageable pageable);
}

