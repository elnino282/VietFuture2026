package org.example.farm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.farm.entity.Plot;
import org.example.farm.repository.PlotRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller phục vụ các API nội bộ (inter-service) cho dữ liệu GeoJSON của Plot.
 * Không yêu cầu xác thực FARMER vì được gọi từ season-service qua FeignClient.
 */
@RestController
@RequestMapping("/api/v1/plots")
@RequiredArgsConstructor
public class PlotGeoJsonController {

    private final PlotRepository plotRepository;
    private final ObjectMapper objectMapper;

    @Data
    @Builder
    public static class PlotGeoJsonDto {
        private Long plotId;
        private String plotName;
        private Map<String, Object> boundary;
    }

    /**
     * Bulk API: Nhận danh sách plotIds, trả về Map<plotId, PlotGeoJsonDto>.
     * Giải quyết N+1 HTTP call khi season-service cần lấy GeoJSON cho nhiều plot cùng lúc.
     */
    @PostMapping("/bulk-geojson")
    public ResponseEntity<Map<Long, PlotGeoJsonDto>> getBulkPlotGeoJson(@RequestBody List<Long> plotIds) {
        if (plotIds == null || plotIds.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyMap());
        }

        List<Integer> intIds = plotIds.stream()
                .map(Long::intValue)
                .toList();

        List<Plot> plots = plotRepository.findAllById(intIds);

        Map<Long, PlotGeoJsonDto> result = new HashMap<>();
        for (Plot plot : plots) {
            Map<String, Object> boundary = parseBoundaryGeoJson(plot.getBoundaryGeoJson());
            result.put(Long.valueOf(plot.getId()), PlotGeoJsonDto.builder()
                    .plotId(Long.valueOf(plot.getId()))
                    .plotName(plot.getPlotName())
                    .boundary(boundary)
                    .build());
        }
        return ResponseEntity.ok(result);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseBoundaryGeoJson(String boundaryGeoJson) {
        if (boundaryGeoJson == null || boundaryGeoJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(boundaryGeoJson, Map.class);
        } catch (Exception ignored) {
            return null;
        }
    }
}
