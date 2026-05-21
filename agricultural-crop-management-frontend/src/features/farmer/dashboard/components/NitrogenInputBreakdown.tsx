import { Button } from '@/shared/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card';
import { Skeleton } from '@/shared/ui/skeleton';
import type { DashboardFdnOverview } from '@/entities/dashboard';
import { Bar, BarChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { useTranslation } from 'react-i18next';
import { hasNumericValue } from '../lib/metrics';
import {
  buildUnavailableActionLinks,
  getUnavailableReasons,
  unavailableReasonLabel,
} from '../lib/unavailable';

interface NitrogenInputBreakdownProps {
  overview: DashboardFdnOverview | null;
  isLoading: boolean;
  errorMessage?: string | null;
}

export function NitrogenInputBreakdown({
  overview,
  isLoading,
  errorMessage,
}: NitrogenInputBreakdownProps) {
  const { t } = useTranslation();

  if (isLoading) {
    return (
      <Card className="border-border acm-card-elevated">
        <CardHeader>
          <CardTitle>
            {t('dashboard.fdn.inputBreakdownTitle', {
              defaultValue: 'Nitrogen Input Breakdown',
            })}
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
            {t('dashboard.fdn.inputBreakdownTitle', {
              defaultValue: 'Nitrogen Input Breakdown',
            })}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="h-72 flex flex-col justify-center gap-3 rounded-md border border-dashed border-border p-4">
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

  const breakdown = overview.inputsBreakdown;
  const unavailableReasons = getUnavailableReasons(overview);
  const unavailableActions = buildUnavailableActionLinks(overview, unavailableReasons, t);
  const data = [
    {
      name: t('dashboard.fdn.breakdown.mineral', { defaultValue: 'Mineral fertilizer' }),
      value: breakdown.mineralFertilizerN,
    },
    {
      name: t('dashboard.fdn.breakdown.organic', { defaultValue: 'Organic fertilizer' }),
      value: breakdown.organicFertilizerN,
    },
    {
      name: t('dashboard.fdn.breakdown.irrigation', { defaultValue: 'Irrigation water' }),
      value: breakdown.irrigationWaterN,
    },
    {
      name: t('dashboard.fdn.breakdown.fixation', { defaultValue: 'Biological fixation' }),
      value: breakdown.biologicalFixationN,
    },
    {
      name: t('dashboard.fdn.breakdown.deposition', { defaultValue: 'Atmospheric deposition' }),
      value: breakdown.atmosphericDepositionN,
    },
    {
      name: t('dashboard.fdn.breakdown.seed', { defaultValue: 'Seed import' }),
      value: breakdown.seedImportN,
    },
    {
      name: t('dashboard.fdn.breakdown.soilLegacy', { defaultValue: 'Soil legacy' }),
      value: breakdown.soilLegacyN,
    },
    {
      name: t('dashboard.fdn.breakdown.controlSupply', { defaultValue: 'Control supply' }),
      value: breakdown.controlSupplyN,
    },
  ];

  const hasAnyNumericValue = data.some((item) => hasNumericValue(item.value));
  const numericTotal = data.reduce((acc, item) => acc + (hasNumericValue(item.value) ? item.value : 0), 0);
  const shouldShowEmpty =
    !hasAnyNumericValue ||
    (numericTotal === 0 && (overview.dataQualitySummary?.measuredInputCount ?? 0) === 0);
  const showUnavailable = shouldShowEmpty && unavailableReasons.length > 0;

  return (
    <Card className="border-border acm-card-elevated acm-hover-surface">
      <CardHeader className="pb-2">
        <CardTitle>
          {t('dashboard.fdn.inputBreakdownTitle', {
            defaultValue: 'Nitrogen Input Breakdown',
          })}
        </CardTitle>
      </CardHeader>
      <CardContent>
        {shouldShowEmpty ? (
          <div className="h-72 flex flex-col justify-center gap-3 rounded-md border border-dashed border-border p-4">
            <p className="text-base font-medium">
              {showUnavailable
                ? t('dashboard.fdn.unavailableTitle', {
                  defaultValue: 'FDN dashboard metrics are unavailable',
                })
                : t('dashboard.fdn.breakdown.emptyTitle', {
                  defaultValue: 'Not enough input data to build nitrogen breakdown yet.',
                })}
            </p>
            <p className="acm-body-text text-muted-foreground">
              {showUnavailable
                ? t('dashboard.fdn.unavailableHint', {
                  defaultValue: 'Complete missing season, harvest and nitrogen inputs to unlock FDN indicators.',
                })
                : t('dashboard.fdn.breakdown.emptyHint', {
                  defaultValue:
                    'Record fertilizer, irrigation-water and biological input data to unlock this panel.',
                })}
            </p>
            {showUnavailable && (
              <ul className="space-y-1 acm-body-text text-muted-foreground">
                {unavailableReasons.map((reason) => (
                  <li key={reason}>- {unavailableReasonLabel(reason, t)}</li>
                ))}
              </ul>
            )}
            {overview.missingInputs.length > 0 && (
              <p className="acm-body-text text-amber-700">
                {t('dashboard.fdn.missingInputs', { defaultValue: 'Missing inputs' })}:{' '}
                {overview.missingInputs.join(', ')}
              </p>
            )}
            {showUnavailable && unavailableActions.length > 0 && (
              <div className="flex flex-wrap gap-2">
                {unavailableActions.map((action) => (
                  <Button key={action.key} asChild variant="outline" size="sm">
                    <a href={action.href}>{action.label}</a>
                  </Button>
                ))}
              </div>
            )}
          </div>
        ) : (
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart
                data={data
                  .filter((item) => hasNumericValue(item.value))
                  .map((item) => ({ ...item, value: item.value ?? 0 }))}
              >
                <XAxis dataKey="name" tick={{ fontSize: 14 }} interval={0} angle={-18} textAnchor="end" height={70} />
                <YAxis tick={{ fontSize: 14 }} />
                <Tooltip
                  formatter={(value: number) => [
                    `${value.toFixed(2)} ${overview.unit}`,
                    t('dashboard.fdn.breakdown.nInput', { defaultValue: 'N input' }),
                  ]}
                />
                <Bar dataKey="value" fill="#84cc16" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
