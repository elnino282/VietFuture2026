package org.example.marketplace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.marketplace.client.FarmClient;
import org.example.marketplace.client.SeasonClient;
import org.example.marketplace.dto.client.FarmCertificationDto;
import org.example.marketplace.dto.client.PesticideRecordDto;
import org.example.marketplace.dto.response.ComplianceCheckResponse;
import org.example.marketplace.entity.MarketplaceProduct;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplaceComplianceGateService {

    private final FarmClient farmClient;
    private final SeasonClient seasonClient;
    private final ObjectMapper objectMapper;

    /**
     * Kiểm tra tính tuân thủ của sản phẩm trước khi đưa lên sàn.
     * @param product sản phẩm cần kiểm tra
     * @return ComplianceCheckResponse chứa kết quả và các lý do nếu không đạt.
     */
    public ComplianceCheckResponse checkCompliance(MarketplaceProduct product) {
        List<String> reasons = new ArrayList<>();
        boolean isEligible = true;
        String certSnapshot = null;
        String phiSnapshot = null;

        String claim = product.getComplianceClaim();
        // BR-G-01: Bỏ qua nếu không claim
        if (claim == null || "NONE".equalsIgnoreCase(claim)) {
            return new ComplianceCheckResponse(true, reasons, claim, null, null);
        }

        // 1. Kiểm tra chứng nhận (nếu claim VIETGAP hoặc ORGANIC)
        if ("VIETGAP".equalsIgnoreCase(claim) || "ORGANIC".equalsIgnoreCase(claim)) {
            if (product.getFarmId() == null) {
                isEligible = false;
                reasons.add("Sản phẩm chưa liên kết với nông trại.");
            } else {
                try {
                    String standardCode = "VIETGAP".equalsIgnoreCase(claim) ? "VIETGAP-PLANTING-2024" : "ORGANIC";
                    FarmCertificationDto cert = farmClient.getFarmCertification(product.getFarmId(), standardCode);
                    
                    if (cert == null || !"PUBLISHED".equalsIgnoreCase(cert.status())) {
                        isEligible = false;
                        reasons.add("Chứng nhận " + claim + " chưa được cấp hoặc chưa được công bố (PUBLISHED).");
                        if (cert != null && cert.missingMandatoryEvidenceCount() != null && cert.missingMandatoryEvidenceCount() > 0) {
                            reasons.add("Farm thiếu " + cert.missingMandatoryEvidenceCount() + " bằng chứng bắt buộc.");
                            cert.missingEvidenceItems().forEach(item -> 
                                reasons.add("- " + item.category() + ": " + item.description())
                            );
                        }
                    } else if (cert.expiryDate() != null && cert.expiryDate().isBefore(LocalDate.now())) {
                        isEligible = false;
                        reasons.add("Chứng nhận " + claim + " đã hết hạn vào ngày " + cert.expiryDate() + ".");
                    } else {
                        certSnapshot = objectMapper.writeValueAsString(cert);
                    }
                } catch (Exception e) {
                    log.error("Lỗi khi kiểm tra chứng nhận cho farmId {}: {}", product.getFarmId(), e.getMessage());
                    isEligible = false;
                    reasons.add("Không thể xác minh chứng nhận với hệ thống farm-service.");
                }
            }
        }

        // 2. Kiểm tra an toàn thu hoạch (PHI) nếu có seasonId
        if (product.getSeasonId() != null) {
            try {
                List<PesticideRecordDto> pesticideRecords = seasonClient.getSeasonPesticideRecords(product.getSeasonId());
                if (pesticideRecords != null && !pesticideRecords.isEmpty()) {
                    LocalDate harvestDate = product.getLotHarvestDate() != null ? product.getLotHarvestDate().toLocalDate() : LocalDate.now();
                    boolean hasPhiViolation = false;

                    for (PesticideRecordDto record : pesticideRecords) {
                        LocalDate allowedDate = record.harvestAllowedDate();
                        if (allowedDate == null && record.applicationDate() != null) {
                            allowedDate = record.applicationDate().plusDays(record.phiDays());
                        }

                        if (allowedDate != null && harvestDate.isBefore(allowedDate)) {
                            hasPhiViolation = true;
                            reasons.add("Vi phạm PHI: Thuốc " + record.pesticideName() + 
                                    " phun ngày " + record.applicationDate() + 
                                    ", chỉ an toàn sau ngày " + allowedDate + 
                                    " (ngày thu hoạch lô hàng: " + harvestDate + ").");
                        }
                    }

                    if (hasPhiViolation) {
                        isEligible = false;
                    } else {
                        phiSnapshot = objectMapper.writeValueAsString(pesticideRecords);
                    }
                } else {
                    phiSnapshot = "[]";
                }
            } catch (Exception e) {
                log.error("Lỗi khi kiểm tra PHI cho seasonId {}: {}", product.getSeasonId(), e.getMessage());
                isEligible = false;
                reasons.add("Không thể xác minh dữ liệu an toàn thu hoạch với season-service.");
            }
        }

        return new ComplianceCheckResponse(isEligible, reasons, claim, certSnapshot, phiSnapshot);
    }
}
