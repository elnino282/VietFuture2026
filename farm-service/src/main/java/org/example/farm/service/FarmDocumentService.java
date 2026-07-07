package org.example.farm.service;

import lombok.RequiredArgsConstructor;
import org.example.farm.dto.request.FarmDocumentCreateRequest;
import org.example.farm.dto.response.FarmDocumentResponse;
import org.example.farm.entity.FarmDocument;
import org.example.farm.repository.FarmDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FarmDocumentService {

    private final FarmDocumentRepository repository;

    @Transactional
    public FarmDocumentResponse create(Integer farmId, Long userId,
                                       FarmDocumentCreateRequest req) {
        FarmDocument doc = FarmDocument.builder()
            .farmId(farmId)
            .documentType(req.documentType())
            .title(req.title())
            .description(req.description())
            .fileUrl(req.fileUrl())
            .issuedDate(req.issuedDate())
            .expiryDate(req.expiryDate())
            .verificationStatus("PENDING")
            .createdBy(userId)
            .build();
        return toResponse(repository.save(doc));
    }

    @Transactional(readOnly = true)
    public List<FarmDocumentResponse> getByFarmId(Integer farmId) {
        return repository.findByFarmId(farmId)
            .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FarmDocumentResponse> getExpiringDocuments(Integer farmId) {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(30);
        return repository.findExpiringDocuments(farmId, today, deadline)
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(Integer id) {
        repository.deleteById(id);
    }

    // === Mapping ===
    private FarmDocumentResponse toResponse(FarmDocument d) {
        LocalDate today = LocalDate.now();
        boolean isExpired = d.getExpiryDate() != null && d.getExpiryDate().isBefore(today);
        boolean expiringSoon = d.getExpiryDate() != null
            && !isExpired
            && d.getExpiryDate().isBefore(today.plusDays(30));

        return new FarmDocumentResponse(
            d.getId(), d.getFarmId(), d.getDocumentType(),
            getLabel(d.getDocumentType()), d.getTitle(),
            d.getDescription(), d.getFileUrl(),
            d.getIssuedDate(), d.getExpiryDate(),
            isExpired, expiringSoon,
            d.getVerificationStatus(), null, // verifiedByName
            d.getCreatedAt(), d.getUpdatedAt()
        );
    }

    private String getLabel(String type) {
        return switch (type) {
            case "LAND_CERTIFICATE"      -> "Giấy phép đất";
            case "SOIL_TEST_REPORT"       -> "Báo cáo phân tích đất";
            case "WATER_TEST_REPORT"       -> "Báo cáo phân tích nguồn nước";
            case "PESTICIDE_RECORD"        -> "Hồ sơ thuốc BVTV";
            case "FERTILIZER_RECORD"       -> "Hồ sơ phân bón";
            case "HARVEST_LOG"            -> "Hồ sơ thu hoạch";
            case "INTERNAL_AUDIT"          -> "Biên bản kiểm tra nội bộ";
            case "CERTIFICATE"            -> "Giấy chứng nhận";
            default                       -> "Khác";
        };
    }
}
