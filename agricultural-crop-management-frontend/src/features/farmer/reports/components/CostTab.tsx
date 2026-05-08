import {
  Bar,
  BarChart as RechartsBarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
  XAxis,
  YAxis,
} from "recharts";
import { AlertCircle, AlertTriangle, Sparkles, Wallet } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { usePreferences } from "@/shared/contexts";
import {
  convertCostPerKg,
  convertToDisplayCurrency,
  formatMoney,
  getWeightUnitLabel,
} from "@/shared/lib";
import type {
  CostOptimizationAiSuggestion,
  CostOptimizationSummary,
} from "../types";

interface CostTabProps {
  summary: CostOptimizationSummary | null;
  summaryLoading: boolean;
  summaryError: string | null;
  onRetrySummary: () => void;
  aiSuggestion: CostOptimizationAiSuggestion | null;
  aiLoading: boolean;
  aiError: string | null;
  onAnalyzeWithAi: () => void;
}

const FALLBACK_DISCLAIMER =
  "AI suggestions are for reference only. Do not auto-apply expense, budget, or inventory changes.";

export function CostTab({
  summary,
  summaryLoading,
  summaryError,
  onRetrySummary,
  aiSuggestion,
  aiLoading,
  aiError,
  onAnalyzeWithAi,
}: CostTabProps) {
  const { preferences } = usePreferences();
  const resolvedSummary = aiSuggestion ?? summary;
  const unitLabel = getWeightUnitLabel(preferences.weightUnit);
  const noData = "--";

  const formatCurrency = (value: number | null | undefined) => {
    if (typeof value !== "number" || Number.isNaN(value)) return noData;
    return formatMoney(
      convertToDisplayCurrency(value, preferences.currency),
      preferences.currency,
      preferences.locale
    );
  };

  const formatPercent = (value: number | null | undefined) => {
    if (typeof value !== "number" || Number.isNaN(value)) return noData;
    return `${new Intl.NumberFormat(preferences.locale, {
      minimumFractionDigits: 0,
      maximumFractionDigits: 1,
    }).format(value)}%`;
  };

  const formatCostPerWeightUnit = (value: number | null | undefined) => {
    if (typeof value !== "number" || Number.isNaN(value)) return noData;
    const convertedValue = convertCostPerKg(value, preferences.weightUnit);
    return `${formatCurrency(convertedValue)} / ${unitLabel}`;
  };

  const generatedAtLabel = (() => {
    const generatedAt = aiSuggestion?.generatedAt;
    if (!generatedAt) return null;

    const parsedDate = new Date(generatedAt);
    if (Number.isNaN(parsedDate.getTime())) return generatedAt;
    return parsedDate.toLocaleString(preferences.locale);
  })();

  const costCategoryRows = resolvedSummary?.topCostCategories?.length
    ? resolvedSummary.topCostCategories
    : resolvedSummary?.expenseByCategory ?? [];

  const categoryChartData = costCategoryRows.slice(0, 6).map((row, index) => ({
    category: row.category || `Category #${index + 1}`,
    amount: row.amount ?? 0,
  }));

  const warnings = resolvedSummary?.warnings ?? [];
  const disclaimer =
    aiSuggestion?.disclaimer ?? resolvedSummary?.disclaimer ?? FALLBACK_DISCLAIMER;

  if (summaryLoading) {
    return (
      <Card className="border-border rounded-xl">
        <CardContent className="py-6 text-sm text-muted-foreground">
          Loading cost optimization summary...
        </CardContent>
      </Card>
    );
  }

  if (summaryError) {
    return (
      <Card className="border-destructive/20 bg-destructive/5 rounded-xl">
        <CardContent className="py-5 space-y-3">
          <div className="flex items-start gap-2 text-destructive text-sm">
            <AlertCircle className="w-4 h-4 mt-0.5" />
            <p>Failed to load cost optimization summary: {summaryError}</p>
          </div>
          <Button variant="outline" onClick={onRetrySummary}>
            Retry
          </Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h3 className="text-lg text-foreground mb-1">Cost Optimization</h3>
        <p className="text-sm text-muted-foreground">
          Seasonal budget health, cost concentration, and AI reference guidance.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
        <Card className="border-border rounded-xl">
          <CardContent className="p-4 space-y-2">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">
              Seasonal Budget
            </p>
            <p className="text-2xl font-semibold numeric">
              {formatCurrency(resolvedSummary?.budgetAmount)}
            </p>
          </CardContent>
        </Card>
        <Card className="border-border rounded-xl">
          <CardContent className="p-4 space-y-2">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">
              Total Expense
            </p>
            <p className="text-2xl font-semibold numeric">
              {formatCurrency(resolvedSummary?.totalExpense)}
            </p>
          </CardContent>
        </Card>
        <Card className="border-border rounded-xl">
          <CardContent className="p-4 space-y-2">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">
              Remaining Budget
            </p>
            <p className="text-2xl font-semibold numeric">
              {formatCurrency(resolvedSummary?.remainingBudget)}
            </p>
          </CardContent>
        </Card>
        <Card className="border-border rounded-xl">
          <CardContent className="p-4 space-y-2">
            <p className="text-xs uppercase tracking-wide text-muted-foreground">
              Cost per Expected Yield
            </p>
            <p className="text-xl font-semibold numeric">
              {formatCostPerWeightUnit(resolvedSummary?.costPerExpectedKg)}
            </p>
            <p className="text-xs text-muted-foreground">
              Actual: {formatCostPerWeightUnit(resolvedSummary?.costPerActualKg)}
            </p>
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
        <Card className="border-border rounded-xl">
          <CardHeader className="pb-3">
            <CardTitle className="text-base flex items-center gap-2">
              <Wallet className="w-4 h-4 text-primary" />
              Top cost categories
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {categoryChartData.length > 0 ? (
              <ResponsiveContainer width="100%" height={260}>
                <RechartsBarChart data={categoryChartData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                  <XAxis
                    dataKey="category"
                    tick={{ fontSize: 12, fill: "var(--muted-foreground)" }}
                  />
                  <YAxis tick={{ fontSize: 12, fill: "var(--muted-foreground)" }} />
                  <RechartsTooltip
                    formatter={(value: number | string) => {
                      const numericValue = Number(value);
                      return formatCurrency(
                        Number.isFinite(numericValue) ? numericValue : null
                      );
                    }}
                  />
                  <Bar dataKey="amount" fill="var(--chart-1)" radius={[6, 6, 0, 0]} />
                </RechartsBarChart>
              </ResponsiveContainer>
            ) : (
              <p className="text-sm text-muted-foreground">
                No category cost data available for this season.
              </p>
            )}

            <div className="rounded-xl border border-border overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow className="bg-muted hover:bg-muted">
                    <TableHead>Category</TableHead>
                    <TableHead className="text-right">Amount</TableHead>
                    <TableHead className="text-right">% of total</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {costCategoryRows.map((row, index) => (
                    <TableRow key={`${row.category ?? "category"}-${index}`}>
                      <TableCell>{row.category || "UNCATEGORIZED"}</TableCell>
                      <TableCell className="text-right numeric">
                        {formatCurrency(row.amount)}
                      </TableCell>
                      <TableCell className="text-right numeric">
                        {formatPercent(row.percentageOfTotal)}
                      </TableCell>
                    </TableRow>
                  ))}
                  {costCategoryRows.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={3} className="text-center text-muted-foreground">
                        No top categories available.
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </div>
          </CardContent>
        </Card>

        <Card className="border-border rounded-xl">
          <CardHeader className="pb-3">
            <CardTitle className="text-base flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-amber-600" />
              Warnings
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {warnings.length > 0 ? (
              warnings.map((warning, index) => (
                <div
                  key={`${warning}-${index}`}
                  className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900"
                >
                  {warning}
                </div>
              ))
            ) : (
              <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800">
                No warning rules were triggered for this season.
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <Card className="border-secondary/20 rounded-xl bg-secondary/5">
        <CardHeader className="pb-3">
          <CardTitle className="text-base flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-secondary" />
            AI cost optimization suggestion
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap items-center gap-3">
            <Button onClick={onAnalyzeWithAi} disabled={aiLoading}>
              {aiLoading
                ? "Dang phan tich chi phi..."
                : "Phan tich toi uu chi phi bang AI"}
            </Button>
            {generatedAtLabel && (
              <Badge variant="outline" className="text-xs">
                Generated at: {generatedAtLabel}
              </Badge>
            )}
          </div>

          {aiError && (
            <div className="rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-2 text-sm text-destructive">
              {aiError}
            </div>
          )}

          {aiSuggestion?.aiSuggestionText ? (
            <div className="rounded-xl border border-border bg-card p-4">
              <p className="text-sm leading-relaxed whitespace-pre-wrap">
                {aiSuggestion.aiSuggestionText}
              </p>
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">
              AI suggestions appear here after analysis. Use them as reference only.
            </p>
          )}

          <div className="rounded-lg border border-border bg-muted/30 px-3 py-2 text-xs text-muted-foreground">
            <strong>Disclaimer:</strong> {disclaimer}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
