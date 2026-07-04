package org.example.season.service;

import lombok.RequiredArgsConstructor;
import org.example.season.entity.WorkTeam;
import org.example.season.entity.WorkTeamMember;
import org.example.season.enums.TeamRole;
import org.example.season.repository.WorkTeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamManagementService {

    private final WorkTeamRepository workTeamRepository;

    @Transactional
    public WorkTeam createWorkTeam(Long seasonId, String teamName, Long leaderId, List<Long> memberIds) {
        WorkTeam team = WorkTeam.builder()
                .seasonId(seasonId)
                .teamName(teamName)
                .teamLeaderUserId(leaderId)
                .build();

        List<WorkTeamMember> members = memberIds.stream()
                .map(memberId -> WorkTeamMember.builder()
                        .workTeam(team)
                        .employeeUserId(memberId)
                        .role(memberId.equals(leaderId) ? TeamRole.LEADER : TeamRole.MEMBER)
                        .build())
                .collect(Collectors.toList());

        team.setMembers(members);
        return workTeamRepository.save(team);
    }

    @Transactional(readOnly = true)
    public List<WorkTeam> getWorkTeamsBySeason(Long seasonId) {
        return workTeamRepository.findBySeasonId(seasonId);
    }
}
