package org.example.season.repository;

import org.example.season.entity.WorkTeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkTeamMemberRepository extends JpaRepository<WorkTeamMember, Long> {
    List<WorkTeamMember> findByEmployeeUserId(Long employeeUserId);
    List<WorkTeamMember> findByWorkTeamIdIn(List<Long> workTeamIds);
}
