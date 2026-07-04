package org.example.season.repository;

import org.example.season.entity.WorkTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkTeamRepository extends JpaRepository<WorkTeam, Long> {
    List<WorkTeam> findBySeasonId(Long seasonId);
}
