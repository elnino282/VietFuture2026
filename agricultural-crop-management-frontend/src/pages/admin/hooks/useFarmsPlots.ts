import { useQuery } from "@tanstack/react-query";
import {
  adminFarmApi,
  adminPlotApi,
  adminKeys,
  type AdminFarmDetail,
} from "@/services/api.admin";

// Re-export keys so page components don't need the restricted @/services import
export const adminFarmsPlotsKeys = {
  farms: adminKeys.farms,
  plots: adminKeys.plots,
};

// ═══════════════════════════════════════════════════════════════
// Interfaces (matching backend response shapes)
// ═══════════════════════════════════════════════════════════════

export interface Farm {
  id: number;
  name: string;
  area: number | null;
  active: boolean;
  ownerUsername: string | null;
  provinceName: string | null;
  wardName: string | null;
}

export interface Plot {
  id: number;
  plotName: string;
  area: number | null;
  soilType: string | null;
  farmId: number;
  farmName: string;
}

export interface Season {
  id: number;
  seasonName: string;
  cropName: string;
  status: string;
  startDate: string;
  endDate: string | null;
}

// ═══════════════════════════════════════════════════════════════
// Farm Queries
// ═══════════════════════════════════════════════════════════════

interface UseFarmsParams {
  page?: number;
  size?: number;
  keyword?: string;
}

/**
 * Fetches a paginated list of farms from the admin API.
 * React Query handles caching, deduplication, and background refetches.
 */
export function useAdminFarms(params?: UseFarmsParams) {
  const queryParams = {
    page: params?.page ?? 0,
    size: params?.size ?? 20,
    keyword: params?.keyword || undefined,
  };

  return useQuery({
    queryKey: adminKeys.farmList(queryParams),
    queryFn: async () => {
      const response = await adminFarmApi.list(queryParams);
      const items: Farm[] = response?.result?.items ?? [];
      const totalPages: number = response?.result?.totalPages ?? 0;
      return { items, totalPages };
    },
  });
}

/**
 * Fetches a single farm detail by ID.
 * Enabled only when farmId is a positive number.
 */
export function useAdminFarmDetail(farmId: number | null) {
  return useQuery({
    queryKey: adminKeys.farmDetail(farmId ?? 0),
    queryFn: () => adminFarmApi.getById(farmId!),
    enabled: Boolean(farmId && farmId > 0),
    select: (detail: AdminFarmDetail): Farm => ({
      id: detail.id,
      name: detail.name,
      area: detail.area ?? null,
      active: detail.active,
      ownerUsername: detail.ownerUsername ?? null,
      provinceName: detail.provinceName ?? null,
      wardName: detail.wardName ?? null,
    }),
  });
}

// ═══════════════════════════════════════════════════════════════
// Plot Queries
// ═══════════════════════════════════════════════════════════════

interface UsePlotsParams {
  page?: number;
  size?: number;
  keyword?: string;
  farmId?: number | null;
}

/**
 * Fetches a paginated list of plots from the admin API.
 * Supports optional farmId filtering.
 */
export function useAdminPlots(params?: UsePlotsParams) {
  const queryParams = {
    page: params?.page ?? 0,
    size: params?.size ?? 20,
    keyword: params?.keyword || undefined,
    farmId: params?.farmId || undefined,
  };

  return useQuery({
    queryKey: adminKeys.plotList(queryParams),
    queryFn: async () => {
      const response = await adminPlotApi.list(queryParams);
      const items: Plot[] = response?.result?.items ?? [];
      const totalPages: number = response?.result?.totalPages ?? 0;
      return { items, totalPages };
    },
  });
}

/**
 * Fetches a single plot detail by ID.
 * Enabled only when plotId is a positive number.
 */
export function useAdminPlotDetail(plotId: number | null) {
  return useQuery({
    queryKey: adminKeys.plotDetail(plotId ?? 0),
    queryFn: async () => {
      const response = await adminPlotApi.getById(plotId!);
      const payload = response?.result ?? response;
      return {
        id: payload.id,
        plotName: payload.plotName,
        area: payload.area ?? null,
        soilType: payload.soilType ?? null,
        farmId: payload.farmId ?? 0,
        farmName: payload.farmName ?? "",
      } as Plot;
    },
    enabled: Boolean(plotId && plotId > 0),
  });
}

// ═══════════════════════════════════════════════════════════════
// Detail-Drawer Queries (farm plots & plot seasons)
// ═══════════════════════════════════════════════════════════════

/**
 * Fetches all plots belonging to a specific farm (for the farm detail drawer).
 * Uses a separate query key from the paginated plot list to avoid conflicts.
 */
export function useAdminFarmPlots(farmId: number | null, enabled: boolean) {
  return useQuery({
    queryKey: [...adminKeys.farms, "plots", farmId ?? 0] as const,
    queryFn: async () => {
      const response = await adminPlotApi.list({ farmId: farmId! });
      const items: Plot[] = response?.result?.items ?? [];
      return items;
    },
    enabled: enabled && Boolean(farmId && farmId > 0),
  });
}

/**
 * Fetches all seasons belonging to a specific plot (for the plot detail drawer).
 */
export function useAdminPlotSeasons(plotId: number | null, enabled: boolean) {
  return useQuery({
    queryKey: adminKeys.plotSeasons(plotId ?? 0),
    queryFn: async () => {
      const response = await adminPlotApi.getSeasons(plotId!);
      const result = response?.result;
      return (Array.isArray(result) ? result : []) as Season[];
    },
    enabled: enabled && Boolean(plotId && plotId > 0),
  });
}

/**
 * Fetches ALL farms (unpaginated, for filter dropdown on Plots tab).
 * Uses a large page size and long staleTime to minimize refetches.
 */
export function useAdminFarmsForFilter() {
  return useQuery({
    queryKey: [...adminKeys.farms, "filter-options"] as const,
    queryFn: async () => {
      const response = await adminFarmApi.list({ size: 500 });
      const items: Farm[] = response?.result?.items ?? [];
      return items;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes - farm list rarely changes
  });
}
