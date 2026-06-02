package org.example.QuanLyMuaVu.module.marketplace.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.QuanLyMuaVu.Config.AppProperties;
import org.example.QuanLyMuaVu.Exception.AppException;
import org.example.QuanLyMuaVu.Exception.ErrorCode;
import org.example.QuanLyMuaVu.module.marketplace.dto.response.MarketplaceProductImageUploadResponse;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MarketplaceProductImageStorageService {

    static final long MAX_PRODUCT_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;
    static final String PRODUCT_IMAGE_PUBLIC_PATH = "/api/v1/marketplace/product-images/";

    private static final DateTimeFormatter STORED_FILE_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");
    private static final Map<String, String> CONTENT_TYPE_BY_EXTENSION = Map.of(
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".png", "image/png",
            ".webp", "image/webp");

    AppProperties appProperties;

    public MarketplaceProductImageUploadResponse storeProductImage(MultipartFile file, String baseUrl) {
        validateProductImage(file);

        String contentType = normalizeContentType(file.getContentType());
        String extension = extensionForContentType(contentType);
        Path rootPath = getRootPath();
        String storedFileName = LocalDateTime.now().format(STORED_FILE_TIMESTAMP_FORMAT)
                + "-"
                + UUID.randomUUID().toString().replace("-", "")
                + extension;
        Path target = rootPath.resolve(storedFileName).normalize();

        try {
            if (!target.startsWith(rootPath)) {
                throw new AppException(ErrorCode.MARKETPLACE_PRODUCT_IMAGE_INVALID);
            }
            Files.createDirectories(rootPath);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        String publicUrl = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path(PRODUCT_IMAGE_PUBLIC_PATH)
                .path(storedFileName)
                .toUriString();
        return new MarketplaceProductImageUploadResponse(publicUrl, storedFileName, contentType, file.getSize());
    }

    public StoredProductImage loadProductImage(String fileName) {
        String safeFileName = normalizeSafeFileName(fileName);
        Path rootPath = getRootPath();
        Path target = rootPath.resolve(safeFileName).normalize();

        if (!target.startsWith(rootPath) || !Files.exists(target) || !Files.isRegularFile(target)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        try {
            Resource resource = new UrlResource(target.toUri());
            String contentType = resolveContentType(safeFileName, target);
            return new StoredProductImage(safeFileName, contentType, Files.size(target), resource);
        } catch (MalformedURLException ex) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateProductImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.MARKETPLACE_PRODUCT_IMAGE_INVALID);
        }
        if (file.getSize() > MAX_PRODUCT_IMAGE_SIZE_BYTES) {
            throw new AppException(ErrorCode.MARKETPLACE_PRODUCT_IMAGE_TOO_LARGE);
        }

        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new AppException(ErrorCode.MARKETPLACE_PRODUCT_IMAGE_UNSUPPORTED_TYPE);
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)
                || !contentType.equals(CONTENT_TYPE_BY_EXTENSION.get(extension))) {
            throw new AppException(ErrorCode.MARKETPLACE_PRODUCT_IMAGE_UNSUPPORTED_TYPE);
        }
    }

    private Path getRootPath() {
        String root = appProperties.getMarketplace().getStorage().getProductImageRoot();
        return Path.of(root).toAbsolutePath().normalize();
    }

    private String normalizeSafeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        String normalized = fileName.trim();
        if (normalized.contains("/") || normalized.contains("\\") || normalized.contains("..")) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return normalized;
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int separatorIndex = contentType.indexOf(';');
        String normalized = separatorIndex >= 0 ? contentType.substring(0, separatorIndex) : contentType;
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    private String extensionForContentType(String contentType) {
        String extension = EXTENSION_BY_CONTENT_TYPE.get(contentType);
        if (extension == null) {
            throw new AppException(ErrorCode.MARKETPLACE_PRODUCT_IMAGE_UNSUPPORTED_TYPE);
        }
        return extension;
    }

    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        String normalized = Path.of(fileName).getFileName().toString();
        int lastDot = normalized.lastIndexOf('.');
        if (lastDot < 0 || lastDot == normalized.length() - 1) {
            return "";
        }
        return normalized.substring(lastDot).toLowerCase(Locale.ROOT);
    }

    private String resolveContentType(String fileName, Path target) throws IOException {
        String probedContentType = normalizeContentType(Files.probeContentType(target));
        if (ALLOWED_CONTENT_TYPES.contains(probedContentType)) {
            return probedContentType;
        }
        String extension = extractExtension(fileName);
        return CONTENT_TYPE_BY_EXTENSION.getOrDefault(extension, "application/octet-stream");
    }

    public record StoredProductImage(
            String fileName,
            String contentType,
            long size,
            Resource resource) {
    }
}
