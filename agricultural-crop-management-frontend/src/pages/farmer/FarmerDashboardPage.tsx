import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  TrendingUp,
  DollarSign,
  ShoppingCart,
  Shield,
  Edit,
  Package,
  ShoppingBag,
  FileText,
  Calendar,
  Loader2,
  AlertCircle,
  Sparkles,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import {
  BarChart,
  Bar,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip as RechartsTooltip,
  Legend,
  ResponsiveContainer,
  PieChart,
  Pie,
} from 'recharts';

import { PageContainer } from '@/shared/ui';
import { Button } from '@/shared/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/ui/card';
import { Alert, AlertDescription, AlertTitle } from '@/shared/ui/alert';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/ui/select';

import { useSeason, usePreferences } from '@/shared/contexts';
import { useBudgetTracker, useExpenseAnalyticsByCategory } from '@/entities/expense';
import { useMarketplaceFarmerDashboard } from '@/features/marketplace/hooks';
import { certificationApi } from '@/api/certificationApi';
import { formatMoney, convertToDisplayCurrency } from '@/shared/lib';
import type { Season } from '@/types/Season';

import { WeatherWidget } from '@/features/farmer/weather-widget';
import { FdnAssistantPanel } from '@/features/farmer/dashboard/components/FdnAssistantPanel';
import { FdnHistoryChart } from '@/features/farmer/dashboard/components/FdnHistoryChart';
import { FdnKpiCards } from '@/features/farmer/dashboard/components/FdnKpiCards';
import { FieldSustainabilityMap } from '@/features/farmer/dashboard/components/FieldSustainabilityMap';
import { IncidentAlerts } from '@/features/farmer/dashboard/components/IncidentAlerts';
import { InventoryAlertsPanel } from '@/features/farmer/dashboard/components/InventoryAlertsPanel';
import { NitrogenInputBreakdown } from '@/features/farmer/dashboard/components/NitrogenInputBreakdown';
import { RecentActivityTimeline } from '@/features/farmer/dashboard/components/RecentActivityTimeline';
import { SeasonTaskPanels } from '@/features/farmer/dashboard/components/SeasonTaskPanels';
import { useFarmerDashboard } from '@/features/farmer/dashboard/hooks/useFarmerDashboard';

// ═══════════════════════════════════════════════════════════════
// LOCAL SUB-COMPONENTS
// ═══════════════════════════════════════════════════════════════

interface KPICardProps {
  label: string;
  value: React.ReactNode;
  trend?: string;
  trendType?: 'up' | 'down';
  icon: React.ReactNode;
  highlight?: boolean;
  tone?: 'success' | 'warning' | 'info' | 'default';
  onClick?: () => void;
}

function KPICard({ label, value, trend, trendType = 'up', icon, highlight, tone = 'default', onClick }: KPICardProps) {
  const toneClasses = {
    success: 'bg-emerald-50 border-emerald-200 text-emerald-700 dark:bg-emerald-950 dark:border-emerald-800 dark:text-emerald-300',
    warning: 'bg-amber-50 border-amber-200 text-amber-700 dark:bg-amber-950 dark:border-amber-800 dark:text-amber-300',
    info: 'bg-blue-50 border-blue-200 text-blue-700 dark:bg-blue-950 dark:border-blue-800 dark:text-blue-300',
    default: 'bg-card border-border text-card-foreground',
  };

  return (
    <Card 
      onClick={onClick}
      className={`border transition-all duration-300 ${onClick ? 'cursor-pointer hover:shadow-md hover:-translate-y-1' : ''} ${highlight ? 'ring-2 ring-primary/20' : ''}`}
    >
      <CardContent className="p-5 flex items-center justify-between">
        <div className="space-y-1 overflow-hidden">
          <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider truncate">{label}</p>
          <p className="text-2xl font-bold tracking-tight truncate">{value}</p>
          {trend && (
            <div className="flex items-center gap-1 text-xs">
              <span className={trendType === 'up' ? 'text-emerald-600 font-semibold' : 'text-rose-600 font-semibold'}>
                {trend}
              </span>
              <span className="text-muted-foreground">so với kỳ trước</span>
            </div>
          )}
        </div>
        <div className={`p-3 rounded-xl shrink-0 ${toneClasses[tone]}`}>
          {icon}
        </div>
      </CardContent>
    </Card>
  );
}

interface QuickAction {
  label: string;
  icon: React.ReactNode;
  href: string;
}

interface QuickActionsPanelProps {
  actions: QuickAction[];
}

function QuickActionsPanel({ actions }: QuickActionsPanelProps) {
  const navigate = useNavigate();
  return (
    <Card className="border border-border">
      <CardHeader className="pb-3 pt-4 px-5">
        <CardTitle className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
          Thao tác nhanh
        </CardTitle>
      </CardHeader>
      <CardContent className="px-5 pb-4">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {actions.map((action, index) => (
            <Button
              key={index}
              variant="outline"
              onClick={() => navigate(action.href)}
              className="h-16 flex flex-col items-center justify-center gap-1.5 border-border hover:border-primary hover:bg-primary/5 hover:text-primary transition-all duration-300 group rounded-xl"
            >
              <div className="text-muted-foreground group-hover:text-primary group-hover:scale-110 transition-all duration-300">
                {action.icon}
              </div>
              <span className="text-xs font-medium">{action.label}</span>
            </Button>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}

interface SeasonYieldChartProps {
  seasons: Season[];
  selectedSeasonId: number | null;
}

function SeasonYieldChart({ seasons, selectedSeasonId }: SeasonYieldChartProps) {
  const chartData = useMemo(() => {
    return seasons
      .slice(0, 5) // Show top 5 seasons
      .reverse() // Oldest first
      .map(s => ({
        name: s.seasonName,
        expected: s.expectedYieldKg ?? 0,
        actual: s.actualYieldKg ?? 0,
        isSelected: s.id === selectedSeasonId,
      }));
  }, [seasons, selectedSeasonId]);

  if (chartData.length === 0) {
    return (
      <Card className="border border-border h-full flex flex-col justify-between">
        <CardHeader>
          <CardTitle className="text-base font-semibold">Hiệu suất Sản lượng Mùa vụ</CardTitle>
        </CardHeader>
        <CardContent className="h-64 flex items-center justify-center text-muted-foreground text-sm">
          Chưa có dữ liệu sản lượng.
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="border border-border">
      <CardHeader className="pb-2">
        <CardTitle className="text-base font-semibold flex items-center justify-between">
          <span>Hiệu suất Sản lượng Mùa vụ</span>
          <span className="text-xs font-normal text-muted-foreground">(đơn vị: kg)</span>
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="h-72 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData} margin={{ top: 20, right: 10, left: 10, bottom: 20 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
              <XAxis 
                dataKey="name" 
                tick={{ fontSize: 11, fill: "var(--muted-foreground)" }}
                axisLine={false}
                tickLine={false}
              />
              <YAxis 
                tick={{ fontSize: 11, fill: "var(--muted-foreground)" }}
                axisLine={false}
                tickLine={false}
              />
              <RechartsTooltip 
                cursor={{ fill: 'rgba(0, 0, 0, 0.05)' }}
                contentStyle={{ borderRadius: '8px', border: '1px solid var(--border)' }}
              />
              <Legend verticalAlign="top" height={36} iconType="circle" />
              <Bar dataKey="expected" name="Sản lượng Dự kiến" fill="#94a3b8" radius={[4, 4, 0, 0]} />
              <Bar dataKey="actual" name="Sản lượng Thực tế" fill="#10b981" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
}

interface ExpenseBreakdownChartProps {
  seasonId: number | null;
}

function ExpenseBreakdownChart({ seasonId }: ExpenseBreakdownChartProps) {
  const { preferences } = usePreferences();
  const { data: categoryAnalytics, isLoading } = useExpenseAnalyticsByCategory(
    seasonId ? { seasonId } : undefined,
    { enabled: !!seasonId }
  );

  const chartData = useMemo(() => {
    if (!categoryAnalytics) return [];
    return categoryAnalytics.map(c => ({
      name: c.category,
      value: c.totalAmount,
    }));
  }, [categoryAnalytics]);

  const COLORS = ['#10b981', '#3b82f6', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#14b8a6'];

  const formatValue = (value: number) =>
    formatMoney(
      convertToDisplayCurrency(value, preferences.currency),
      preferences.currency,
      preferences.locale
    );

  if (isLoading) {
    return (
      <Card className="border border-border h-full flex flex-col justify-between">
        <CardHeader>
          <CardTitle className="text-base font-semibold">Phân tích Chi phí Mùa vụ</CardTitle>
        </CardHeader>
        <CardContent className="h-64 flex items-center justify-center text-muted-foreground text-sm">
          Đang tải dữ liệu...
        </CardContent>
      </Card>
    );
  }

  if (chartData.length === 0) {
    return (
      <Card className="border border-border h-full flex flex-col justify-between">
        <CardHeader>
          <CardTitle className="text-base font-semibold">Phân tích Chi phí Mùa vụ</CardTitle>
        </CardHeader>
        <CardContent className="h-64 flex items-center justify-center text-muted-foreground text-sm">
          Không có dữ liệu chi phí cho mùa vụ này.
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="border border-border">
      <CardHeader className="pb-2">
        <CardTitle className="text-base font-semibold flex items-center justify-between">
          <span>Phân tích Chi phí Mùa vụ</span>
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 items-center">
          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={chartData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={80}
                  paddingAngle={3}
                  dataKey="value"
                >
                  {chartData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <RechartsTooltip formatter={(value: number) => formatValue(value)} />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="space-y-2 max-h-64 overflow-y-auto pr-2">
            {chartData.map((item, index) => (
              <div key={item.name} className="flex items-center justify-between text-xs border-b border-border/50 pb-1.5">
                <div className="flex items-center gap-2">
                  <div 
                    className="w-2.5 h-2.5 rounded-full shrink-0" 
                    style={{ backgroundColor: COLORS[index % COLORS.length] }}
                  />
                  <span className="text-muted-foreground font-medium truncate max-w-[120px]">{item.name}</span>
                </div>
                <span className="font-semibold text-foreground shrink-0">{formatValue(item.value)}</span>
              </div>
            ))}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

// ═══════════════════════════════════════════════════════════════
// MAIN DASHBOARD PAGE Component
// ═══════════════════════════════════════════════════════════════

export function FarmerDashboardPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { preferences } = usePreferences();
  const [fdnExpanded, setFdnExpanded] = useState(false);

  // Load state from primary hook
  const {
    selectedSeason,
    setSelectedSeason,
    seasonOptions,
    overview,
    fieldMap,
    mapLoading,
    todayTasks,
    upcomingTasks,
    dataCompletenessWarnings,
    incidentAlerts,
    recentActivities,
    inventoryAlerts,
    inventoryAlertsSummary,
    isCriticalLoading,
    isDataLoading,
    overviewLoading,
    hasNoSeasons,
    seasonsError,
    overviewError,
    mapError,
    todayTasksError,
    upcomingTasksError,
    dataCompletenessWarningsError,
    incidentAlertsError,
    recentActivitiesError,
    inventoryAlertsError,
  } = useFarmerDashboard();

  // Load Season Detail / List from Context
  const { selectedSeason: activeSeason, seasons: allSeasons, selectedSeasonId } = useSeason();

  // Load Budget / Expenses for Selected Season
  const { data: budgetTracker } = useBudgetTracker(selectedSeasonId || 0, {
    enabled: !!selectedSeasonId,
  });

  // Load Marketplace stats
  const { data: marketplaceDashboard } = useMarketplaceFarmerDashboard();

  // Load VietGAP Score
  const [complianceScore, setComplianceScore] = useState<number | null>(null);
  const farmId = activeSeason?.farmId;

  useEffect(() => {
    if (farmId) {
      certificationApi.getCertificationDetails(farmId)
        .then(res => setComplianceScore(res.complianceScore))
        .catch(err => console.error("Error fetching compliance score", err));
    } else {
      setComplianceScore(null);
    }
  }, [farmId]);

  const weatherSeasonId = selectedSeason
    ? Number.parseInt(selectedSeason, 10)
    : undefined;

  // Format monetary value helper
  const formatValue = (value: number) =>
    formatMoney(
      convertToDisplayCurrency(value, preferences.currency),
      preferences.currency,
      preferences.locale
    );

  if (isCriticalLoading) {
    return (
      <div className="min-h-screen acm-main-content flex items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <Loader2 className="w-8 h-8 animate-spin text-secondary" />
          <p className="text-muted-foreground">
            {t('dashboard.loading', { defaultValue: 'Loading dashboard...' })}
          </p>
        </div>
      </div>
    );
  }

  if (seasonsError) {
    return (
      <div className="min-h-screen acm-main-content flex items-center justify-center p-6">
        <Alert variant="destructive" className="max-w-md">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>
            {t('dashboard.error.title', {
              defaultValue: 'Failed to Load Dashboard',
            })}
          </AlertTitle>
          <AlertDescription>
            {t('dashboard.error.description', {
              defaultValue:
                'Unable to load seasons data. Please check your connection and try again.',
            })}
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  if (hasNoSeasons) {
    return (
      <PageContainer variant="dashboard">
        <div className="flex flex-col items-center justify-center min-h-[60vh] gap-6">
          <Calendar className="w-20 h-20 text-primary opacity-40" />
          <div className="text-center space-y-2">
            <h2 className="text-2xl font-semibold text-foreground">
              {t('dashboard.empty.title', {
                defaultValue: 'No Seasons Available',
              })}
            </h2>
            <p className="text-muted-foreground max-w-md">
              {t('dashboard.empty.description', {
                defaultValue:
                  'Start by creating a new season to track your crops.',
              })}
            </p>
          </div>
        </div>
      </PageContainer>
    );
  }

  const partialErrors = [
    overviewError,
    mapError,
    todayTasksError,
    upcomingTasksError,
    dataCompletenessWarningsError,
    incidentAlertsError,
    recentActivitiesError,
    inventoryAlertsError,
  ].filter((error): error is Error => Boolean(error));

  // Determine yield value (actual if completed, fallback to estimated)
  const currentSeasonYield = activeSeason?.actualYieldKg ?? overview?.estimatedYieldMetric?.value ?? 0;
  const totalExpense = budgetTracker?.total ?? 0;
  const pendingOrders = marketplaceDashboard?.pendingOrders ?? 0;
  const vietgapScore = complianceScore ?? 0;

  return (
    <PageContainer variant="dashboard" className="pb-10">
      <div className="space-y-6">
        {partialErrors.length > 0 && (
          <Alert variant="destructive">
            <AlertCircle className="h-4 w-4" />
            <AlertTitle>
              {t('dashboard.dataLoadError', {
                defaultValue: 'Some data failed to load',
              })}
            </AlertTitle>
            <AlertDescription>
              {partialErrors.map((error, index) => (
                <div key={`${error.message}-${index}`}>{error.message}</div>
              ))}
            </AlertDescription>
          </Alert>
        )}

        {/* Header Section: Season Selector & Weather */}
        <Card variant="elevated">
          <CardContent className="pt-6">
            <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
              <div className="flex items-center gap-3 w-full md:w-auto">
                <Calendar className="w-5 h-5 text-primary" />
                <Select value={selectedSeason} onValueChange={setSelectedSeason}>
                  <SelectTrigger className="w-full sm:w-72 border-border rounded-lg">
                    <SelectValue
                      placeholder={t('dashboard.fdn.selectSeason', {
                        defaultValue: 'Select season',
                      })}
                    />
                  </SelectTrigger>
                  <SelectContent>
                    {seasonOptions.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <span className="acm-body-text text-muted-foreground">
                  {t('dashboard.fdn.weatherSummary', {
                    defaultValue: 'Weather summary',
                  })}
                </span>
                <WeatherWidget
                  variant="compact"
                  seasonId={Number.isNaN(weatherSeasonId) ? undefined : weatherSeasonId}
                />
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Row 1: Primary Operational KPIs */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <KPICard
            label="Sản lượng mùa vụ"
            value={`${currentSeasonYield.toLocaleString()} kg`}
            icon={<TrendingUp className="w-5 h-5" />}
            tone="info"
          />
          <KPICard
            label="Chi phí mùa vụ"
            value={formatValue(totalExpense)}
            icon={<DollarSign className="w-5 h-5" />}
            tone="default"
          />
          <KPICard
            label="Đơn hàng chờ"
            value={pendingOrders}
            icon={<ShoppingCart className="w-5 h-5" />}
            highlight={pendingOrders > 0}
            tone={pendingOrders > 0 ? 'warning' : 'default'}
            onClick={() => navigate('/farmer/marketplace-orders')}
          />
          <KPICard
            label="Điểm VietGAP"
            value={complianceScore !== null ? `${complianceScore}%` : 'Chưa có'}
            icon={<Shield className="w-5 h-5" />}
            tone={vietgapScore >= 80 ? 'success' : 'warning'}
            onClick={farmId ? () => navigate(`/farmer/farms/${farmId}/certification`) : undefined}
          />
        </div>

        {/* Row 2: Quick Actions */}
        <QuickActionsPanel
          actions={[
            { label: 'Ghi nhật ký', icon: <Edit className="w-5 h-5" />, href: `/farmer/seasons/${selectedSeasonId}/workspace/field-logs` },
            { label: 'Tạo thu hoạch', icon: <Package className="w-5 h-5" />, href: `/farmer/seasons/${selectedSeasonId}/workspace/harvest` },
            { label: 'Xem đơn hàng', icon: <ShoppingBag className="w-5 h-5" />, href: '/farmer/marketplace-orders' },
            { label: 'Tài liệu nông trại', icon: <FileText className="w-5 h-5" />, href: '/farmer/farm-documents' },
          ]}
        />

        {/* Row 3: Analytical Charts */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <SeasonYieldChart seasons={allSeasons} selectedSeasonId={selectedSeasonId} />
          <ExpenseBreakdownChart seasonId={selectedSeasonId} />
        </div>

        {/* Row 4: Expandable FDN & Nitrogen Sustainability Dashboard Section */}
        <Card className="border border-border overflow-hidden">
          <CardHeader 
            className="pb-4 pt-4 px-5 flex flex-row items-center justify-between cursor-pointer hover:bg-muted/10 transition-colors"
            onClick={() => setFdnExpanded(!fdnExpanded)}
          >
            <div className="flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-primary animate-pulse" />
              <CardTitle className="text-base font-semibold">
                Báo cáo FDN & Chỉ số Bền vững
              </CardTitle>
            </div>
            <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
              {fdnExpanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
            </Button>
          </CardHeader>
          {fdnExpanded && (
            <CardContent className="px-5 pb-6 border-t border-border pt-6 space-y-6">
              <FdnKpiCards
                overview={overview}
                isLoading={overviewLoading}
                errorMessage={overviewError?.message ?? null}
              />

              <div className="grid grid-cols-1 xl:grid-cols-[1.2fr_0.8fr] gap-6">
                <NitrogenInputBreakdown
                  overview={overview}
                  isLoading={overviewLoading}
                  errorMessage={overviewError?.message ?? null}
                />
                <FdnAssistantPanel
                  overview={overview}
                  isLoading={overviewLoading}
                  errorMessage={overviewError?.message ?? null}
                />
              </div>

              <FieldSustainabilityMap
                mapData={fieldMap}
                isLoading={mapLoading}
                apiErrorMessage={mapError?.message ?? null}
              />

              <div className="grid grid-cols-1 xl:grid-cols-[1.15fr_0.85fr] gap-6">
                <FdnHistoryChart
                  overview={overview}
                  isLoading={overviewLoading}
                  errorMessage={overviewError?.message ?? null}
                />
                <SeasonTaskPanels
                  todayTasks={todayTasks}
                  upcomingTasks={upcomingTasks}
                  dataCompletenessWarnings={dataCompletenessWarnings}
                  isLoading={isDataLoading}
                  errorMessage={
                    partialErrors.length > 0
                      ? t('dashboard.dataLoadError', {
                        defaultValue: 'Some data failed to load',
                      })
                      : null
                  }
                />
              </div>

              <IncidentAlerts
                alerts={incidentAlerts}
                isLoading={isDataLoading}
                errorMessage={incidentAlertsError?.message ?? null}
              />

              <div id="recent-activity">
                <RecentActivityTimeline
                  activities={recentActivities}
                  isLoading={isDataLoading}
                  errorMessage={recentActivitiesError?.message ?? null}
                />
              </div>

              <InventoryAlertsPanel
                alerts={inventoryAlerts}
                summary={inventoryAlertsSummary}
                isLoading={isDataLoading}
                errorMessage={inventoryAlertsError?.message ?? null}
              />
            </CardContent>
          )}
        </Card>
      </div>
    </PageContainer>
  );
}
