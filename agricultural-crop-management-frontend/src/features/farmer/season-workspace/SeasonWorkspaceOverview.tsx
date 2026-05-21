import { Button } from "@/shared/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/card";
import { useI18n } from "@/hooks/useI18n";
import {
  ArrowRight,
  BarChart3,
  Beaker,
  Bug,
  ClipboardList,
  DollarSign,
  Droplets,
  FileText,
  TestTubeDiagonal,
  Users,
  Wheat,
} from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";

const MODULE_CARDS = [
  {
    titleKey: "seasonWorkspaceOverview.modules.tasks.title",
    descriptionKey: "seasonWorkspaceOverview.modules.tasks.description",
    icon: ClipboardList,
    path: "tasks",
  },
  {
    titleKey: "seasonWorkspaceOverview.modules.expenses.title",
    descriptionKey: "seasonWorkspaceOverview.modules.expenses.description",
    icon: DollarSign,
    path: "expenses",
  },
  {
    titleKey: "seasonWorkspaceOverview.modules.fieldLogs.title",
    descriptionKey: "seasonWorkspaceOverview.modules.fieldLogs.description",
    icon: FileText,
    path: "field-logs",
  },
  {
    titleKey: "seasonWorkspaceOverview.modules.disease.title",
    descriptionKey: "seasonWorkspaceOverview.modules.disease.description",
    icon: Bug,
    path: "disease",
  },
  {
    titleKey: "seasonWorkspaceOverview.modules.harvest.title",
    descriptionKey: "seasonWorkspaceOverview.modules.harvest.description",
    icon: Wheat,
    path: "harvest",
  },
  {
    titleKey: "seasonWorkspaceOverview.modules.labor.title",
    descriptionKey: "seasonWorkspaceOverview.modules.labor.description",
    icon: Users,
    path: "labor-management",
  },
  {
    titleKey: "seasonWorkspaceOverview.modules.nutrient.title",
    descriptionKey: "seasonWorkspaceOverview.modules.nutrient.description",
    icon: Beaker,
    path: "nutrient-inputs",
  },
  {
    titleKey: "seasonWorkspaceOverview.modules.irrigation.title",
    descriptionKey: "seasonWorkspaceOverview.modules.irrigation.description",
    icon: Droplets,
    path: "irrigation-water-analyses",
  },
  {
    titleKey: "seasonWorkspaceOverview.modules.soil.title",
    descriptionKey: "seasonWorkspaceOverview.modules.soil.description",
    icon: TestTubeDiagonal,
    path: "soil-tests",
  },
  {
    titleKey: "seasonWorkspaceOverview.modules.reports.title",
    descriptionKey: "seasonWorkspaceOverview.modules.reports.description",
    icon: BarChart3,
    path: "reports",
  },
] as const;

export function SeasonWorkspaceOverview() {
  const { t } = useI18n();
  const { seasonId } = useParams();
  const navigate = useNavigate();
  const seasonIdSegment = seasonId ?? "";

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
      {MODULE_CARDS.map((module) => {
          const Icon = module.icon;
          return (
            <Card key={module.path} className="border border-border rounded-2xl acm-card-elevated acm-hover-surface">
              <CardHeader className="pb-2">
                <CardTitle className="text-lg text-foreground flex items-center gap-2">
                  <Icon className="w-5 h-5 text-primary" />
                  {t(module.titleKey)}
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <p className="acm-body-text text-muted-foreground min-h-[66px]">{t(module.descriptionKey)}</p>
                <Button
                  className="w-full rounded-xl acm-hover-surface acm-body-text"
                  variant="outline"
                  onClick={() => navigate(`/farmer/seasons/${seasonIdSegment}/workspace/${module.path}`)}
                >
                {t("seasonWorkspaceOverview.openModule")}
                <ArrowRight className="w-4 h-4 ml-2" />
              </Button>
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
}
