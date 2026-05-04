package org.example.QuanLyMuaVu.module.inventory.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.example.QuanLyMuaVu.DTO.Common.PageResponse;
import org.example.QuanLyMuaVu.module.inventory.dto.response.SupplierResponse;
import org.example.QuanLyMuaVu.module.inventory.service.SuppliesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SuppliesController.class)
class InventorySuppliesModuleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SuppliesService suppliesService;

    @Test
    @WithMockUser(roles = "FARMER")
    void getSuppliers_returnsPagedSupplierCatalog() throws Exception {
        PageResponse<SupplierResponse> response = new PageResponse<>();
        response.setItems(List.of(
                SupplierResponse.builder().id(1).name("Agri Co-op").build(),
                SupplierResponse.builder().id(2).name("Green Inputs").build()));
        response.setPage(0);
        response.setSize(20);
        response.setTotalElements(2);
        response.setTotalPages(1);

        when(suppliesService.getSuppliers(eq(null), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/supplies/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items", hasSize(2)))
                .andExpect(jsonPath("$.result.items[0].name").value("Agri Co-op"));
    }
}
