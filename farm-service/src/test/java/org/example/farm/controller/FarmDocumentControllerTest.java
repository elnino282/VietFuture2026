package org.example.farm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.farm.config.TestSecurityConfig;
import org.example.farm.dto.request.FarmDocumentCreateRequest;
import org.example.farm.service.FarmStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class FarmDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FarmStorageService storageService;

    @Test
    public void testFarmDocumentsWorkflow() throws Exception {
        // 1. Test POST /api/v1/farms/1/documents/upload (Multipart Upload)
        String mockFileUrl = "http://localhost:9000/farm-documents/user-1/test-file.pdf";
        when(storageService.storeDocument(any(), any())).thenReturn(mockFileUrl);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-file.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "Hello, World!".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/farms/1/documents/upload")
                        .file(file)
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.result").value(mockFileUrl));

        // 2. Test POST /api/v1/farms/1/documents (Create Document)
        FarmDocumentCreateRequest createRequest = new FarmDocumentCreateRequest(
                "Giấy Chứng Nhận Đất",
                "LAND_CERTIFICATE",
                "Mô tả giấy phép sử dụng đất",
                mockFileUrl,
                LocalDate.now(),
                LocalDate.now().plusDays(15) // expiring in 15 days
        );

        mockMvc.perform(post("/api/v1/farms/1/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.result.title").value("Giấy Chứng Nhận Đất"))
                .andExpect(jsonPath("$.result.documentType").value("LAND_CERTIFICATE"))
                .andExpect(jsonPath("$.result.isExpiringSoon").value(true));

        // 3. Test GET /api/v1/farms/1/documents (List Documents)
        mockMvc.perform(get("/api/v1/farms/1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.result[0].title").value("Giấy Chứng Nhận Đất"));

        // 4. Test GET /api/v1/farms/1/documents/expiring (Get Expiring Docs)
        mockMvc.perform(get("/api/v1/farms/1/documents/expiring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.result[0].title").value("Giấy Chứng Nhận Đất"));

        // 5. Test DELETE /api/v1/farms/1/documents/{id}
        mockMvc.perform(delete("/api/v1/farms/1/documents/1")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        // List should now be empty
        mockMvc.perform(get("/api/v1/farms/1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isEmpty());
    }
}
