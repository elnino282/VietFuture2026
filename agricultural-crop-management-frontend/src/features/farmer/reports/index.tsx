import { Card, CardContent } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Wheat, DollarSign, CheckCircle2, AlertTriangle } from "lucide-react";
import { useReports } from "./hooks/useReports";
import { Sidebar } from "./components/Sidebar";
import { HeaderBar } from "./components/HeaderBar";
import { KPICards } from "./components/KPICards";
import { YieldTab } from "./components/YieldTab";
import { CostTab } from "./components/CostTab";
import { PerformanceTab } from "./components/PerformanceTab";
import { PesticideTab } from "./components/PesticideTab";
import { FilterDrawer } from "./components/FilterDrawer";
import { ExportModal } from "./components/ExportModal";
import type { ReportSection } from "./types";

export type SeasonReportMode = "interim" | "final";

interface ReportsProps {
    workspaceSeasonId?: number;
    workspaceSeasonName?: string;
    reportMode?: SeasonReportMode;
    harvestProgressPercent?: number;
}

export function Reports({
    workspaceSeasonId,
    workspaceSeasonName,
    reportMode,
    harvestProgressPercent,
}: ReportsProps) {
    const {
        activeSection, selectedSeason, yieldViewMode, isFilterDrawerOpen,
        isExportModalOpen, isExporting, exportFormat, includeCharts,
        includeNotes, filters, setActiveSection, setSelectedSeason,
        setYieldViewMode, setIsFilterDrawerOpen, setIsExportModalOpen,
        setExportFormat, setIncludeCharts, setIncludeNotes, setFilters,
        handleExport, handleApplyFilters, handleClearFilters,
        getYieldChartData, getPesticideStatusBadge, isLoading, hasError, kpiData,
        costOptimizationSummary, costOptimizationSummaryLoading,
        costOptimizationSummaryError, refetchCostOptimizationSummary,
        costOptimizationAiSuggestion, costOptimizationAiLoading,
        costOptimizationAiError, handleAnalyzeCostOptimizationWithAi,
    } = useReports({
        seasonId: workspaceSeasonId,
        initialSeasonValue: workspaceSeasonId ? String(workspaceSeasonId) : undefined,
    });

    const seasonOptions = workspaceSeasonId
        ? [{
            value: String(workspaceSeasonId),
            label: workspaceSeasonName ?? `Season #${workspaceSeasonId}`,
        }]
        : undefined;

    const tabConfig = [
        { value: "yield", icon: Wheat, label: "Yield" },
        { value: "cost", icon: DollarSign, label: "Cost" },
        { value: "performance", icon: CheckCircle2, label: "Tasks" },
        { value: "pesticide", icon: AlertTriangle, label: "Pesticide" },
    ];

    const resolvedMode: SeasonReportMode = reportMode ?? "final";
    const reportTitle = resolvedMode === "final"
        ? "Báo cáo tổng kết mùa vụ"
        : "Báo cáo tạm tính";
    const reportSubtitle = resolvedMode === "final"
        ? "Bộ số liệu đã được chốt theo mùa vụ hoàn tất."
        : "Dữ liệu được cập nhật theo tiến độ thu hoạch, các chỉ số có thể thay đổi.";

    return (
        <div className="min-h-screen acm-main-content pb-20">
            <div className="max-w-[1920px] mx-auto">
                <div className="grid grid-cols-1 lg:grid-cols-[240px_1fr] gap-0">
                    <Sidebar activeSection={activeSection} onSectionChange={setActiveSection} />

                    <main className="p-6">
                        <HeaderBar
                            selectedSeason={selectedSeason}
                            onSeasonChange={setSelectedSeason}
                            onFilterClick={() => setIsFilterDrawerOpen(true)}
                            onExportClick={() => setIsExportModalOpen(true)}
                            seasonOptions={seasonOptions}
                            disableSeasonSelect={Boolean(workspaceSeasonId)}
                            title={reportTitle}
                            subtitle={reportSubtitle}
                            progressPercent={harvestProgressPercent}
                        />
                        {hasError && (
                            <Card className="mb-4 border-destructive/20 bg-destructive/5">
                                <CardContent className="py-3 text-sm text-destructive">
                                    Failed to load report data. Please try again.
                                </CardContent>
                            </Card>
                        )}
                        <KPICards
                            totalCost={kpiData.totalCost}
                            netProfit={kpiData.netProfit}
                            totalYieldKg={kpiData.totalYieldKg}
                            onTimeTasksPercent={kpiData.onTimeTasksPercent}
                        />

                        <Card className="border-border rounded-2xl shadow-sm">
                            <CardContent className="px-6 py-4">
                                <Tabs value={activeSection} onValueChange={(v: string) => setActiveSection(v as ReportSection)}>
                                    <TabsList className="w-full grid grid-cols-2 md:grid-cols-4 mb-6 bg-muted rounded-xl p-1">
                                        {tabConfig.map(({ value, icon: Icon, label }) => (
                                            <TabsTrigger
                                                key={value}
                                                value={value}
                                                className="rounded-lg data-[state=active]:bg-card data-[state=active]:shadow-sm data-[state=active]:text-primary"
                                            >
                                                <Icon className="w-4 h-4 mr-2" />
                                                <span className="hidden sm:inline">{label}</span>
                                            </TabsTrigger>
                                        ))}
                                    </TabsList>

                                    <TabsContent value="yield" className="mt-0">
                                        {isLoading ? (
                                            <p className="text-sm text-muted-foreground py-8">Loading report data...</p>
                                        ) : (
                                            <YieldTab yieldViewMode={yieldViewMode} onViewModeChange={setYieldViewMode} chartData={getYieldChartData()} />
                                        )}
                                    </TabsContent>
                                    <TabsContent value="cost" className="mt-0">
                                        <CostTab
                                            summary={costOptimizationSummary}
                                            summaryLoading={costOptimizationSummaryLoading}
                                            summaryError={costOptimizationSummaryError}
                                            onRetrySummary={refetchCostOptimizationSummary}
                                            aiSuggestion={costOptimizationAiSuggestion}
                                            aiLoading={costOptimizationAiLoading}
                                            aiError={costOptimizationAiError}
                                            onAnalyzeWithAi={handleAnalyzeCostOptimizationWithAi}
                                        />
                                    </TabsContent>
                                    <TabsContent value="performance" className="mt-0">
                                        <PerformanceTab />
                                    </TabsContent>
                                    <TabsContent value="pesticide" className="mt-0">
                                        <PesticideTab getPesticideStatusBadge={getPesticideStatusBadge} />
                                    </TabsContent>
                                </Tabs>
                            </CardContent>
                        </Card>
                    </main>
                </div>
            </div>

            <FilterDrawer
                isOpen={isFilterDrawerOpen}
                onOpenChange={setIsFilterDrawerOpen}
                filters={filters}
                onFiltersChange={setFilters}
                onApplyFilters={handleApplyFilters}
                onClearFilters={handleClearFilters}
            />
            <ExportModal
                isOpen={isExportModalOpen}
                onOpenChange={setIsExportModalOpen}
                isExporting={isExporting}
                exportFormat={exportFormat}
                onExportFormatChange={setExportFormat}
                includeCharts={includeCharts}
                onIncludeChartsChange={setIncludeCharts}
                includeNotes={includeNotes}
                onIncludeNotesChange={setIncludeNotes}
                onExport={handleExport}
            />
        </div>
    );
}
