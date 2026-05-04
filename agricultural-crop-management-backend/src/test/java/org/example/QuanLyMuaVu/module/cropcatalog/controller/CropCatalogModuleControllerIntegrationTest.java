package org.example.QuanLyMuaVu.module.cropcatalog.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.example.QuanLyMuaVu.module.cropcatalog.dto.response.CropResponse;
import org.example.QuanLyMuaVu.module.cropcatalog.dto.response.VarietyResponse;
import org.example.QuanLyMuaVu.module.cropcatalog.service.CropService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CatalogController.class)
class CropCatalogModuleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CropService cropService;

    @Test
    @WithMockUser(roles = "FARMER")
    void getAllCrops_returnsCatalogData() throws Exception {
        when(cropService.getAll()).thenReturn(List.of(
                CropResponse.builder().id(1).cropName("Rice").build(),
                CropResponse.builder().id(2).cropName("Corn").build()));

        mockMvc.perform(get("/api/v1/catalog/crops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(2)))
                .andExpect(jsonPath("$.result[0].cropName").value("Rice"));
    }

    @Test
    @WithMockUser(roles = "FARMER")
    void getVarietiesByCrop_returnsVarieties() throws Exception {
        when(cropService.getVarietiesByCropId(1)).thenReturn(List.of(
                VarietyResponse.builder().id(10).name("ST25").build(),
                VarietyResponse.builder().id(11).name("Jasmine").build()));

        mockMvc.perform(get("/api/v1/catalog/crops/1/varieties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(2)))
                .andExpect(jsonPath("$.result[0].name").value("ST25"));
    }
}
