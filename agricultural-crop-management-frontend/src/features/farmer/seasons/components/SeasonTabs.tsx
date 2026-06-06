import { Card, CardContent, CardHeader } from '@/shared/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/shared/ui/tabs';
import { Label } from '@/shared/ui/label';
import { useI18n } from '@/shared/lib/hooks/useI18n';
import { Season, Activity } from '../types';

interface SeasonTabsProps {
  season: Season;
  activeTab: string;
  setActiveTab: (tab: string) => void;
  activities: Activity[];
}

export function SeasonTabs({ season, activeTab, setActiveTab, activities }: SeasonTabsProps) {
  const { t } = useI18n();
  const effectiveEndDate = season.endDate || season.plannedHarvestDate || season.startDate;
  const endDateLabel = effectiveEndDate
    ? new Date(effectiveEndDate).toLocaleDateString()
    : '-';

  return (
    <Card className="border-border acm-rounded-lg acm-card-shadow">
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <CardHeader className="border-b border-border overflow-x-auto">
          <TabsList className="bg-muted w-max">
            <TabsTrigger value="overview">{t("seasonTabs.overview", "Overview")}</TabsTrigger>
          </TabsList>
        </CardHeader>
        <CardContent className="p-6">
          <TabsContent value="overview" className="mt-0">
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label className="text-sm text-muted-foreground">{t("seasonTabs.crop", "Crop")}</Label>
                  <div className="mt-1 text-foreground">{season.crop}</div>
                </div>
                <div>
                  <Label className="text-sm text-muted-foreground">{t("seasonTabs.variety", "Variety")}</Label>
                  <div className="mt-1 text-foreground">{season.variety}</div>
                </div>
                <div>
                  <Label className="text-sm text-muted-foreground">{t("seasonTabs.startDate", "Start Date")}</Label>
                  <div className="mt-1 text-foreground">
                    {new Date(season.startDate).toLocaleDateString()}
                  </div>
                </div>
                <div>
                  <Label className="text-sm text-muted-foreground">{t("seasonTabs.endDate", "End Date")}</Label>
                  <div className="mt-1 text-foreground">
                    {endDateLabel}
                  </div>
                </div>
                <div>
                  <Label className="text-sm text-muted-foreground">{t("seasonTabs.totalTasks", "Total Tasks")}</Label>
                  <div className="mt-1 text-foreground numeric">
                    {season.tasksCompleted} / {season.tasksTotal}
                  </div>
                </div>
                <div>
                  <Label className="text-sm text-muted-foreground">{t("seasonTabs.incidents", "Incidents")}</Label>
                  <div className="mt-1 text-foreground numeric">{season.incidentCount}</div>
                </div>
              </div>
            </div>
          </TabsContent>
        </CardContent>
      </Tabs>
    </Card>
  );
}
