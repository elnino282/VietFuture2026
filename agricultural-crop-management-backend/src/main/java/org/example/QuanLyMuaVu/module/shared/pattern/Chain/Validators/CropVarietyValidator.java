package org.example.QuanLyMuaVu.module.shared.pattern.Chain.Validators;

import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.shared.pattern.Chain.SeasonValidationContext;
import org.example.QuanLyMuaVu.module.shared.pattern.Chain.ValidationHandler;
import org.example.QuanLyMuaVu.module.cropcatalog.port.CropCatalogQueryPort;
import org.springframework.stereotype.Component;

/**
 * Chain of Responsibility: org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop and org.example.QuanLyMuaVu.module.cropcatalog.entity.Variety Validator.
 * <p>
 * Validates:
 * 1. org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop exists
 * 2. If variety is provided, it belongs to the specified crop
 * <p>
 * Also populates org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop and org.example.QuanLyMuaVu.module.cropcatalog.entity.Variety entities in context for downstream use.
 */
@Component
@RequiredArgsConstructor
public class CropVarietyValidator extends ValidationHandler<SeasonValidationContext> {

    private final CropCatalogQueryPort cropCatalogQueryPort;

    @Override
    protected void doValidate(SeasonValidationContext ctx) {
        if (ctx.getCropId() == null) {
            throw new AppException(ErrorCode.CROP_NOT_FOUND);
        }

        org.example.QuanLyMuaVu.module.cropcatalog.entity.Crop crop = cropCatalogQueryPort.findCropById(ctx.getCropId())
                .orElseThrow(() -> new AppException(ErrorCode.CROP_NOT_FOUND));
        ctx.setCrop(crop);

        // Validate variety if provided
        if (ctx.getVarietyId() != null) {
            org.example.QuanLyMuaVu.module.cropcatalog.entity.Variety variety = cropCatalogQueryPort.findVarietyById(ctx.getVarietyId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

            // Ensure variety belongs to the specified crop
            if (variety.getCrop() == null || !variety.getCrop().getId().equals(crop.getId())) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }

            ctx.setVariety(variety);
        }
    }

    @Override
    public String getValidatorName() {
        return "CropVarietyValidator";
    }
}

