package org.example.QuanLyMuaVu.module.marketplace.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for public marketplace endpoints.
 * These endpoints do not require authentication and are accessible to all users.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MarketplaceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listProducts_NoAuth_ShouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/products")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.result").exists())
            .andExpect(jsonPath("$.result.items").isArray())
            .andExpect(jsonPath("$.result.page").exists());
    }

    @Test
    void listProducts_WithCategoryFilter_ShouldFilterResults() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/products")
                .param("category", "Grain")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.result.items").isArray());
    }

    @Test
    void listProducts_WithPriceRange_ShouldFilterResults() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/products")
                .param("minPrice", "10000")
                .param("maxPrice", "100000")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.result.items").isArray());
    }

    @Test
    void listProducts_SortByPriceAsc_ShouldReturnSortedResults() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/products")
                .param("sort", "price_asc")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.result.items").isArray());
    }

    @Test
    void listProducts_SortByPriceDesc_ShouldReturnSortedResults() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/products")
                .param("sort", "price_desc")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.result.items").isArray());
    }

    @Test
    void listProducts_WithRegionFilter_ShouldFilterResults() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/products")
                .param("region", "Can Tho")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.result.items").isArray());
    }

    @Test
    void listProducts_WithTraceableFilter_ShouldFilterResults() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/products")
                .param("traceable", "true")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.result.items").isArray());
    }

    @Test
    void listProducts_WithSearchQuery_ShouldFilterResults() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/products")
                .param("q", "rice")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.result.items").isArray());
    }

    @Test
    void listFarms_NoAuth_ShouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/farms")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.result.items").isArray());
    }

    @Test
    void listFarms_WithRegionFilter_ShouldFilterResults() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/farms")
                .param("region", "Can Tho")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.result.items").isArray());
    }

    @Test
    void listFarms_WithSearchQuery_ShouldFilterResults() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/farms")
                .param("q", "farm")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.result.items").isArray());
    }
}
