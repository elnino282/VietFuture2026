import {
    Wheat,
    Package,
    Award,
    Droplets,
    TrendingUp,
    TrendingDown,
} from "lucide-react";
import { Card, CardContent } from "@/shared/ui/card";
import { usePreferences } from "@/shared/contexts";
import { formatWeight } from "@/shared/lib";
import { useI18n } from "@/shared/lib/hooks/useI18n";

interface HarvestKPICardsProps {
    totalHarvested: number;
    lotsCount: number;
    avgGrade: string;
    avgMoisture: string;
    yieldVsPlan: string;
}

export function HarvestKPICards({
    totalHarvested,
    lotsCount,
    avgGrade,
    avgMoisture,
    yieldVsPlan,
}: HarvestKPICardsProps) {
    const yieldVsPlanValue = Number.parseFloat(yieldVsPlan);
    const hasYieldVsPlan = Number.isFinite(yieldVsPlanValue);
    const isOnTarget = hasYieldVsPlan && yieldVsPlanValue >= 100;
    const { preferences } = usePreferences();
    const { t } = useI18n();
    const totalHarvestedLabel = formatWeight(
        totalHarvested,
        preferences.weightUnit,
        preferences.locale
    );

    return (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4 mb-6">
            <Card className="border-border rounded-2xl shadow-sm">
                <CardContent className="px-6 py-4">
                    <div className="flex items-start justify-between">
                        <div>
                            <p className="text-sm text-muted-foreground mb-1">{t("harvests.kpi.totalHarvested")}</p>
                            <p className="text-2xl numeric text-foreground">
                                {totalHarvestedLabel}
                            </p>
                        </div>
                        <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center">
                            <Wheat className="w-5 h-5 text-primary" />
                        </div>
                    </div>
                </CardContent>
            </Card>

            <Card className="border-border rounded-2xl shadow-sm">
                <CardContent className="px-6 py-4">
                    <div className="flex items-start justify-between">
                        <div>
                            <p className="text-sm text-muted-foreground mb-1">{t("harvests.kpi.lotsCount")}</p>
                            <p className="text-2xl numeric text-foreground">{lotsCount}</p>
                            <p className="text-xs text-muted-foreground mt-1">{t("harvests.kpi.batches")}</p>
                        </div>
                        <div className="w-10 h-10 rounded-lg bg-secondary/10 flex items-center justify-center">
                            <Package className="w-5 h-5 text-secondary" />
                        </div>
                    </div>
                </CardContent>
            </Card>

            <Card className="border-border rounded-2xl shadow-sm">
                <CardContent className="px-6 py-4">
                    <div className="flex items-start justify-between">
                        <div>
                            <p className="text-sm text-muted-foreground mb-1">{t("harvests.kpi.avgGrade")}</p>
                            <p className="text-2xl text-foreground">{avgGrade}</p>
                            <p className="text-xs text-muted-foreground mt-1">{t("harvests.kpi.quality")}</p>
                        </div>
                        <div className="w-10 h-10 rounded-lg bg-accent/10 flex items-center justify-center">
                            <Award className="w-5 h-5 text-accent" />
                        </div>
                    </div>
                </CardContent>
            </Card>

            <Card className="border-border rounded-2xl shadow-sm">
                <CardContent className="px-6 py-4">
                    <div className="flex items-start justify-between">
                        <div>
                            <p className="text-sm text-muted-foreground mb-1">{t("harvests.kpi.avgMoisture")}</p>
                            <p className="text-2xl numeric text-foreground">{avgMoisture}</p>
                            <p className="text-xs text-muted-foreground mt-1">%</p>
                        </div>
                        <div className="w-10 h-10 rounded-lg bg-secondary/10 flex items-center justify-center">
                            <Droplets className="w-5 h-5 text-secondary" />
                        </div>
                    </div>
                </CardContent>
            </Card>

            <Card className="border-border rounded-2xl shadow-sm">
                <CardContent className="px-6 py-4">
                    <div className="flex items-start justify-between">
                        <div>
                            <p className="text-sm text-muted-foreground mb-1">{t("harvests.kpi.yieldVsPlan")}</p>
                            <div className="flex items-baseline gap-2">
                                <p className="text-2xl numeric text-foreground">{yieldVsPlan}</p>
                                {hasYieldVsPlan && <p className="text-xs text-muted-foreground">%</p>}
                            </div>
                            {!hasYieldVsPlan ? (
                                <div className="flex items-center gap-1 mt-1">
                                    <p className="text-xs text-muted-foreground">{t("harvests.kpi.noPlanData")}</p>
                                </div>
                            ) : isOnTarget ? (
                                <div className="flex items-center gap-1 mt-1">
                                    <TrendingUp className="w-3 h-3 text-primary" />
                                    <p className="text-xs text-primary">{t("harvests.kpi.onTarget")}</p>
                                </div>
                            ) : (
                                <div className="flex items-center gap-1 mt-1">
                                    <TrendingDown className="w-3 h-3 text-destructive" />
                                    <p className="text-xs text-destructive">{t("harvests.kpi.belowPlan")}</p>
                                </div>
                            )}
                        </div>
                        <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center">
                            <TrendingUp className="w-5 h-5 text-primary" />
                        </div>
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}



