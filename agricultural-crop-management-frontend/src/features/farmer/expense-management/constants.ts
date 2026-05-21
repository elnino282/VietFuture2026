import type {
    AITip,
    Payable,
    CategoryData,
    MonthlyTrendData,
    CategoryComparisonData,
} from "./types";

export interface LocalizedOption {
    value: string;
    labelKey: string;
    fallbackLabel: string;
}

// Category Colors
export const CATEGORY_COLORS: Record<string, string> = {
    Fertilizer: "var(--primary)",
    Seeds: "var(--secondary)",
    Equipment: "var(--accent)",
    Pesticide: "var(--chart-4)",
    Labor: "var(--muted-foreground)",
    Transportation: "var(--primary)",
    Utilities: "var(--secondary)",
    Maintenance: "var(--chart-5)",
    Other: "var(--muted-foreground)",
};

// Select Options
export const SEASON_OPTIONS: LocalizedOption[] = [
    { value: "all", labelKey: "expenses.filters.allSeasons", fallbackLabel: "All seasons" },
    { value: "Rice Season 2025", labelKey: "expenses.filters.demoSeasonRice2025", fallbackLabel: "Rice Season 2025" },
    { value: "Corn Season 2025", labelKey: "expenses.filters.demoSeasonCorn2025", fallbackLabel: "Corn Season 2025" },
    { value: "Wheat Season 2025", labelKey: "expenses.filters.demoSeasonWheat2025", fallbackLabel: "Wheat Season 2025" },
];

export const CATEGORY_OPTIONS: LocalizedOption[] = [
    { value: "all", labelKey: "expenses.filters.allCategories", fallbackLabel: "All categories" },
    { value: "Fertilizer", labelKey: "expenses.categories.fertilizer", fallbackLabel: "Fertilizer" },
    { value: "Seeds", labelKey: "expenses.categories.seeds", fallbackLabel: "Seeds" },
    { value: "Labor", labelKey: "expenses.categories.labor", fallbackLabel: "Labor" },
    { value: "Equipment", labelKey: "expenses.categories.equipment", fallbackLabel: "Equipment" },
    { value: "Pesticide", labelKey: "expenses.categories.pesticide", fallbackLabel: "Pesticide" },
    { value: "Transportation", labelKey: "expenses.categories.transportation", fallbackLabel: "Transportation" },
    { value: "Utilities", labelKey: "expenses.categories.utilities", fallbackLabel: "Utilities" },
    { value: "Maintenance", labelKey: "expenses.categories.maintenance", fallbackLabel: "Maintenance" },
    { value: "Other", labelKey: "expenses.categories.other", fallbackLabel: "Other" },
];

export const STATUS_OPTIONS: LocalizedOption[] = [
    { value: "all", labelKey: "expenses.filters.allStatuses", fallbackLabel: "All statuses" },
    { value: "PAID", labelKey: "expenses.status.paid", fallbackLabel: "Paid" },
    { value: "PENDING", labelKey: "expenses.status.pending", fallbackLabel: "Pending" },
    { value: "UNPAID", labelKey: "expenses.status.unpaid", fallbackLabel: "Unpaid" },
];
