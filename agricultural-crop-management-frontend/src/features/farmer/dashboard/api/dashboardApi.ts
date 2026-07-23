import httpClient from '@/shared/api/http';
import type { ApiResponse } from '@/shared/api/types';
import type { FarmingLog, ReportData } from '../types/dashboard-types';

export const dashboardApi = {
  /**
   * Lấy danh sách nhật ký canh tác cho một mùa vụ cụ thể
   */
  getFarmingLogs: async (seasonId: string): Promise<FarmingLog[]> => {
    const response = await httpClient.get<ApiResponse<FarmingLog[]>>(
      `/api/v1/farmer/dashboard/farming-logs`,
      { params: { seasonId } }
    );
    return response.data.data ?? response.data.result ?? [];
  },

  /**
   * Lấy báo cáo thống kê cho một mùa vụ cụ thể (Sản lượng, Chi phí)
   */
  getSeasonStats: async (seasonId: string): Promise<ReportData> => {
    const response = await httpClient.get<ApiResponse<ReportData>>(
      `/api/v1/farmer/dashboard/season-stats`,
      { params: { seasonId } }
    );
    const stats = response.data.data ?? response.data.result;
    if (!stats) {
      throw new Error('Season stats response is missing both "data" and "result" payloads.');
    }
    return stats;
  }
};
