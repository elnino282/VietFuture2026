package org.example.QuanLyMuaVu.module.inventory.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = InventoryController.class)
@Import(InventoryControllerSecurityTest.MethodSecurityTestConfig.class)
class InventoryControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryService inventoryService;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Test
    void recordAdjustment_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/movements")
                .with(csrf())
                .contentType("application/json")
                .content("""
                        {
                          "supplyLotId": 1,
                          "warehouseId": 1,
                          "movementType": "ADJUST",
                          "quantity": 3,
                          "note": "stock count correction"
                        }
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "BUYER")
    void recordAdjustment_withNonFarmerRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/movements")
                .with(csrf())
                .contentType("application/json")
                .content("""
                        {
                          "supplyLotId": 1,
                          "warehouseId": 1,
                          "movementType": "ADJUST",
                          "quantity": 3,
                          "note": "stock count correction"
                        }
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FARMER")
    void recordAdjustment_whenNotOwner_returnsForbidden() throws Exception {
        when(inventoryService.recordMovement(any()))
                .thenThrow(new AppException(ErrorCode.FORBIDDEN));

        mockMvc.perform(post("/api/v1/inventory/movements")
                .with(csrf())
                .contentType("application/json")
                .content("""
                        {
                          "supplyLotId": 1,
                          "warehouseId": 1,
                          "movementType": "ADJUST",
                          "quantity": 3,
                          "note": "stock count correction"
                        }
                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ERR_FORBIDDEN"));
    }
}
