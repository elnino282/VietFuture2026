import { Badge } from '@/shared/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card';
import { Separator } from '@/shared/ui/separator';
import { Skeleton } from '@/shared/ui/skeleton';
import { AlertTriangle, CheckCircle2, Circle } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import type {
  DashboardDataCompletenessWarningItem,
  DashboardTaskItem,
} from '../types';

interface SeasonTaskPanelsProps {
  todayTasks: DashboardTaskItem[];
  upcomingTasks: DashboardTaskItem[];
  dataCompletenessWarnings: DashboardDataCompletenessWarningItem[];
  isLoading: boolean;
  errorMessage?: string | null;
}

function TaskList({
  tasks,
  emptyLabel,
}: {
  tasks: DashboardTaskItem[];
  emptyLabel: string;
}) {
  if (tasks.length === 0) {
    return <p className="acm-body-text text-muted-foreground">{emptyLabel}</p>;
  }

  return (
    <ul className="space-y-3">
      {tasks.map((task) => (
        <li key={task.id} className="flex items-start gap-3">
          {task.done ? (
            <CheckCircle2 className="h-4 w-4 mt-0.5 text-primary" />
          ) : (
            <Circle className="h-4 w-4 mt-0.5 text-muted-foreground" />
          )}
          <div className="flex-1">
            <p className="text-base font-medium leading-snug">{task.title}</p>
            <p className="acm-body-text text-muted-foreground">
              {task.plotName} - {task.dueDateLabel}
            </p>
          </div>
        </li>
      ))}
    </ul>
  );
}

function WarningList({
  warnings,
  emptyLabel,
}: {
  warnings: DashboardDataCompletenessWarningItem[];
  emptyLabel: string;
}) {
  if (warnings.length === 0) {
    return <p className="acm-body-text text-muted-foreground">{emptyLabel}</p>;
  }

  return (
    <ul className="space-y-3">
      {warnings.map((warning) => (
        <li key={warning.id} className="flex items-start gap-3">
          <AlertTriangle className="h-4 w-4 mt-0.5 text-amber-600" />
          <div className="flex-1">
            <p className="text-base font-medium leading-snug">{warning.title}</p>
            <p className="acm-body-text text-muted-foreground">
              {warning.source} - {warning.dueDateLabel}
            </p>
          </div>
        </li>
      ))}
    </ul>
  );
}

export function SeasonTaskPanels({
  todayTasks,
  upcomingTasks,
  dataCompletenessWarnings,
  isLoading,
  errorMessage,
}: SeasonTaskPanelsProps) {
  const { t } = useTranslation();

  return (
    <Card className="border-border acm-card-elevated acm-hover-surface">
      <CardHeader className="pb-4">
        <CardTitle>
          {t('dashboard.fdn.tasksTitle', {
            defaultValue: 'Today and Upcoming Tasks',
          })}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-5">
        {!isLoading && errorMessage ? (
          <p className="acm-body-text text-destructive">{errorMessage}</p>
        ) : null}

        <div>
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-base font-semibold">
              {t('dashboard.fdn.todayTasksTitle', { defaultValue: "Today's Tasks" })}
            </h3>
            <Badge variant="outline">{todayTasks.length}</Badge>
          </div>
          {isLoading ? (
            <div className="space-y-2">
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-4/5" />
              <Skeleton className="h-4 w-2/3" />
            </div>
          ) : (
            <TaskList
              tasks={todayTasks}
              emptyLabel={t('dashboard.fdn.noTodayTasks', {
                defaultValue: 'No tasks for today.',
              })}
            />
          )}
        </div>

        <Separator />

        <div>
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-base font-semibold">
              {t('dashboard.fdn.upcomingTasksTitle', {
                defaultValue: 'Upcoming Tasks',
              })}
            </h3>
            <Badge variant="outline">{upcomingTasks.length}</Badge>
          </div>
          {isLoading ? (
            <div className="space-y-2">
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-3/4" />
              <Skeleton className="h-4 w-2/3" />
            </div>
          ) : (
            <TaskList
              tasks={upcomingTasks}
              emptyLabel={t('dashboard.fdn.noUpcomingTasks', {
                defaultValue: 'No upcoming tasks.',
              })}
            />
          )}
        </div>

        <Separator />

        <div>
          <div className="mb-3 flex items-center justify-between">
            <h3 className="text-base font-semibold">
              {t('dashboard.fdn.dataCompletenessTitle', {
                defaultValue: 'Missing required inputs',
              })}
            </h3>
            <Badge variant="outline">{dataCompletenessWarnings.length}</Badge>
          </div>
          {isLoading ? (
            <div className="space-y-2">
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-4/5" />
              <Skeleton className="h-4 w-2/3" />
            </div>
          ) : (
            <WarningList
              warnings={dataCompletenessWarnings}
              emptyLabel={t('dashboard.fdn.noDataCompletenessWarnings', {
                defaultValue: 'No data completeness warnings.',
              })}
            />
          )}
        </div>
      </CardContent>
    </Card>
  );
}
