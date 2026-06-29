package org.example.adminreporting.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import org.example.adminreporting.config.CustomJwtDecoder;
import org.example.adminreporting.dto.PageResponse;
import org.example.adminreporting.dto.response.AdminInventoryLotDetailResponse;
import org.example.adminreporting.dto.response.AdminInventoryOptionsResponse;
import org.example.adminreporting.dto.response.AdminInventoryRiskLotResponse;
import org.example.adminreporting.service.AdminInventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminInventoryController.class)
class AdminInventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminInventoryService adminInventoryService;

    @MockBean
    private CustomJwtDecoder customJwtDecoder;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getOptions_returnsFarmsAndCategories() throws Exception {
        AdminInventoryOptionsResponse options = AdminInventoryOptionsResponse.builder()
                .categories(List.of("EXPIRED", "EXPIRING_SOON"))
                .farms(List.of(AdminInventoryOptionsResponse.FarmOption.builder().id(1).name("Farm Alpha").build()))
                .build();
        when(adminInventoryService.getOptions()).thenReturn(options);

        mockMvc.perform(get("/api/v1/admin/inventory/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.categories[0]").value("EXPIRED"))
                .andExpect(jsonPath("$.result.farms[0].name").value("Farm Alpha"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listRiskLots_returnsPagedLots() throws Exception {
        AdminInventoryRiskLotResponse lot = AdminInventoryRiskLotResponse.builder()
                .lotId(10)
                .lotCode("LOT-10")
                .itemName("Lot 10")
                .farmId(1)
                .farmName("Farm Alpha")
                .onHand(10.0)
                .build();

        PageResponse<AdminInventoryRiskLotResponse> response = new PageResponse<>();
        response.setItems(List.of(lot));
        response.setPage(0);
        response.setSize(20);
        response.setTotalElements(1);

        when(adminInventoryService.listRiskLots(
                eq(1), eq(null), eq("EXPIRED"), eq(null), eq(null), eq(null), eq(null), eq(null), eq(0), eq(20)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/inventory/lots")
                .param("farmId", "1")
                .param("status", "EXPIRED")
                .param("page", "0")
                .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].lotId").value(10))
                .andExpect(jsonPath("$.result.items[0].farmName").value("Farm Alpha"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getLotDetail_returnsDetail() throws Exception {
        AdminInventoryLotDetailResponse detail = AdminInventoryLotDetailResponse.builder()
                .lotId(10)
                .lotCode("LOT-10")
                .balances(List.of(AdminInventoryLotDetailResponse.BalanceRow.builder()
                        .farmName("Farm Alpha")
                        .build()))
                .build();
        when(adminInventoryService.getLotDetail(10)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/admin/inventory/lots/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.lotId").value(10))
                .andExpect(jsonPath("$.result.balances[0].farmName").value("Farm Alpha"));
    }
}
