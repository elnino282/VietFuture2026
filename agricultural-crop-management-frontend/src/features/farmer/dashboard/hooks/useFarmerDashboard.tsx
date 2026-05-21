import { useMemo } from 'react';
import {
  useDataCompletenessWarnings,
  useDashboardFdnOverview,
  useDashboardFieldMap,
  useIncidentAlerts,
  useInventoryAlerts,
  useRecentActivities,
  useTodayTasks,
  useUpcomingTasks,
} from '@/entities/dashboard';
import type {
  DashboardDataCompletenessWarning,
  DashboardIncidentAlert,
  DashboardInventoryAlertItem,
  DashboardInventoryAlertsSummary,
  DashboardFdnOverview,
  DashboardFieldMapResponse,
  DashboardRecentActivity,
} from '@/entities/dashboard';
import { useSeason } from '@/shared/contexts';
import type {
  DashboardDataCompletenessWarningItem,
  RecentActivityItem,
  DashboardTaskItem,
} from '../types';

function toDateLabel(dueDate?: string | null, fallback = 'N/A'): string {
  const due = dueDate ? new Date(`${dueDate}T00:00:00`) : null;
  if (!due || Number.isNaN(due.getTime())) {
    return fallback;
  }
  return due.toLocaleDateString();
}

function toTaskItem(task: {
  taskId: number;
  title: string;
  plotName?: string | null;
  dueDate?: string | null;
  status: string;
}): DashboardTaskItem {
  const fallback = 'N/A';
  return {
    id: String(task.taskId),
    title: task.title,
    plotName: task.plotName ?? fallback,
    status: task.status,
    dueDateLabel: toDateLabel(task.dueDate, fallback),
    done: task.status === 'DONE',
  };
}

function toDataCompletenessWarningItem(
  warning: DashboardDataCompletenessWarning
): DashboardDataCompletenessWarningItem {
  return {
    id: warning.warningId,
    title: warning.title,
    source: warning.source,
    type: warning.type,
    status: warning.status,
    dueDateLabel: toDateLabel(warning.dueDate, 'Action required'),
    actionTarget: warning.actionTarget,
    inputCode: warning.inputCode,
  };
}

function toRecentActivityItem(
  activity: DashboardRecentActivity
): RecentActivityItem {
  return {
    id: activity.id,
    type: activity.type,
    title: activity.title,
    description: activity.description ?? '',
    occurredAt: activity.occurredAt,
    actorName: activity.actorName ?? null,
    entityType: activity.entityType,
    entityId: activity.entityId,
    actionUrl: activity.actionUrl ?? null,
  };
}

export interface UseFarmerDashboardReturn {
  selectedSeason: string;
  setSelectedSeason: (season: string) => void;
  seasonOptions: { value: string; label: string }[];
  overview: DashboardFdnOverview | null;
  fieldMap: DashboardFieldMapResponse | null;
  mapLoading: boolean;
  todayTasks: DashboardTaskItem[];
  upcomingTasks: DashboardTaskItem[];
  dataCompletenessWarnings: DashboardDataCompletenessWarningItem[];
  incidentAlerts: DashboardIncidentAlert[];
  recentActivities: RecentActivityItem[];
  inventoryAlerts: DashboardInventoryAlertItem[];
  inventoryAlertsSummary: DashboardInventoryAlertsSummary | null;
  isCriticalLoading: boolean;
  isDataLoading: boolean;
  overviewLoading: boolean;
  hasNoSeasons: boolean;
  seasonsError: Error | null;
  overviewError: Error | null;
  mapError: Error | null;
  todayTasksError: Error | null;
  upcomingTasksError: Error | null;
  dataCompletenessWarningsError: Error | null;
  incidentAlertsError: Error | null;
  recentActivitiesError: Error | null;
  inventoryAlertsError: Error | null;
}

export const useFarmerDashboard = (): UseFarmerDashboardReturn => {
  const {
    selectedSeasonId,
    setSelectedSeasonId,
    seasons: seasonsData,
    isLoading: seasonsLoading,
    error: seasonsError,
  } = useSeason();

  const seasonOptions = useMemo(() => {
    return (
      seasonsData?.map((season) => ({
        value: String(season.id),
        label: season.seasonName,
      })) ?? []
    );
  }, [seasonsData]);

  const selectedSeasonValue =
    selectedSeasonId !== null ? String(selectedSeasonId) : '';
  const hasInitialized = !seasonsLoading;
  const hasSeason = selectedSeasonId !== null && selectedSeasonId > 0;

  const setSelectedSeason = (season: string) => {
    const parsedId = Number.parseInt(season, 10);
    setSelectedSeasonId(Number.isNaN(parsedId) ? null : parsedId);
  };

  const {
    data: overviewData,
    isLoading: overviewLoading,
    error: overviewError,
  } = useDashboardFdnOverview(
    {
      scope: 'field',
      seasonId: hasSeason ? selectedSeasonId! : undefined,
    },
    { enabled: hasInitialized }
  );

  const {
    data: mapData,
    isLoading: mapLoading,
    error: mapError,
  } = useDashboardFieldMap(
    {},
    { enabled: hasInitialized }
  );

  const {
    data: todayTasksData,
    isLoading: todayTasksLoading,
    error: todayTasksError,
  } = useTodayTasks(
    { seasonId: hasSeason ? selectedSeasonId! : undefined },
    { enabled: hasInitialized }
  );

  const {
    data: upcomingTasksData,
    isLoading: upcomingTasksLoading,
    error: upcomingTasksError,
  } = useUpcomingTasks(
    { days: 7, seasonId: hasSeason ? selectedSeasonId! : undefined },
    { enabled: hasInitialized }
  );

  const {
    data: dataCompletenessWarningsData,
    isLoading: dataCompletenessWarningsLoading,
    error: dataCompletenessWarningsError,
  } = useDataCompletenessWarnings(
    { seasonId: hasSeason ? selectedSeasonId! : undefined },
    { enabled: hasInitialized }
  );

  const {
    data: incidentAlertsData,
    isLoading: incidentAlertsLoading,
    error: incidentAlertsError,
  } = useIncidentAlerts(
    { seasonId: hasSeason ? selectedSeasonId! : undefined },
    { enabled: hasInitialized }
  );

  const {
    data: recentActivitiesData,
    isLoading: recentActivitiesLoading,
    error: recentActivitiesError,
  } = useRecentActivities(
    { limit: 10 },
    { enabled: hasInitialized }
  );

  const {
    data: inventoryAlertsData,
    isLoading: inventoryAlertsLoading,
    error: inventoryAlertsError,
  } = useInventoryAlerts(
    { limit: 20 },
    { enabled: hasInitialized }
  );

  const todayTasks = useMemo(
    () => (todayTasksData?.content ?? []).map(toTaskItem),
    [todayTasksData]
  );

  const upcomingTasks = useMemo(
    () =>
      [...(upcomingTasksData ?? [])]
        .sort((a, b) => (a.dueDate ?? '').localeCompare(b.dueDate ?? ''))
        .map(toTaskItem),
    [upcomingTasksData]
  );

  const dataCompletenessWarnings = useMemo(
    () =>
      (dataCompletenessWarningsData ?? []).map(
        toDataCompletenessWarningItem
      ),
    [dataCompletenessWarningsData]
  );

  const recentActivities = useMemo(
    () =>
      [...(recentActivitiesData ?? [])]
        .sort((a, b) => b.occurredAt.localeCompare(a.occurredAt))
        .map(toRecentActivityItem),
    [recentActivitiesData]
  );

  const isCriticalLoading = !hasInitialized || seasonsLoading;
  const isDataLoading =
    overviewLoading
    || mapLoading
    || todayTasksLoading
    || upcomingTasksLoading
    || dataCompletenessWarningsLoading
    || incidentAlertsLoading
    || recentActivitiesLoading
    || inventoryAlertsLoading;
  const hasNoSeasons =
    hasInitialized && !seasonsLoading && seasonOptions.length === 0;

  return {
    selectedSeason: selectedSeasonValue,
    setSelectedSeason,
    seasonOptions,
    overview: overviewData ?? null,
    fieldMap: mapData ?? null,
    mapLoading,
    todayTasks,
    upcomingTasks,
    dataCompletenessWarnings,
    incidentAlerts: incidentAlertsData ?? [],
    recentActivities,
    inventoryAlerts: inventoryAlertsData?.alerts ?? [],
    inventoryAlertsSummary: inventoryAlertsData?.summary ?? null,
    isCriticalLoading,
    isDataLoading,
    overviewLoading,
    hasNoSeasons,
    seasonsError: seasonsError ?? null,
    overviewError: overviewError ?? null,
    mapError: mapError ?? null,
    todayTasksError: todayTasksError ?? null,
    upcomingTasksError: upcomingTasksError ?? null,
    dataCompletenessWarningsError: dataCompletenessWarningsError ?? null,
    incidentAlertsError: incidentAlertsError ?? null,
    recentActivitiesError: recentActivitiesError ?? null,
    inventoryAlertsError: inventoryAlertsError ?? null,
  };
};
