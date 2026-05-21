import { describe, expect, it } from 'vitest';
import {
  AdminPendingApprovalItemSchema,
  AdminPendingApprovalsSchema,
} from '@/services/api.admin';

describe('admin pending approvals schema', () => {
  it('parses real pending approval item payload', () => {
    const parsed = AdminPendingApprovalItemSchema.parse({
      id: 501,
      type: 'PAYMENT_PROOF_VERIFICATION',
      title: 'Verify payment proof',
      subtitle: 'Order ORD-501 | Buyer #22',
      submittedAt: '2026-05-17T09:30:00',
      priority: 'HIGH',
      severity: 'HIGH',
      actionUrl: '/admin/marketplace-orders?orderId=501',
      actionTarget: 'PAYMENT_PROOF_VERIFICATION',
    });

    expect(parsed.id).toBe(501);
    expect(parsed.type).toBe('PAYMENT_PROOF_VERIFICATION');
    expect(parsed.actionUrl).toContain('/admin/marketplace-orders');
  });

  it('accepts empty list from API', () => {
    const parsed = AdminPendingApprovalsSchema.parse([]);
    expect(parsed).toEqual([]);
  });
});
