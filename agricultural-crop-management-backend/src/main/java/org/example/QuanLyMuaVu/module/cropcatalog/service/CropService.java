package org.example.QuanLyMuaVu.module.cropcatalog.service;

import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.cropcatalog.dto.request.CropRequest;
import org.example.QuanLyMuaVu.module.cropcatalog.dto.response.CropResponse;
import org.example.QuanLyMuaVu.module.cropcatalog.dto.response.VarietyResponse;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop;
import org.example.QuanLyMuaVu.module.cropcatalog.mapper.CropMapper;
import org.example.QuanLyMuaVu.module.cropcatalog.mapper.VarietyMapper;
import org.example.QuanLyMuaVu.module.cropcatalog.repository.CropRepository;
import org.example.QuanLyMuaVu.module.cropcatalog.repository.VarietyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class CropService {
    CropRepository cropRepository;
    CropMapper cropMapper;
    VarietyRepository varietyRepository;
    VarietyMapper varietyMapper;

    public CropResponse create(CropRequest request) {
        if (cropRepository.existsByCropNameIgnoreCase(request.getCropName())) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }
        Crop crop = cropMapper.toEntity(request);
        return cropMapper.toResponse(cropRepository.save(crop));
    }

    public List<CropResponse> getAll() {
        return cropRepository.findAll().stream().map(cropMapper::toResponse).toList();
    }

    public CropResponse getById(Integer id) {
        return cropRepository.findById(id)
                .map(cropMapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.CROP_NOT_FOUND));
    }

    public CropResponse update(Integer id, CropRequest request) {
        Crop crop = cropRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CROP_NOT_FOUND));

        if (!crop.getCropName().equalsIgnoreCase(request.getCropName())
                && cropRepository.existsByCropNameIgnoreCase(request.getCropName())) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE);
        }

        cropMapper.update(crop, request);
        return cropMapper.toResponse(cropRepository.save(crop));
    }

    public void delete(Integer id) {
        Crop crop = cropRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CROP_NOT_FOUND));
        cropRepository.delete(crop);
    }

    public List<VarietyResponse> getVarietiesByCropId(Integer cropId) {
        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new AppException(ErrorCode.CROP_NOT_FOUND));
        return varietyRepository.findAllByCrop(crop)
                .stream()
                .map(varietyMapper::toResponse)
                .toList();
    }
}


