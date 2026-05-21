import { Badge } from '@/shared/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card';
import { Separator } from '@/shared/ui/separator';
import { Skeleton } from '@/shared/ui/skeleton';
import type {
  DashboardInventoryAlertItem,
  DashboardInventoryAlertsSummary,
} from '@/entities/dashboard';
import { AlertTriangle } from 'lucide-react';
import { useTranslation } from 'react-i18next';

interface InventoryAlertsPanelProps {
  alerts: DashboardInventoryAlertItem[];
  summary: DashboardInventoryAlertsSummary | null;
  isLoading: boolean;
  errorMessage?: string | null;
}

function severityClass(severity: string): string {
  switch (severity.toUpperCase()) {
    case 'CRITICAL':
      return 'bg-destructive text-destructive-foreground';
    case 'HIGH':
      return 'bg-orange-100 text-orange-800';
    case 'MEDIUM':
      return 'bg-amber-100 text-amber-800';
    default:
      return 'bg-secondary text-secondary-foreground';
  }
}

function formatDate(date: string | null): string {
  if (!date) {
    return '-';
  }
  const parsed = new Date(`${date}T00:00:00`);
  if (Number.isNaN(parsed.getTime())) {
    return date;
  }
  return parsed.toLocaleDateString();
}

export function InventoryAlertsPanel({
  alerts,
  summary,
  isLoading,
  errorMessage,
}: InventoryAlertsPanelProps) {
  const { t } = useTranslation();
  const totalAlerts = summary?.totalAlerts ?? alerts.length;

  return (
    <Card className="border-border acm-card-elevated acm-hover-surface">
      <CardHeader className="pb-4">
        <div className="flex items-center justify-between gap-2">
          <CardTitle>
            {t('dashboard.inventoryAlerts.title', {
              defaultValue: 'Inventory Alerts',
            })}
          </CardTitle>
          <Badge variant="outline">{totalAlerts}</Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex flex-wrap items-center gap-2 text-xs">
          <Badge variant="secondary">
            {t('dashboard.inventoryAlerts.summary.expired', { defaultValue: 'Expired' })}:{' '}
            {summary?.expired ?? 0}
          </Badge>
          <Badge variant="secondary">
            {t('dashboard.inventoryAlerts.summary.expiringSoon', { defaultValue: 'Expiring Soon' })}:{' '}
            {summary?.expiringSoon ?? 0}
          </Badge>
          <Badge variant="secondary">
            {t('dashboard.inventoryAlerts.summary.lowStock', { defaultValue: 'Low Stock' })}:{' '}
            {summary?.lowStock ?? 0}
          </Badge>
          <Badge variant="secondary">
            {t('dashboard.inventoryAlerts.summary.noMovement', { defaultValue: 'No Movement' })}:{' '}
            {summary?.noMovement ?? 0}
          </Badge>
        </div>

        {isLoading ? (
          <div className="space-y-3">
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
          </div>
        ) : null}

        {!isLoading && errorMessage ? (
          <p className="acm-body-text text-destructive">{errorMessage}</p>
        ) : null}

        {!isLoading && !errorMessage && alerts.length === 0 ? (
          <p className="acm-body-text text-muted-foreground">
            {t('dashboard.inventoryAlerts.empty', { defaultValue: 'No inventory alerts' })}
          </p>
        ) : null}

        {!isLoading && !errorMessage && alerts.length > 0 ? (
          <div className="space-y-3">
            {alerts.map((alert, index) => (
              <div key={`${alert.alertType}-${alert.supplyLotId}-${index}`} className="space-y-2">
                <div className="flex items-start justify-between gap-3">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <AlertTriangle className="h-4 w-4 text-amber-600" />
                      <p className="text-sm font-semibold">{alert.itemName}</p>
                    </div>
                    <p className="text-xs text-muted-foreground">
                      {alert.lotCode ?? '-'} • {alert.locationLabel ?? '-'}
                    </p>
                  </div>
                  <Badge className={severityClass(alert.severity)}>{alert.severity}</Badge>
                </div>
                <p className="text-xs text-muted-foreground">
                  {t('dashboard.inventoryAlerts.quantity', { defaultValue: 'Quantity' })}: {alert.quantity}{' '}
                  {alert.unit ?? ''} •{' '}
                  {t('dashboard.inventoryAlerts.expiryDate', { defaultValue: 'Expiry' })}:{' '}
                  {formatDate(alert.expiryDate)}
                </p>
                {alert.reason ? (
                  <p className="text-xs text-muted-foreground">
                    {t('dashboard.inventoryAlerts.reason', { defaultValue: 'Reason' })}: {alert.reason}
                  </p>
                ) : null}
                {index < alerts.length - 1 ? <Separator /> : null}
              </div>
            ))}
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}
