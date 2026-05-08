import { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Building2, MapPin, Search, RefreshCw, ChevronRight } from 'lucide-react';
import { AsyncState, PageContainer } from '@/shared/ui';
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
    ? (farmsQuery.isError ? 'Failed to load farms' : null)
    : (plotsQuery.isError ? 'Failed to load plots' : null);
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
    COMPLETED: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400',
    CANCELLED: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
  };

  const renderFarms = () => (
    <div className="space-y-4">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex flex-col sm:flex-row sm:items-center gap-2 w-full lg:w-auto">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <input
              type="text"
              placeholder="Search farms..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
              className="pl-10 pr-4 py-2 border border-border rounded-lg bg-background text-sm w-full sm:w-64"
            />
          </div>
          <button
            onClick={() => handleSearch()}
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-3 py-2 border border-border rounded-lg text-sm hover:bg-muted/50"
          >
            <Search className="h-4 w-4" />
            Search
          </button>
        </div>
        <button
          onClick={handleRefresh}
          className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-3 py-2 border border-border rounded-lg text-sm hover:bg-muted/50"
        >
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      <AsyncState
        isLoading={loading}
        isEmpty={!loading && !error && farms.length === 0}
        error={error ? new Error(error) : null}
        onRetry={handleRefresh}
        loadingText="Loading farms..."
        emptyIcon={<Building2 className="h-6 w-6 text-muted-foreground" />}
        emptyTitle="No farms found"
        emptyDescription="There are no farms matching your current search criteria."
      >
        <div className="bg-card border border-border rounded-lg overflow-hidden overflow-x-auto">
          <table className="w-full min-w-[860px]">
            <thead className="bg-muted/50 border-b border-border">
              <tr>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Name</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Owner</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Area (ha)</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Status</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Location</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Actions</th>
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
                      {farm.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-muted-foreground">
                    {[farm.wardName, farm.provinceName].filter(Boolean).join(', ') || '-'}
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => handleViewFarm(farm)}
                      className="text-sm text-primary hover:underline inline-flex items-center gap-1"
                    >
                      View <ChevronRight className="h-4 w-4" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AsyncState>

      {totalPages > 1 && (
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <button
            onClick={() => setPage(Math.max(0, page - 1))}
            disabled={page === 0}
            className="px-3 py-1 border border-border rounded text-sm disabled:opacity-50"
          >
            Previous
          </button>
          <span className="text-sm text-muted-foreground">
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => setPage(page + 1)}
            disabled={page >= totalPages - 1}
            className="px-3 py-1 border border-border rounded text-sm disabled:opacity-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );

  const renderPlots = () => (
    <div className="space-y-4">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex flex-col sm:flex-row sm:items-center gap-2 w-full lg:w-auto">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <input
              type="text"
              placeholder="Search plots..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
              className="pl-10 pr-4 py-2 border border-border rounded-lg bg-background text-sm w-full sm:w-64"
            />
          </div>
          <select
            value={farmFilter || ''}
            onChange={(e) => { setFarmFilter(e.target.value ? Number(e.target.value) : null); setPage(0); }}
            className="w-full sm:w-auto px-3 py-2 border border-border rounded-lg bg-background text-sm"
          >
            <option value="">All Farms</option>
            {(filterFarms ?? []).map(f => (
              <option key={f.id} value={f.id}>{f.name}</option>
            ))}
          </select>
        </div>
        <button
          onClick={handleRefresh}
          className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-3 py-2 border border-border rounded-lg text-sm hover:bg-muted/50"
        >
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      <AsyncState
        isLoading={loading}
        isEmpty={!loading && !error && plots.length === 0}
        error={error ? new Error(error) : null}
        onRetry={handleRefresh}
        loadingText="Loading plots..."
        emptyIcon={<MapPin className="h-6 w-6 text-muted-foreground" />}
        emptyTitle="No plots found"
        emptyDescription="There are no plots matching your current search criteria."
      >
        <div className="bg-card border border-border rounded-lg overflow-hidden overflow-x-auto">
          <table className="w-full min-w-[760px]">
            <thead className="bg-muted/50 border-b border-border">
              <tr>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Name</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Farm</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Area (ha)</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Soil Type</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-muted-foreground">Actions</th>
              </tr>
            </thead>
            <tbody>
              {plots.map((plot) => (
                <tr key={plot.id} className="border-b border-border hover:bg-muted/30">
                  <td className="px-4 py-3 text-sm font-medium">{plot.plotName || '-'}</td>
                  <td className="px-4 py-3 text-sm">{plot.farmName || '-'}</td>
                  <td className="px-4 py-3 text-sm">{plot.area?.toFixed(2) || '-'}</td>
                  <td className="px-4 py-3 text-sm">{plot.soilType || '-'}</td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => handleViewPlot(plot)}
                      className="text-sm text-primary hover:underline inline-flex items-center gap-1"
                    >
                      View Seasons <ChevronRight className="h-4 w-4" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AsyncState>

      {totalPages > 1 && (
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <button
            onClick={() => setPage(Math.max(0, page - 1))}
            disabled={page === 0}
            className="px-3 py-1 border border-border rounded text-sm disabled:opacity-50"
          >
            Previous
          </button>
          <span className="text-sm text-muted-foreground">
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => setPage(page + 1)}
            disabled={page >= totalPages - 1}
            className="px-3 py-1 border border-border rounded text-sm disabled:opacity-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );

  return (
    <PageContainer>
      <div className="mb-6">
        <h1 className="text-2xl font-bold mb-1">Farms & Plots</h1>
        <p className="text-muted-foreground">System-wide overview of all farms and plots</p>
      </div>

      {/* Tab Navigation */}
      <div className="flex overflow-x-auto border-b border-border mb-6">
        <button
          onClick={() => { setActiveTab('farms'); setPage(0); setSearchTerm(''); }}
          className={`px-4 py-2 text-sm font-medium whitespace-nowrap border-b-2 -mb-px ${activeTab === 'farms'
            ? 'border-primary text-primary'
            : 'border-transparent text-muted-foreground hover:text-foreground'
            }`}
        >
          <Building2 className="inline-block h-4 w-4 mr-2" />
          Farms
        </button>
        <button
          onClick={() => { setActiveTab('plots'); setPage(0); setSearchTerm(''); }}
          className={`px-4 py-2 text-sm font-medium whitespace-nowrap border-b-2 -mb-px ${activeTab === 'plots'
            ? 'border-primary text-primary'
            : 'border-transparent text-muted-foreground hover:text-foreground'
            }`}
        >
          <MapPin className="inline-block h-4 w-4 mr-2" />
          Plots
        </button>
      </div>

      {/* Tab Content */}
      {activeTab === 'farms' && renderFarms()}
      {activeTab === 'plots' && renderPlots()}

      {/* Farm Detail Drawer */}
      {showFarmDetail && selectedFarm && (
        <div
          className="fixed inset-0 z-50 bg-background/80 backdrop-blur-sm cursor-pointer"
          onClick={closeFarmDetail}
        >
          <div
            className="fixed right-0 top-0 h-full w-full max-w-md bg-card border-l border-border shadow-lg overflow-auto cursor-default"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-lg font-semibold">Farm Details</h2>
                <button
                  onClick={closeFarmDetail}
                  className="p-2 hover:bg-muted rounded"
                >
                  ✕
                </button>
              </div>

              <div className="space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="text-sm font-medium text-muted-foreground">Name</label>
                    <p className="text-sm">{selectedFarm.name}</p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-muted-foreground">Owner</label>
                    <p className="text-sm">{selectedFarm.ownerUsername || '-'}</p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-muted-foreground">Area</label>
                    <p className="text-sm">{selectedFarm.area?.toFixed(2) || '-'} ha</p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-muted-foreground">Status</label>
                    <p className="text-sm">
                      <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs ${selectedFarm.active
                        ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400'
                        : 'bg-gray-100 text-gray-800'
                        }`}>
                        {selectedFarm.active ? 'Active' : 'Inactive'}
                      </span>
                    </p>
                  </div>
                </div>

                <div className="pt-4 border-t border-border">
                  <h3 className="text-sm font-medium mb-3">Plots ({farmPlots.length})</h3>
                  {detailLoading ? (
                    <div className="flex items-center gap-2 text-muted-foreground text-sm">
                      <RefreshCw className="h-4 w-4 animate-spin" />
                      Loading plots...
                    </div>
                  ) : farmPlots.length === 0 ? (
                    <p className="text-sm text-muted-foreground">No plots found for this farm</p>
                  ) : (
                    <div className="space-y-2">
                      {farmPlots.map(plot => (
                        <div key={plot.id} className="p-3 bg-muted/30 rounded-lg border border-border">
                          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                            <div>
                              <p className="font-medium text-sm">{plot.plotName || '-'}</p>
                              <p className="text-xs text-muted-foreground">
                                {plot.area?.toFixed(2) || '-'} ha • {plot.soilType || 'No soil type'}
                              </p>
                            </div>
                            <button
                              onClick={() => {
                                closeFarmDetail();
                                handleViewPlot(plot);
                              }}
                              className="text-xs text-primary hover:underline"
                            >
                              View Seasons
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Plot Detail Drawer */}
      {showPlotDetail && selectedPlot && (
        <div
          className="fixed inset-0 z-50 bg-background/80 backdrop-blur-sm cursor-pointer"
          onClick={closePlotDetail}
        >
          <div
            className="fixed right-0 top-0 h-full w-full max-w-md bg-card border-l border-border shadow-lg overflow-auto cursor-default"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-lg font-semibold">Plot Details</h2>
                <button
                  onClick={closePlotDetail}
                  className="p-2 hover:bg-muted rounded"
                >
                  ✕
                </button>
              </div>

              <div className="space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="text-sm font-medium text-muted-foreground">Name</label>
                    <p className="text-sm">{selectedPlot.plotName || '-'}</p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-muted-foreground">Farm</label>
                    <p className="text-sm">{selectedPlot.farmName || '-'}</p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-muted-foreground">Area</label>
                    <p className="text-sm">{selectedPlot.area?.toFixed(2) || '-'} ha</p>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-muted-foreground">Soil Type</label>
                    <p className="text-sm">{selectedPlot.soilType || '-'}</p>
                  </div>
                </div>

                <div className="pt-4 border-t border-border">
                  <h3 className="text-sm font-medium mb-3">Seasons ({plotSeasons.length})</h3>
                  {detailLoading ? (
                    <div className="flex items-center gap-2 text-muted-foreground text-sm">
                      <RefreshCw className="h-4 w-4 animate-spin" />
                      Loading seasons...
                    </div>
                  ) : plotSeasons.length === 0 ? (
                    <p className="text-sm text-muted-foreground">No seasons found for this plot</p>
                  ) : (
                    <div className="space-y-2">
                      {plotSeasons.map(season => (
                        <div key={season.id} className="p-3 bg-muted/30 rounded-lg border border-border">
                          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                            <div>
                              <p className="font-medium text-sm">{season.seasonName}</p>
                              <p className="text-xs text-muted-foreground">
                                {season.cropName} • {new Date(season.startDate).toLocaleDateString()}
                              </p>
                            </div>
                            <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs ${STATUS_COLORS[season.status] || 'bg-gray-100 text-gray-800'
                              }`}>
                              {season.status}
                            </span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </PageContainer>
  );
}
