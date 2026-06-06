import { useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import { PageContainer, QueryError, Skeleton } from "@/shared/ui";
import { usePlotManagement } from "./hooks/usePlotManagement";
import { PlotToolbar } from "./components/PlotToolbar";
import { PlotListView } from "./components/PlotListView";
import { PlotMapView } from "./components/PlotMapView";
import { PlotDetailDialog } from "./components/PlotDetailDrawer";
import { AddPlotDialog } from "./components/AddPlotDialog";
import { MergePlotsWizard } from "./components/MergePlotsWizard";
import { SplitPlotDialog } from "./components/SplitPlotDialog";
import { DeletePlotDialog } from "./components/DeletePlotDialog";

/**
 * PlotManagement Component
 *
 * Main container for plot management feature.
 * Orchestrates all sub-components and manages the overall layout.
 *
 * Refactored following Clean Code principles:
 * - Single Responsibility: Each component handles one aspect
 * - Separation of Concerns: Logic in hook, UI in components
 * - Colocation: Related code grouped in feature folder
 */
export function PlotManagement() {
  const [searchParams, setSearchParams] = useSearchParams();
  const {
    viewMode,
    setViewMode,
    searchQuery,
    setSearchQuery,
    filterCrop,
    setFilterCrop,
    filterStatus,
    setFilterStatus,
    filterSoilType,
    setFilterSoilType,
    cropOptions,
    soilTypeOptions,
    statusOptions,
    isLoadingFilterOptions,
    plots,
    filteredPlots,
    isLoading,
    error,
    refetch,
    selectedPlot,
    isDrawerOpen,
    setIsDrawerOpen,
    isAddPlotOpen,
    setIsAddPlotOpen,
    isMergeWizardOpen,
    setIsMergeWizardOpen,
    isSplitDialogOpen,
    setIsSplitDialogOpen,
    isDeleteDialogOpen,
    setIsDeleteDialogOpen,
    mergeStep,
    setMergeStep,
    selectedPlots,
    setSelectedPlots,
    plotToSplit,
    setPlotToSplit,
    setPlotToDelete,
    handleClearFilters,
    handleViewPlotDetails,
    handleAddPlot,
    handleDeletePlot,
    handleGenerateQR,
    handleMarkDormant,
    handleReactivatePlot,
    handleOpenSplitPlot,
    handleSplitPlot,
    handleMergePlots,
    handleToggleSelection,
    handleToggleAllSelection,
    handleBulkDelete,
    handleBulkStatusChange,
    handleClearSelection,
    selectedCount,
    isCreating,
    isMerging,
    isSplitting,
  } = usePlotManagement();

  const plotIdParam = searchParams.get("plotId");

  const handleCloseDrawer = () => {
    // If plot detail was opened via deep-link (?plotId=...), clear the param so the drawer can stay closed.
    if (plotIdParam) {
      const next = new URLSearchParams(searchParams);
      next.delete("plotId");
      setSearchParams(next, { replace: true });
    }
    setIsDrawerOpen(false);
  };

  useEffect(() => {
    if (!plotIdParam) return;
    if (selectedPlot?.id === plotIdParam && isDrawerOpen) return;
    const match = plots.find((plot) => plot.id === plotIdParam);
    if (match) {
      handleViewPlotDetails(match);
    }
  }, [
    plotIdParam,
    plots,
    selectedPlot?.id,
    isDrawerOpen,
    handleViewPlotDetails,
  ]);

  // Loading skeleton
  const LoadingSkeleton = () => (
    <div className="space-y-4">
      {[1, 2, 3].map((i) => (
        <div key={i} className="bg-card rounded-xl p-6 border border-border">
          <div className="flex items-center gap-4">
            <Skeleton className="h-12 w-12 rounded-lg" />
            <div className="flex-1 space-y-2">
              <Skeleton className="h-4 w-1/4" />
              <Skeleton className="h-3 w-1/2" />
            </div>
            <Skeleton className="h-8 w-20" />
          </div>
        </div>
      ))}
    </div>
  );

  return (
    <>
      <PageContainer variant="dashboard" className="space-y-6">
        {/* Unified Toolbar */}
        <PlotToolbar
          viewMode={viewMode}
          setViewMode={setViewMode}
          onAddPlot={() => setIsAddPlotOpen(true)}
          onMergePlots={() => setIsMergeWizardOpen(true)}
          searchQuery={searchQuery}
          setSearchQuery={setSearchQuery}
          filterCrop={filterCrop}
          setFilterCrop={setFilterCrop}
          filterStatus={filterStatus}
          setFilterStatus={setFilterStatus}
          filterSoilType={filterSoilType}
          setFilterSoilType={setFilterSoilType}
          cropOptions={cropOptions}
          soilTypeOptions={soilTypeOptions}
          statusOptions={statusOptions}
          isLoadingFilterOptions={isLoadingFilterOptions}
          filteredCount={filteredPlots.length}
          totalCount={plots.length}
          selectedCount={selectedCount}
          onClearFilters={handleClearFilters}
          showMergeButton={filteredPlots.length > 1}
        />

        {/* Main Content */}
        {error ? (
          <QueryError error={error} onRetry={refetch} className="my-6" />
        ) : isLoading ? (
          <LoadingSkeleton />
        ) : viewMode === "list" ? (
          <PlotListView
            plots={filteredPlots}
            selectedPlots={selectedPlots}
            onToggleSelection={handleToggleSelection}
            onToggleAllSelection={handleToggleAllSelection}
            onViewDetails={handleViewPlotDetails}
            onDelete={(id) => {
              setPlotToDelete(id);
              setIsDeleteDialogOpen(true);
            }}
            onBulkDelete={handleBulkDelete}
            onBulkStatusChange={handleBulkStatusChange}
            onClearSelection={handleClearSelection}
            onClearFilters={handleClearFilters}
          />
        ) : (
          <PlotMapView
            plots={filteredPlots}
            onViewDetails={handleViewPlotDetails}
            onGenerateQR={handleGenerateQR}
          />
        )}
      </PageContainer>

      {/* Plot Detail Dialog */}
      <PlotDetailDialog
        plot={selectedPlot}
        isOpen={isDrawerOpen}
        onClose={handleCloseDrawer}
        onEdit={() => setIsAddPlotOpen(true)}
        onMerge={() => setIsMergeWizardOpen(true)}
        onSplit={handleOpenSplitPlot}
        onMarkDormant={handleMarkDormant}
        onReactivate={handleReactivatePlot}
        onGenerateQR={handleGenerateQR}
        onDelete={(id) => {
          setPlotToDelete(id);
          setIsDeleteDialogOpen(true);
        }}
      />

      {/* Add Plot Dialog */}
      <AddPlotDialog
        isOpen={isAddPlotOpen}
        onClose={() => setIsAddPlotOpen(false)}
        onSubmit={handleAddPlot}
        isSubmitting={isCreating}
      />

      {/* Merge Plots Wizard */}
      <MergePlotsWizard
        isOpen={isMergeWizardOpen}
        onClose={() => setIsMergeWizardOpen(false)}
        plots={plots}
        selectedPlots={selectedPlots}
        setSelectedPlots={setSelectedPlots}
        mergeStep={mergeStep}
        setMergeStep={setMergeStep}
        onConfirm={handleMergePlots}
        isMerging={isMerging}
      />

      {/* Split Plot Dialog */}
      <SplitPlotDialog
        isOpen={isSplitDialogOpen}
        plot={plotToSplit}
        onClose={() => {
          setIsSplitDialogOpen(false);
          setPlotToSplit(null);
        }}
        onConfirm={handleSplitPlot}
        isSplitting={isSplitting}
      />

      {/* Delete Plot Dialog */}
      <DeletePlotDialog
        isOpen={isDeleteDialogOpen}
        onClose={() => setIsDeleteDialogOpen(false)}
        onConfirm={handleDeletePlot}
      />
    </>
  );
}





