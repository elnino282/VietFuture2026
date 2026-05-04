package org.example.QuanLyMuaVu.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.QuanLyMuaVu.module.shared.controller.ModuleHealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ModuleHealthController.class)
class ModuleHealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void moduleHealthOverview_returnsAllConfiguredModules() throws Exception {
        mockMvc.perform(get("/api/v1/public/health/modules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", hasSize(10)))
                .andExpect(jsonPath("$.result[0].module").value("ai"))
                .andExpect(jsonPath("$.result[0].status").value("UP"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void moduleHealth_returnsSingleModuleStatus() throws Exception {
        mockMvc.perform(get("/api/v1/public/health/modules/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.module").value("inventory"))
                .andExpect(jsonPath("$.result.status").value("UP"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void moduleHealth_returnsNotFoundForUnknownModule() throws Exception {
        mockMvc.perform(get("/api/v1/public/health/modules/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MODULE_NOT_FOUND"));
    }
}
