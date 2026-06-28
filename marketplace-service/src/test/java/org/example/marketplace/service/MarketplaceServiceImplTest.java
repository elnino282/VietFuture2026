package org.example.marketplace.service;

import org.example.marketplace.client.FarmClient;
import org.example.marketplace.client.IdentityClient;
import org.example.marketplace.client.InventoryClient;
import org.example.marketplace.client.SeasonClient;
import org.example.marketplace.dto.client.FarmSummaryDto;
import org.example.marketplace.dto.client.SeasonDetailDto;
import org.example.marketplace.dto.response.*;
import org.example.marketplace.entity.MarketplaceProduct;
import org.example.marketplace.model.MarketplaceProductStatus;
import org.example.marketplace.repository.MarketplaceProductRepository;
import org.example.marketplace.shared.security.CurrentUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketplaceServiceImpl Tests")
class MarketplaceServiceImplTest {

    @Mock
    private MarketplaceProductRepository marketplaceProductRepository;
    @Mock
    private FarmClient farmClient;
    @Mock
    private SeasonClient seasonClient;
    @Mock
    private InventoryClient inventoryClient;
    @Mock
    private IdentityClient identityClient;
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private MarketplaceServiceImpl marketplaceService;

    @Test
    @DisplayName("Should successfully retrieve farmer product form options")
    void getFarmerProductFormOptions_shouldReturnMappedOptions() {
        // Given
        Long userId = 1L;
        when(currentUserService.getCurrentUserId()).thenReturn(userId);

        when(farmClient.getFarmIdsByUserId(userId)).thenReturn(List.of(10));
        when(farmClient.getFarmsByIds(List.of(10))).thenReturn(List.of(new FarmSummaryDto(10, "Happy Farm", null, null, null)));

        when(seasonClient.getSeasonIdsByOwnerId(userId)).thenReturn(List.of(100));
        when(seasonClient.getSeasonsByIds(List.of(100))).thenReturn(List.of(new SeasonDetailDto(100, "Spring 2026", null, null, null, null, 10)));

        when(inventoryClient.getLotsBySeasonIds(List.of(100))).thenReturn(List.of(
                new InventoryClient.LotDetailDto(
                        500,
                        "LOT-500",
                        10,
                        100,
                        "Carrot",
                        "Premium",
                        "kg",
                        BigDecimal.TEN,
                        BigDecimal.TEN,
                        "AVAILABLE",
                        "Happy Farm",
                        "Spring 2026",
                        20,
                        "Plot A",
                        30,
                        "Main Warehouse",
                        "Zone A",
                        null,
                        null,
                        "A",
                        "GOOD")
        ));

        MarketplaceProduct linkedProduct = new MarketplaceProduct();
        linkedProduct.setId(888L);
        linkedProduct.setLotId(500);
        linkedProduct.setStatus(MarketplaceProductStatus.PUBLISHED);
        when(marketplaceProductRepository.findByLotIdIn(List.of(500))).thenReturn(List.of(linkedProduct));

        // When
        MarketplaceFarmerProductFormOptionsResponse response = marketplaceService.getFarmerProductFormOptions();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.farms()).hasSize(1);
        assertThat(response.farms().get(0).name()).isEqualTo("Happy Farm");

        assertThat(response.seasons()).hasSize(1);
        assertThat(response.seasons().get(0).seasonName()).isEqualTo("Spring 2026");

        assertThat(response.lots()).hasSize(1);
        MarketplaceFarmerProductFormLotOptionResponse lotOpt = response.lots().get(0);
        assertThat(lotOpt.lotCode()).isEqualTo("LOT-500");
        assertThat(lotOpt.linkedProductId()).isEqualTo(888L);
        assertThat(lotOpt.linkedProductStatus()).isEqualTo(MarketplaceProductStatus.PUBLISHED);
        assertThat(lotOpt.farmName()).isEqualTo("Happy Farm");
        assertThat(lotOpt.seasonName()).isEqualTo("Spring 2026");
    }
}
