import {
  Button,
  CardContent,
  Input,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui";
import { AdminFilterCard } from "@/features/admin/shared/ui";
import { useI18n } from "@/shared/lib/hooks/useI18n";

export interface ReportFilters {
  dateFrom: string;
  dateTo: string;
  farmId: string;
  plotId: string;
  cropId: string;
  varietyId: string;
}

interface FilterOption {
  value: string;
  label: string;
}

interface ReportsFilterCardProps {
  filters: ReportFilters;
  onFiltersChange: (filters: ReportFilters) => void;
  onApply: () => void;
  onReset: () => void;
  farms?: FilterOption[];
  plots?: FilterOption[];
  crops?: FilterOption[];
  varieties?: FilterOption[];
  isPlotDisabled?: boolean;
  isVarietyDisabled?: boolean;
  hasPendingChanges?: boolean;
}

export const ReportsFilterCard: React.FC<ReportsFilterCardProps> = ({
  filters,
  onFiltersChange,
  onApply,
  onReset,
  farms = [],
  plots = [],
  crops = [],
  varieties = [],
  isPlotDisabled = false,
  isVarietyDisabled = false,
  hasPendingChanges = false,
}) => {
  const { t } = useI18n();

  const handleChange = (key: keyof ReportFilters, value: string) => {
    onFiltersChange({ ...filters, [key]: value });
  };

  return (
    <AdminFilterCard>
      <CardContent className="p-4">
        <div className="flex flex-col gap-3 lg:flex-row lg:flex-wrap lg:items-end">
          {/* Date Range - Inline */}
          <div className="flex items-center gap-1.5 h-9 px-3 rounded-[14px] border border-border bg-card w-full sm:w-auto sm:min-w-[260px]">
            <svg
              className="w-4 h-4 text-muted-foreground flex-shrink-0"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
              />
            </svg>
            <Input
              type="date"
              value={filters.dateFrom}
              onChange={(e) => handleChange("dateFrom", e.target.value)}
              className="h-7 border-0 bg-transparent text-sm p-0 focus-visible:ring-0 w-[42%] sm:w-[110px] text-foreground"
            />
            <span className="text-muted-foreground text-sm">{t('pagination.to')}</span>
            <Input
              type="date"
              value={filters.dateTo}
              onChange={(e) => handleChange("dateTo", e.target.value)}
              className="h-7 border-0 bg-transparent text-sm p-0 focus-visible:ring-0 w-[42%] sm:w-[110px] text-foreground"
            />
          </div>

          {/* Farm Select */}
          <div className="flex flex-col gap-1.5 w-full sm:min-w-[140px] sm:w-auto">
            <Select
              value={filters.farmId}
              onValueChange={(value) => handleChange("farmId", value)}
            >
              <SelectTrigger className="h-9 w-full rounded-[14px] border-border bg-card text-foreground text-sm">
                <SelectValue placeholder={t('admin.reportsAnalytics.filters.allFarms')} />
              </SelectTrigger>
              <SelectContent className="rounded-[14px]">
                <SelectItem value="all">{t('admin.reportsAnalytics.filters.allFarms')}</SelectItem>
                {farms.map((farm) => (
                  <SelectItem key={farm.value} value={farm.value}>
                    {farm.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Plot Select */}
          <div className="flex flex-col gap-1.5 w-full sm:min-w-[120px] sm:w-auto">
            <Select
              value={filters.plotId}
              onValueChange={(value) => handleChange("plotId", value)}
              disabled={isPlotDisabled}
            >
              <SelectTrigger
                className={`h-9 w-full rounded-[14px] border-border bg-card text-foreground text-sm ${isPlotDisabled ? "opacity-50 cursor-not-allowed" : ""}`}
              >
                <SelectValue placeholder={t('admin.reportsAnalytics.filters.allPlots')} />
              </SelectTrigger>
              <SelectContent className="rounded-[14px]">
                <SelectItem value="all">{t('admin.reportsAnalytics.filters.allPlots')}</SelectItem>
                {plots.map((plot) => (
                  <SelectItem key={plot.value} value={plot.value}>
                    {plot.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Crop Select */}
          <div className="flex flex-col gap-1.5 w-full sm:min-w-[120px] sm:w-auto">
            <Select
              value={filters.cropId}
              onValueChange={(value) => handleChange("cropId", value)}
            >
              <SelectTrigger className="h-9 w-full rounded-[14px] border-border bg-card text-foreground text-sm">
                <SelectValue placeholder={t('admin.reportsAnalytics.filters.allCrops')} />
              </SelectTrigger>
              <SelectContent className="rounded-[14px]">
                <SelectItem value="all">{t('admin.reportsAnalytics.filters.allCrops')}</SelectItem>
                {crops.map((crop) => (
                  <SelectItem key={crop.value} value={crop.value}>
                    {crop.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Variety Select */}
          <div className="flex flex-col gap-1.5 w-full sm:min-w-[140px] sm:w-auto">
            <Select
              value={filters.varietyId}
              onValueChange={(value) => handleChange("varietyId", value)}
              disabled={isVarietyDisabled}
            >
              <SelectTrigger
                className={`h-9 w-full rounded-[14px] border-border bg-card text-foreground text-sm ${isVarietyDisabled ? "opacity-50 cursor-not-allowed" : ""}`}
              >
                <SelectValue placeholder={t('admin.reportsAnalytics.filters.allVarieties')} />
              </SelectTrigger>
              <SelectContent className="rounded-[14px]">
                <SelectItem value="all">{t('admin.reportsAnalytics.filters.allVarieties')}</SelectItem>
                {varieties.map((variety) => (
                  <SelectItem key={variety.value} value={variety.value}>
                    {variety.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Action Buttons */}
          <div className="flex flex-wrap items-center gap-2 w-full sm:w-auto sm:ml-auto">
            {hasPendingChanges && (
              <span className="text-xs text-warning bg-warning/10 border border-warning/30 px-2 py-0.5 rounded-full">
                {t('admin.reportsAnalytics.filters.unsavedChanges')}
              </span>
            )}
            <button
              onClick={onReset}
              className="text-sm text-muted-foreground hover:text-foreground hover:underline transition-colors"
            >
              {t('common.reset')}
            </button>
            <Button
              size="sm"
              onClick={onApply}
              className="h-9 px-4 rounded-[14px] bg-primary hover:bg-primary/90 text-primary-foreground font-medium text-sm"
            >
              {t('admin.reportsAnalytics.filters.apply')}
            </Button>
          </div>
        </div>
      </CardContent>
    </AdminFilterCard>
  );
};
