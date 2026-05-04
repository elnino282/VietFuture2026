package org.example.QuanLyMuaVu.module.cropcatalog.service;

import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.CropNitrogenReference;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.Variety;
import org.example.QuanLyMuaVu.module.cropcatalog.port.CropCatalogQueryPort;
import org.example.QuanLyMuaVu.module.cropcatalog.repository.CropNitrogenReferenceRepository;
import org.example.QuanLyMuaVu.module.cropcatalog.repository.CropRepository;
import org.example.QuanLyMuaVu.module.cropcatalog.repository.VarietyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class CropCatalogQueryService implements CropCatalogQueryPort {

    CropRepository cropRepository;
    VarietyRepository varietyRepository;
    CropNitrogenReferenceRepository cropNitrogenReferenceRepository;

    @Override
    public Optional<Crop> findCropById(Integer cropId) {
        if (cropId == null) {
            return Optional.empty();
        }
        return cropRepository.findById(cropId);
    }

    @Override
    public Optional<Variety> findVarietyById(Integer varietyId) {
        if (varietyId == null) {
            return Optional.empty();
        }
        return varietyRepository.findById(varietyId);
    }

    @Override
    public Optional<CropNitrogenReference> findActiveNitrogenReferenceByCropId(Integer cropId) {
        if (cropId == null) {
            return Optional.empty();
        }
        return cropNitrogenReferenceRepository.findFirstByCrop_IdAndActiveTrue(cropId);
    }
}
