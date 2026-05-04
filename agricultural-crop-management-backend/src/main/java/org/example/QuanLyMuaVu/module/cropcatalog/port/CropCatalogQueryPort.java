package org.example.QuanLyMuaVu.module.cropcatalog.port;

import java.util.Optional;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.CropNitrogenReference;
import org.example.QuanLyMuaVu.module.cropcatalog.entity.Variety;

public interface CropCatalogQueryPort {

    Optional<Crop> findCropById(Integer cropId);

    Optional<Variety> findVarietyById(Integer varietyId);

    Optional<CropNitrogenReference> findActiveNitrogenReferenceByCropId(Integer cropId);
}
