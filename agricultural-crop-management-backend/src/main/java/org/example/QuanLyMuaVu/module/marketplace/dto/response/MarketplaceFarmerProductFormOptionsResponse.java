package org.example.QuanLyMuaVu.module.marketplace.dto.response;

import java.util.List;

public record MarketplaceFarmerProductFormOptionsResponse(
        List<MarketplaceFarmerProductFormFarmOptionResponse> farms,
        List<MarketplaceFarmerProductFormSeasonOptionResponse> seasons,
        List<MarketplaceFarmerProductFormLotOptionResponse> lots) {
}
