import { useMemo } from "react";
import { AlertTriangle, BarChart3, RefreshCcw, Sparkles, TrendingUp } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/shared/ui/card";
import { Badge } from "@/shared/ui/badge";
import { usePreferences } from "@/shared/contexts";
import { convertToDisplayCurrency, formatMoney } from "@/shared/lib";
import { useI18n } from "@/shared/lib/hooks/useI18n";
import {
    useExpenseCostAiSuggestion,
    useExpenseCostInsightsSummary,
    type ExpenseCostCategoryBreakdown,
} from "@/entities/expense";
import type { Expense } from "../types";

interface CostInsightsPanelProps {
    seasonId: number | null;
    expenses: Expense[];
}

interface InsightRow {
    id: string;
    kind: "info" | "warning";
    title: string;
    description: string;
    amount?: number;
}

const getSafeAmount = (value: number | null | undefined) =>
    typeof value === "number" && Number.isFinite(value) ? value : 0;

const getTopCategory = (rows: ExpenseCostCategoryBreakdown[]) =>
    rows
        .filter((row) => row.category && typeof row.amount === "number")
        .sort((left, right) => getSafeAmount(right.amount) - getSafeAmount(left.amount))[0];

const buildExpenseRows = (
    expenses: Expense[],
    t: (key: string, optionsOrDefault?: Record<string, unknown> | string) => string,
): InsightRow[] => {
    if (expenses.length === 0) {
        return [];
    }

    const totalsByCategory = new Map<string, number>();
    let totalAmount = 0;
    for (const expense of expenses) {
        const category = expense.category?.trim() || t("expenses.insights.uncategorized");
        const amount = getSafeAmount(expense.amount);
        totalAmount += amount;
        totalsByCategory.set(category, (totalsByCategory.get(category) ?? 0) + amount);
    }

    const sortedCategories = [...totalsByCategory.entries()].sort((a, b) => b[1] - a[1]);
    const topCategory = sortedCategories[0];
    const averageAmount = totalAmount / expenses.length;
    const missingReceipts = expenses.filter((expense) => !expense.attachmentUrl).length;

    const now = new Date();
    const recentStart = new Date(now);
    recentStart.setDate(recentStart.getDate() - 30);
    const previousStart = new Date(now);
    previousStart.setDate(previousStart.getDate() - 60);
    const previousEnd = new Date(now);
    previousEnd.setDate(previousEnd.getDate() - 30);

    let recentTotal = 0;
    let previousTotal = 0;
    for (const expense of expenses) {
        const expenseDate = new Date(`${expense.date}T00:00:00`);
        if (Number.isNaN(expenseDate.getTime())) continue;

        const amount = getSafeAmount(expense.amount);
        if (expenseDate >= recentStart && expenseDate <= now) {
            recentTotal += amount;
        } else if (expenseDate >= previousStart && expenseDate < previousEnd) {
            previousTotal += amount;
        }
    }

    const rows: InsightRow[] = [];
    if (topCategory) {
        const percent = totalAmount > 0 ? (topCategory[1] / totalAmount) * 100 : 0;
        rows.push({
            id: "top-category",
            kind: "info",
            title: t("expenses.insights.local.highestCostCategory.title"),
            description: t("expenses.insights.local.highestCostCategory.description", {
                category: topCategory[0],
                percent: percent.toFixed(1),
            }),
            amount: topCategory[1],
        });
    }

    rows.push({
        id: "avg-expense",
        kind: "info",
        title: t("expenses.insights.local.averageExpense.title"),
        description: t("expenses.insights.local.averageExpense.description", {
            count: expenses.length,
        }),
        amount: averageAmount,
    });

    if (missingReceipts > 0) {
        rows.push({
            id: "missing-receipts",
            kind: "warning",
            title: t("expenses.insights.local.missingReceipts.title"),
            description: t("expenses.insights.local.missingReceipts.description", {
                count: missingReceipts,
            }),
        });
    }

    if (recentTotal > 0 || previousTotal > 0) {
        const deltaPercent = previousTotal > 0
            ? ((recentTotal - previousTotal) / previousTotal) * 100
            : 100;
        const trendLabel = deltaPercent >= 0
            ? t("expenses.insights.local.recentTrend.up")
            : t("expenses.insights.local.recentTrend.down");
        rows.push({
            id: "recent-trend",
            kind: deltaPercent >= 30 ? "warning" : "info",
            title: t("expenses.insights.local.recentTrend.title"),
            description: t("expenses.insights.local.recentTrend.description", {
                trend: trendLabel,
                percent: Math.abs(deltaPercent).toFixed(1),
            }),
            amount: recentTotal,
        });
    }

    return rows;
};

export function AIOptimizationTips({ seasonId, expenses }: CostInsightsPanelProps) {
    const { preferences } = usePreferences();
    const { t } = useI18n();
    const hasSeason = typeof seasonId === "number" && seasonId > 0;
    const {
        data: summary,
        isLoading: isSummaryLoading,
        isError: isSummaryError,
        error: summaryError,
        refetch: refetchSummary,
    } = useExpenseCostInsightsSummary(seasonId ?? 0, { enabled: hasSeason });

    const aiSuggestionMutation = useExpenseCostAiSuggestion(seasonId ?? 0);

    const formatCurrency = (value: number) =>
        formatMoney(
            convertToDisplayCurrency(value, preferences.currency),
            preferences.currency,
            preferences.locale
        );

    const summaryRows = useMemo<InsightRow[]>(() => {
        if (!summary) {
            return [];
        }
        const rows: InsightRow[] = [];

        const topCategory = getTopCategory(summary.topCostCategories);
        if (topCategory?.category) {
            rows.push({
                id: "summary-top-category",
                kind: "info",
                title: t("expenses.insights.summary.topCategory.title"),
                description: t("expenses.insights.summary.topCategory.description", {
                    category: topCategory.category,
                }),
                amount: getSafeAmount(topCategory.amount),
            });
        }

        for (const [index, warning] of summary.warnings.entries()) {
            rows.push({
                id: `summary-warning-${index}`,
                kind: "warning",
                title: t("expenses.insights.summary.warningTitle"),
                description: warning,
            });
        }

        return rows;
    }, [summary, t]);

    const localRows = useMemo(() => buildExpenseRows(expenses, t), [expenses, t]);
    const insightRows = summaryRows.length > 0 ? summaryRows : localRows;
    const aiSuggestion = aiSuggestionMutation.data?.aiSuggestionText;
    const disclaimer = aiSuggestionMutation.data?.disclaimer
        ?? summary?.disclaimer
        ?? t("expenses.insights.fallbackDisclaimer");

    if (!hasSeason) {
        return (
            <Card className="border-secondary rounded-2xl shadow-sm bg-secondary/5">
                <CardHeader className="pb-3">
                    <CardTitle className="text-base text-foreground flex items-center gap-2">
                        <BarChart3 className="w-5 h-5 text-secondary" />
                        {t("expenses.insights.title")}
                    </CardTitle>
                    <CardDescription className="text-xs text-muted-foreground">
                        {t("expenses.insights.selectSeason")}
                    </CardDescription>
                </CardHeader>
            </Card>
        );
    }

    return (
        <Card className="border-secondary rounded-2xl shadow-sm bg-secondary/5">
            <CardHeader className="pb-3">
                <CardTitle className="text-base text-foreground flex items-center gap-2">
                    <BarChart3 className="w-5 h-5 text-secondary" />
                    {t("expenses.insights.title")}
                </CardTitle>
                <CardDescription className="text-xs text-muted-foreground">
                    {t("expenses.insights.description")}
                </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
                {isSummaryLoading && insightRows.length === 0 && (
                    <div className="rounded-xl border border-border bg-card p-3 text-xs text-muted-foreground">
                        {t("expenses.insights.loading")}
                    </div>
                )}

                {isSummaryError && insightRows.length === 0 && (
                    <div className="rounded-xl border border-destructive/30 bg-destructive/5 p-3 text-xs text-destructive space-y-2">
                        <div>
                            {t("expenses.insights.summaryLoadError", {
                                error: summaryError?.message ?? t("expenses.insights.unknownError"),
                            })}
                        </div>
                        <Button size="sm" variant="outline" onClick={() => refetchSummary()}>
                            <RefreshCcw className="w-3.5 h-3.5 mr-2" />
                            {t("common.retry")}
                        </Button>
                    </div>
                )}

                {insightRows.length === 0 && !isSummaryLoading && (
                    <div className="rounded-xl border border-border bg-card p-3 text-xs text-muted-foreground">
                        {t("expenses.insights.empty")}
                    </div>
                )}

                {insightRows.map((row) => (
                    <div
                        key={row.id}
                        className={`p-3 rounded-xl bg-card border ${row.kind === "warning"
                            ? "border-amber-300"
                            : "border-border"
                            }`}
                    >
                        <div className="flex items-start gap-3">
                            <div className={`w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 ${row.kind === "warning" ? "bg-amber-100" : "bg-primary/10"
                                }`}>
                                {row.kind === "warning" ? (
                                    <AlertTriangle className="w-4 h-4 text-amber-700" />
                                ) : (
                                    <TrendingUp className="w-4 h-4 text-primary" />
                                )}
                            </div>
                            <div className="flex-1 min-w-0">
                                <div className="flex items-start justify-between gap-2 mb-1">
                                    <h4 className="text-xs text-foreground">{row.title}</h4>
                                    {typeof row.amount === "number" && (
                                        <Badge className="bg-primary/10 text-primary border-primary/20 text-xs numeric flex-shrink-0">
                                            {formatCurrency(row.amount)}
                                        </Badge>
                                    )}
                                </div>
                                <p className="text-xs text-muted-foreground leading-relaxed">{row.description}</p>
                            </div>
                        </div>
                    </div>
                ))}

                <div className="pt-2 space-y-2">
                    <Button
                        variant="outline"
                        className="w-full rounded-xl border-secondary text-secondary hover:bg-secondary/10"
                        onClick={() => aiSuggestionMutation.mutate({ includeInventory: true })}
                        disabled={aiSuggestionMutation.isPending}
                    >
                        <Sparkles className="w-4 h-4 mr-2" />
                        {aiSuggestionMutation.isPending
                            ? t("expenses.insights.aiAnalyzing")
                            : t("expenses.insights.aiAnalyze")}
                    </Button>

                    {aiSuggestionMutation.isError && (
                        <div className="rounded-xl border border-destructive/30 bg-destructive/5 p-3 text-xs text-destructive">
                            {aiSuggestionMutation.error?.message ?? t("expenses.insights.aiError")}
                        </div>
                    )}

                    {aiSuggestion && (
                        <div className="rounded-xl border border-border bg-card p-3">
                            <p className="text-xs text-muted-foreground mb-2">{t("expenses.insights.aiSuggestion")}</p>
                            <p className="text-xs text-foreground whitespace-pre-wrap leading-relaxed">{aiSuggestion}</p>
                        </div>
                    )}

                    <p className="text-[11px] text-muted-foreground">
                        <strong>{t("expenses.insights.disclaimerLabel")}</strong> {disclaimer}
                    </p>
                </div>
            </CardContent>
        </Card>
    );
}
