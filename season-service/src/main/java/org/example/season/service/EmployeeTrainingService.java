package org.example.season.service;

import lombok.RequiredArgsConstructor;
import org.example.season.dto.request.EmployeeTrainingRecordRequest;
import org.example.season.dto.request.TrainingProgramRequest;
import org.example.season.dto.response.EmployeeTrainingRecordDto;
import org.example.season.dto.response.TrainingProgramDto;
import org.example.season.entity.EmployeeTrainingRecord;
import org.example.season.entity.TrainingProgram;
import org.example.season.entity.WorkTeamMember;
import org.example.season.repository.EmployeeTrainingRecordRepository;
import org.example.season.repository.TrainingProgramRepository;
import org.example.season.repository.WorkTeamMemberRepository;
import org.example.season.repository.WorkTeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeTrainingService {

    private final TrainingProgramRepository trainingProgramRepository;
    private final EmployeeTrainingRecordRepository employeeTrainingRecordRepository;
    private final WorkTeamRepository workTeamRepository;
    private final WorkTeamMemberRepository workTeamMemberRepository;

    @Transactional
    public TrainingProgramDto createTrainingProgram(TrainingProgramRequest req) {
        TrainingProgram program = TrainingProgram.builder()
                .title(req.getTitle())
                .category(req.getCategory())
                .description(req.getDescription())
                .isMandatory(req.getIsMandatory() != null ? req.getIsMandatory() : false)
                .build();
        program = trainingProgramRepository.save(program);
        return toDto(program);
    }

    public List<TrainingProgramDto> getTrainingPrograms(String category) {
        List<TrainingProgram> programs;
        if (category != null && !category.isBlank()) {
            programs = trainingProgramRepository.findByCategory(category);
        } else {
            programs = trainingProgramRepository.findAll();
        }
        return programs.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public EmployeeTrainingRecordDto recordTraining(Long userId, EmployeeTrainingRecordRequest req) {
        TrainingProgram program = trainingProgramRepository.findById(req.getTrainingProgramId())
                .orElseThrow(() -> new IllegalArgumentException(\"Training program not found\"));

        EmployeeTrainingRecord record = EmployeeTrainingRecord.builder()
                .userId(userId)
                .workTeamId(req.getWorkTeamId())
                .trainingProgram(program)
                .trainedAt(req.getTrainedAt())
                .trainerName(req.getTrainerName())
                .evidenceUrls(req.getEvidenceUrls())
                .certifiedUntil(req.getCertifiedUntil())
                .build();
        record = employeeTrainingRecordRepository.save(record);
        return toDto(record);
    }

    public List<EmployeeTrainingRecordDto> getTrainingRecordsForUser(Long userId) {
        return employeeTrainingRecordRepository.findByUserId(userId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Trả về tình trạng đào tạo của tất cả thành viên trong một mùa vụ (theo seasonId).
     * Dành cho API nội bộ hoặc Farmer kiểm tra.
     */
    public Map<Long, List<EmployeeTrainingRecordDto>> getTrainingStatusForSeason(Integer seasonId) {
        // 1. Tìm các WorkTeam thuộc seasonId
        var teams = workTeamRepository.findBySeasonId(seasonId);
        if (teams.isEmpty()) {
            return new HashMap<>();
        }

        // 2. Tìm tất cả userId trong các đội này
        List<Integer> teamIds = teams.stream().map(t -> t.getId()).toList();
        List<WorkTeamMember> members = workTeamMemberRepository.findByWorkTeamIdIn(teamIds);
        List<Long> userIds = members.stream().map(WorkTeamMember::getUserId).distinct().toList();

        if (userIds.isEmpty()) {
            return new HashMap<>();
        }

        // 3. Lấy tất cả records của các userId này
        List<EmployeeTrainingRecord> records = employeeTrainingRecordRepository.findByUserIdIn(userIds);

        // 4. Nhóm theo userId
        Map<Long, List<EmployeeTrainingRecordDto>> result = new HashMap<>();
        for (Long uid : userIds) {
            result.put(uid, records.stream()
                    .filter(r -> r.getUserId().equals(uid))
                    .map(this::toDto)
                    .toList());
        }
        return result;
    }

    private TrainingProgramDto toDto(TrainingProgram p) {
        return new TrainingProgramDto(
                p.getId(), p.getTitle(), p.getCategory(), p.getDescription(), p.getIsMandatory(), p.getCreatedAt()
        );
    }

    private EmployeeTrainingRecordDto toDto(EmployeeTrainingRecord r) {
        return new EmployeeTrainingRecordDto(
                r.getId(), r.getUserId(), r.getWorkTeamId(), toDto(r.getTrainingProgram()),
                r.getTrainedAt(), r.getTrainerName(), r.getEvidenceUrls(),
                r.getCertifiedUntil(), r.getStatus(), r.getCreatedAt(), r.getUpdatedAt()
        );
    }
}

