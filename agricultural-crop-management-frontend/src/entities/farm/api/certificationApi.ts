import httpClient from '@/shared/api/http';

export interface CertificationItemDetail {
  id: number;
  itemCode: string;
  category: string;
  description: string;
  isMandatory: boolean;
  weightPct: number;
  dataSourceType: string;
  dataSourceQuery: string;
  status: string; // PASS, FAIL, PENDING, NOT_APPLICABLE
  evidenceUrl?: string;
  notes?: string;
  checkedAt?: string;
}

export interface CertificationDetails {
  recordId: number;
  farmId: number;
  standardCode: string;
  standardName: string;
  complianceScore: number;
  status: string; // IN_PROGRESS, READY_TO_APPLY, APPLIED, CERTIFIED, REJECTED, EXPIRED
  appliedAt?: string;
  certifiedAt?: string;
  expiryDate?: string;
  auditorNotes?: string;
  items: CertificationItemDetail[];
  isEligible: boolean;
}

export interface ApiResponse<T> {
  code: string;
  result: T;
  message?: string;
}

export const certificationApi = {
  async getCertificationDetails(farmId: number): Promise<CertificationDetails> {
    const response = await httpClient.get<ApiResponse<CertificationDetails>>(`/api/v1/farms/${farmId}/certification`);
    return response.data.result;
  },

  async updateItemStatus(
    farmId: number,
    itemId: number,
    data: { status: string; evidenceUrl?: string; notes?: string }
  ): Promise<string> {
    const response = await httpClient.put<ApiResponse<string>>(`/api/v1/farms/${farmId}/certification/items/${itemId}`, data);
    return response.data.result;
  },

  async applyCertification(farmId: number): Promise<string> {
    const response = await httpClient.post<ApiResponse<string>>(`/api/v1/farms/${farmId}/certification/apply`);
    return response.data.result;
  },
};
