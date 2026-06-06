import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/card";
import type { SummaryStats } from "../types";
import { usePreferences } from "@/shared/contexts";
import { formatWeight } from "@/shared/lib";
import { useI18n } from "@/shared/lib/hooks/useI18n";

interface SeasonSummaryPanelProps {
    summaryStats: SummaryStats;
}

export function SeasonSummaryPanel({ summaryStats }: SeasonSummaryPanelProps) {
    const { preferences } = usePreferences();
    const { t } = useI18n();
    const summaryItems = [
        {
            label: t("harvests.summary.totalStored"),
            value: formatWeight(summaryStats.totalStored, preferences.weightUnit, preferences.locale),
            className: "text-foreground",
        },
        {
            label: t("harvests.summary.totalSold"),
            value: formatWeight(summaryStats.totalSold, preferences.weightUnit, preferences.locale),
            className: "text-primary",
        },
        {
            label: t("harvests.summary.processing"),
            value: formatWeight(summaryStats.totalProcessing, preferences.weightUnit, preferences.locale),
            className: "text-accent",
        },
        {
            label: t("harvests.summary.premiumGradePercentage"),
            value: `${summaryStats.premiumGradePercentage.toFixed(0)}%`,
            className: "text-foreground",
        },
    ];

    return (
        <Card className="border-border rounded-2xl shadow-sm bg-muted/30">
            <CardHeader className="pb-3">
                <CardTitle className="text-base text-foreground">
                    {t("harvests.summary.title")}
                </CardTitle>
            </CardHeader>
            <CardContent>
                <div className="grid grid-cols-1 divide-y divide-border sm:grid-cols-2 sm:divide-x sm:divide-y-0 xl:grid-cols-4">
                    {summaryItems.map((item) => (
                        <div key={item.label} className="flex items-center justify-between gap-4 px-0 py-3 first:pt-0 last:pb-0 sm:block sm:px-5 sm:first:pl-0 sm:first:pt-3 sm:last:pr-0 xl:px-6">
                            <span className="text-sm text-muted-foreground">{item.label}</span>
                            <span className={`numeric text-base font-semibold sm:mt-2 sm:block ${item.className}`}>
                                {item.value}
                            </span>
                        </div>
                    ))}
                </div>
            </CardContent>
        </Card>
    );
}
