package org.example.season.repository;

import java.util.List;
import java.util.Optional;
import org.example.season.entity.SeasonEmployee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SeasonEmployeeRepository extends JpaRepository<SeasonEmployee, Integer> {

    Optional<SeasonEmployee> findBySeasonIdAndEmployeeUserId(Integer seasonId, Long employeeUserId);

    boolean existsBySeasonIdAndEmployeeUserId(Integer seasonId, Long employeeUserId);

    List<SeasonEmployee> findAllBySeasonIdAndActiveTrue(Integer seasonId);

    List<SeasonEmployee> findAllByEmployeeUserIdAndActiveTrue(Long employeeUserId);

    @Query(value = """
            SELECT se.* FROM season_employees se
            JOIN identity_db.users u ON se.employee_user_id = u.user_id
            WHERE se.season_id = :seasonId
            AND (:keyword IS NULL OR :keyword = ''
            OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.full_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """,
            countQuery = """
            SELECT COUNT(*) FROM season_employees se
            JOIN identity_db.users u ON se.employee_user_id = u.user_id
            WHERE se.season_id = :seasonId
            AND (:keyword IS NULL OR :keyword = ''
            OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.full_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """,
            nativeQuery = true)
    Page<SeasonEmployee> searchBySeasonAndKeyword(@Param("seasonId") Integer seasonId,
            @Param("keyword") String keyword,
            Pageable pageable);
}
