
import { useI18n } from '@/shared/lib/hooks/useI18n';
import { Season, Activity } from '../types';

interface SeasonTabsProps {
  season: Season;
}

export function SeasonTabs({ season }: SeasonTabsProps) {
  const { t } = useI18n();
  const effectiveEndDate = season.endDate || season.plannedHarvestDate || season.startDate;
  const endDateLabel = effectiveEndDate
    ? new Date(effectiveEndDate).toLocaleDateString()
    : '-';

  return (
    <div className="w-full pt-2 mb-8">
      <div className="mb-6 pb-2 border-b border-border/40">
        <h2 className="text-lg font-bold tracking-tight text-foreground">{t("seasonTabs.infoTitle", "Thông tin chung")}</h2>
      </div>
      
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-x-12 gap-y-8">
        <div className="flex flex-col gap-1.5">
          <span className="text-sm text-muted-foreground">{t("seasonTabs.crop", "Crop")}</span>
          <span className="text-base font-semibold text-foreground">{season.crop}</span>
        </div>
        <div className="flex flex-col gap-1.5">
          <span className="text-sm text-muted-foreground">{t("seasonTabs.variety", "Variety")}</span>
          <span className="text-base font-semibold text-foreground">{season.variety || '-'}</span>
        </div>
        <div className="flex flex-col gap-1.5">
          <span className="text-sm text-muted-foreground">{t("seasonTabs.startDate", "Start Date")}</span>
          <span className="text-base font-semibold text-foreground">
            {new Date(season.startDate).toLocaleDateString()}
          </span>
        </div>
        <div className="flex flex-col gap-1.5">
          <span className="text-sm text-muted-foreground">{t("seasonTabs.endDate", "End Date")}</span>
          <span className="text-base font-semibold text-foreground">
            {endDateLabel}
          </span>
        </div>
        <div className="flex flex-col gap-1.5">
          <span className="text-sm text-muted-foreground">{t("seasonTabs.totalTasks", "Total Tasks")}</span>
          <span className="text-base font-semibold text-foreground numeric">
            {season.tasksCompleted} / {season.tasksTotal}
          </span>
        </div>
        <div className="flex flex-col gap-1.5">
          <span className="text-sm text-muted-foreground">{t("seasonTabs.incidents", "Incidents")}</span>
          <span className="text-base font-semibold text-foreground numeric">
            {season.incidentCount}
          </span>
        </div>
      </div>
    </div>
  );
}
