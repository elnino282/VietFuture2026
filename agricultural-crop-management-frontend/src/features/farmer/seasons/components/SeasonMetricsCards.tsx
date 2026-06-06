import { TrendingUp, Calendar, Package, BarChart3 } from 'lucide-react';
import { usePreferences } from '@/shared/contexts';
import { formatWeight } from '@/shared/lib';
import { useI18n } from '@/shared/lib/hooks/useI18n';
import type { Season } from '../types';

interface SeasonMetricsCardsProps {
  seasons: Season[];
}

export function SeasonMetricsCards({ seasons }: SeasonMetricsCardsProps) {
  const { preferences } = usePreferences();
  const { t } = useI18n();

  // Calculate metrics
  const totalSeasons = seasons.length;
  const activeSeasons = seasons.filter(s => s.status === 'ACTIVE').length;
  
  // Harvest due in next 30 days
  const now = new Date();
  const thirtyDaysFromNow = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000);
  const harvestDue = seasons.filter(s => {
    if (!s.endDate) return false;
    const harvestDate = new Date(s.endDate);
    return harvestDate >= now && harvestDate <= thirtyDaysFromNow;
  }).length;

  // Total expected yield from active seasons
  const expectedYield = seasons
    .filter(s => s.status === 'ACTIVE')
    .reduce((sum, s) => sum + (s.yieldPerHa || 0), 0);
  const expectedYieldLabel = `${formatWeight(expectedYield, preferences.weightUnit, preferences.locale)}/ha`;

  const metrics = [
    {
      title: t('seasonMetrics.totalSeasons'),
      value: totalSeasons,
      subtitle: t('seasonMetrics.completedCount', {
        count: seasons.filter(s => s.status === 'COMPLETED').length,
      }),
      icon: Package,
      color: 'text-blue-600',
      bgColor: 'bg-blue-50',
    },
    {
      title: t('seasonMetrics.activeSeasons'),
      value: activeSeasons,
      subtitle: t('seasonMetrics.percentOfTotal', {
        percent: ((activeSeasons / totalSeasons || 0) * 100).toFixed(0),
      }),
      icon: TrendingUp,
      color: 'text-green-600',
      bgColor: 'bg-green-50',
    },
    {
      title: t('seasonMetrics.harvestDue'),
      value: harvestDue,
      subtitle: t('seasonMetrics.next30Days'),
      icon: Calendar,
      color: 'text-orange-600',
      bgColor: 'bg-orange-50',
    },
    {
      title: t('seasonMetrics.expectedYield'),
      value: expectedYieldLabel,
      subtitle: t('seasonMetrics.activeSeasonsLabel'),
      icon: BarChart3,
      color: 'text-purple-600',
      bgColor: 'bg-purple-50',
    },
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
      {metrics.map((metric) => {
        const Icon = metric.icon;
        return (
          <div
            key={metric.title}
            className="bg-card rounded-lg border border-border p-4 shadow-sm hover:shadow-md transition-shadow"
          >
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium text-muted-foreground">{metric.title}</span>
              <div className={`p-2 rounded-lg ${metric.bgColor}`}>
                <Icon className={`w-4 h-4 ${metric.color}`} />
              </div>
            </div>
            <div className="text-2xl font-bold text-foreground mb-1">
              {metric.value}
            </div>
            <div className="text-xs text-muted-foreground">{metric.subtitle}</div>
          </div>
        );
      })}
    </div>
  );
}



