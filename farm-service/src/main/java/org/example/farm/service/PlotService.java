package org.example.farm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.farm.config.CurrentUserService;
import org.example.farm.dto.request.PlotRequest;
import org.example.farm.dto.response.PlotResponse;
import org.example.farm.entity.Farm;
import org.example.farm.entity.OutboxEvent;
import org.example.farm.entity.Plot;
import org.example.farm.enums.PlotStatus;
import org.example.farm.event.PlotChangedEvent;
import org.example.farm.event.PlotChangedEvent.Action;
import org.example.farm.exception.AppException;
import org.example.farm.exception.ErrorCode;
import org.example.farm.repository.FarmRepository;
import org.example.farm.repository.OutboxEventRepository;
import org.example.farm.repository.PlotRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlotService {

    private final PlotRepository plotRepository;
    private final FarmRepository farmRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;
    private final RestTemplate restTemplate;

    @Value("${app.backend-url:http://localhost:8080}")
    private String backendUrl;

    @Transactional(readOnly = true)
    public List<PlotResponse> listPlotsForCurrentFarmer() {
        Long userId = currentUserService.getCurrentUserId();
        List<Plot> plots = plotRepository.findAllByFarmUserId(userId);
        return plots.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public PlotResponse createPlotForCurrentFarmer(PlotRequest request) {
        if (request.getFarmId() == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return createPlotForFarm(request.getFarmId(), request);
    }

    @Transactional(readOnly = true)
    public List<PlotResponse> listPlotsByFarm(Integer farmId) {
        Long userId = currentUserService.getCurrentUserId();

        Farm farm = farmRepository.findByIdAndUserId(farmId, userId)
                .orElseThrow(() -> new AccessDeniedException("Access Denied: You do not own this farm."));

        List<Plot> plots = plotRepository.findAllByFarmUserIdAndFarmId(userId, farm.getId());
        return plots.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public PlotResponse createPlotForFarm(Integer farmId, PlotRequest request) {
        Long userId = currentUserService.getCurrentUserId();

        Farm farm = farmRepository.findByIdAndUserId(farmId, userId)
                .orElseThrow(() -> new AccessDeniedException("Access Denied: You do not own this farm."));

        if (!Boolean.TRUE.equals(farm.getActive())) {
            throw new AppException(ErrorCode.FARM_INACTIVE);
        }

        String statusCode = (request.getStatus() != null) ? request.getStatus().getCode() : PlotStatus.IDLE.getCode();

        Plot plot = Plot.builder()
                .farm(farm)
                .createdBy(userId)
                .plotName(request.getPlotName())
                .area(request.getArea())
                .soilType(request.getSoilType())
                .boundaryGeoJson(request.getBoundaryGeoJson())
                .status(statusCode)
                .build();

        Plot savedPlot = plotRepository.save(plot);
        writeOutboxEvent(savedPlot, Action.CREATED);
        return toResponse(savedPlot);
    }

    @Transactional(readOnly = true)
    public PlotResponse getPlotForCurrentFarmer(Integer id) {
        Long userId = currentUserService.getCurrentUserId();
        Plot plot = plotRepository.findByIdAndFarmUserId(id, userId)
                .orElseThrow(() -> new AppException(ErrorCode.PLOT_NOT_FOUND));
        return toResponse(plot);
    }

    @Transactional
    public PlotResponse updatePlotForCurrentFarmer(Integer id, PlotRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        Plot plot = plotRepository.findByIdAndFarmUserId(id, userId)
                .orElseThrow(() -> new AppException(ErrorCode.PLOT_NOT_FOUND));

        if (request.getPlotName() != null) {
            plot.setPlotName(request.getPlotName());
        }
        if (request.getArea() != null) {
            plot.setArea(request.getArea());
        }
        if (request.getSoilType() != null) {
            plot.setSoilType(request.getSoilType());
        }
        if (request.getBoundaryGeoJson() != null) {
            plot.setBoundaryGeoJson(request.getBoundaryGeoJson());
        }
        if (request.getStatus() != null) {
            plot.setStatus(request.getStatus().getCode());
        }

        Plot savedPlot = plotRepository.save(plot);
        writeOutboxEvent(savedPlot, Action.UPDATED);
        return toResponse(savedPlot);
    }

    @Transactional
    public void deletePlotForCurrentFarmer(Integer id) {
        Long userId = currentUserService.getCurrentUserId();
        Plot plot = plotRepository.findByIdAndFarmUserId(id, userId)
                .orElseThrow(() -> new AppException(ErrorCode.PLOT_NOT_FOUND));

        // Check if there are active seasons on this plot via monolith/season-service
        try {
            String url = backendUrl + "/api/v1/public/seasons/exists-active-by-plot/" + id;
            Boolean response = restTemplate.getForObject(url, Boolean.class);
            if (response != null && response) {
                throw new AppException(ErrorCode.PLOT_HAS_ACTIVE_SEASONS);
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to check active seasons via season service: {}", e.getMessage());
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        // Check if there are active tasks on this plot via monolith/season-service
        try {
            String url = backendUrl + "/api/v1/public/seasons/exists-active-tasks-by-plot/" + id;
            Boolean response = restTemplate.getForObject(url, Boolean.class);
            if (response != null && response) {
                throw new AppException(ErrorCode.PLOT_HAS_ACTIVE_TASKS);
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to check active tasks via season service: {}", e.getMessage());
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        writeOutboxEvent(plot, Action.DELETED);
        plotRepository.delete(plot);
    }

    private void writeOutboxEvent(Plot plot, Action action) {
        try {
            PlotChangedEvent eventDto = new PlotChangedEvent(plot, action);
            String jsonPayload = objectMapper.writeValueAsString(eventDto);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("Plot")
                    .aggregateId(String.valueOf(plot.getId()))
                    .eventType(eventDto.getEventType())
                    .payload(jsonPayload)
                    .processed(false)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Saved outbox event for plot: {} (type: {})", plot.getId(), eventDto.getEventType());
        } catch (Exception e) {
            log.error("Failed to write outbox event for plot: {}", plot.getId(), e);
            throw new RuntimeException("Outbox write failed", e);
        }
    }

    private PlotResponse toResponse(Plot plot) {
        if (plot == null) return null;
        return PlotResponse.builder()
                .id(plot.getId())
                .farmId(plot.getFarm() != null ? plot.getFarm().getId() : null)
                .farmName(plot.getFarm() != null ? plot.getFarm().getName() : null)
                .plotName(plot.getPlotName())
                .area(plot.getArea())
                .soilType(plot.getSoilType())
                .boundaryGeoJson(plot.getBoundaryGeoJson())
                .status(PlotStatus.fromCode(plot.getStatus()))
                .build();
    }
}
