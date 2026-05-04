package org.example.QuanLyMuaVu.module.shared.pattern.Chain.Validators;

import lombok.RequiredArgsConstructor;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.shared.pattern.Chain.SeasonValidationContext;
import org.example.QuanLyMuaVu.module.shared.pattern.Chain.ValidationHandler;
import org.example.QuanLyMuaVu.module.farm.port.FarmAccessPort;
import org.example.QuanLyMuaVu.module.farm.port.FarmQueryPort;
import org.springframework.stereotype.Component;

/**
 * Chain of Responsibility: org.example.QuanLyMuaVu.module.farm.entity.Plot Ownership Validator.
 * <p>
 * Validates that:
 * 1. The plot exists
 * 2. Current user has access to the plot (via farm ownership)
 * <p>
 * Also populates the org.example.QuanLyMuaVu.module.farm.entity.Plot entity in the context for downstream validators.
 */
@Component
@RequiredArgsConstructor
public class PlotOwnershipValidator extends ValidationHandler<SeasonValidationContext> {

    private final FarmQueryPort farmQueryPort;
    private final FarmAccessPort farmAccessPort;

    @Override
    protected void doValidate(SeasonValidationContext ctx) {
        if (ctx.getPlotId() == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        org.example.QuanLyMuaVu.module.farm.entity.Plot plot = farmQueryPort.findPlotById(ctx.getPlotId())
                .orElseThrow(() -> new AppException(ErrorCode.PLOT_NOT_FOUND));

        // Verify ownership using existing FarmAccessService
        farmAccessPort.assertCurrentUserCanAccessPlot(plot);

        // Populate context for downstream validators
        ctx.setPlot(plot);
    }

    @Override
    public String getValidatorName() {
        return "PlotOwnershipValidator";
    }
}
