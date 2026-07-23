import httpClient from '@/shared/api/http';
import type { FarmingLog, ReportData } from '../types/dashboard-types';

export const dashboardApi = {
  /**
   * Lấy danh sách nhật ký canh tác cho một mùa vụ cụ thể
   */
  getFarmingLogs: async (seasonId: string): Promise<FarmingLog[]> => {
    const response = await httpClient.get(`/api/v1/farmer/dashboard/farming-logs`, {
      params: { seasonId }
    });
    return response.data;
  },

  /**
   * Lấy báo cáo thống kê cho một mùa vụ cụ thể (Sản lượng, Chi phí)
   */
  getSeasonStats: async (seasonId: string): Promise<ReportData> => {
    const response = await httpClient.get(`/api/v1/farmer/dashboard/season-stats`, {
      params: { seasonId }
    });
    return response.data;
  }
};
