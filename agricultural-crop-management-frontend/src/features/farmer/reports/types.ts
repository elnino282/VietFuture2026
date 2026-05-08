export type ReportSection = "yield" | "cost" | "performance" | "pesticide";

export type YieldViewMode = "season" | "crop" | "plot";

export type ExportFormat = "excel" | "pdf" | "csv";

export type PesticideStatus = "safe" | "approaching" | "violated";

export interface PesticideRecord {
    id: string;
    lotId: string;
    chemical: string;
    quantity: number;
    phi: number;
    daysRemaining: number;
    status: PesticideStatus;
}

export interface YieldBySeason {
    season: string;
    yield: number;
    avgYield: number;
}

export interface YieldByCrop {
    crop: string;
    yield: number;
    target: number;
}

export interface YieldByPlot {
    plot: string;
    yield: number;
    area: number;
}

export interface CostDistribution {
    name: string;
    value: number;
    color: string;
    percentage: number;
}

export interface MonthlyCost {
    month: string;
    seeds: number;
    fertilizer: number;
    labor: number;
    fuel: number;
    machinery: number;
}

export interface TaskPerformance {
    month: string;
    onTime: number;
    late: number;
    overdue: number;
}

export interface CostOptimizationCategory {
    category?: string | null;
    amount: number | null;
    percentageOfTotal: number | null;
}

export interface CostOptimizationInventoryUsage {
    itemName?: string | null;
    unit?: string | null;
    totalOutQuantity: number | null;
    movementCount?: number | null;
}

export interface CostOptimizationSummary {
    seasonId: number;
    seasonName?: string | null;
    budgetAmount: number | null;
    totalExpense: number | null;
    remainingBudget: number | null;
    expenseByCategory: CostOptimizationCategory[];
    topCostCategories: CostOptimizationCategory[];
    expectedYieldKg: number | null;
    actualYieldKg: number | null;
    costPerExpectedKg: number | null;
    costPerActualKg: number | null;
    laborCost: number | null;
    pesticideTreatmentCost: number | null;
    inventoryUsageSummary: CostOptimizationInventoryUsage[];
    warnings: string[];
    disclaimer?: string | null;
}

export interface CostOptimizationAiSuggestion extends CostOptimizationSummary {
    aiSuggestionText?: string | null;
    usedContextSummary?: Record<string, unknown> | null;
    generatedAt?: string | null;
}

export interface FilterState {
    plots: string[];
    cropType: string;
    season: string;
    timeRange: string;
    includeClosedSeasons: boolean;
}

export interface SidebarItem {
    id: ReportSection;
    label: string;
    icon: React.ComponentType<{ className?: string }>;
}



