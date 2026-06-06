import { useQuery } from '@tanstack/react-query';
import { Users, Boxes, Map, Sprout } from 'lucide-react';
import {
  adminDashboardStatsApi,
  dashboardStatsKeys,
  type AdminPendingApprovalItem,
  type DashboardStats,
} from '@/features/admin/shared/api';
import { useI18n } from '@/shared/lib/hooks/useI18n';
import {
  type RiskLevel,
  type RiskDataCoverage,
  type TransformedRiskySeason,
} from '../types';

type Translator = (key: string, defaultValue?: string) => string;

/**
 * Calculate risk level based on riskScore
 * High: >= 5, Medium: 2-4, Low: < 2
 */
const getRiskLevel = (riskScore: number): RiskLevel => {
  if (riskScore >= 5) return 'high';
  if (riskScore >= 2) return 'medium';
  return 'low';
};

/**
 * Transform raw API data to UI-friendly format
 */
const transformDashboardData = (data: DashboardStats, t: Translator) => {
  // Summary KPIs with icons and colors
  const kpiMetrics = [
    {
      key: 'totalUsers',
      title: t('adminDashboard.kpi.totalUsers', 'Total Users'),
      value: data.summary.totalUsers.toLocaleString(),
      icon: Users,
      color: '#3BA55D',
      bgColor: 'bg-emerald-50',
      textColor: 'text-emerald-600',
    },
    {
      key: 'totalFarms',
      title: t('adminDashboard.kpi.totalFarms', 'Total Farms'),
      value: data.summary.totalFarms.toLocaleString(),
      icon: Boxes,
      color: '#2F8A4D',
      bgColor: 'bg-green-50',
      textColor: 'text-green-700',
    },
    {
      key: 'totalPlots',
      title: t('adminDashboard.kpi.totalPlots', 'Total Plots'),
      value: data.summary.totalPlots.toLocaleString(),
      icon: Map,
      color: '#64748B',
      bgColor: 'bg-slate-50',
      textColor: 'text-slate-600',
    },
    {
      key: 'totalSeasons',
      title: t('adminDashboard.kpi.totalSeasons', 'Total Seasons'),
      value: data.summary.totalSeasons.toLocaleString(),
      icon: Sprout,
      color: '#10B981',
      bgColor: 'bg-emerald-50',
      textColor: 'text-emerald-600',
    },
  ];

  // Charts data
  const charts = {
    userRoles: data.userRoleCounts.map((r) => ({
      name: r.role || 'Unknown',
      value: r.total,
      color: r.role === 'ADMIN' ? '#4A90E2' : r.role === 'FARMER' ? '#3BA55D' : r.role === 'BUYER' ? '#F97316' : '#F4C542',
    })),
    userStatus: data.userStatusCounts.map((s) => ({
      name: s.status || 'Unknown',
      value: s.total,
      color: s.status === 'ACTIVE' ? '#10B981' : s.status === 'LOCKED' ? '#EF4444' : '#64748B',
    })),
    seasonStatus: data.seasonStatusCounts.map((s) => ({
      name: s.status || 'Unknown',
      value: s.total,
      color:
        s.status === 'ACTIVE'
          ? '#3BA55D'
          : s.status === 'COMPLETED'
            ? '#F4C542'
            : s.status === 'CANCELLED'
              ? '#EF4444'
              : '#64748B',
    })),
  };

  // Risky seasons with calculated risk level
  const risks: TransformedRiskySeason[] = data.riskySeasons.map((s) => ({
    ...s,
    riskLevel: getRiskLevel(s.riskScore),
  }));

  const riskDataCoverage: RiskDataCoverage = data.dataCoverage;
  const riskDataLimited = !riskDataCoverage.incidentDataAvailable || !riskDataCoverage.taskDataAvailable;

  return { kpiMetrics, charts, risks, riskDataCoverage, riskDataLimited };
};

/**
 * Custom hook for Admin Dashboard with real-time data
 *
 * Features:
 * - Auto-refetch every 30s for real-time updates
 * - Keeps previous data while fetching to avoid flashing
 * - Transforms API data to UI-friendly format via select
 */
export const useAdminDashboard = () => {
  const { t } = useI18n();

  const statsQuery = useQuery({
    queryKey: dashboardStatsKeys.stats(),
    queryFn: adminDashboardStatsApi.getStats,
    refetchInterval: 30_000, // Real-time: poll every 30s
    staleTime: 15_000, // Consider data fresh for 15s
    placeholderData: (previousData) => previousData, // Avoid flashing
    select: (data) => transformDashboardData(data, t),
  });

  const pendingApprovalsQuery = useQuery({
    queryKey: dashboardStatsKeys.pendingApprovals(10),
    queryFn: () => adminDashboardStatsApi.getPendingApprovals({ limit: 10 }),
    refetchInterval: 30_000,
    staleTime: 15_000,
    placeholderData: (previousData) => previousData,
  });

  const refetch = async () => {
    await Promise.all([statsQuery.refetch(), pendingApprovalsQuery.refetch()]);
  };

  return {
    // Query state
    isLoading: statsQuery.isLoading,
    isError: statsQuery.isError,
    error: statsQuery.error,
    isFetching: statsQuery.isFetching || pendingApprovalsQuery.isFetching, // Background refetch indicator

    // Transformed data (undefined when loading)
    kpiMetrics: statsQuery.data?.kpiMetrics ?? [],
    charts: statsQuery.data?.charts ?? { userRoles: [], userStatus: [], seasonStatus: [] },
    risks: statsQuery.data?.risks ?? [],
    riskDataCoverage: statsQuery.data?.riskDataCoverage,
    riskDataLimited: statsQuery.data?.riskDataLimited ?? false,
    pendingApprovals: pendingApprovalsQuery.data ?? [],
    pendingApprovalsLoading: pendingApprovalsQuery.isLoading,
    pendingApprovalsError: pendingApprovalsQuery.isError
      ? (pendingApprovalsQuery.error as Error)
      : null,
    // Refetch manually if needed
    refetch,
  };
};

export type UseAdminDashboardReturn = ReturnType<typeof useAdminDashboard>;
export type AdminDashboardPendingApproval = AdminPendingApprovalItem;
