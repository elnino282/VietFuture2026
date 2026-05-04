package org.example.QuanLyMuaVu.module.cropcatalog.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.cropcatalog.dto.request.VarietyRequest;
import org.example.QuanLyMuaVu.module.cropcatalog.dto.response.VarietyResponse;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.Variety;
import org.example.QuanLyMuaVu.module.cropcatalog.mapper.VarietyMapper;
import org.example.QuanLyMuaVu.module.cropcatalog.repository.CropRepository;
import org.example.QuanLyMuaVu.module.cropcatalog.repository.VarietyRepository;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
@Slf4j
public class VarietyService {

    VarietyRepository varietyRepository;
    CropRepository cropRepository;
    SeasonQueryPort seasonQueryPort;
    VarietyMapper varietyMapper;

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // READ OPERATIONS
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    public VarietyResponse get(Integer id) {
        Variety variety = varietyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        return varietyMapper.toResponse(variety);
    }

    public List<VarietyResponse> listByCrop(Integer cropId) {
        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new AppException(ErrorCode.CROP_NOT_FOUND));
        return varietyRepository.findAllByCrop(crop)
                .stream()
                .map(varietyMapper::toResponse)
                .toList();
    }

    /**
     * Get all varieties (Admin).
     */
    public List<VarietyResponse> getAll() {
        log.info("Fetching all varieties");
        return varietyRepository.findAll()
                .stream()
                .map(varietyMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get varieties by crop ID (Admin alias for listByCrop).
     */
    public List<VarietyResponse> getByCropId(Integer cropId) {
        return listByCrop(cropId);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // ADMIN CRUD OPERATIONS
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    /**
     * Create a new variety (Admin).
     */
    public VarietyResponse create(VarietyRequest request) {
        log.info("Creating variety: name={}, cropId={}", request.getName(), request.getCropId());

        Crop crop = cropRepository.findById(request.getCropId())
                .orElseThrow(() -> new AppException(ErrorCode.CROP_NOT_FOUND));

        Variety variety = varietyMapper.toEntity(request, crop);
        Variety saved = varietyRepository.save(variety);

        log.info("Created variety: id={}", saved.getId());
        return varietyMapper.toResponse(saved);
    }

    /**
     * Update an existing variety (Admin).
     */
    public VarietyResponse update(Integer id, VarietyRequest request) {
        log.info("Updating variety: id={}", id);

        Variety variety = varietyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        Crop crop = cropRepository.findById(request.getCropId())
                .orElseThrow(() -> new AppException(ErrorCode.CROP_NOT_FOUND));

        varietyMapper.update(variety, request, crop);
        Variety saved = varietyRepository.save(variety);

        log.info("Updated variety: id={}", saved.getId());
        return varietyMapper.toResponse(saved);
    }

    /**
     * Delete a variety (Admin).
     * Fails if the variety is referenced in any seasons.
     */
    public void delete(Integer id) {
        log.info("Deleting variety: id={}", id);

        Variety variety = varietyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        // Check if variety is referenced in seasons
        boolean hasSeasons = seasonQueryPort.existsSeasonByVarietyId(id);
        if (hasSeasons) {
            log.warn("Cannot delete variety {} - referenced in seasons", id);
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE); // Using existing error code for conflict
        }

        varietyRepository.delete(variety);
        log.info("Deleted variety: id={}", id);
    }
}


