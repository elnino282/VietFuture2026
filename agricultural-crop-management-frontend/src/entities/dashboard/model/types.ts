import { z } from 'zod';
import {
    DashboardOverviewSchema,
    TodayTaskSchema,
    DashboardDataCompletenessWarningSchema,
    DashboardIncidentAlertSchema,
    DashboardRecentActivitySchema,
    PlotStatusSchema,
    LowStockAlertSchema,
    DashboardInventoryAlertItemSchema,
    DashboardInventoryAlertsSummarySchema,
    DashboardInventoryAlertsResponseSchema,
    TodayTasksPageSchema,
    SeasonContextSchema,
    CountsSchema,
    KpisSchema,
    ExpensesSchema,
    HarvestSchema,
    AlertsSchema,
} from './schemas';

// ═══════════════════════════════════════════════════════════════
// DASHBOARD TYPES
// Inferred from Zod schemas
// ═══════════════════════════════════════════════════════════════

export type SeasonContext = z.infer<typeof SeasonContextSchema>;
export type Counts = z.infer<typeof CountsSchema>;
export type Kpis = z.infer<typeof KpisSchema>;
export type Expenses = z.infer<typeof ExpensesSchema>;
export type Harvest = z.infer<typeof HarvestSchema>;
export type Alerts = z.infer<typeof AlertsSchema>;

export type DashboardOverview = z.infer<typeof DashboardOverviewSchema>;
export type TodayTask = z.infer<typeof TodayTaskSchema>;
export type DashboardDataCompletenessWarning = z.infer<typeof DashboardDataCompletenessWarningSchema>;
export type DashboardIncidentAlert = z.infer<typeof DashboardIncidentAlertSchema>;
export type DashboardRecentActivity = z.infer<typeof DashboardRecentActivitySchema>;
export type PlotStatus = z.infer<typeof PlotStatusSchema>;
export type LowStockAlert = z.infer<typeof LowStockAlertSchema>;
export type DashboardInventoryAlertItem = z.infer<typeof DashboardInventoryAlertItemSchema>;
export type DashboardInventoryAlertsSummary = z.infer<typeof DashboardInventoryAlertsSummarySchema>;
export type DashboardInventoryAlertsResponse = z.infer<typeof DashboardInventoryAlertsResponseSchema>;
export type TodayTasksPage = z.infer<typeof TodayTasksPageSchema>;

// Dashboard overview params
export interface DashboardOverviewParams {
    seasonId?: number;
}

// Today tasks params
export interface TodayTasksParams {
    seasonId?: number;
    page?: number;
    size?: number;
    sort?: string;
}

// Upcoming tasks params
export interface UpcomingTasksParams {
    days?: number;
    seasonId?: number;
}

// Data completeness warnings params
export interface DataCompletenessWarningsParams {
    seasonId?: number;
}

export interface IncidentAlertsParams {
    seasonId?: number;
}

export interface RecentActivitiesParams {
    limit?: number;
}

// Low stock params
export interface LowStockParams {
    limit?: number;
}

export interface InventoryAlertsParams {
    limit?: number;
}
