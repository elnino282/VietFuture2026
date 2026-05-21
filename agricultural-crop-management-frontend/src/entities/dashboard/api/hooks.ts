import { useQuery, UseQueryOptions } from '@tanstack/react-query';
import { dashboardApi } from './client';
import { dashboardKeys } from '../model/keys';
import type {
    DashboardOverview,
    TodayTask,
    DashboardDataCompletenessWarning,
    DashboardIncidentAlert,
    DashboardRecentActivity,
    PlotStatus,
    LowStockAlert,
    DashboardInventoryAlertsResponse,
    TodayTasksPage,
    TodayTasksParams,
    UpcomingTasksParams,
    DataCompletenessWarningsParams,
    IncidentAlertsParams,
    RecentActivitiesParams,
    LowStockParams,
    InventoryAlertsParams,
} from '../model/types';

// ═══════════════════════════════════════════════════════════════
// DASHBOARD REACT QUERY HOOKS
// ═══════════════════════════════════════════════════════════════

/**
 * Hook for dashboard overview data
 */
export function useDashboardOverview(
    seasonId?: number,
    options?: Omit<UseQueryOptions<DashboardOverview, Error>, 'queryKey' | 'queryFn'>
) {
    return useQuery({
        queryKey: dashboardKeys.overview(seasonId),
        queryFn: () => dashboardApi.getOverview(seasonId),
        staleTime: 30 * 1000, // 30 seconds
        ...options,
    });
}

/**
 * Hook for today's tasks
 */
export function useTodayTasks(
    params?: TodayTasksParams,
    options?: Omit<UseQueryOptions<TodayTasksPage, Error>, 'queryKey' | 'queryFn'>
) {
    return useQuery({
        queryKey: dashboardKeys.todayTasks({
            seasonId: params?.seasonId,
            page: params?.page,
            size: params?.size,
        }),
        queryFn: () => dashboardApi.getTodayTasks(params),
        staleTime: 30 * 1000,
        ...options,
    });
}

/**
 * Hook for upcoming tasks
 */
export function useUpcomingTasks(
    params?: UpcomingTasksParams,
    options?: Omit<UseQueryOptions<TodayTask[], Error>, 'queryKey' | 'queryFn'>
) {
    return useQuery({
        queryKey: dashboardKeys.upcomingTasks({
            days: params?.days ?? 7,
            seasonId: params?.seasonId,
        }),
        queryFn: () => dashboardApi.getUpcomingTasks(params),
        staleTime: 30 * 1000,
        ...options,
    });
}

/**
 * Hook for data completeness warnings
 */
export function useDataCompletenessWarnings(
    params?: DataCompletenessWarningsParams,
    options?: Omit<UseQueryOptions<DashboardDataCompletenessWarning[], Error>, 'queryKey' | 'queryFn'>
) {
    return useQuery({
        queryKey: dashboardKeys.dataCompletenessWarnings({
            seasonId: params?.seasonId,
        }),
        queryFn: () => dashboardApi.getDataCompletenessWarnings(params),
        staleTime: 30 * 1000,
        ...options,
    });
}

/**
 * Hook for operational incident/risk alerts on farmer dashboard
 */
export function useIncidentAlerts(
    params?: IncidentAlertsParams,
    options?: Omit<UseQueryOptions<DashboardIncidentAlert[], Error>, 'queryKey' | 'queryFn'>
) {
    return useQuery({
        queryKey: dashboardKeys.incidentAlerts({
            seasonId: params?.seasonId,
        }),
        queryFn: () => dashboardApi.getIncidentAlerts(params),
        staleTime: 30 * 1000,
        ...options,
    });
}

/**
 * Hook for recent timeline activities on farmer dashboard
 */
export function useRecentActivities(
    params?: RecentActivitiesParams,
    options?: Omit<UseQueryOptions<DashboardRecentActivity[], Error>, 'queryKey' | 'queryFn'>
) {
    return useQuery({
        queryKey: dashboardKeys.recentActivities(params?.limit),
        queryFn: () => dashboardApi.getRecentActivities(params),
        staleTime: 30 * 1000,
        ...options,
    });
}

/**
 * Hook for plot status
 */
export function usePlotStatus(
    seasonId?: number,
    options?: Omit<UseQueryOptions<PlotStatus[], Error>, 'queryKey' | 'queryFn'>
) {
    return useQuery({
        queryKey: dashboardKeys.plotStatus(seasonId),
        queryFn: () => dashboardApi.getPlotStatus(seasonId),
        staleTime: 60 * 1000, // 1 minute
        ...options,
    });
}

/**
 * Hook for low stock alerts
 */
export function useLowStock(
    params?: LowStockParams,
    options?: Omit<UseQueryOptions<LowStockAlert[], Error>, 'queryKey' | 'queryFn'>
) {
    return useQuery({
        queryKey: dashboardKeys.lowStock(params?.limit),
        queryFn: () => dashboardApi.getLowStock(params),
        staleTime: 60 * 1000, // 1 minute
        ...options,
    });
}

/**
 * Hook for inventory risk alerts (low stock, expired, expiring, no movement)
 */
export function useInventoryAlerts(
    params?: InventoryAlertsParams,
    options?: Omit<UseQueryOptions<DashboardInventoryAlertsResponse, Error>, 'queryKey' | 'queryFn'>
) {
    return useQuery({
        queryKey: dashboardKeys.inventoryAlerts(params?.limit),
        queryFn: () => dashboardApi.getInventoryAlerts(params),
        staleTime: 60 * 1000,
        ...options,
    });
}
