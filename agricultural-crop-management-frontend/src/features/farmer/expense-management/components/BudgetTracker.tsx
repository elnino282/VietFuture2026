import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/card";
import { useTranslation } from "react-i18next";
import { Progress } from "@/shared/ui/progress";
import { Button } from "@/shared/ui/button";
import { DollarSign, TrendingUp, TrendingDown, Wallet } from "lucide-react";
import { formatMoney, convertToDisplayCurrency } from "@/shared/lib";
import { usePreferences } from "@/shared/contexts";
import { Link } from "react-router-dom";

interface BudgetTrackerProps {
    totalExpenses: number;
    budgetUsagePercentage: number | null;
    remainingBudget: number | null;
    paidExpenses: number;
    unpaidExpenses: number;
    budgetAmount: number | null;
}

export function BudgetTracker({
    totalExpenses,
    budgetUsagePercentage,
    remainingBudget,
    paidExpenses,
    unpaidExpenses,
    budgetAmount,
}: BudgetTrackerProps) {
    const { t } = useTranslation();
    const { preferences } = usePreferences();

    const budgetSet = budgetAmount !== null && budgetAmount > 0;
    const usagePercent = budgetSet ? budgetUsagePercentage ?? 0 : 0;

    const getStatusColor = (percentage: number) => {
        if (percentage >= 90) return "text-destructive";
        if (percentage >= 70) return "text-warning";
        return "text-primary";
    };

    return (
        <Card className="border-border rounded-2xl shadow-sm">
            <CardHeader className="pb-4">
                <CardTitle className="flex items-center gap-2 text-lg font-semibold">
                    <Wallet className="w-5 h-5 text-primary" />
                    {t("expenses.overview.budgetTracker")}
                </CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
                {!budgetSet ? (
                    <div className="space-y-3">
                        <div className="rounded-xl border border-dashed border-border p-4 text-sm text-muted-foreground">
                            {t("expenses.overview.budgetNotSet")}
                        </div>
                        <Button asChild variant="outline" className="w-full">
                            <Link to="/farmer/seasons">{t("expenses.overview.setBudget")}</Link>
                        </Button>
                    </div>
                ) : (
                    <div className="space-y-3">
                        <div className="flex items-center justify-between text-sm">
                            <span className="text-muted-foreground">{t("expenses.overview.budgetUsage")}</span>
                            <span className={`font-semibold ${getStatusColor(usagePercent)}`}>
                                {usagePercent.toFixed(1)}%
                            </span>
                        </div>
                        <Progress
                            value={Math.min(usagePercent, 100)}
                            className="h-3 rounded-full"
                        />
                        <div className="flex items-center justify-between text-xs text-muted-foreground">
                            <span>{t("expenses.overview.used")} {formatMoney(convertToDisplayCurrency(totalExpenses, preferences.currency), preferences.currency, preferences.locale)}</span>
                            <span>{t("expenses.overview.remaining")} {formatMoney(convertToDisplayCurrency(remainingBudget ?? 0, preferences.currency), preferences.currency, preferences.locale)}</span>
                        </div>
                    </div>
                )}

                <div className="grid grid-cols-2 gap-4">
                    <div className="p-4 rounded-xl bg-muted/50 space-y-2">
                        <div className="flex items-center gap-2">
                            <div className="p-2 rounded-lg bg-primary/10">
                                <DollarSign className="w-4 h-4 text-primary" />
                            </div>
                            <span className="text-xs text-muted-foreground">{t("expenses.overview.total")}</span>
                        </div>
                        <p className="text-lg font-bold">
                            {formatMoney(convertToDisplayCurrency(totalExpenses, preferences.currency), preferences.currency, preferences.locale)}
                        </p>
                    </div>

                    <div className="p-4 rounded-xl bg-muted/50 space-y-2">
                        <div className="flex items-center gap-2">
                            <div className="p-2 rounded-lg bg-emerald-500/10">
                                <Wallet className="w-4 h-4 text-emerald-500" />
                            </div>
                            <span className="text-xs text-muted-foreground">{t("expenses.overview.remaining")}</span>
                        </div>
                        <p className="text-lg font-bold text-emerald-600">
                            {formatMoney(convertToDisplayCurrency(remainingBudget ?? 0, preferences.currency), preferences.currency, preferences.locale)}
                        </p>
                    </div>

                    <div className="p-4 rounded-xl bg-muted/50 space-y-2">
                        <div className="flex items-center gap-2">
                            <div className="p-2 rounded-lg bg-green-500/10">
                                <TrendingUp className="w-4 h-4 text-green-500" />
                            </div>
                            <span className="text-xs text-muted-foreground">{t("expenses.overview.paid")}</span>
                        </div>
                        <p className="text-lg font-bold text-green-600">
                            {formatMoney(convertToDisplayCurrency(paidExpenses, preferences.currency), preferences.currency, preferences.locale)}
                        </p>
                    </div>

                    <div className="p-4 rounded-xl bg-muted/50 space-y-2">
                        <div className="flex items-center gap-2">
                            <div className="p-2 rounded-lg bg-amber-500/10">
                                <TrendingDown className="w-4 h-4 text-amber-500" />
                            </div>
                            <span className="text-xs text-muted-foreground">{t("expenses.overview.unpaid")}</span>
                        </div>
                        <p className="text-lg font-bold text-amber-600">
                            {formatMoney(convertToDisplayCurrency(unpaidExpenses, preferences.currency), preferences.currency, preferences.locale)}
                        </p>
                    </div>
                </div>
            </CardContent>
        </Card>
    );
}
