import { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertCircle, Clock3 } from 'lucide-react';
import { AdminContentCard } from '@/features/admin/shared/ui';
import {
  Alert,
  AlertDescription,
  AlertTitle,
  Badge,
  Button,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  Skeleton,
} from '@/shared/ui';
import { cn } from '@/shared/lib';
import type { AdminDashboardPendingApproval } from '../hooks/useAdminDashboard';
import { useI18n } from '@/shared/lib/hooks/useI18n';

type PendingApprovalsProps = {
  items: AdminDashboardPendingApproval[];
  isLoading: boolean;
  error: Error | null;
  onRetry: () => void;
};

const severityClasses: Record<string, string> = {
  CRITICAL: 'bg-red-100 text-red-700 border-red-300',
  HIGH: 'bg-orange-100 text-orange-700 border-orange-300',
  MEDIUM: 'bg-amber-100 text-amber-700 border-amber-300',
  LOW: 'bg-emerald-100 text-emerald-700 border-emerald-300',
};

function resolveSeverityTag(input?: string | null): string {
  const normalized = input?.trim().toUpperCase();
  if (normalized && severityClasses[normalized]) {
    return normalized;
  }
  return 'LOW';
}

function formatSubmittedAt(value: string | null | undefined, locale: string, fallback: string): string {
  if (!value) {
    return fallback;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return fallback;
  }
  return new Intl.DateTimeFormat(locale, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date);
}

export function PendingApprovals({
  items,
  isLoading,
  error,
  onRetry,
}: PendingApprovalsProps) {
  const navigate = useNavigate();
  const { t, locale } = useI18n();
  const sortedItems = useMemo(
    () => [...items].sort((a, b) => (a.submittedAt || '').localeCompare(b.submittedAt || '')),
    [items],
  );

  return (
    <AdminContentCard>
      <CardHeader>
        <div className="flex items-center justify-between gap-3">
          <div>
            <CardTitle className="text-lg">{t('admin.dashboard.pendingApprovals.title')}</CardTitle>
            <CardDescription>
              {t('admin.dashboard.pendingApprovals.description')}
            </CardDescription>
          </div>
          <Badge variant="outline">{items.length}</Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        {isLoading && (
          <div className="space-y-3">
            {[...Array(4)].map((_, index) => (
              <Skeleton key={index} className="h-[72px] w-full rounded-xl" />
            ))}
          </div>
        )}

        {!isLoading && error && (
          <Alert variant="destructive">
            <AlertCircle className="h-4 w-4" />
            <AlertTitle>{t('admin.dashboard.pendingApprovals.errorTitle')}</AlertTitle>
            <AlertDescription className="mt-2 flex items-center justify-between gap-3">
              <span>{error.message || t('common.tryAgain')}</span>
              <Button variant="outline" size="sm" onClick={onRetry}>
                {t('common.retry')}
              </Button>
            </AlertDescription>
          </Alert>
        )}

        {!isLoading && !error && sortedItems.length === 0 && (
          <div className="rounded-xl border border-dashed p-6 text-center text-sm text-muted-foreground">
            {t('admin.dashboard.pendingApprovals.empty')}
          </div>
        )}

        {!isLoading && !error && sortedItems.length > 0 && (
          <div className="space-y-2">
            {sortedItems.map((item) => {
              const severity = resolveSeverityTag(item.severity || item.priority);
              return (
                <div
                  key={`${item.type}-${item.id}`}
                  className="rounded-xl border border-border p-3 sm:p-4"
                >
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div className="min-w-0 space-y-1">
                      <p className="font-medium leading-tight">{item.title}</p>
                      {item.subtitle ? (
                        <p className="text-sm text-muted-foreground">{item.subtitle}</p>
                      ) : null}
                      <div className="flex items-center gap-2 text-xs text-muted-foreground">
                        <Clock3 className="h-3.5 w-3.5" />
                        <span>
                          {formatSubmittedAt(
                            item.submittedAt,
                            locale,
                            t('admin.dashboard.pendingApprovals.timeUnavailable'),
                          )}
                        </span>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge className={cn('border', severityClasses[severity])}>
                        {t(`admin.dashboard.pendingApprovals.severity.${severity}`)}
                      </Badge>
                      {item.actionUrl ? (
                        <Button
                          size="sm"
                          onClick={() => {
                            if (item.actionUrl) {
                              navigate(item.actionUrl);
                            }
                          }}
                        >
                          {t('admin.dashboard.pendingApprovals.review')}
                        </Button>
                      ) : null}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </CardContent>
    </AdminContentCard>
  );
}
