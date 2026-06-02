package org.example.QuanLyMuaVu.module.marketplace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.example.QuanLyMuaVu.Config.AppProperties;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceProductImageUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class MarketplaceProductImageStorageServiceTest {

    @TempDir
    Path tempDir;

    MarketplaceProductImageStorageService service;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getMarketplace().getStorage().setProductImageRoot(tempDir.toString());
        service = new MarketplaceProductImageStorageService(appProperties);
    }

    @Test
    void storeProductImage_StoresJpegAndReturnsPublicUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "durian.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3, 4});

        MarketplaceProductImageUploadResponse response =
                service.storeProductImage(file, "http://localhost:8080");

        assertThat(response.url()).startsWith("http://localhost:8080/api/v1/marketplace/product-images/");
        assertThat(response.fileName()).endsWith(".jpg");
        assertThat(response.contentType()).isEqualTo("image/jpeg");
        assertThat(response.size()).isEqualTo(4L);

        MarketplaceProductImageStorageService.StoredProductImage stored =
                service.loadProductImage(response.fileName());
        assertThat(stored.contentType()).isEqualTo("image/jpeg");
        assertThat(stored.size()).isEqualTo(4L);
        assertThat(stored.resource().getContentAsByteArray()).containsExactly(1, 2, 3, 4);
    }

    @Test
    void storeProductImage_RejectsUnsupportedType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "durian.txt",
                "text/plain",
                "not an image".getBytes());

        assertThatThrownBy(() -> service.storeProductImage(file, "http://localhost:8080"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MARKETPLACE_PRODUCT_IMAGE_UNSUPPORTED_TYPE);
    }

    @Test
    void storeProductImage_RejectsTooLargeFile() {
        byte[] oversized = new byte[(int) MarketplaceProductImageStorageService.MAX_PRODUCT_IMAGE_SIZE_BYTES + 1];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "durian.png",
                "image/png",
                oversized);

        assertThatThrownBy(() -> service.storeProductImage(file, "http://localhost:8080"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MARKETPLACE_PRODUCT_IMAGE_TOO_LARGE);
    }

    @Test
    void loadProductImage_RejectsPathTraversal() {
        assertThatThrownBy(() -> service.loadProductImage("../secret.jpg"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
