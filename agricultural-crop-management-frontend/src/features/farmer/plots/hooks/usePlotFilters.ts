import { useState, useMemo, useCallback } from "react";
import { useCrops } from "@/entities/crop";
import { usePlotStatuses } from "@/entities/plot-status";
import { useTranslation } from "react-i18next";
import type { Plot } from "../types";
import { mapStatusToPlotStatus } from "../utils";
import { getPlotStatusLabel } from "../components/PlotStatusChip";
import { normalizeSoilTypeCode, SOIL_TYPE_OPTIONS } from "@/features/farmer/shared/plotOptions";

// ═══════════════════════════════════════════════════════════════
// TYPES
// ═══════════════════════════════════════════════════════════════

export type FilterOption = {
    value: string;
    label: string;
};

export interface UsePlotFiltersReturn {
    // Filter state
    searchQuery: string;
    setSearchQuery: (query: string) => void;
    filterCrop: string;
    setFilterCrop: (crop: string) => void;
    filterStatus: string;
    setFilterStatus: (status: string) => void;
    filterSoilType: string;
    setFilterSoilType: (type: string) => void;

    // Filter options from API
    cropOptions: FilterOption[];
    soilTypeOptions: FilterOption[];
    statusOptions: FilterOption[];
    isLoadingFilterOptions: boolean;

    // Handlers
    handleClearFilters: () => void;

    // Computed filtered data
    getFilteredPlots: (plots: Plot[]) => Plot[];
}

// ═══════════════════════════════════════════════════════════════
// HOOK IMPLEMENTATION
// ═══════════════════════════════════════════════════════════════

/**
 * Custom hook for plot filtering logic
 * Separates filter concerns from the main usePlotManagement hook
 */
export const usePlotFilters = (): UsePlotFiltersReturn => {
    const { t } = useTranslation();
    // ─────────────────────────────────────────────────────────────
    // API HOOKS - FILTER OPTIONS
    // ─────────────────────────────────────────────────────────────

    const { data: cropsData, isLoading: isLoadingCrops } = useCrops();
    const { data: plotStatusesData, isLoading: isLoadingStatuses } = usePlotStatuses();

    // ─────────────────────────────────────────────────────────────
    // FILTER STATE
    // ─────────────────────────────────────────────────────────────

    const [searchQuery, setSearchQuery] = useState("");
    const [filterCrop, setFilterCrop] = useState("all");
    const [filterStatus, setFilterStatus] = useState("all");
    const [filterSoilType, setFilterSoilType] = useState("all");

    // ─────────────────────────────────────────────────────────────
    // FILTER OPTIONS TRANSFORMATION
    // ─────────────────────────────────────────────────────────────

    const cropOptions = useMemo<FilterOption[]>(() => {
        const options: FilterOption[] = [{ value: "all", label: t("plots.filters.allPlots") }];
        if (cropsData && cropsData.length > 0) {
            cropsData.forEach((crop) => {
                options.push({
                    value: crop.cropName,
                    label: crop.cropName,
                });
            });
        }
        return options;
    }, [cropsData, t]);

    const soilTypeOptions = useMemo<FilterOption[]>(() => {
        const options: FilterOption[] = [{ value: "all", label: t("plots.filters.allSoilTypes") }];
        SOIL_TYPE_OPTIONS.forEach((soilType) => {
            options.push({
                value: soilType.value,
                label: t(soilType.labelKey),
            });
        });
        return options;
    }, [t]);

    const statusOptions = useMemo<FilterOption[]>(() => {
        const options: FilterOption[] = [{ value: "all", label: t("plots.filters.allStatuses") }];
        const seen = new Set<string>(["all"]);
        if (plotStatusesData && plotStatusesData.length > 0) {
            plotStatusesData.forEach((status) => {
                const featureStatus = mapStatusToPlotStatus(status.statusName);
                if (seen.has(featureStatus)) return;
                seen.add(featureStatus);
                options.push({
                    value: featureStatus,
                    label: getPlotStatusLabel(featureStatus, t),
                });
            });
        }
        return options;
    }, [plotStatusesData, t]);

    const isLoadingFilterOptions = isLoadingCrops || isLoadingStatuses;

    // ─────────────────────────────────────────────────────────────
    // HANDLERS
    // ─────────────────────────────────────────────────────────────

    const handleClearFilters = useCallback(() => {
        setSearchQuery("");
        setFilterCrop("all");
        setFilterStatus("all");
        setFilterSoilType("all");
    }, []);

    // ─────────────────────────────────────────────────────────────
    // FILTER LOGIC
    // ─────────────────────────────────────────────────────────────

    const getFilteredPlots = useCallback((plots: Plot[]): Plot[] => {
        return plots.filter((plot) => {
            const matchesSearch =
                searchQuery === "" ||
                plot.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
                plot.id.toLowerCase().includes(searchQuery.toLowerCase());

            // Crop filter - plots don't have crop field in response, so always match
            const matchesCrop = filterCrop === "all" || true;

            // Status filter - compare with plot.status
            const matchesStatus =
                filterStatus === "all" ||
                (plot.status && plot.status.toLowerCase() === filterStatus.toLowerCase());

            // Soil type filter - compare with plot.soilType
            const matchesSoilType =
                filterSoilType === "all" ||
                normalizeSoilTypeCode(plot.soilType) === normalizeSoilTypeCode(filterSoilType) ||
                (plot.soilType && plot.soilType.toLowerCase() === filterSoilType.toLowerCase());

            return matchesSearch && matchesCrop && matchesStatus && matchesSoilType;
        });
    }, [searchQuery, filterCrop, filterStatus, filterSoilType]);

    return {
        // State
        searchQuery,
        setSearchQuery,
        filterCrop,
        setFilterCrop,
        filterStatus,
        setFilterStatus,
        filterSoilType,
        setFilterSoilType,

        // Options
        cropOptions,
        soilTypeOptions,
        statusOptions,
        isLoadingFilterOptions,

        // Handlers
        handleClearFilters,
        getFilteredPlots,
    };
};



