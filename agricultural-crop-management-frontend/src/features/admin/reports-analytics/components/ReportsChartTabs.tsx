import {
  Button,
  CardContent,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tabs,
  TabsList,
  TabsTrigger,
} from "@/shared/ui";
import {
  AdminContentCard,
  ADMIN_CHART_COLORS,
  adminTabsListClass,
  adminTabsTriggerClass,
} from "@/features/admin/shared/ui";
import type {
  CostCategoryRow,
  CostTimeRow,
  CostVendorRow,
  ProfitRow,
  RevenueRow,
  YieldAnalyticsRow,
} from "@/features/admin/shared/api";
import { usePreferences } from "@/shared/contexts";
import { convertWeight, formatMoney, getWeightUnitLabel } from "@/shared/lib";
import { useI18n } from "@/shared/lib/hooks/useI18n";
import { ChevronDown } from "lucide-react";
import { useMemo, useState } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

export interface YieldTotals {
  actualYield: number;
  harvestCount: number;
}

export interface CostTotals {
  totalCost: number;
  expenseCount: number;
}

export interface RevenueTotals {
  totalQuantity: number;
  totalRevenue: number;
  avgPrice: number | null;
}

export interface ProfitTotals {
  totalRevenue: number;
  totalCost: number;
  grossProfit: number;
  marginPercent: number | null;
}

interface ReportsChartTabsProps {
  yieldRows: YieldAnalyticsRow[];
  yieldTotals?: YieldTotals | null;
  costRows: CostCategoryRow[];
  costTotals?: CostTotals | null;
  costVendorRows: CostVendorRow[];
  costTimeSeries: CostTimeRow[];
  revenueRows: RevenueRow[];
  revenueTotals?: RevenueTotals | null;
  profitRows: ProfitRow[];
  profitTotals?: ProfitTotals | null;
  activeTab: "yield" | "cost" | "revenue" | "profit";
  onTabChange: (tab: "yield" | "cost" | "revenue" | "profit") => void;
  onReset?: () => void;
  isLoading?: boolean;
  costGranularity: "DAY" | "WEEK" | "MONTH";
  onCostGranularityChange: (value: "DAY" | "WEEK" | "MONTH") => void;
  drilldownAvailable?: boolean;
  onRowClick?: (
    tab: "yield" | "cost" | "revenue" | "profit",
    row: unknown,
  ) => void;
}

const formatNumber = (
  num: number,
  locale: string,
  maximumFractionDigits?: number,
) => new Intl.NumberFormat(locale, { maximumFractionDigits }).format(num);

const formatCompactNumber = (num: number, locale: string) =>
  new Intl.NumberFormat(locale, {
    notation: "compact",
    maximumFractionDigits: 1,
  }).format(num);

const buildYieldLabel = (row: YieldAnalyticsRow, fallback: string) => {
  const parts = [
    row.farmName,
    row.plotName,
    row.cropName,
    row.varietyName,
  ].filter(Boolean);
  return parts.length > 0 ? parts.join(" / ") : fallback;
};

const buildCropPlotLabel = (row: {
  cropName?: string | null;
  plotName?: string | null;
}, fallback: string) => {
  const parts = [row.cropName, row.plotName].filter(Boolean);
  return parts.length > 0 ? parts.join(" / ") : fallback;
};

const getCategoryLabel = (category: string | null | undefined, fallback: string) =>
  category?.trim() || fallback;
const getVendorLabel = (vendor: string | null | undefined, fallback: string) =>
  vendor?.trim() || fallback;

// Empty state component
const EmptyState: React.FC<{ onReset?: () => void; text: string; resetText: string }> = ({ onReset, text, resetText }) => (
  <div className="h-[320px] flex flex-col items-center justify-center text-muted-foreground">
    <p className="mb-2 text-sm">{text}</p>
    {onReset && (
      <Button variant="link" onClick={onReset} className="text-primary text-sm">
        {resetText}
      </Button>
    )}
  </div>
);

// Loading spinner
const LoadingSpinner: React.FC = () => (
  <div className="h-[320px] flex items-center justify-center">
    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
  </div>
);

export const ReportsChartTabs: React.FC<ReportsChartTabsProps> = ({
  yieldRows,
  yieldTotals,
  costRows,
  costTotals,
  costVendorRows,
  costTimeSeries,
  revenueRows,
  revenueTotals,
  profitRows,
  profitTotals,
  activeTab,
  onTabChange,
  onReset,
  isLoading = false,
  costGranularity,
  onCostGranularityChange,
  drilldownAvailable = false,
  onRowClick,
}) => {
  const { t } = useI18n();
  const { preferences } = usePreferences();
  const unitLabel = getWeightUnitLabel(preferences.weightUnit);
  const weightDecimals = preferences.weightUnit === "G" ? 0 : 2;
  const [showTable, setShowTable] = useState(false);
  const canDrilldown = drilldownAvailable && !!onRowClick;

  const displayYieldData = useMemo(() => {
    return yieldRows.map((row) => {
      const actual = convertWeight(
        row.actualYield ?? 0,
        preferences.weightUnit,
      );
      return {
        label: buildYieldLabel(row, t('admin.reportsAnalytics.unknown')),
        actual,
        harvestCount: row.harvestCount ?? 0,
        row,
      };
    });
  }, [yieldRows, preferences.weightUnit, t]);

  const displayCostCategories = useMemo(() => {
    return costRows.map((row) => ({
      label: getCategoryLabel(row.category, t('admin.reportsAnalytics.uncategorized')),
      totalCost: row.totalCost ?? 0,
      expenseCount: row.expenseCount ?? 0,
      row,
    }));
  }, [costRows, t]);

  const displayRevenueData = useMemo(() => {
    return revenueRows.map((row) => ({
      label: buildCropPlotLabel(row, t('admin.reportsAnalytics.unknown')),
      totalRevenue: row.totalRevenue ?? 0,
      totalQuantity: row.totalQuantity ?? 0,
      avgPrice: row.avgPrice ?? null,
      row,
    }));
  }, [revenueRows, t]);

  const displayProfitData = useMemo(() => {
    return profitRows.map((row) => ({
      label: buildCropPlotLabel(row, t('admin.reportsAnalytics.unknown')),
      totalRevenue: row.totalRevenue ?? 0,
      totalCost: row.totalCost ?? 0,
      grossProfit: row.grossProfit ?? 0,
      marginPercent: row.marginPercent ?? null,
      row,
    }));
  }, [profitRows, t]);

  const formatWeightValue = (value: number) =>
    formatNumber(value, preferences.locale, weightDecimals);
  const formatCurrencyValue = (value: number) =>
    formatMoney(value, preferences.currency, preferences.locale);

  const isEmpty = (() => {
    switch (activeTab) {
      case "yield":
        return yieldRows.length === 0;
      case "cost":
        return (
          costRows.length === 0 &&
          costVendorRows.length === 0 &&
          costTimeSeries.length === 0
        );
      case "revenue":
        return revenueRows.length === 0;
      case "profit":
        return profitRows.length === 0;
      default:
        return true;
    }
  })();

  const handleRowClick = (
    tab: "yield" | "cost" | "revenue" | "profit",
    row: unknown,
  ) => {
    if (canDrilldown && onRowClick) {
      onRowClick(tab, row);
    }
  };

  return (
    <div className="space-y-6">
      {/* Tab Header with Controls */}
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        {/* Tab List */}
        <Tabs
          value={activeTab}
          onValueChange={(v) =>
            onTabChange(v as "yield" | "cost" | "revenue" | "profit")
          }
        >
          <div className="overflow-x-auto pb-1">
            <TabsList className={adminTabsListClass}>
              <TabsTrigger
                value="yield"
                className={adminTabsTriggerClass}
              >
                {t('admin.reportsAnalytics.tabs.yield')}
              </TabsTrigger>
              <TabsTrigger
                value="cost"
                className={adminTabsTriggerClass}
              >
                {t('admin.reportsAnalytics.tabs.cost')}
              </TabsTrigger>
              <TabsTrigger
                value="revenue"
                className={adminTabsTriggerClass}
              >
                {t('admin.reportsAnalytics.tabs.revenue')}
              </TabsTrigger>
              <TabsTrigger
                value="profit"
                className={adminTabsTriggerClass}
              >
                {t('admin.reportsAnalytics.tabs.profit')}
              </TabsTrigger>
            </TabsList>
          </div>
        </Tabs>

        <div className="flex flex-col sm:flex-row sm:items-center gap-2 w-full md:w-auto">
          {activeTab === "cost" && (
            <Select
              value={costGranularity}
              onValueChange={(value) =>
                onCostGranularityChange(value as "DAY" | "WEEK" | "MONTH")
              }
            >
              <SelectTrigger className="h-8 w-full sm:w-[140px] rounded-[14px] border-border bg-card text-foreground text-sm">
                <SelectValue placeholder={t('admin.reportsAnalytics.chart.granularity')} />
              </SelectTrigger>
              <SelectContent className="rounded-[14px]">
                <SelectItem value="DAY">{t('admin.reportsAnalytics.chart.byDay')}</SelectItem>
                <SelectItem value="WEEK">{t('admin.reportsAnalytics.chart.byWeek')}</SelectItem>
                <SelectItem value="MONTH">{t('admin.reportsAnalytics.chart.byMonth')}</SelectItem>
              </SelectContent>
            </Select>
          )}

          {/* Show Table Toggle */}
          <Button
            variant="outline"
            size="sm"
            onClick={() => setShowTable(!showTable)}
            className="h-8 w-full sm:w-auto px-3 rounded-[14px] border-border bg-muted hover:bg-muted/80 text-foreground font-medium text-sm"
          >
            {showTable
              ? t('admin.reportsAnalytics.chart.hideTable')
              : t('admin.reportsAnalytics.chart.showTable')}
            <ChevronDown
              className={`w-4 h-4 ml-2 transition-transform ${showTable ? "rotate-180" : ""}`}
            />
          </Button>
        </div>
      </div>

      {/* Chart Card */}
      <AdminContentCard>
        <CardContent className="p-4 sm:p-6">
          {/* Chart Header */}
          <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between mb-6">
            <div className="flex flex-col gap-0.5">
              <h3 className="text-lg sm:text-xl font-medium text-foreground">
                {activeTab === "yield" && t('admin.reportsAnalytics.chart.title.yield')}
                {activeTab === "cost" && t('admin.reportsAnalytics.chart.title.cost')}
                {activeTab === "revenue" && t('admin.reportsAnalytics.chart.title.revenue')}
                {activeTab === "profit" && t('admin.reportsAnalytics.chart.title.profit')}
              </h3>
              <p className="text-sm text-muted-foreground">
                {activeTab === "yield" &&
                  t('admin.reportsAnalytics.chart.description.yield')}
                {activeTab === "cost" && t('admin.reportsAnalytics.chart.description.cost')}
                {activeTab === "revenue" &&
                  t('admin.reportsAnalytics.chart.description.revenue')}
                {activeTab === "profit" &&
                  t('admin.reportsAnalytics.chart.description.profit')}
              </p>
            </div>
          </div>

          {isLoading ? (
            <LoadingSpinner />
          ) : isEmpty ? (
            <EmptyState
              onReset={onReset}
              text={t('admin.reportsAnalytics.chart.empty')}
              resetText={t('admin.reportsAnalytics.chart.resetFilters')}
            />
          ) : (
            <ResponsiveContainer width="100%" height={320}>
              {activeTab === "yield" ? (
                <BarChart
                  data={displayYieldData}
                  margin={{ top: 20, right: 30, left: 20, bottom: 60 }}
                  barCategoryGap="18%"
                >
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="var(--border)"
                    vertical={false}
                  />
                  <XAxis
                    dataKey="label"
                    tick={({ x, y, payload }) => {
                      const label = payload.value;
                      const shortLabel =
                        label.length > 20 ? `${label.slice(0, 20)}...` : label;
                      return (
                        <g transform={`translate(${x},${y})`}>
                          <title>{label}</title>
                          <text
                            x={0}
                            y={0}
                            dy={16}
                            textAnchor="middle"
                            fill="var(--muted-foreground)"
                            fontSize={12}
                          >
                            {shortLabel}
                          </text>
                        </g>
                      );
                    }}
                    tickLine={false}
                    axisLine={{ stroke: "var(--border)" }}
                    height={50}
                  />
                  <YAxis
                    tick={{ fontSize: 12, fill: "var(--muted-foreground)" }}
                    tickLine={false}
                    axisLine={false}
                    tickFormatter={(value) =>
                      formatCompactNumber(value as number, preferences.locale)
                    }
                  />
                  <Tooltip
                    formatter={(value: number) => [
                      formatWeightValue(value),
                      t('admin.reportsAnalytics.chart.yieldWithUnit', { unit: unitLabel }),
                    ]}
                    contentStyle={{
                      borderRadius: "12px",
                      border: "1px solid var(--border)",
                      backgroundColor: "var(--card)",
                      color: "var(--foreground)",
                      boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1)",
                      padding: "12px",
                    }}
                    cursor={{ fill: "rgba(0, 0, 0, 0.04)" }}
                  />
                  <Legend
                    verticalAlign="bottom"
                    wrapperStyle={{ paddingTop: "20px" }}
                  />
                  <Bar
                    dataKey="actual"
                    name={t('admin.reportsAnalytics.chart.yieldWithUnit', { unit: unitLabel })}
                    fill={ADMIN_CHART_COLORS.yield}
                    radius={[6, 6, 0, 0]}
                    maxBarSize={100}
                  />
                </BarChart>
              ) : activeTab === "cost" ? (
                <BarChart
                  data={displayCostCategories}
                  margin={{ top: 20, right: 30, left: 20, bottom: 60 }}
                  barCategoryGap="18%"
                >
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="var(--border)"
                    vertical={false}
                  />
                  <XAxis
                    dataKey="label"
                    tick={({ x, y, payload }) => {
                      const label = payload.value;
                      const shortLabel =
                        label.length > 18 ? `${label.slice(0, 18)}...` : label;
                      return (
                        <g transform={`translate(${x},${y})`}>
                          <title>{label}</title>
                          <text
                            x={0}
                            y={0}
                            dy={16}
                            textAnchor="middle"
                            fill="var(--muted-foreground)"
                            fontSize={12}
                          >
                            {shortLabel}
                          </text>
                        </g>
                      );
                    }}
                    tickLine={false}
                    axisLine={{ stroke: "var(--border)" }}
                    height={50}
                  />
                  <YAxis
                    tick={{ fontSize: 12, fill: "var(--muted-foreground)" }}
                    tickLine={false}
                    axisLine={false}
                    tickFormatter={(value) =>
                      formatCompactNumber(value as number, preferences.locale)
                    }
                  />
                  <Tooltip
                    formatter={(value: number) => [
                      formatCurrencyValue(value),
                      t('admin.reportsAnalytics.chart.totalCost'),
                    ]}
                    contentStyle={{
                      borderRadius: "12px",
                      border: "1px solid var(--border)",
                      backgroundColor: "var(--card)",
                      color: "var(--foreground)",
                      boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1)",
                      padding: "12px",
                    }}
                    cursor={{ fill: "rgba(0, 0, 0, 0.04)" }}
                  />
                  <Legend
                    verticalAlign="bottom"
                    wrapperStyle={{ paddingTop: "20px" }}
                  />
                  <Bar
                    dataKey="totalCost"
                    name={t('admin.reportsAnalytics.chart.totalCostWithCurrency', { currency: preferences.currency })}
                    fill={ADMIN_CHART_COLORS.cost}
                    radius={[6, 6, 0, 0]}
                    maxBarSize={100}
                  />
                </BarChart>
              ) : activeTab === "revenue" ? (
                <BarChart
                  data={displayRevenueData}
                  margin={{ top: 20, right: 30, left: 20, bottom: 60 }}
                  barCategoryGap="18%"
                >
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="var(--border)"
                    vertical={false}
                  />
                  <XAxis
                    dataKey="label"
                    tick={({ x, y, payload }) => {
                      const label = payload.value;
                      const shortLabel =
                        label.length > 18 ? `${label.slice(0, 18)}...` : label;
                      return (
                        <g transform={`translate(${x},${y})`}>
                          <title>{label}</title>
                          <text
                            x={0}
                            y={0}
                            dy={16}
                            textAnchor="middle"
                            fill="var(--muted-foreground)"
                            fontSize={12}
                          >
                            {shortLabel}
                          </text>
                        </g>
                      );
                    }}
                    tickLine={false}
                    axisLine={{ stroke: "var(--border)" }}
                    height={50}
                  />
                  <YAxis
                    tick={{ fontSize: 12, fill: "var(--muted-foreground)" }}
                    tickLine={false}
                    axisLine={false}
                    tickFormatter={(value) =>
                      formatCompactNumber(value as number, preferences.locale)
                    }
                  />
                  <Tooltip
                    formatter={(value: number) => [
                      formatCurrencyValue(value),
                      t('admin.reportsAnalytics.summary.revenue'),
                    ]}
                    contentStyle={{
                      borderRadius: "12px",
                      border: "1px solid var(--border)",
                      backgroundColor: "var(--card)",
                      color: "var(--foreground)",
                      boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1)",
                      padding: "12px",
                    }}
                    cursor={{ fill: "rgba(0, 0, 0, 0.04)" }}
                  />
                  <Legend
                    verticalAlign="bottom"
                    wrapperStyle={{ paddingTop: "20px" }}
                  />
                  <Bar
                    dataKey="totalRevenue"
                    name={t('admin.reportsAnalytics.chart.revenueWithCurrency', { currency: preferences.currency })}
                    fill={ADMIN_CHART_COLORS.revenue}
                    radius={[6, 6, 0, 0]}
                    maxBarSize={100}
                  />
                </BarChart>
              ) : (
                <BarChart
                  data={displayProfitData}
                  margin={{ top: 20, right: 30, left: 20, bottom: 60 }}
                  barCategoryGap="18%"
                >
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="var(--border)"
                    vertical={false}
                  />
                  <XAxis
                    dataKey="label"
                    tick={({ x, y, payload }) => {
                      const label = payload.value;
                      const shortLabel =
                        label.length > 18 ? `${label.slice(0, 18)}...` : label;
                      return (
                        <g transform={`translate(${x},${y})`}>
                          <title>{label}</title>
                          <text
                            x={0}
                            y={0}
                            dy={16}
                            textAnchor="middle"
                            fill="var(--muted-foreground)"
                            fontSize={12}
                          >
                            {shortLabel}
                          </text>
                        </g>
                      );
                    }}
                    tickLine={false}
                    axisLine={{ stroke: "var(--border)" }}
                    height={50}
                  />
                  <YAxis
                    tick={{ fontSize: 12, fill: "var(--muted-foreground)" }}
                    tickLine={false}
                    axisLine={false}
                    tickFormatter={(value) =>
                      formatCompactNumber(value as number, preferences.locale)
                    }
                  />
                  <Tooltip
                    formatter={(value: number, name: string) => {
                      const labels: Record<string, string> = {
                        totalRevenue: t('admin.reportsAnalytics.summary.revenue'),
                        totalCost: t('admin.reportsAnalytics.tabs.cost'),
                        grossProfit: t('admin.reportsAnalytics.summary.grossProfit'),
                      };
                      return [formatCurrencyValue(value), labels[name] || name];
                    }}
                    contentStyle={{
                      borderRadius: "12px",
                      border: "1px solid var(--border)",
                      backgroundColor: "var(--card)",
                      color: "var(--foreground)",
                      boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1)",
                      padding: "12px",
                    }}
                    cursor={{ fill: "rgba(0, 0, 0, 0.04)" }}
                  />
                  <Legend
                    verticalAlign="bottom"
                    wrapperStyle={{ paddingTop: "20px" }}
                  />
                  <Bar
                    dataKey="totalRevenue"
                    name={t('admin.reportsAnalytics.chart.revenueWithCurrency', { currency: preferences.currency })}
                    fill={ADMIN_CHART_COLORS.profitRevenue}
                    radius={[6, 6, 0, 0]}
                    maxBarSize={70}
                  />
                  <Bar
                    dataKey="totalCost"
                    name={t('admin.reportsAnalytics.chart.costWithCurrency', { currency: preferences.currency })}
                    fill={ADMIN_CHART_COLORS.profitCost}
                    radius={[6, 6, 0, 0]}
                    maxBarSize={70}
                  />
                  <Bar
                    dataKey="grossProfit"
                    name={t('admin.reportsAnalytics.chart.grossProfitWithCurrency', { currency: preferences.currency })}
                    fill={ADMIN_CHART_COLORS.profitGross}
                    radius={[6, 6, 0, 0]}
                    maxBarSize={70}
                  />
                </BarChart>
              )}
            </ResponsiveContainer>
          )}
        </CardContent>
      </AdminContentCard>

      {/* Data Tables (Collapsible) */}
      {showTable && (
        <AdminContentCard>
          <CardContent className="p-4 sm:p-6 space-y-6">
            {/* Yield Table */}
            {activeTab === "yield" && (
              <Table>
                <TableHeader>
                  <TableRow className="border-b border-border hover:bg-transparent">
                    <TableHead className="text-sm font-medium text-foreground">
                      {t('admin.reportsAnalytics.inventory.table.farm')}
                    </TableHead>
                    <TableHead className="text-sm font-medium text-foreground">
                      {t('admin.reportsAnalytics.table.plot')}
                    </TableHead>
                    <TableHead className="text-sm font-medium text-foreground">
                      {t('admin.reportsAnalytics.table.crop')}
                    </TableHead>
                    <TableHead className="text-sm font-medium text-foreground">
                      {t('admin.reportsAnalytics.table.variety')}
                    </TableHead>
                    <TableHead className="text-sm font-medium text-foreground text-right">
                      {t('admin.reportsAnalytics.chart.yieldWithUnit', { unit: unitLabel })}
                    </TableHead>
                    <TableHead className="text-sm font-medium text-foreground text-right">
                      {t('admin.reportsAnalytics.table.harvests')}
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {yieldRows.map((row, index) => {
                    const displayYield = convertWeight(
                      row.actualYield ?? 0,
                      preferences.weightUnit,
                    );
                    return (
                      <TableRow
                        key={index}
                        onClick={() => handleRowClick("yield", row)}
                        title={!canDrilldown ? t('admin.reportsAnalytics.pageNotAvailable') : undefined}
                        className={`border-b border-border/50 ${canDrilldown ? "hover:bg-muted/50 cursor-pointer" : "cursor-not-allowed"}`}
                      >
                        <TableCell className="text-sm text-foreground font-medium">
                          {row.farmName ?? "—"}
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {row.plotName ?? "—"}
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {row.cropName ?? "—"}
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {row.varietyName ?? "—"}
                        </TableCell>
                        <TableCell className="text-sm text-foreground text-right">
                          {formatWeightValue(displayYield)}
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground text-right">
                          {row.harvestCount ?? 0}
                        </TableCell>
                      </TableRow>
                    );
                  })}
                  {yieldTotals && (
                    <TableRow className="border-t border-border bg-muted/50">
                      <TableCell
                        className="text-sm font-semibold text-foreground"
                        colSpan={4}
                      >
                        {t('admin.reportsAnalytics.table.total')}
                      </TableCell>
                      <TableCell className="text-sm font-semibold text-foreground text-right">
                        {formatWeightValue(
                          convertWeight(
                            yieldTotals.actualYield ?? 0,
                            preferences.weightUnit,
                          ),
                        )}
                      </TableCell>
                      <TableCell className="text-sm font-semibold text-foreground text-right">
                        {yieldTotals.harvestCount ?? 0}
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            )}

            {/* Cost Tables */}
            {activeTab === "cost" && (
              <div className="space-y-6">
                <div className="space-y-2">
                  <div className="text-sm font-semibold text-foreground">
                    {t('admin.reportsAnalytics.table.categoryBreakdown')}
                  </div>
                  <Table>
                    <TableHeader>
                      <TableRow className="border-b border-border hover:bg-transparent">
                        <TableHead className="text-sm font-medium text-foreground">
                          {t('admin.reportsAnalytics.table.category')}
                        </TableHead>
                        <TableHead className="text-sm font-medium text-foreground text-right">
                          {t('admin.reportsAnalytics.chart.totalCostWithCurrency', { currency: preferences.currency })}
                        </TableHead>
                        <TableHead className="text-sm font-medium text-foreground text-right">
                          {t('admin.reportsAnalytics.expensesYield.expenses')}
                        </TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {costRows.length === 0 && (
                        <TableRow>
                          <TableCell
                            colSpan={3}
                            className="text-sm text-muted-foreground text-center py-6"
                          >
                            {t('admin.reportsAnalytics.table.noCategoryData')}
                          </TableCell>
                        </TableRow>
                      )}
                      {costRows.map((row, index) => (
                        <TableRow
                          key={index}
                          onClick={() => handleRowClick("cost", row)}
                          title={
                            !canDrilldown ? t('admin.reportsAnalytics.pageNotAvailable') : undefined
                          }
                          className={`border-b border-border/50 ${canDrilldown ? "hover:bg-muted/50 cursor-pointer" : "cursor-not-allowed"}`}
                        >
                          <TableCell className="text-sm text-foreground font-medium">
                            {getCategoryLabel(row.category, t('admin.reportsAnalytics.uncategorized'))}
                          </TableCell>
                          <TableCell className="text-sm text-foreground text-right">
                            {formatCurrencyValue(row.totalCost ?? 0)}
                          </TableCell>
                          <TableCell className="text-sm text-muted-foreground text-right">
                            {row.expenseCount ?? 0}
                          </TableCell>
                        </TableRow>
                      ))}
                      {costTotals && (
                        <TableRow className="border-t border-border bg-muted/50">
                          <TableCell className="text-sm font-semibold text-foreground">
                            {t('admin.reportsAnalytics.table.total')}
                          </TableCell>
                          <TableCell className="text-sm font-semibold text-foreground text-right">
                            {formatCurrencyValue(costTotals.totalCost ?? 0)}
                          </TableCell>
                          <TableCell className="text-sm font-semibold text-foreground text-right">
                            {costTotals.expenseCount ?? 0}
                          </TableCell>
                        </TableRow>
                      )}
                    </TableBody>
                  </Table>
                </div>

                <div className="space-y-2">
                  <div className="text-sm font-semibold text-foreground">
                    {t('admin.reportsAnalytics.table.vendorBreakdown')}
                  </div>
                  <Table>
                    <TableHeader>
                      <TableRow className="border-b border-border hover:bg-transparent">
                        <TableHead className="text-sm font-medium text-foreground">
                          {t('admin.reportsAnalytics.table.vendor')}
                        </TableHead>
                        <TableHead className="text-sm font-medium text-foreground text-right">
                          {t('admin.reportsAnalytics.chart.totalCostWithCurrency', { currency: preferences.currency })}
                        </TableHead>
                        <TableHead className="text-sm font-medium text-foreground text-right">
                          {t('admin.reportsAnalytics.expensesYield.expenses')}
                        </TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {costVendorRows.length === 0 && (
                        <TableRow>
                          <TableCell
                            colSpan={3}
                            className="text-sm text-muted-foreground text-center py-6"
                          >
                            {t('admin.reportsAnalytics.table.noVendorData')}
                          </TableCell>
                        </TableRow>
                      )}
                      {costVendorRows.map((row, index) => (
                        <TableRow
                          key={index}
                          onClick={() => handleRowClick("cost", row)}
                          title={
                            !canDrilldown ? t('admin.reportsAnalytics.pageNotAvailable') : undefined
                          }
                          className={`border-b border-border/50 ${canDrilldown ? "hover:bg-muted/50 cursor-pointer" : "cursor-not-allowed"}`}
                        >
                          <TableCell className="text-sm text-foreground font-medium">
                            {getVendorLabel(row.vendorName, t('admin.reportsAnalytics.unassigned'))}
                          </TableCell>
                          <TableCell className="text-sm text-foreground text-right">
                            {formatCurrencyValue(row.totalCost ?? 0)}
                          </TableCell>
                          <TableCell className="text-sm text-muted-foreground text-right">
                            {row.expenseCount ?? 0}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>

                <div className="space-y-2">
                  <div className="text-sm font-semibold text-foreground">
                    {t('admin.reportsAnalytics.table.timeSeries')}
                  </div>
                  <Table>
                    <TableHeader>
                      <TableRow className="border-b border-border hover:bg-transparent">
                        <TableHead className="text-sm font-medium text-foreground">
                          {t('admin.reportsAnalytics.table.period')}
                        </TableHead>
                        <TableHead className="text-sm font-medium text-foreground text-right">
                          {t('admin.reportsAnalytics.chart.totalCostWithCurrency', { currency: preferences.currency })}
                        </TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {costTimeSeries.length === 0 && (
                        <TableRow>
                          <TableCell
                            colSpan={2}
                            className="text-sm text-muted-foreground text-center py-6"
                          >
                            {t('admin.reportsAnalytics.table.noTimeSeriesData')}
                          </TableCell>
                        </TableRow>
                      )}
                      {costTimeSeries.map((row, index) => (
                        <TableRow
                          key={index}
                          onClick={() => handleRowClick("cost", row)}
                          title={
                            !canDrilldown ? t('admin.reportsAnalytics.pageNotAvailable') : undefined
                          }
                          className={`border-b border-border/50 ${canDrilldown ? "hover:bg-muted/50 cursor-pointer" : "cursor-not-allowed"}`}
                        >
                          <TableCell className="text-sm text-foreground font-medium">
                            {row.label ?? row.periodStart ?? "—"}
                          </TableCell>
                          <TableCell className="text-sm text-foreground text-right">
                            {formatCurrencyValue(row.totalCost ?? 0)}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              </div>
            )}

            {/* Revenue Table */}
            {activeTab === "revenue" && (
              <Table>
                <TableHeader>
                  <TableRow className="border-b border-border hover:bg-transparent">
                    <TableHead className="text-sm font-medium text-foreground">
                      {t('admin.reportsAnalytics.table.crop')}
                    </TableHead>
                    <TableHead className="text-sm font-medium text-foreground">
                      {t('admin.reportsAnalytics.table.plot')}
                    </TableHead>
                    <TableHead className="text-sm font-medium text-foreground text-right">
                      {t('admin.reportsAnalytics.table.quantityWithUnit', { unit: unitLabel })}
                    </TableHead>
                    <TableHead className="text-sm font-medium text-foreground text-right">
                      {t('admin.reportsAnalytics.chart.revenueWithCurrency', { currency: preferences.currency })}
                    </TableHead>
                    <TableHead className="text-sm font-medium text-foreground text-right">
                      {t('admin.reportsAnalytics.table.avgPrice')}
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {revenueRows.map((row, index) => {
                    const displayQuantity = convertWeight(
                      row.totalQuantity ?? 0,
                      preferences.weightUnit,
                    );
                    const avgPriceLabel =
                      row.avgPrice != null
                        ? `${formatCurrencyValue(row.avgPrice)}/${unitLabel}`
                        : "—";
                    return (
                      <TableRow
                        key={index}
                        onClick={() => handleRowClick("revenue", row)}
                        title={!canDrilldown ? t('admin.reportsAnalytics.pageNotAvailable') : undefined}
                        className={`border-b border-border/50 ${canDrilldown ? "hover:bg-muted/50 cursor-pointer" : "cursor-not-allowed"}`}
                      >
                        <TableCell className="text-sm text-foreground font-medium">
                          {row.cropName ?? "—"}
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {row.plotName ?? "—"}
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground text-right">
                          {formatWeightValue(displayQuantity)}
                        </TableCell>
                        <TableCell className="text-sm text-foreground text-right">
                          {formatCurrencyValue(row.totalRevenue ?? 0)}
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground text-right">
                          {avgPriceLabel}
                        </TableCell>
                      </TableRow>
                    );
                  })}
                  {revenueTotals && (
                    <TableRow className="border-t border-border bg-muted/50">
                      <TableCell
                        className="text-sm font-semibold text-foreground"
                        colSpan={2}
                      >
                        {t('admin.reportsAnalytics.table.total')}
                      </TableCell>
                      <TableCell className="text-sm font-semibold text-foreground text-right">
                        {formatWeightValue(
                          convertWeight(
                            revenueTotals.totalQuantity ?? 0,
                            preferences.weightUnit,
                          ),
                        )}
                      </TableCell>
                      <TableCell className="text-sm font-semibold text-foreground text-right">
                        {formatCurrencyValue(revenueTotals.totalRevenue ?? 0)}
                      </TableCell>
                      <TableCell className="text-sm font-semibold text-foreground text-right">
                        {revenueTotals.avgPrice != null
                          ? `${formatCurrencyValue(revenueTotals.avgPrice)}/${unitLabel}`
                          : "—"}
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            )}

            {/* Profit Table */}
            {activeTab === "profit" && (
              <Table>
                <TableHeader>
                  <TableRow className="border-b border-border hover:bg-transparent">
                    <TableHead className="text-sm font-medium text-foreground">
                      {t('admin.reportsAnalytics.table.crop')}
                    </TableHead>
                    <TableHead className="text-sm font-medium text-foreground">
                      {t('admin.reportsAnalytics.table.plot')}
                    </TableHead>
                    <TableHead className="text-sm font-medium text-foreground text-right">
                      {t('admin.reportsAnalytics.chart.revenueWithCurrency', { currency: preferences.currency })}
                    </TableHead>
                    <TableHead className="text-sm font-medium text-foreground text-right">
                      {t('admin.reportsAnalytics.chart.costWithCurrency', { currency: preferences.currency })}
                    </TableHead>
                    <TableHead className="text-sm font-medium text-foreground text-right">
                      {t('admin.reportsAnalytics.chart.grossProfitWithCurrency', { currency: preferences.currency })}
                    </TableHead>
                    <TableHead className="text-sm font-medium text-foreground text-right">
                      {t('admin.reportsAnalytics.table.margin')}
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {profitRows.map((row, index) => (
                    <TableRow
                      key={index}
                      onClick={() => handleRowClick("profit", row)}
                      title={!canDrilldown ? t('admin.reportsAnalytics.pageNotAvailable') : undefined}
                      className={`border-b border-border/50 ${canDrilldown ? "hover:bg-muted/50 cursor-pointer" : "cursor-not-allowed"}`}
                    >
                      <TableCell className="text-sm text-foreground font-medium">
                        {row.cropName ?? "—"}
                      </TableCell>
                      <TableCell className="text-sm text-muted-foreground">
                        {row.plotName ?? "—"}
                      </TableCell>
                      <TableCell className="text-sm text-foreground text-right">
                        {formatCurrencyValue(row.totalRevenue ?? 0)}
                      </TableCell>
                      <TableCell className="text-sm text-warning text-right">
                        {formatCurrencyValue(row.totalCost ?? 0)}
                      </TableCell>
                      <TableCell
                        className={`text-sm text-right font-medium ${(row.grossProfit ?? 0) >= 0 ? "text-success" : "text-destructive"}`}
                      >
                        {(row.grossProfit ?? 0) >= 0 ? "+" : ""}
                        {formatCurrencyValue(row.grossProfit ?? 0)}
                      </TableCell>
                      <TableCell
                        className={`text-sm text-right font-medium ${(row.marginPercent ?? 0) >= 0 ? "text-success" : "text-destructive"}`}
                      >
                        {row.marginPercent != null
                          ? `${row.marginPercent.toFixed(1)}%`
                          : "—"}
                      </TableCell>
                    </TableRow>
                  ))}
                  {profitTotals && (
                    <TableRow className="border-t border-border bg-muted/50">
                      <TableCell
                        className="text-sm font-semibold text-foreground"
                        colSpan={2}
                      >
                        {t('admin.reportsAnalytics.table.total')}
                      </TableCell>
                      <TableCell className="text-sm font-semibold text-foreground text-right">
                        {formatCurrencyValue(profitTotals.totalRevenue ?? 0)}
                      </TableCell>
                      <TableCell className="text-sm font-semibold text-foreground text-right">
                        {formatCurrencyValue(profitTotals.totalCost ?? 0)}
                      </TableCell>
                      <TableCell className="text-sm font-semibold text-foreground text-right">
                        {formatCurrencyValue(profitTotals.grossProfit ?? 0)}
                      </TableCell>
                      <TableCell className="text-sm font-semibold text-foreground text-right">
                        {profitTotals.marginPercent != null
                          ? `${profitTotals.marginPercent.toFixed(1)}%`
                          : "—"}
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </AdminContentCard>
      )}
    </div>
  );
};
