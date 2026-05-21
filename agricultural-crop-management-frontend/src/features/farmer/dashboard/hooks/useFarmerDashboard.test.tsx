import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderHook } from '@testing-library/react';
import { useFarmerDashboard } from './useFarmerDashboard';

const dashboardMocks = vi.hoisted(() => ({
  useDashboardFdnOverview: vi.fn(),
  useDashboardFieldMap: vi.fn(),
  useIncidentAlerts: vi.fn(),
  useRecentActivities: vi.fn(),
  useInventoryAlerts: vi.fn(),
  useTodayTasks: vi.fn(),
  useUpcomingTasks: vi.fn(),
  useDataCompletenessWarnings: vi.fn(),
}));

const seasonMocks = vi.hoisted(() => ({
  useSeason: vi.fn(),
}));

vi.mock('@/entities/dashboard', () => ({
  useDashboardFdnOverview: dashboardMocks.useDashboardFdnOverview,
  useDashboardFieldMap: dashboardMocks.useDashboardFieldMap,
  useIncidentAlerts: dashboardMocks.useIncidentAlerts,
  useRecentActivities: dashboardMocks.useRecentActivities,
  useInventoryAlerts: dashboardMocks.useInventoryAlerts,
  useTodayTasks: dashboardMocks.useTodayTasks,
  useUpcomingTasks: dashboardMocks.useUpcomingTasks,
  useDataCompletenessWarnings: dashboardMocks.useDataCompletenessWarnings,
}));

vi.mock('@/shared/contexts', () => ({
  useSeason: seasonMocks.useSeason,
}));

describe('useFarmerDashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('maps real tasks from backend and keeps data-completeness warnings separate', () => {
    seasonMocks.useSeason.mockReturnValue({
      selectedSeasonId: 33,
      setSelectedSeasonId: vi.fn(),
      seasons: [{ id: 33, seasonName: 'Season 33' }],
      isLoading: false,
      error: null,
    });

    dashboardMocks.useDashboardFdnOverview.mockReturnValue({
      data: {
        missingInputs: ['MINERAL_FERTILIZER', 'IRRIGATION_WATER'],
      } as any,
      isLoading: false,
      error: null,
    });

    dashboardMocks.useDashboardFieldMap.mockReturnValue({
      data: {
        fieldsWithBoundary: [{ fieldId: 22, fieldName: 'Field A' }],
        fieldsMissingBoundary: [],
        defaultViewport: null,
        unavailableReason: null,
      },
      isLoading: false,
      error: null,
    });

    dashboardMocks.useTodayTasks.mockReturnValue({
      data: {
        content: [
          {
            taskId: 1,
            title: 'Irrigate field',
            plotName: 'Field A',
            dueDate: '2026-03-17',
            status: 'DONE',
          },
        ],
      },
      isLoading: false,
      error: null,
    });

    dashboardMocks.useUpcomingTasks.mockReturnValue({
      data: [
        {
          taskId: 2,
          title: 'Soil test',
          plotName: 'Field B',
          dueDate: '2026-03-20',
          status: 'PENDING',
        },
      ],
      isLoading: false,
      error: null,
    });

    dashboardMocks.useDataCompletenessWarnings.mockReturnValue({
      data: [
        {
          warningId: 'missing-input-mineral_fertilizer',
          title: 'Missing required input: Mineral Fertilizer',
          source: 'SUSTAINABILITY_OVERVIEW',
          type: 'DATA_COMPLETENESS',
          status: 'ACTION_REQUIRED',
          dueDate: '2026-03-17',
          actionTarget: '/farmer/seasons/33/workspace/nutrient-inputs',
          seasonId: 33,
          inputCode: 'MINERAL_FERTILIZER',
        },
      ],
      isLoading: false,
      error: null,
    });

    dashboardMocks.useIncidentAlerts.mockReturnValue({
      data: [
        {
          id: 'incident-1',
          type: 'OPEN_INCIDENT',
          severity: 'HIGH',
          title: 'Open incident requires attention',
          description: 'Plot risk detected',
          seasonId: 33,
          plotId: 22,
          createdAt: '2026-03-16T10:00:00',
          dueDate: '2026-03-18',
          actionUrl: '/farmer/seasons/33/workspace/disease',
          actionTarget: 'DISEASE_WORKSPACE',
        },
      ],
      isLoading: false,
      error: null,
    });

    dashboardMocks.useInventoryAlerts.mockReturnValue({
      data: {
        summary: {
          totalAlerts: 1,
          lowStock: 1,
          expired: 0,
          expiringSoon: 0,
          noMovement: 0,
          abnormalMovement: 0,
        },
        alerts: [
          {
            supplyLotId: 9001,
            itemName: 'NPK 20-20-15',
            lotCode: 'LOT-001',
            warehouseName: 'Main Warehouse',
            locationLabel: 'Z1-A1-S1-B1',
            quantity: 2,
            unit: 'kg',
            expiryDate: '2026-03-30',
            alertType: 'LOW_STOCK',
            severity: 'HIGH',
            reason: null,
            lastMovementAt: '2026-03-10T10:00:00',
          },
        ],
      },
      isLoading: false,
      error: null,
    });

    dashboardMocks.useRecentActivities.mockReturnValue({
      data: [
        {
          id: 'task-progress-1',
          type: 'TASK_UPDATE',
          title: 'Irrigate field',
          description: 'Progress updated to 65%',
          occurredAt: '2026-03-17T09:00:00',
          actorName: 'Worker A',
          entityType: 'TASK',
          entityId: '1',
          actionUrl: '/farmer/seasons/33/workspace/tasks',
        },
      ],
      isLoading: false,
      error: null,
    });

    const { result } = renderHook(() => useFarmerDashboard());

    expect(result.current.selectedSeason).toBe('33');
    expect(result.current.todayTasks).toHaveLength(1);
    expect(result.current.todayTasks[0].done).toBe(true);
    expect(result.current.upcomingTasks).toHaveLength(1);
    expect(result.current.upcomingTasks[0].title).toBe('Soil test');
    expect(result.current.dataCompletenessWarnings).toHaveLength(1);
    expect(result.current.dataCompletenessWarnings[0].type).toBe('DATA_COMPLETENESS');
    expect(result.current.incidentAlerts).toHaveLength(1);
    expect(result.current.incidentAlerts[0].type).toBe('OPEN_INCIDENT');
    expect(result.current.fieldMap?.fieldsWithBoundary).toHaveLength(1);
    expect(result.current.inventoryAlerts).toHaveLength(1);
    expect(result.current.inventoryAlertsSummary?.lowStock).toBe(1);
    expect(result.current.recentActivities).toHaveLength(1);
    expect(result.current.recentActivities[0].title).toBe('Irrigate field');
  });

  it('returns hasNoSeasons=true when season list is empty after initialization', () => {
    seasonMocks.useSeason.mockReturnValue({
      selectedSeasonId: null,
      setSelectedSeasonId: vi.fn(),
      seasons: [],
      isLoading: false,
      error: null,
    });

    dashboardMocks.useDashboardFdnOverview.mockReturnValue({
      data: null,
      isLoading: false,
      error: null,
    });
    dashboardMocks.useDashboardFieldMap.mockReturnValue({
      data: {
        fieldsWithBoundary: [],
        fieldsMissingBoundary: [],
        defaultViewport: null,
        unavailableReason: null,
      },
      isLoading: false,
      error: null,
    });
    dashboardMocks.useTodayTasks.mockReturnValue({
      data: { content: [] },
      isLoading: false,
      error: null,
    });
    dashboardMocks.useUpcomingTasks.mockReturnValue({
      data: [],
      isLoading: false,
      error: null,
    });
    dashboardMocks.useDataCompletenessWarnings.mockReturnValue({
      data: [],
      isLoading: false,
      error: null,
    });
    dashboardMocks.useIncidentAlerts.mockReturnValue({
      data: [],
      isLoading: false,
      error: null,
    });
    dashboardMocks.useInventoryAlerts.mockReturnValue({
      data: { summary: null, alerts: [] },
      isLoading: false,
      error: null,
    });
    dashboardMocks.useRecentActivities.mockReturnValue({
      data: [],
      isLoading: false,
      error: null,
    });

    const { result } = renderHook(() => useFarmerDashboard());

    expect(result.current.hasNoSeasons).toBe(true);
    expect(result.current.isCriticalLoading).toBe(false);
    expect(result.current.seasonOptions).toEqual([]);
    expect(result.current.incidentAlerts).toEqual([]);
    expect(result.current.inventoryAlerts).toEqual([]);
    expect(result.current.recentActivities).toEqual([]);
  });
});
