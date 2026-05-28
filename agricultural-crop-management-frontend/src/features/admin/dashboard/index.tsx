import { BarChart3, RefreshCw, AlertCircle } from 'lucide-react';
import {
  Alert,
  AlertDescription,
  AlertTitle,
  Button,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/shared/ui';
import { useAdminDashboard } from './hooks/useAdminDashboard';
import { DashboardSkeleton } from './components/DashboardSkeleton';
import { RiskySeasonsTable } from './components/RiskySeasonsTable';
import { InventoryHealthCards } from './components/InventoryHealthCards';
import { PendingApprovals } from './components/PendingApprovals';
import { cn } from '@/shared/lib';
import { useI18n } from '@/shared/lib/hooks/useI18n';
import {
  AdminContentCard,
  AdminHeaderCard,
  AdminMetricCard,
  AdminPageContainer,
} from '@/features/admin/shared/ui';
import {
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  Tooltip,
  Legend,
} from 'recharts';

/**
 * AdminDashboard Component
 *
 * Real-time dashboard for administrators with comprehensive platform monitoring.
 * Features:
 * - Auto-refresh every 30s for real-time data
 * - Skeleton loading instead of spinner
 * - Risky seasons with drill-down navigation
 * - Inventory health with expiry warnings
 */
export function AdminDashboard() {
  const { t } = useI18n();
  const {
    isLoading,
    isError,
    error,
    isFetching,
    kpiMetrics,
    charts,
    risks,
    riskDataCoverage,
    riskDataLimited,
    pendingApprovals,
    pendingApprovalsLoading,
    pendingApprovalsError,
    refetch,
  } = useAdminDashboard();

  // Show skeleton while loading
  if (isLoading) {
    return <DashboardSkeleton />;
  }

  // Show error state with retry option
  if (isError) {
    return (
      <AdminPageContainer>
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>{t('admin.dashboard.error.title')}</AlertTitle>
          <AlertDescription className="mt-2">
            {error?.message || t('admin.dashboard.error.description')}
            <Button
              variant="outline"
              size="sm"
              className="ml-4"
              onClick={() => refetch()}
            >
              <RefreshCw className="h-4 w-4 mr-2" />
              {t('common.retry')}
            </Button>
          </AlertDescription>
        </Alert>
      </AdminPageContainer>
    );
  }

  return (
    <AdminPageContainer>
      <AdminHeaderCard
        title={t('admin.dashboard.title')}
        description={t('admin.dashboard.subtitle')}
        metadata={
          <>
            {isFetching && (
              <RefreshCw className="h-4 w-4 text-muted-foreground animate-spin" />
            )}
          </>
        }
        actions={
          <>
          <Button
            variant="outline"
            onClick={() => refetch()}
            disabled={isFetching}
            className="w-full rounded-[14px] sm:w-auto"
          >
            <RefreshCw className={cn('h-4 w-4 mr-2', isFetching && 'animate-spin')} />
            {t('common.refresh')}
          </Button>
          <Button
            className="w-full rounded-[14px] sm:w-auto"
            disabled
            disabledHint="Dashboard export is not implemented yet"
          >
            <BarChart3 className="w-4 h-4 mr-2" />
            {t('admin.dashboard.exportReport')}
          </Button>
          </>
        }
      />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
        {kpiMetrics.map((kpi) => (
          <AdminMetricCard
            key={kpi.key}
            label={kpi.title}
            value={kpi.value}
            icon={kpi.icon}
            iconWrapClassName={kpi.bgColor}
            iconClassName={kpi.textColor}
          />
        ))}
      </div>

      {/* Charts Row - User Distribution & Season Status */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* User Roles Distribution */}
        <AdminContentCard>
          <CardHeader>
            <CardTitle className="text-lg">User Distribution by Role</CardTitle>
            <CardDescription>Breakdown of users by their assigned roles</CardDescription>
          </CardHeader>
          <CardContent>
            {charts.userRoles.length === 0 ? (
              <div className="flex items-center justify-center h-[200px] text-muted-foreground">
                No user data available
              </div>
            ) : (
              <ResponsiveContainer width="100%" height={200}>
                <PieChart>
                  <Pie
                    data={charts.userRoles}
                    cx="50%"
                    cy="50%"
                    innerRadius={40}
                    outerRadius={80}
                    paddingAngle={5}
                    dataKey="value"
                  >
                    {charts.userRoles.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </AdminContentCard>

        {/* Season Status Distribution */}
        <AdminContentCard>
          <CardHeader>
            <CardTitle className="text-lg">Season Status Distribution</CardTitle>
            <CardDescription>Current status of all seasons</CardDescription>
          </CardHeader>
          <CardContent>
            {charts.seasonStatus.length === 0 ? (
              <div className="flex items-center justify-center h-[200px] text-muted-foreground">
                No season data available
              </div>
            ) : (
              <ResponsiveContainer width="100%" height={200}>
                <PieChart>
                  <Pie
                    data={charts.seasonStatus}
                    cx="50%"
                    cy="50%"
                    innerRadius={40}
                    outerRadius={80}
                    paddingAngle={5}
                    dataKey="value"
                  >
                    {charts.seasonStatus.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </AdminContentCard>
      </div>

      {/* Risky Seasons & Inventory Health */}
      <PendingApprovals
        items={pendingApprovals}
        isLoading={pendingApprovalsLoading}
        error={pendingApprovalsError}
        onRetry={() => void refetch()}
      />

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <RiskySeasonsTable
          seasons={risks}
          isLoading={isLoading}
          error={isError ? (error as Error) : null}
          riskDataLimited={riskDataLimited}
          dataCoverage={riskDataCoverage}
        />
        <InventoryHealthCards />
      </div>
    </AdminPageContainer>
  );
}
