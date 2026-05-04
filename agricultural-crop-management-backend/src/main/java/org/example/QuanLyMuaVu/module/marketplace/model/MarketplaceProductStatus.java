package org.example.QuanLyMuaVu.module.marketplace.model;

/**
 * Product visibility lifecycle in marketplace catalog.
 * <p>
 * Status flow: DRAFT → PENDING_REVIEW → ACTIVE | REJECTED
 * <p>
 * Active statuses:
 * <ul>
 *   <li>DRAFT - Initial state, not visible to buyers</li>
 *   <li>PENDING_REVIEW - Submitted for admin review</li>
 *   <li>ACTIVE - Approved and visible in marketplace</li>
 *   <li>REJECTED - Rejected by admin, needs revision</li>
 *   <li>INACTIVE - Temporarily hidden by farmer</li>
 *   <li>SOLD_OUT - No stock available</li>
 * </ul>
 * <p>
 * Deprecated statuses (for backward compatibility):
 * <ul>
 *   <li>PUBLISHED - Use ACTIVE instead</li>
 *   <li>HIDDEN - Use INACTIVE instead</li>
 * </ul>
 */
public enum MarketplaceProductStatus {
    DRAFT,
    PENDING_REVIEW,
    ACTIVE,
    REJECTED,
    INACTIVE,
    SOLD_OUT,
    @Deprecated PUBLISHED,  // Legacy - use ACTIVE instead
    @Deprecated HIDDEN      // Legacy - use INACTIVE instead
}
