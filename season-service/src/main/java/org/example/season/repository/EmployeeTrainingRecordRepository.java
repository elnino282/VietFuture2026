package org.example.season.repository;

import org.example.season.entity.EmployeeTrainingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeTrainingRecordRepository extends JpaRepository<EmployeeTrainingRecord, Integer> {
    List<EmployeeTrainingRecord> findByUserId(Long userId);
    List<EmployeeTrainingRecord> findByWorkTeamId(Integer workTeamId);
    List<EmployeeTrainingRecord> findByUserIdIn(List<Long> userIds);
}

