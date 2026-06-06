import { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Building2, MapPin, Search, RefreshCw, MoreVertical } from 'lucide-react';
import {
  AsyncState,
  BackButton,
  CardContent,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/shared/ui';
import {
  AdminContentCard,
  AdminFilterCard,
  AdminHeaderCard,
  AdminPageContainer,
  adminTabsListClass,
} from '@/features/admin/shared/ui';
import { cn } from '@/shared/lib';
import { useI18n } from '@/shared/lib/hooks/useI18n';
import { useQueryClient } from '@tanstack/react-query';
import {
  useAdminFarms,
  useAdminFarmDetail,
  useAdminPlots,
  useAdminPlotDetail,
  useAdminFarmPlots,
  useAdminPlotSeasons,
  useAdminFarmsForFilter,
  adminFarmsPlotsKeys,
  type Farm,
  type Plot,
} from './hooks/useFarmsPlots';

// Types re-exported from hooks/useFarmsPlots.ts

export function FarmsPlotsPage() {
  const { t } = useI18n();
  const queryClient = useQueryClient();

  // UI state (tabs, drawers, search, pagination, filters)
  const [activeTab, setActiveTab] = useState<'farms' | 'plots'>('farms');
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(0);
  const [selectedFarm, setSelectedFarm] = useState<Farm | null>(null);
  const [selectedPlot, setSelectedPlot] = useState<Plot | null>(null);
  const [showFarmDetail, setShowFarmDetail] = useState(false);
  const [showPlotDetail, setShowPlotDetail] = useState(false);
  const [farmFilter, setFarmFilter] = useState<number | null>(null);
  const [searchParams, setSearchParams] = useSearchParams();

  // URL params
  const tabParam = searchParams.get("tab");
  const qParam = searchParams.get("q") ?? "";
  const farmIdParam = Number(searchParams.get("farmId"));
  const plotIdParam = Number(searchParams.get("plotId"));
  const seasonIdParam = Number(searchParams.get("seasonId"));
  const parsedFarmId = Number.isFinite(farmIdParam) ? farmIdParam : null;
  const parsedPlotId = Number.isFinite(plotIdParam) ? plotIdParam : null;
  const parsedSeasonId = Number.isFinite(seasonIdParam) ? seasonIdParam : null;

  // ── React Query: Farms list ──
  const farmsQuery = useAdminFarms({ page, size: 20, keyword: searchTerm || undefined });
  const farms = farmsQuery.data?.items ?? [];
  const farmsTotalPages = farmsQuery.data?.totalPages ?? 0;

  // ── React Query: Plots list ──
  const plotsQuery = useAdminPlots({ page, size: 20, keyword: searchTerm || undefined, farmId: farmFilter });
  const plots = plotsQuery.data?.items ?? [];
  const plotsTotalPages = plotsQuery.data?.totalPages ?? 0;

  // ── React Query: Farm filter dropdown options ──
  const { data: filterFarms } = useAdminFarmsForFilter();

  // ── React Query: Farm detail drawer – plots for selected farm ──
  const farmPlotsQuery = useAdminFarmPlots(selectedFarm?.id ?? null, showFarmDetail);
  const farmPlots = farmPlotsQuery.data ?? [];

  // ── React Query: Plot detail drawer – seasons for selected plot ──
  const plotSeasonsQuery = useAdminPlotSeasons(selectedPlot?.id ?? null, showPlotDetail);
  const plotSeasons = plotSeasonsQuery.data ?? [];

  // ── React Query: Deep-link farm/plot detail (when not in list) ──
  const farmDetailQuery = useAdminFarmDetail(
    parsedFarmId && activeTab === 'farms' && !showFarmDetail ? parsedFarmId : null
  );
  const plotDetailQuery = useAdminPlotDetail(
    parsedPlotId && activeTab === 'plots' && !showPlotDetail ? parsedPlotId : null
  );

  // Derived loading/error for active tab
  const loading = activeTab === 'farms' ? farmsQuery.isLoading : plotsQuery.isLoading;
  const error = activeTab === 'farms'
    ? (farmsQuery.isError ? t('admin.farmsPlots.farms.loadError') : null)
    : (plotsQuery.isError ? t('admin.farmsPlots.plots.loadError') : null);
  const totalPages = activeTab === 'farms' ? farmsTotalPages : plotsTotalPages;
  const detailLoading = showFarmDetail ? farmPlotsQuery.isLoading : plotSeasonsQuery.isLoading;

  const clearQueryParams = (keys: string[]) => {
    const next = new URLSearchParams(searchParams);
    let changed = false;
    keys.forEach((key) => {
      if (next.has(key)) {
        next.delete(key);
        changed = true;
      }
    });
    if (changed) {
      setSearchParams(next, { replace: true });
    }
  };

  const closeFarmDetail = () => {
    // Farm detail is opened via ?farmId=... on the farms tab; clear it so it won't re-open.
    clearQueryParams(["farmId", "seasonId"]);
    setShowFarmDetail(false);
  };

  const closePlotDetail = () => {
    // Plot detail is opened via ?plotId=...; clear it so it won't re-open.
    clearQueryParams(["plotId", "seasonId"]);
    setShowPlotDetail(false);
  };

  // ── Handlers: lightweight, just set UI state ──
  const handleViewFarm = useCallback((farm: Farm) => {
    setSelectedFarm(farm);
    setShowFarmDetail(true);
  }, []);

  const handleViewPlot = useCallback((plot: Plot) => {
    setSelectedPlot(plot);
    setShowPlotDetail(true);
  }, []);

  const handleSearch = () => {
    setPage(0);
    // React Query auto-refetches because searchTerm is in the queryKey
  };

  const handleRefresh = () => {
    if (activeTab === 'farms') {
      void queryClient.invalidateQueries({ queryKey: adminFarmsPlotsKeys.farms });
    } else {
      void queryClient.invalidateQueries({ queryKey: adminFarmsPlotsKeys.plots });
    }
  };

  // ── Sync tab from URL ──
  useEffect(() => {
    if (tabParam === "farms" || tabParam === "plots") {
      setActiveTab(tabParam);
      return;
    }
    if (parsedPlotId || parsedSeasonId) {
      setActiveTab("plots");
      return;
    }
    if (parsedFarmId) {
      setActiveTab("farms");
    }
  }, [tabParam, parsedFarmId, parsedPlotId, parsedSeasonId]);

  // ── Sync farmFilter from URL ──
  useEffect(() => {
    if (parsedFarmId && farmFilter !== parsedFarmId) {
      setFarmFilter(parsedFarmId);
      setPage(0);
    }
  }, [parsedFarmId, farmFilter]);

  // ── Sync search term from URL ──
  useEffect(() => {
    if (qParam === searchTerm) return;
    setSearchTerm(qParam);
    setPage(0);
  }, [qParam, activeTab, farmFilter, searchTerm]);

  // ── Deep-link: open farm detail from ?farmId= ──
  useEffect(() => {
    if (!parsedFarmId || activeTab !== "farms") return;
    if (selectedFarm?.id === parsedFarmId && showFarmDetail) return;

    const match = farms.find((farm) => farm.id === parsedFarmId);
    if (match) {
      handleViewFarm(match);
      return;
    }
    // If not in list, use the detail query result
    if (farmDetailQuery.data) {
      handleViewFarm(farmDetailQuery.data);
    }
  }, [parsedFarmId, activeTab, farms, selectedFarm?.id, showFarmDetail, handleViewFarm, farmDetailQuery.data]);

  // ── Deep-link: open plot detail from ?plotId= ──
  useEffect(() => {
    if (!parsedPlotId || activeTab !== "plots") return;
    if (selectedPlot?.id === parsedPlotId && showPlotDetail) return;

    const match = plots.find((plot) => plot.id === parsedPlotId);
    if (match) {
      handleViewPlot(match);
      return;
    }
    // If not in list, use the detail query result
    if (plotDetailQuery.data) {
      handleViewPlot(plotDetailQuery.data);
    }
  }, [parsedPlotId, activeTab, plots, selectedPlot?.id, showPlotDetail, handleViewPlot, plotDetailQuery.data]);

  const STATUS_COLORS: Record<string, string> = {
    PLANNED: 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300',
    ACTIVE: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
    COMPLETED: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-400',
    CANCELLED: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
  };

  const renderFarms = () => (
    <div className="space-y-4">
      <AdminFilterCard>
        <CardContent className="p-4">
          <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex flex-col sm:flex-row sm:items-center gap-2 w-full lg:w-auto">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <input
                  type="text"
                  placeholder={t('admin.farmsPlots.farms.searchPlaceholder')}
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                  className="pl-10 pr-4 py-2 border border-border rounded-[14px] bg-card text-sm w-full sm:w-64"
                />
              </div>
              <button
                onClick={() => handleSearch()}
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-3 py-2 border border-border rounded-[14px] text-sm hover:bg-muted/50"
              >
                <Search className="h-4 w-4" />
                {t('common.search')}
              </button>
            </div>
            <button
              onClick={handleRefresh}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-3 py-2 border border-border rounded-[14px] text-sm hover:bg-muted/50"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
              {t('common.refresh')}
            </button>
          </div>
        </CardContent>
      </AdminFilterCard>

      <AsyncState
        isLoading={loading}
        isEmpty={!loading && !error && farms.length === 0}
        error={error ? new Error(error) : null}
        onRetry={handleRefresh}
        loadingText={t('admin.farmsPlots.farms.loading')}
        emptyIcon={<Building2 className="h-6 w-6 text-muted-foreground" />}
        emptyTitle={t('admin.farmsPlots.farms.emptyTitle')}
        emptyDescription={t('admin.farmsPlots.farms.emptyDescription')}
      >
        <AdminContentCard>
          <div className="overflow-x-auto">
          <table className="w-full min-w-[860px]">
            <thead className="bg-muted/50 border-b border-border">
              <tr>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('common.name')}</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('admin.farmsPlots.table.owner')}</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('admin.farmsPlots.table.areaHa')}</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('common.status')}</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('admin.farmsPlots.table.location')}</th>
                <th className="px-4 py-3 text-right text-sm font-medium text-muted-foreground">{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {farms.map((farm) => (
                <tr key={farm.id} className="border-b border-border hover:bg-muted/30">
                  <td className="px-4 py-3 text-sm font-medium">{farm.name}</td>
                  <td className="px-4 py-3 text-sm">{farm.ownerUsername || '-'}</td>
                  <td className="px-4 py-3 text-sm">{farm.area?.toFixed(2) || '-'}</td>
                  <td className="px-4 py-3 text-sm">
                    <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs ${farm.active
                      ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400'
                      : 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300'
                      }`}>
                      {farm.active ? t('common.active') : t('common.inactive')}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-muted-foreground">
                    {[farm.wardName, farm.provinceName].filter(Boolean).join(', ') || '-'}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <button
                          type="button"
                          className="inline-flex h-8 w-8 items-center justify-center rounded-[14px] border border-border text-muted-foreground hover:bg-muted/50 hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                          aria-label={t('admin.farmsPlots.actionsFor', { name: farm.name })}
                        >
                          <MoreVertical className="h-4 w-4" />
                        </button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" className="w-40">
                        <DropdownMenuItem onSelect={() => handleViewFarm(farm)}>
                          {t('admin.farmsPlots.actions.viewDetails')}
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          </div>
        </AdminContentCard>
      </AsyncState>

      {totalPages > 1 && (
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <button
            onClick={() => setPage(Math.max(0, page - 1))}
            disabled={page === 0}
            className="px-3 py-1 border border-border rounded text-sm disabled:opacity-50"
          >
            {t('pagination.previousPage')}
          </button>
          <span className="text-sm text-muted-foreground">
            {t('pagination.page')} {page + 1} {t('pagination.of')} {totalPages}
          </span>
          <button
            onClick={() => setPage(page + 1)}
            disabled={page >= totalPages - 1}
            className="px-3 py-1 border border-border rounded text-sm disabled:opacity-50"
          >
            {t('pagination.nextPage')}
          </button>
        </div>
      )}
    </div>
  );

  const renderPlots = () => (
    <div className="space-y-4">
      <AdminFilterCard>
        <CardContent className="p-4">
          <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex flex-col sm:flex-row sm:items-center gap-2 w-full lg:w-auto">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <input
                  type="text"
                  placeholder={t('admin.farmsPlots.plots.searchPlaceholder')}
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                  className="pl-10 pr-4 py-2 border border-border rounded-[14px] bg-card text-sm w-full sm:w-64"
                />
              </div>
              <select
                value={farmFilter || ''}
                onChange={(e) => { setFarmFilter(e.target.value ? Number(e.target.value) : null); setPage(0); }}
                className="w-full sm:w-auto px-3 py-2 border border-border rounded-[14px] bg-card text-sm"
              >
                <option value="">{t('admin.farmsPlots.farms.all')}</option>
                {(filterFarms ?? []).map(f => (
                  <option key={f.id} value={f.id}>{f.name}</option>
                ))}
              </select>
            </div>
            <button
              onClick={handleRefresh}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-3 py-2 border border-border rounded-[14px] text-sm hover:bg-muted/50"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
              {t('common.refresh')}
            </button>
          </div>
        </CardContent>
      </AdminFilterCard>

      <AsyncState
        isLoading={loading}
        isEmpty={!loading && !error && plots.length === 0}
        error={error ? new Error(error) : null}
        onRetry={handleRefresh}
        loadingText={t('admin.farmsPlots.plots.loading')}
        emptyIcon={<MapPin className="h-6 w-6 text-muted-foreground" />}
        emptyTitle={t('admin.farmsPlots.plots.emptyTitle')}
        emptyDescription={t('admin.farmsPlots.plots.emptyDescription')}
      >
        <AdminContentCard>
          <div className="overflow-x-auto">
          <table className="w-full min-w-[760px]">
            <thead className="bg-muted/50 border-b border-border">
              <tr>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('common.name')}</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('admin.farmsPlots.table.farm')}</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('admin.farmsPlots.table.areaHa')}</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">{t('admin.farmsPlots.table.soilType')}</th>
                <th className="px-4 py-3 text-right text-sm font-medium text-muted-foreground">{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {plots.map((plot) => (
                <tr key={plot.id} className="border-b border-border hover:bg-muted/30">
                  <td className="px-4 py-3 text-sm font-medium">{plot.plotName || '-'}</td>
                  <td className="px-4 py-3 text-sm">{plot.farmName || '-'}</td>
                  <td className="px-4 py-3 text-sm">{plot.area?.toFixed(2) || '-'}</td>
                  <td className="px-4 py-3 text-sm">{plot.soilType || '-'}</td>
                  <td className="px-4 py-3 text-right">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <button
                          type="button"
                          className="inline-flex h-8 w-8 items-center justify-center rounded-[14px] border border-border text-muted-foreground hover:bg-muted/50 hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                          aria-label={t('admin.farmsPlots.actionsFor', { name: plot.plotName || t('admin.farmsPlots.plotFallback') })}
                        >
                          <MoreVertical className="h-4 w-4" />
                        </button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" className="w-44">
                        <DropdownMenuItem onSelect={() => handleViewPlot(plot)}>
                          {t('admin.farmsPlots.actions.viewSeasons')}
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          </div>
        </AdminContentCard>
      </AsyncState>

      {totalPages > 1 && (
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <button
            onClick={() => setPage(Math.max(0, page - 1))}
            disabled={page === 0}
            className="px-3 py-1 border border-border rounded text-sm disabled:opacity-50"
          >
            {t('pagination.previousPage')}
          </button>
          <span className="text-sm text-muted-foreground">
            {t('pagination.page')} {page + 1} {t('pagination.of')} {totalPages}
          </span>
          <button
            onClick={() => setPage(page + 1)}
            disabled={page >= totalPages - 1}
            className="px-3 py-1 border border-border rounded text-sm disabled:opacity-50"
          >
            {t('pagination.nextPage')}
          </button>
        </div>
      )}
    </div>
  );

  return (
    <AdminPageContainer>
      <AdminHeaderCard
        title={t('admin.farmsPlots.title')}
        description={t('admin.farmsPlots.subtitle')}
      />

      {/* Tab Navigation */}
      <div className="overflow-x-auto pb-1">
        <div className={adminTabsListClass}>
        <button
          onClick={() => { setActiveTab('farms'); setPage(0); setSearchTerm(''); }}
          className={cn(
            "inline-flex h-8 items-center justify-center rounded-[18px] px-4 text-sm font-medium whitespace-nowrap transition-all",
            activeTab === 'farms'
              ? 'bg-card text-foreground shadow-sm'
              : 'text-muted-foreground hover:text-foreground',
          )}
        >
          <Building2 className="inline-block h-4 w-4 mr-2" />
          {t('admin.farmsPlots.tabs.farms')}
        </button>
        <button
          onClick={() => { setActiveTab('plots'); setPage(0); setSearchTerm(''); }}
          className={cn(
            "inline-flex h-8 items-center justify-center rounded-[18px] px-4 text-sm font-medium whitespace-nowrap transition-all",
            activeTab === 'plots'
              ? 'bg-card text-foreground shadow-sm'
              : 'text-muted-foreground hover:text-foreground',
          )}
        >
          <MapPin className="inline-block h-4 w-4 mr-2" />
          {t('admin.farmsPlots.tabs.plots')}
        </button>
        </div>
      </div>

      {/* Tab Content */}
      {activeTab === 'farms' && renderFarms()}
      {activeTab === 'plots' && renderPlots()}

      {/* Farm Detail Dialog */}
      <Dialog open={showFarmDetail} onOpenChange={(open) => !open && closeFarmDetail()}>
        <DialogContent className="sm:max-w-[500px] sm:max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <BackButton onClick={closeFarmDetail} className="w-fit" />
            <DialogTitle>{t('admin.farmsPlots.farmDetails.title')}</DialogTitle>
          </DialogHeader>
          {selectedFarm && (
            <div className="space-y-6 mt-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-muted-foreground">{t('common.name')}</label>
                  <p className="text-base font-medium">{selectedFarm.name}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-muted-foreground">{t('admin.farmsPlots.table.owner')}</label>
                  <p className="text-base font-medium">{selectedFarm.ownerUsername || '-'}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-muted-foreground">{t('admin.farmsPlots.table.area')}</label>
                  <p className="text-base font-medium">{selectedFarm.area?.toFixed(2) || '-'} ha</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-muted-foreground">{t('common.status')}</label>
                  <div className="mt-1">
                    <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${selectedFarm.active
                      ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400'
                      : 'bg-gray-100 text-gray-800'
                      }`}>
                      {selectedFarm.active ? t('common.active') : t('common.inactive')}
                    </span>
                  </div>
                </div>
              </div>

              <div>
                <h3 className="text-base font-medium mb-3">{t('admin.farmsPlots.farmDetails.plotsCount', { count: farmPlots.length })}</h3>
                {detailLoading ? (
                  <div className="flex items-center gap-2 text-muted-foreground text-sm">
                    <RefreshCw className="h-4 w-4 animate-spin" />
                    {t('admin.farmsPlots.plots.loading')}
                  </div>
                ) : farmPlots.length === 0 ? (
                  <p className="text-sm text-muted-foreground">{t('admin.farmsPlots.farmDetails.noPlots')}</p>
                ) : (
                  <div className="space-y-2">
                    {farmPlots.map(plot => (
                      <div key={plot.id} className="p-4 bg-muted/30 rounded-xl border border-border flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                        <div>
                          <p className="font-medium text-sm">{plot.plotName || '-'}</p>
                          <p className="text-xs text-muted-foreground">
                            {plot.area?.toFixed(2) || '-'} ha • {plot.soilType || t('admin.farmsPlots.noSoilType')}
                          </p>
                        </div>
                        <button
                          onClick={() => {
                            closeFarmDetail();
                            handleViewPlot(plot);
                          }}
                          className="text-xs text-primary font-medium hover:underline p-2 -mr-2 min-h-[44px] sm:min-h-0"
                        >
                          {t('admin.farmsPlots.actions.viewSeasons')}
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Plot Detail Dialog */}
      <Dialog open={showPlotDetail} onOpenChange={(open) => !open && closePlotDetail()}>
        <DialogContent className="sm:max-w-[600px] sm:max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <BackButton onClick={closePlotDetail} className="w-fit" />
            <DialogTitle>{t('admin.farmsPlots.plotDetails.title')}</DialogTitle>
          </DialogHeader>
          {selectedPlot && (
            <div className="space-y-6 mt-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-muted-foreground">{t('common.name')}</label>
                  <p className="text-base font-medium">{selectedPlot.plotName || '-'}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-muted-foreground">{t('admin.farmsPlots.table.farm')}</label>
                  <p className="text-base font-medium">{selectedPlot.farmName || '-'}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-muted-foreground">{t('admin.farmsPlots.table.area')}</label>
                  <p className="text-base font-medium">{selectedPlot.area?.toFixed(2) || '-'} ha</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-muted-foreground">{t('admin.farmsPlots.table.soilType')}</label>
                  <p className="text-base font-medium">{selectedPlot.soilType || '-'}</p>
                </div>
              </div>

              <div>
                <h3 className="text-base font-medium mb-3">{t('admin.farmsPlots.plotDetails.seasonsCount', { count: plotSeasons.length })}</h3>
                {detailLoading ? (
                  <div className="flex items-center gap-2 text-muted-foreground text-sm">
                    <RefreshCw className="h-4 w-4 animate-spin" />
                    {t('admin.farmsPlots.plotDetails.loadingSeasons')}
                  </div>
                ) : plotSeasons.length === 0 ? (
                  <p className="text-sm text-muted-foreground">{t('admin.farmsPlots.plotDetails.noSeasons')}</p>
                ) : (
                  <div className="space-y-2">
                    {plotSeasons.map(season => (
                      <div key={season.id} className="p-4 bg-muted/30 rounded-xl border border-border flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                        <div>
                          <p className="font-medium text-sm">{season.seasonName}</p>
                          <p className="text-xs text-muted-foreground">
                            {season.cropName} • {new Date(season.startDate).toLocaleDateString()}
                          </p>
                        </div>
                        <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${STATUS_COLORS[season.status] || 'bg-gray-100 text-gray-800'
                          }`}>
                          {t(`admin.farmsPlots.seasonStatus.${season.status}`, season.status)}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </AdminPageContainer>
  );
}
