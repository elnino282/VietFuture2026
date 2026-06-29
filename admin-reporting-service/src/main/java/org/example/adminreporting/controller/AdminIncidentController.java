package org.example.adminreporting.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.adminreporting.dto.ApiResponse;
import org.example.adminreporting.dto.PageResponse;
import org.example.adminreporting.dto.response.IncidentResponse;
import org.example.adminreporting.entity.IncidentSummary;
import org.example.adminreporting.entity.SeasonSummary;
import org.example.adminreporting.repository.IncidentSummaryRepository;
import org.example.adminreporting.repository.SeasonSummaryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/incidents")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminIncidentController {

    private final IncidentSummaryRepository incidentSummaryRepository;
    private final SeasonSummaryRepository seasonSummaryRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<IncidentResponse>>> listAllIncidents(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "severity", required = false) String severity,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        log.info("Admin requesting all incidents from reporting service, status={}, severity={}, type={}",
                status, severity, type);

        Pageable pageable = PageRequest.of(page, size, Sort.by("incidentId").descending());
        Page<IncidentSummary> incidentPage = status != null && !status.isBlank()
                ? incidentSummaryRepository.findByStatus(status, pageable)
                : incidentSummaryRepository.findAll(pageable);

        // Pre-fetch season names to avoid N+1 queries
        List<Integer> seasonIds = incidentPage.getContent().stream().map(IncidentSummary::getSeasonId).distinct().toList();
        Map<Integer, String> seasonNameMap = seasonSummaryRepository.findAllById(seasonIds).stream()
                .collect(Collectors.toMap(SeasonSummary::getSeasonId, SeasonSummary::getSeasonName));

        Page<IncidentResponse> responsePage = incidentPage.map(incident -> toIncidentResponse(incident, seasonNameMap.get(incident.getSeasonId())));
        PageResponse<IncidentResponse> pageResponse = PageResponse.of(responsePage, responsePage.getContent());

        return ResponseEntity.ok(ApiResponse.success("Incidents retrieved", pageResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IncidentResponse>> getIncidentDetail(@PathVariable Integer id) {
        log.info("Admin requesting incident detail for incident ID: {}", id);
        IncidentSummary incident = incidentSummaryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found with ID: " + id));

        String seasonName = seasonSummaryRepository.findById(incident.getSeasonId())
                .map(SeasonSummary::getSeasonName)
                .orElse("Season " + incident.getSeasonId());

        return ResponseEntity.ok(ApiResponse.success("Incident detail retrieved", toIncidentResponse(incident, seasonName)));
    }

    private IncidentResponse toIncidentResponse(IncidentSummary incident, String seasonName) {
        return IncidentResponse.builder()
                .incidentId(incident.getIncidentId())
                .seasonId(incident.getSeasonId())
                .seasonName(seasonName != null ? seasonName : "Season " + incident.getSeasonId())
                .incidentType("PEST") // Default/Fallback
                .severity("MEDIUM") // Default/Fallback
                .status(incident.getStatus())
                .createdAt(LocalDateTime.now()) // Default/Fallback
                .build();
    }
}
