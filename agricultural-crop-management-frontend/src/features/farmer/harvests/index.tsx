import { useEffect, useMemo, useState } from "react";
import { ExternalLink, Search } from "lucide-react";
import { useHarvestManagement } from "./hooks/useHarvestManagement";
import { HarvestHeader } from "./components/HarvestHeader";
import { HarvestKPICards } from "./components/HarvestKPICards";
import { HarvestTable } from "./components/HarvestTable";
import { HarvestCharts } from "./components/HarvestCharts";
import { QuickActionsPanel } from "./components/QuickActionsPanel";
import { AddBatchDialog } from "./components/AddBatchDialog";
import { HarvestDetailsDialog } from "./components/HarvestDetailsDrawer";
import { toast } from "sonner";
import { Card, CardContent } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import { useMyWarehouses } from "@/entities/inventory";
import { useSeasons } from "@/entities/season";
import { useParams } from "react-router-dom";

export function HarvestManagement() {
  const { seasonId: workspaceSeasonIdParam } = useParams();
  const workspaceSeasonId = Number(workspaceSeasonIdParam);
  const isWorkspaceScoped = Number.isFinite(workspaceSeasonId) && workspaceSeasonId > 0;

  // Fetch real seasons from API
  const { data: seasonsData } = useSeasons();
  const { data: warehousesData } = useMyWarehouses("OUTPUT");
  const outputWarehouses = useMemo(() => warehousesData ?? [], [warehousesData]);
  const seasonOptions = useMemo(() => {
    const options = [{ value: "all", label: "All Seasons" }];
    if (seasonsData?.items) {
      seasonsData.items.forEach((season) => {
        options.push({
          value: String(season.id),
          label: season.seasonName,
        });
      });
    }
    return options;
  }, [seasonsData]);

  const {
    // State
    effectiveSeasonId,
    selectedSeason,
    setSelectedSeason,
    isAddBatchOpen,
    setIsAddBatchOpen,
    selectedBatch,
    isDetailsDrawerOpen,
    setIsDetailsDrawerOpen,
    selectedBatchIds,
    formData,
    setFormData,

    // Computed values
    filteredBatches,
    totalHarvested,
    lotsCount,
    avgGrade,
    avgMoisture,
    yieldVsPlan,
    dailyTrend,
    gradeDistribution,
    summaryStats,

    // Utilities
    getStatusBadge,
    getGradeBadge,

    // Handlers
    handleAddBatch,
    handleDeleteBatch,
    handleDeleteSelectedBatches,
    handleToggleBatchSelection,
    handleToggleAllSelection,
    resetForm,
    handleViewDetails,
    handleQuickAction,
    handleExport,
    handlePrint,
    isCreating,
  } = useHarvestManagement();

  useEffect(() => {
    if (!isWorkspaceScoped) return;
    const scopedSeasonId = String(workspaceSeasonId);
    if (selectedSeason === scopedSeasonId) return;
    setSelectedSeason(scopedSeasonId);
  }, [isWorkspaceScoped, workspaceSeasonId, selectedSeason, setSelectedSeason]);

  const scopedSeasonLabel = useMemo(() => {
    if (!isWorkspaceScoped) return "";
    return seasonOptions.find((option) => option.value === String(workspaceSeasonId))?.label
      ?? `Mùa vụ #${workspaceSeasonId}`;
  }, [isWorkspaceScoped, workspaceSeasonId, seasonOptions]);
  const selectedSeasonStatus = useMemo(() => {
    if (!seasonsData?.items) return null;
    if (isWorkspaceScoped) {
      return seasonsData.items.find((season) => season.id === workspaceSeasonId)?.status ?? null;
    }
    const parsedSeasonId = selectedSeason === "all"
      ? (effectiveSeasonId ?? 0)
      : Number(selectedSeason);
    if (!Number.isFinite(parsedSeasonId) || parsedSeasonId <= 0) return null;
    return seasonsData.items.find((season) => season.id === parsedSeasonId)?.status ?? null;
  }, [effectiveSeasonId, isWorkspaceScoped, seasonsData?.items, selectedSeason, workspaceSeasonId]);

  const isHarvestWriteLocked =
    selectedSeasonStatus !== null && selectedSeasonStatus !== "ACTIVE";
  const seasonWriteLockReason = isHarvestWriteLocked
    ? `Harvest write actions require an ACTIVE season. Current status: ${selectedSeasonStatus}.`
    : undefined;

  const [searchQuery, setSearchQuery] = useState("");
  const warehouseListHref = useMemo(() => {
    const scopedSeasonId = isWorkspaceScoped ? workspaceSeasonId : effectiveSeasonId;
    if (scopedSeasonId && Number.isFinite(scopedSeasonId) && scopedSeasonId > 0) {
      return `/farmer/product-warehouse?seasonId=${scopedSeasonId}`;
    }
    return "/farmer/product-warehouse";
  }, [effectiveSeasonId, isWorkspaceScoped, workspaceSeasonId]);
  const filteredBySearch = useMemo(() => {
    const normalizedQuery = searchQuery.trim().toLowerCase();
    if (!normalizedQuery) return filteredBatches;

    return filteredBatches.filter((batch) => {
      const haystack = [
        batch.batchId,
        batch.crop,
        batch.plot,
        batch.season,
        batch.grade,
        batch.status,
        batch.linkedSale ?? "",
        batch.notes ?? "",
      ]
        .join(" ")
        .toLowerCase();

      return haystack.includes(normalizedQuery);
    });
  }, [filteredBatches, searchQuery]);

  const handleDrawerAction = (action: string, batch: typeof selectedBatch) => {
    if (!batch) return;

    if (action === "qr") {
      toast.success("Generating QR Code", {
        description: `QR for ${batch.batchId}`,
      });
    } else if (action === "handover") {
      toast.success("Printing Handover Note", {
        description: `For batch ${batch.batchId}`,
      });
    }
  };

  return (
    <div className="min-h-screen acm-main-content pb-20">
      <div className="max-w-[1920px] mx-auto p-4 md:p-6">
        <HarvestHeader
          onAddBatch={() => {
            if (isHarvestWriteLocked) {
              toast.error(seasonWriteLockReason);
              return;
            }
            if (outputWarehouses.length === 0) {
              toast.error("No output warehouses found. Create one before recording harvest.", {
                description: "Dùng nút Danh sách kho (Mở tab mới) bên dưới để tạo kho.",
              });
              return;
            }
            resetForm();
            let nextSeasonValue = isWorkspaceScoped
              ? String(workspaceSeasonId)
              : (selectedSeason !== "all"
                  ? selectedSeason
                  : (effectiveSeasonId ? String(effectiveSeasonId) : ""));
            let nextSeasonId = Number(nextSeasonValue);
            let nextSeasonMeta = Number.isFinite(nextSeasonId) && nextSeasonId > 0
              ? seasonsData?.items?.find((season) => season.id === nextSeasonId)
              : undefined;
            if (!isWorkspaceScoped && selectedSeason === "all" && nextSeasonMeta?.status !== "ACTIVE") {
              const firstActiveSeason = seasonsData?.items?.find((season) => season.status === "ACTIVE");
              if (firstActiveSeason) {
                nextSeasonValue = String(firstActiveSeason.id);
                nextSeasonId = firstActiveSeason.id;
                nextSeasonMeta = firstActiveSeason;
                setSelectedSeason(nextSeasonValue);
              }
            }
            if (!nextSeasonMeta || nextSeasonMeta.status !== "ACTIVE") {
              toast.error("Harvest requires an ACTIVE season", {
                description: "Select or start an ACTIVE season before adding a harvest batch.",
              });
              return;
            }
            setFormData((prev) => ({
              ...prev,
              season: nextSeasonValue,
              plot: nextSeasonMeta?.plotId ? String(nextSeasonMeta.plotId) : "",
              plotName: nextSeasonMeta?.plotName ?? "",
              crop: nextSeasonMeta?.cropName ?? "",
              productName: nextSeasonMeta?.cropName ?? prev.productName,
            }));
            setIsAddBatchOpen(true);
          }}
          addDisabled={isHarvestWriteLocked}
          lockMessage={seasonWriteLockReason}
        />

        <Card className="mb-6 border border-border rounded-xl shadow-sm">
          <CardContent className="px-6 py-4">
            <div className="flex flex-wrap items-center justify-start gap-3 md:gap-4">
              <div className="relative w-full sm:w-[320px]">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                  placeholder="Search batches..."
                  value={searchQuery}
                  onChange={(event) => setSearchQuery(event.target.value)}
                  className="pl-10 rounded-xl border-border focus:border-primary"
                />
              </div>

              {isWorkspaceScoped ? (
                <div className="rounded-xl border border-border px-3 py-2 text-sm bg-card">
                  Đang xem: <span className="font-medium">{scopedSeasonLabel}</span>
                </div>
              ) : (
                <Select value={selectedSeason} onValueChange={setSelectedSeason}>
                  <SelectTrigger className="rounded-xl border-border w-full sm:w-[180px]">
                    <SelectValue placeholder="All Seasons" />
                  </SelectTrigger>
                  <SelectContent>
                    {seasonOptions.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
              <Button
                type="button"
                variant="outline"
                className="rounded-xl"
                onClick={() => window.open(warehouseListHref, "_blank", "noopener,noreferrer")}
              >
                Danh sách kho
                <ExternalLink className="w-4 h-4 ml-2" />
                <span className="text-xs">(Mở tab mới)</span>
              </Button>
            </div>
          </CardContent>
        </Card>

        <Card className="mb-6 border border-border rounded-xl shadow-sm">
          <CardContent className="px-6 py-4 space-y-3">
            <div className="flex items-center justify-between gap-3">
              <h3 className="text-sm font-semibold text-foreground">Output Warehouse Directory</h3>
              <span className="text-xs text-muted-foreground">
                {outputWarehouses.length} warehouse(s)
              </span>
            </div>
            {outputWarehouses.length === 0 ? (
              <div className="rounded-xl border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-900">
                No output warehouse available. Please create one before adding harvest batches.
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
                {outputWarehouses.map((warehouse) => (
                  <button
                    key={warehouse.id}
                    type="button"
                    className="text-left rounded-xl border border-border bg-card px-3 py-2 hover:border-primary/50 hover:bg-muted/30 transition-colors"
                    onClick={() =>
                      window.open(
                        `/farmer/product-warehouse?warehouseId=${warehouse.id}${effectiveSeasonId ? `&seasonId=${effectiveSeasonId}` : ""}`,
                        "_blank",
                        "noopener,noreferrer",
                      )
                    }
                  >
                    <p className="text-sm font-medium text-foreground">{warehouse.name}</p>
                    <p className="text-xs text-muted-foreground mt-1">
                      Farm: {warehouse.farmName || "-"}
                    </p>
                    <p className="text-xs text-muted-foreground">Type: {warehouse.type || "OUTPUT"}</p>
                    <p className="text-xs text-primary mt-2 inline-flex items-center gap-1">
                      Mở tab mới
                      <ExternalLink className="w-3 h-3" />
                    </p>
                  </button>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        <HarvestKPICards
          totalHarvested={totalHarvested}
          lotsCount={lotsCount}
          avgGrade={avgGrade}
          avgMoisture={avgMoisture}
          yieldVsPlan={yieldVsPlan}
        />

        <div className="grid grid-cols-1 lg:grid-cols-[1fr_300px] gap-6">
          <div className="space-y-6">
            <HarvestTable
              batches={filteredBySearch}
              totalBatches={filteredBatches.length}
              selectedBatchIds={selectedBatchIds}
              onViewDetails={handleViewDetails}
              onDeleteBatch={handleDeleteBatch}
              onDeleteSelected={handleDeleteSelectedBatches}
              onToggleBatchSelection={handleToggleBatchSelection}
              onToggleAllSelection={handleToggleAllSelection}
              onExport={handleExport}
              onPrint={handlePrint}
              getStatusBadge={getStatusBadge}
              getGradeBadge={getGradeBadge}
              disableMutations={isHarvestWriteLocked}
            />

            <HarvestCharts
              dailyTrend={dailyTrend}
              gradeDistribution={gradeDistribution}
            />
          </div>

          <QuickActionsPanel
            onQuickAction={handleQuickAction}
            summaryStats={summaryStats}
          />
        </div>
      </div>

      <AddBatchDialog
        open={isAddBatchOpen}
        onOpenChange={setIsAddBatchOpen}
        formData={formData}
        onFormChange={setFormData}
        onSubmit={handleAddBatch}
        seasonId={effectiveSeasonId}
        isSeasonLocked={isWorkspaceScoped}
        lockedSeasonLabel={scopedSeasonLabel}
        isWriteLocked={isHarvestWriteLocked}
        warehouseCount={outputWarehouses.length}
        writeLockReason={seasonWriteLockReason}
        isSubmitting={isCreating}
        onCancel={() => {
          setIsAddBatchOpen(false);
          resetForm();
        }}
      />

      <HarvestDetailsDialog
        batch={selectedBatch}
        open={isDetailsDrawerOpen}
        onOpenChange={setIsDetailsDrawerOpen}
        onAction={handleDrawerAction}
        getStatusBadge={getStatusBadge}
        getGradeBadge={getGradeBadge}
      />
    </div>
  );
}
