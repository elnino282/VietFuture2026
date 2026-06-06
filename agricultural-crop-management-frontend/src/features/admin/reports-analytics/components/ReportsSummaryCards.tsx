import {
  Card,
  CardContent,
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/shared/ui";
import { usePreferences } from "@/shared/contexts";
import {
  convertCostPerKg,
  convertToDisplayCurrency,
  formatMoney,
  formatWeight,
  getWeightUnitLabel,
} from "@/shared/lib";
import {
  AlertTriangle,
  Calculator,
  Coins,
  DollarSign,
  Info,
  TrendingUp,
  Wheat,
} from "lucide-react";
import { useI18n } from "@/shared/lib/hooks/useI18n";

export interface SummaryStats {
  actualYield: number;
  totalCost: number;
  costPerTon: number | null;
  revenue: number;
  grossProfit: number;
  marginPercent: number | null;
}

interface ReportsSummaryCardsProps {
  stats: SummaryStats | null;
  warnings?: string[] | null;
  isLoading?: boolean;
  onDrilldown?: (target: "yield" | "cost" | "revenue" | "profit") => void;
  drilldownAvailable?: boolean;
}

export const ReportsSummaryCards: React.FC<ReportsSummaryCardsProps> = ({
  stats,
  warnings,
  isLoading = false,
  onDrilldown,
  drilldownAvailable = false,
}) => {
  const { t } = useI18n();
  const { preferences } = usePreferences();
  const unitLabel = getWeightUnitLabel(preferences.weightUnit);
  const safeStats: SummaryStats = stats ?? {
    actualYield: 0,
    totalCost: 0,
    costPerTon: null,
    revenue: 0,
    grossProfit: 0,
    marginPercent: null,
  };
  const formattedYield = formatWeight(
    safeStats.actualYield,
    preferences.weightUnit,
    preferences.locale,
  );
  // Convert cost per kg to cost per display unit, then convert currency
  const costPerUnit =
    safeStats.costPerTon != null
      ? convertToDisplayCurrency(
          convertCostPerKg(safeStats.costPerTon, preferences.weightUnit),
          preferences.currency,
        )
      : null;
  const costPerUnitLabel =
    costPerUnit != null
      ? `${formatMoney(costPerUnit, preferences.currency, preferences.locale)}/${unitLabel}`
      : t('common.notAvailable');

  // Convert monetary values from VND (backend canonical) to display currency
  const displayTotalCost = convertToDisplayCurrency(
    safeStats.totalCost,
    preferences.currency,
  );
  const displayRevenue = convertToDisplayCurrency(
    safeStats.revenue,
    preferences.currency,
  );
  const displayGrossProfit = convertToDisplayCurrency(
    safeStats.grossProfit,
    preferences.currency,
  );

  const cards = [
    {
      id: "yield" as const,
      title: t('admin.reportsAnalytics.summary.actualYield'),
      value: formattedYield,
      subtitle: t('admin.reportsAnalytics.summary.harvestedInPeriod'),
      icon: Wheat,
      bgColor: "bg-success/10",
      iconColor: "text-success",
      tooltip: t('admin.reportsAnalytics.summary.actualYieldTooltip'),
    },
    {
      id: "cost" as const,
      title: t('admin.reportsAnalytics.summary.totalCost'),
      value: formatMoney(
        displayTotalCost,
        preferences.currency,
        preferences.locale,
      ),
      subtitle: t('admin.reportsAnalytics.summary.allExpenses'),
      icon: Coins,
      bgColor: "bg-warning/10",
      iconColor: "text-warning",
      tooltip: t('admin.reportsAnalytics.summary.totalCostTooltip'),
    },
    {
      title: t('admin.reportsAnalytics.summary.costPerUnit', { unit: unitLabel }),
      value: costPerUnitLabel,
      subtitle: t('admin.reportsAnalytics.summary.currencyPerUnitEfficiency', {
        currency: preferences.currency,
        unit: unitLabel,
      }),
      icon: Calculator,
      bgColor: "bg-info/10",
      iconColor: "text-info",
      tooltip: t('admin.reportsAnalytics.summary.costPerUnitTooltip'),
    },
    {
      id: "revenue" as const,
      title: t('admin.reportsAnalytics.summary.revenue'),
      value: formatMoney(
        displayRevenue,
        preferences.currency,
        preferences.locale,
      ),
      subtitle: t('admin.reportsAnalytics.summary.fromHarvests'),
      icon: DollarSign,
      bgColor: "bg-success/10",
      iconColor: "text-success",
      tooltip: t('admin.reportsAnalytics.summary.revenueTooltip'),
    },
    {
      id: "profit" as const,
      title: t('admin.reportsAnalytics.summary.grossProfit'),
      value: formatMoney(
        displayGrossProfit,
        preferences.currency,
        preferences.locale,
      ),
      subtitle:
        safeStats.marginPercent != null
          ? t('admin.reportsAnalytics.summary.marginPercent', {
              percent: safeStats.marginPercent.toFixed(1),
            })
          : t('admin.reportsAnalytics.summary.noMargin'),
      icon: TrendingUp,
      bgColor: displayGrossProfit >= 0 ? "bg-success/10" : "bg-destructive/10",
      iconColor:
        safeStats.grossProfit >= 0 ? "text-success" : "text-destructive",
      tooltip: t('admin.reportsAnalytics.summary.grossProfitTooltip'),
    },
  ];

  if (isLoading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-4">
        {[...Array(5)].map((_, i) => (
          <Card
            key={i}
            className="!rounded-[18px] border-border bg-card shadow-sm animate-pulse"
          >
            <CardContent className="p-6">
              <div className="flex items-start justify-between">
                <div className="space-y-3 flex-1">
                  <div className="h-4 bg-muted rounded w-24" />
                  <div className="h-7 bg-muted rounded w-32" />
                  <div className="h-3 bg-muted rounded w-20" />
                </div>
                <div className="w-9 h-9 bg-muted rounded-2xl" />
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    );
  }

  return (
    <TooltipProvider>
      <div className="space-y-3">
        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-4">
          {cards.map((card, index) => {
            const Icon = card.icon;
            const isClickable =
              !!card.id && !!onDrilldown && drilldownAvailable;
            const handleClick = () => {
              if (isClickable && card.id) {
                onDrilldown(card.id);
              }
            };
            const handleKeyDown = (
              event: React.KeyboardEvent<HTMLDivElement>,
            ) => {
              if (!isClickable || !card.id) return;
              if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                onDrilldown(card.id);
              }
            };
            return (
              <Card
                key={index}
                onClick={handleClick}
                onKeyDown={isClickable ? handleKeyDown : undefined}
                role={isClickable ? "button" : undefined}
                tabIndex={isClickable ? 0 : undefined}
                aria-disabled={!isClickable && card.id ? true : undefined}
                title={
                  !isClickable && card.id ? t('admin.reportsAnalytics.pageNotAvailable') : undefined
                }
                className={`!rounded-[18px] border-border bg-card shadow-sm transition-shadow duration-200 ${
                  isClickable
                    ? "hover:shadow-md cursor-pointer"
                    : card.id
                      ? "cursor-not-allowed"
                      : ""
                }`}
              >
                <CardContent className="p-5">
                  <div className="flex items-start gap-3">
                    {/* Icon Container - Left Side */}
                    <div
                      className={`w-11 h-11 rounded-full ${card.bgColor} flex items-center justify-center flex-shrink-0`}
                    >
                      <Icon className={`w-5 h-5 ${card.iconColor}`} />
                    </div>

                    {/* Content */}
                    <div className="flex flex-col gap-0.5 min-w-0 flex-1">
                      {/* Title with tooltip */}
                      <div className="flex items-center gap-1.5">
                        <span className="text-sm font-medium text-muted-foreground">
                          {card.title}
                        </span>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <button className="text-muted-foreground hover:text-foreground transition-colors">
                              <Info className="w-3.5 h-3.5" />
                            </button>
                          </TooltipTrigger>
                          <TooltipContent className="rounded-lg">
                            <p className="text-xs">{card.tooltip}</p>
                          </TooltipContent>
                        </Tooltip>
                      </div>

                      {/* Value - Bold */}
                      <p className="text-xl font-bold text-foreground leading-tight tracking-tight truncate">
                        {card.value}
                      </p>

                      {/* Subtitle - Lighter */}
                      <p className="text-xs text-muted-foreground leading-4">
                        {card.subtitle}
                      </p>
                    </div>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>

        {warnings && warnings.length > 0 && (
          <div className="flex items-start gap-2 rounded-[14px] border border-warning/30 bg-warning/10 px-3 py-2 text-sm text-warning-foreground">
            <AlertTriangle className="w-4 h-4 mt-0.5 text-warning" />
            <div className="space-y-1">
              {warnings.map((warning, index) => (
                <div key={index}>{warning}</div>
              ))}
            </div>
          </div>
        )}
      </div>
    </TooltipProvider>
  );
};
