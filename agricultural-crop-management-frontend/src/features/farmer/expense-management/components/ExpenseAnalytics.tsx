import { useMemo } from "react";
import { TrendingUp, PieChart as PieChartIcon, BarChart3, ListTodo, RefreshCcw } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/card";
import {
    PieChart,
    Pie,
    Cell,
    LineChart,
    Line,
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip as RechartsTooltip,
    ResponsiveContainer,
    Legend,
} from "recharts";
import { usePreferences, useOptionalSeason } from "@/shared/contexts";
import { formatMoney, convertToDisplayCurrency } from "@/shared/lib";
import { useAllFarmerExpenses, type Expense as ApiExpense } from "@/entities/expense";
import { CATEGORY_COLORS } from "../constants";

const getExpenseAmount = (expense: ApiExpense) =>
    expense.amount ?? expense.totalCost ?? ((expense.unitPrice ?? 0) * (expense.quantity ?? 1));

const getCategoryLabel = (expense: ApiExpense) =>
    expense.category?.trim() || "Uncategorized";

const getMonthLabel = (rawDate: string) => {
    const date = new Date(`${rawDate}T00:00:00`);
    if (Number.isNaN(date.getTime())) {
        return rawDate;
    }
    return new Intl.DateTimeFormat("en-US", { month: "short", year: "numeric" }).format(date);
};

export function ExpenseAnalytics() {
    const { preferences } = usePreferences();
    const seasonContext = useOptionalSeason();
    const seasonId = seasonContext?.selectedSeasonId ?? null;
    const hasSeason = !!seasonId;

    const {
        data: expensePage,
        isLoading,
        error,
        refetch,
    } = useAllFarmerExpenses(
        hasSeason
            ? {
                seasonId,
                page: 0,
                size: 500,
            }
            : undefined,
        { enabled: hasSeason }
    );

    const expenses = expensePage?.items ?? [];

    const formatValue = (value: number) =>
        formatMoney(
            convertToDisplayCurrency(value, preferences.currency),
            preferences.currency,
            preferences.locale
        );

    const categoryChartData = useMemo(() => {
        const totals = new Map<string, number>();
        for (const expense of expenses) {
            const category = getCategoryLabel(expense);
            totals.set(category, (totals.get(category) ?? 0) + getExpenseAmount(expense));
        }

        return [...totals.entries()].map(([name, value]) => ({
            name,
            value,
            color: CATEGORY_COLORS[name] || "var(--muted-foreground)",
        }));
    }, [expenses]);

    const vendorChartData = useMemo(() => {
        const totals = new Map<string, number>();
        for (const expense of expenses) {
            const vendor = expense.vendorName?.trim() || "Unassigned";
            totals.set(vendor, (totals.get(vendor) ?? 0) + getExpenseAmount(expense));
        }

        return [...totals.entries()]
            .map(([name, value]) => ({ name, value }))
            .sort((a, b) => b.value - a.value)
            .slice(0, 10);
    }, [expenses]);

    const timeSeriesData = useMemo(() => {
        const totals = new Map<string, number>();
        for (const expense of expenses) {
            const month = getMonthLabel(expense.expenseDate);
            totals.set(month, (totals.get(month) ?? 0) + getExpenseAmount(expense));
        }

        return [...totals.entries()].map(([period, total]) => ({ period, total }));
    }, [expenses]);

    const taskData = useMemo(() => {
        const totals = new Map<string, number>();
        for (const expense of expenses) {
            const task = expense.taskTitle?.trim() || "Unassigned";
            totals.set(task, (totals.get(task) ?? 0) + getExpenseAmount(expense));
        }
        return [...totals.entries()]
            .map(([taskTitle, totalAmount]) => ({ taskTitle, totalAmount }))
            .sort((a, b) => b.totalAmount - a.totalAmount);
    }, [expenses]);

    if (!hasSeason) {
        return (
            <div className="rounded-xl border border-border bg-card p-6 text-sm text-muted-foreground">
                Select a season to view analytics.
            </div>
        );
    }

    if (isLoading) {
        return (
            <div className="rounded-xl border border-border bg-card p-6 text-sm text-muted-foreground">
                Loading analytics...
            </div>
        );
    }

    if (error) {
        return (
            <div className="rounded-xl border border-destructive/30 bg-destructive/5 p-6 text-sm text-destructive space-y-3">
                <div>Failed to load analytics: {error.message}</div>
                <Button variant="outline" size="sm" onClick={() => refetch()}>
                    <RefreshCcw className="w-4 h-4 mr-2" />
                    Retry
                </Button>
            </div>
        );
    }

    if (expenses.length === 0) {
        return (
            <div className="rounded-xl border border-border bg-card p-6 text-sm text-muted-foreground">
                No expenses available yet for analytics.
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <Card className="border-border rounded-xl">
                <CardHeader>
                    <CardTitle className="text-base text-foreground flex items-center gap-2">
                        <TrendingUp className="w-5 h-5 text-primary" />
                        Expense Trend (Monthly)
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <ResponsiveContainer width="100%" height={300}>
                        <LineChart data={timeSeriesData}>
                            <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                            <XAxis
                                dataKey="period"
                                tick={{ fontSize: 12, fill: "var(--muted-foreground)" }}
                            />
                            <YAxis tick={{ fontSize: 12, fill: "var(--muted-foreground)" }} />
                            <RechartsTooltip formatter={(value: number) => formatValue(value)} />
                            <Legend />
                            <Line
                                type="monotone"
                                dataKey="total"
                                stroke="var(--primary)"
                                strokeWidth={3}
                                name="Total"
                            />
                        </LineChart>
                    </ResponsiveContainer>
                </CardContent>
            </Card>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <Card className="border-border rounded-xl">
                    <CardHeader>
                        <CardTitle className="text-base text-foreground flex items-center gap-2">
                            <PieChartIcon className="w-5 h-5 text-secondary" />
                            Category Breakdown
                        </CardTitle>
                    </CardHeader>
                    <CardContent>
                        <ResponsiveContainer width="100%" height={250}>
                            <PieChart>
                                <Pie
                                    data={categoryChartData}
                                    cx="50%"
                                    cy="50%"
                                    innerRadius={60}
                                    outerRadius={90}
                                    paddingAngle={3}
                                    dataKey="value"
                                >
                                    {categoryChartData.map((entry, index) => (
                                        <Cell key={`cell-${index}`} fill={entry.color} />
                                    ))}
                                </Pie>
                                <RechartsTooltip formatter={(value: number) => formatValue(value)} />
                            </PieChart>
                        </ResponsiveContainer>

                        <div className="mt-4 space-y-2">
                            {categoryChartData.map((item) => (
                                <div
                                    key={item.name}
                                    className="flex items-center justify-between text-xs"
                                >
                                    <div className="flex items-center gap-2">
                                        <div
                                            className="w-3 h-3 rounded-full"
                                            style={{ backgroundColor: item.color }}
                                        ></div>
                                        <span className="text-muted-foreground">{item.name}</span>
                                    </div>
                                    <span className="numeric text-foreground">
                                        {formatValue(item.value)}
                                    </span>
                                </div>
                            ))}
                        </div>
                    </CardContent>
                </Card>

                <Card className="border-border rounded-xl">
                    <CardHeader>
                        <CardTitle className="text-base text-foreground flex items-center gap-2">
                            <BarChart3 className="w-5 h-5 text-accent" />
                            Vendor Spend
                        </CardTitle>
                    </CardHeader>
                    <CardContent>
                        <ResponsiveContainer width="100%" height={280}>
                            <BarChart data={vendorChartData}>
                                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                                <XAxis
                                    dataKey="name"
                                    tick={{ fontSize: 10, fill: "var(--muted-foreground)" }}
                                    angle={-35}
                                    textAnchor="end"
                                    height={70}
                                />
                                <YAxis tick={{ fontSize: 12, fill: "var(--muted-foreground)" }} />
                                <RechartsTooltip formatter={(value: number) => formatValue(value)} />
                                <Bar dataKey="value" fill="var(--primary)" name="Total" />
                            </BarChart>
                        </ResponsiveContainer>
                    </CardContent>
                </Card>
            </div>

            <Card className="border-border rounded-xl">
                <CardHeader>
                    <CardTitle className="text-base text-foreground flex items-center gap-2">
                        <ListTodo className="w-5 h-5 text-primary" />
                        Task Spend
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="space-y-2 text-sm">
                        {taskData.map((task) => (
                            <div key={task.taskTitle} className="flex items-center justify-between">
                                <span className="text-muted-foreground">{task.taskTitle}</span>
                                <span className="numeric text-foreground">{formatValue(task.totalAmount)}</span>
                            </div>
                        ))}
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
