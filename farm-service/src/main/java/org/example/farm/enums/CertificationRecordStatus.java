package org.example.farm.enums;

/**
 * Trạng thái vòng đời đầy đủ của CertificationRecord.
 * Giữ nguyên 3 state cũ (IN_PROGRESS, READY_TO_APPLY, APPLIED),
 * bổ sung thêm các state sau APPLIED theo BRD §5.4.
 */
public enum CertificationRecordStatus {
    // === State cũ (không đổi) ===
    IN_PROGRESS,
    READY_TO_APPLY,
    APPLIED,

    // === State mới (sau APPLIED) ===
    AUDIT_SCHEDULED,
    AUDIT_IN_PROGRESS,
    NONCONFORMITY_FOUND,
    CORRECTIVE_ACTION_SUBMITTED,
    AUDIT_PASSED,
    CERTIFIED,
    PUBLISHED,
    PERIODIC_REVIEW_DUE,
    EXPIRED,
    REVOKED,
    REJECTED
}

