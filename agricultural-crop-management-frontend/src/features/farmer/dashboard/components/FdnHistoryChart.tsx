import { Button } from '@/shared/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card';
import { Skeleton } from '@/shared/ui/skeleton';
import type { DashboardFdnOverview } from '@/entities/dashboard';
import { Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { useTranslation } from 'react-i18next';
import { hasNumericValue } from '../lib/metrics';
import {
  buildUnavailableActionLinks,
  getUnavailableReasons,
  unavailableReasonLabel,
} from '../lib/unavailable';

interface FdnHistoryChartProps {
  overview: DashboardFdnOverview | null;
  isLoading: boolean;
  errorMessage?: string | null;
}

export function FdnHistoryChart({ overview, isLoading, errorMessage }: FdnHistoryChartProps) {
  const { t } = useTranslation();

  if (isLoading) {
    return (
      <Card className="border-border acm-card-elevated">
        <CardHeader>
          <CardTitle>
            {t('dashboard.fdn.historyTitle', { defaultValue: 'Historical Trends' })}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <Skeleton className="h-64 w-full" />
        </CardContent>
      </Card>
    );
  }

  if (!overview) {
    return (
      <Card className="border-border acm-card-elevated">
        <CardHeader>
          <CardTitle>
            {t('dashboard.fdn.historyTitle', { defaultValue: 'Historical Trends' })}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="rounded-md border border-dashed border-border p-4 space-y-2">
            <p className="text-base font-medium">
              {t('dashboard.fdn.unavailableTitle', {
                defaultValue: 'FDN dashboard metrics are unavailable',
              })}
            </p>
            <p className="acm-body-text text-muted-foreground">
              {errorMessage
                ?? t('dashboard.fdn.unavailableHint', {
                  defaultValue: 'The dashboard is missing context or required records for this panel.',
                })}
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  const history = overview.historicalTrend ?? [];
  const unavailableReasons = getUnavailableReasons(overview);
  const unavailableActions = buildUnavailableActionLinks(overview, unavailableReasons, t);
  const chartData = history
    .filter((point) =>
      hasNumericValue(point.fdnTotal) || hasNumericValue(point.nue) || hasNumericValue(point.yield)
    )
    .map((point) => ({
      label: point.seasonName,
      fdn: hasNumericValue(point.fdnTotal) ? point.fdnTotal : null,
      nue: hasNumericValue(point.nue) ? point.nue : null,
      yield: hasNumericValue(point.yield) ? point.yield : null,
    }));

  const hasEnoughTrendPoints = chartData.length >= 2;
  if (!hasEnoughTrendPoints) {
    const showUnavailableReason = unavailableReasons.length > 0;
    return (
      <Card className="border-border acm-card-elevated">
        <CardHeader>
          <CardTitle>
            {t('dashboard.fdn.historyTitle', { defaultValue: 'Historical Trends' })}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="rounded-md border border-dashed border-border p-4 space-y-2">
            <p className="text-base font-medium">
              {showUnavailableReason
                ? t('dashboard.fdn.unavailableTitle', {
                  defaultValue: 'FDN dashboard metrics are unavailable',
                })
                : t('dashboard.fdn.noHistory', {
                  defaultValue: 'No historical season metrics available.',
                })}
            </p>
            <p className="acm-body-text text-muted-foreground">
              {showUnavailableReason
                ? t('dashboard.fdn.unavailableHint', {
                  defaultValue:
                    'Complete additional seasons, harvest records and nitrogen inputs to unlock trend analysis.',
                })
                : t('dashboard.fdn.noHistoryHint', {
                  defaultValue:
                    'Complete additional seasons and keep nutrient + harvest records to unlock trend analysis.',
                })}
            </p>
            {showUnavailableReason && (
              <ul className="space-y-1 acm-body-text text-muted-foreground">
                {unavailableReasons.map((reason) => (
                  <li key={reason}>- {unavailableReasonLabel(reason, t)}</li>
                ))}
              </ul>
            )}
            {showUnavailableReason && unavailableActions.length > 0 && (
              <div className="flex flex-wrap gap-2">
                {unavailableActions.map((action) => (
                  <Button key={action.key} asChild size="sm" variant="outline">
                    <a href={action.href}>{action.label}</a>
                  </Button>
                ))}
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="border-border acm-card-elevated acm-hover-surface">
      <CardHeader className="pb-2">
        <CardTitle>
          {t('dashboard.fdn.historyTitle', { defaultValue: 'Historical Trends' })}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="h-72">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={chartData}>
              <XAxis dataKey="label" tick={{ fontSize: 14 }} />
              <YAxis tick={{ fontSize: 14 }} />
              <Tooltip />
              <Line
                type="monotone"
                dataKey="fdn"
                name={t('dashboard.fdn.fdnTotal', { defaultValue: 'FDN total' })}
                stroke="#f59e0b"
                strokeWidth={2}
                connectNulls={false}
              />
              <Line
                type="monotone"
                dataKey="nue"
                name={t('dashboard.fdn.nueTitle', { defaultValue: 'NUE' })}
                stroke="#16a34a"
                strokeWidth={2}
                connectNulls={false}
              />
              <Line
                type="monotone"
                dataKey="yield"
                name={t('dashboard.fdn.estimatedYieldTitle', { defaultValue: 'Yield' })}
                stroke="#2563eb"
                strokeWidth={2}
                connectNulls={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
}
