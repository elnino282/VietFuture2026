import { useCallback, useEffect, useMemo, useState } from "react";
import { Award, CheckCircle2, Clock, Package } from "lucide-react";
import { Badge } from "@/shared/ui/badge";
import { toast } from "sonner";
import {
  useAllFarmerHarvests,
  useCreateHarvest,
  useDeleteHarvest,
  useHarvestSummary,
  type Harvest as ApiHarvest,
} from "@/entities/harvest";
import { useOptionalSeason, useWeightUnit } from "@/shared/contexts";
import { convertWeightToKg, formatWeight } from "@/shared/lib";
import type {
  HarvestBatch,
  HarvestFormData,
  HarvestGrade,
  HarvestStatus,
  ChartDataPoint,
  SummaryStats,
} from "../types";
import { GRADE_DISTRIBUTION_COLORS, GRADE_POINTS_MAP } from "../constants";

const INITIAL_FORM_DATA: HarvestFormData = {
  batchId: "",
  date: "",
  quantity: "",
  grade: "A",
  moisture: "",
  season: "",
  plot: "",
  crop: "",
  warehouseId: "",
  locationId: "",
  productId: "",
  productName: "",
  productVariant: "",
  lotCode: "",
  inventoryUnit: "kg",
  status: "stored",
  notes: "",
  purity: "",
  foreignMatter: "",
  brokenGrains: "",
};

const parseSeasonId = (value?: string | number | null): number | undefined => {
  const parsed = Number(value);
  if (Number.isFinite(parsed) && parsed > 0) return parsed;
  return undefined;
};

const parseHarvestGrade = (value?: string | null): HarvestGrade | undefined => {
  if (value === "Premium" || value === "A" || value === "B" || value === "C") {
    return value;
  }
  return undefined;
};

const escapeCsvCell = (value: unknown): string => {
  const normalized = value == null ? "" : String(value);
  return `"${normalized.replace(/"/g, "\"\"")}"`;
};

const escapeHtml = (value: unknown): string => {
  const normalized = value == null ? "" : String(value);
  return normalized
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
};

const transformApiToFeature = (h: ApiHarvest): HarvestBatch => ({
  id: String(h.id),
  batchId: String(h.id),
  seasonId: h.seasonId ?? undefined,
  seasonName: h.seasonName ?? undefined,
  date: h.harvestDate,
  createdAt: h.createdAt ?? undefined,
  quantity: h.quantity,
  unitPrice: h.unit ?? undefined,
  revenue: h.revenue ?? undefined,
  grade: parseHarvestGrade(h.grade),
  notes: h.note ?? undefined,
  season:
    h.seasonName ??
    (h.seasonId && Number.isFinite(h.seasonId) ? `#${h.seasonId}` : undefined),
});

export function useHarvestManagement() {
  const seasonContext = useOptionalSeason();
  const weightUnit = useWeightUnit();

  const [selectedSeason, setSelectedSeason] = useState<string>("all");
  const [isAddBatchOpen, setIsAddBatchOpen] = useState(false);
  const [selectedBatch, setSelectedBatch] = useState<HarvestBatch | null>(null);
  const [isDetailsDrawerOpen, setIsDetailsDrawerOpen] = useState(false);
  const [formData, setFormData] = useState<HarvestFormData>(INITIAL_FORM_DATA);
  const [selectedBatchIds, setSelectedBatchIds] = useState<string[]>([]);

  const effectiveSeasonId = useMemo((): number | undefined => {
    if (selectedSeason !== "all") {
      return parseSeasonId(selectedSeason);
    }
    return parseSeasonId(seasonContext?.selectedSeasonId ?? undefined);
  }, [selectedSeason, seasonContext?.selectedSeasonId]);

  const harvestParams = useMemo(
    () => ({
      page: 0,
      size: 100,
      ...(effectiveSeasonId ? { seasonId: effectiveSeasonId } : {}),
    }),
    [effectiveSeasonId]
  );

  const {
    data: apiData,
    isLoading: apiLoading,
    error: apiError,
    refetch,
  } = useAllFarmerHarvests(harvestParams);

  const { data: summaryData } = useHarvestSummary(effectiveSeasonId);

  const resetForm = useCallback(() => setFormData(INITIAL_FORM_DATA), []);

  const createMutation = useCreateHarvest({
    onSuccess: (_created, variables) => {
      toast.success("Harvest Added", {
        description: `${variables.data.lotCode} - ${formatWeight(
          variables.data.quantity,
          weightUnit
        )}`,
      });
      setIsAddBatchOpen(false);
      resetForm();
    },
    onError: (err) =>
      toast.error("Failed to add harvest", { description: err.message }),
  });

  const deleteMutation = useDeleteHarvest();

  const batches = useMemo(
    () => apiData?.items?.map(transformApiToFeature) ?? [],
    [apiData]
  );

  useEffect(() => {
    setSelectedBatchIds((previous) =>
      previous.filter((id) => batches.some((batch) => batch.id === id))
    );
  }, [batches]);

  const filteredBatches = batches;

  const totalHarvested = useMemo(
    () => filteredBatches.reduce((sum, batch) => sum + batch.quantity, 0),
    [filteredBatches]
  );

  const lotsCount = filteredBatches.length;

  const avgGrade = useMemo(() => {
    const gradedBatches = batches.filter((batch) => !!batch.grade);
    if (gradedBatches.length === 0) return "N/A";
    const avg =
      gradedBatches.reduce(
        (sum, batch) => sum + GRADE_POINTS_MAP[batch.grade as HarvestGrade],
        0
      ) / gradedBatches.length;
    return avg >= 3.5 ? "Premium" : avg >= 2.5 ? "A" : avg >= 1.5 ? "B" : "C";
  }, [batches]);

  const avgMoisture = useMemo(() => {
    const moistureSamples = filteredBatches
      .map((batch) => batch.moisture)
      .filter((value): value is number => typeof value === "number");
    if (moistureSamples.length === 0) return "—";
    const average =
      moistureSamples.reduce((sum, value) => sum + value, 0) /
      moistureSamples.length;
    return average.toFixed(1);
  }, [filteredBatches]);

  const yieldVsPlan = useMemo(() => {
    const value = summaryData?.yieldVsPlanPercent;
    if (typeof value !== "number" || Number.isNaN(value)) return "—";
    return value.toFixed(1);
  }, [summaryData?.yieldVsPlanPercent]);

  const dailyTrend: ChartDataPoint[] = useMemo(() => {
    const dateMap: Record<string, number> = {};
    filteredBatches.forEach((batch) => {
      if (!batch.date) return;
      const dateObj = new Date(batch.date);
      const formattedDate = dateObj.toLocaleDateString("en-US", {
        month: "short",
        day: "numeric",
      });
      dateMap[formattedDate] = (dateMap[formattedDate] || 0) + batch.quantity;
    });
    return Object.entries(dateMap)
      .map(([date, quantity]) => ({ date, quantity }))
      .sort((left, right) => {
        const leftDate = new Date(left.date ?? "").getTime();
        const rightDate = new Date(right.date ?? "").getTime();
        return leftDate - rightDate;
      });
  }, [filteredBatches]);

  const gradeDistribution: ChartDataPoint[] = useMemo(
    () =>
      [
        {
          name: "Premium",
          value: batches.filter((batch) => batch.grade === "Premium").length,
          color: GRADE_DISTRIBUTION_COLORS.Premium,
        },
        {
          name: "Grade A",
          value: batches.filter((batch) => batch.grade === "A").length,
          color: GRADE_DISTRIBUTION_COLORS["Grade A"],
        },
        {
          name: "Grade B",
          value: batches.filter((batch) => batch.grade === "B").length,
          color: GRADE_DISTRIBUTION_COLORS["Grade B"],
        },
        {
          name: "Grade C",
          value: batches.filter((batch) => batch.grade === "C").length,
          color: GRADE_DISTRIBUTION_COLORS["Grade C"],
        },
      ].filter((entry) => (entry.value ?? 0) > 0),
    [batches]
  );

  const summaryStats: SummaryStats = useMemo(
    () => ({
      totalStored: batches
        .filter((batch) => batch.status === "stored")
        .reduce((sum, batch) => sum + batch.quantity, 0),
      totalSold: batches
        .filter((batch) => batch.status === "sold")
        .reduce((sum, batch) => sum + batch.quantity, 0),
      totalProcessing: batches
        .filter((batch) => batch.status === "processing")
        .reduce((sum, batch) => sum + batch.quantity, 0),
      premiumGradePercentage:
        batches.length > 0
          ? (batches.filter((batch) => batch.grade === "Premium").length /
              batches.length) *
            100
          : 0,
    }),
    [batches]
  );

  const getStatusBadge = useCallback((status?: HarvestStatus | null) => {
    if (!status) return null;
    const badges: Record<HarvestStatus, JSX.Element> = {
      stored: (
        <Badge className="bg-secondary/10 text-secondary border-secondary/20">
          <Package className="w-3 h-3 mr-1" />
          Stored
        </Badge>
      ),
      sold: (
        <Badge className="bg-primary/10 text-primary border-primary/20">
          <CheckCircle2 className="w-3 h-3 mr-1" />
          Sold
        </Badge>
      ),
      processing: (
        <Badge className="bg-accent/10 text-foreground border-accent/20">
          <Clock className="w-3 h-3 mr-1" />
          Processing
        </Badge>
      ),
    };
    return badges[status] ?? null;
  }, []);

  const getGradeBadge = useCallback((grade?: HarvestGrade | null) => {
    if (!grade) {
      return <span className="text-xs text-muted-foreground">—</span>;
    }
    const classes: Record<HarvestGrade, string> = {
      Premium: "bg-primary/10 text-primary border-primary/20",
      A: "bg-secondary/10 text-secondary border-secondary/20",
      B: "bg-accent/10 text-foreground border-accent/20",
      C: "bg-muted/30 text-muted-foreground border-border",
    };
    return (
      <Badge className={classes[grade]}>
        <Award className="w-3 h-3 mr-1" />
        {grade}
      </Badge>
    );
  }, []);

  const resolveSubmitSeasonId = useCallback(() => {
    const fromForm = parseSeasonId(formData.season);
    if (fromForm) return fromForm;
    return effectiveSeasonId;
  }, [effectiveSeasonId, formData.season]);

  const handleAddBatch = useCallback(() => {
    if (
      !formData.date ||
      !formData.quantity ||
      !formData.warehouseId ||
      !formData.productName ||
      !formData.lotCode
    ) {
      toast.error("Missing Fields");
      return;
    }

    const quantityDisplay = Number(formData.quantity);
    if (!Number.isFinite(quantityDisplay) || quantityDisplay <= 0) {
      toast.error("Invalid quantity");
      return;
    }

    const seasonId = resolveSubmitSeasonId();
    if (!seasonId) {
      toast.error("Season required", {
        description: "Select a valid season before adding harvest.",
      });
      return;
    }

    const selectedSeasonMeta = seasonContext?.seasons.find(
      (season) => season.id === seasonId
    );
    const selectedSeasonStatus = selectedSeasonMeta?.status;
    if (
      selectedSeasonStatus &&
      ["COMPLETED", "CANCELLED", "ARCHIVED"].includes(selectedSeasonStatus)
    ) {
      toast.error("Season is locked", {
        description: "Harvest write actions are disabled for this season.",
      });
      return;
    }

    const quantityKg = convertWeightToKg(quantityDisplay, weightUnit);
    const warehouseId = Number(formData.warehouseId);
    const locationId = formData.locationId
      ? Number(formData.locationId)
      : undefined;
    const productId = formData.productId ? Number(formData.productId) : undefined;

    if (!Number.isFinite(warehouseId) || warehouseId <= 0) {
      toast.error("Invalid warehouse");
      return;
    }

    createMutation.mutate({
      seasonId,
      data: {
        harvestDate: formData.date,
        quantity: quantityKg,
        unit: 1,
        warehouseId,
        locationId:
          locationId && Number.isFinite(locationId) && locationId > 0
            ? locationId
            : undefined,
        productId:
          productId && Number.isFinite(productId) && productId > 0
            ? productId
            : undefined,
        productName: formData.productName.trim(),
        productVariant: formData.productVariant.trim() || undefined,
        lotCode: formData.lotCode.trim(),
        inventoryUnit: formData.inventoryUnit.trim() || undefined,
        grade: formData.grade,
        note: formData.notes,
      },
    });
  }, [
    createMutation,
    formData,
    resolveSubmitSeasonId,
    seasonContext?.seasons,
    weightUnit,
  ]);

  const handleDeleteBatch = useCallback(
    async (batch: HarvestBatch) => {
      const id = Number(batch.id);
      if (!Number.isFinite(id) || id <= 0) {
        toast.error("Invalid batch id");
        return;
      }
      try {
        await deleteMutation.mutateAsync({
          id,
          seasonId: parseSeasonId(batch.seasonId ?? undefined),
        });
        toast.success("Batch Deleted");
        setSelectedBatchIds((previous) =>
          previous.filter((selectedId) => selectedId !== batch.id)
        );
      } catch (error) {
        const message =
          error instanceof Error ? error.message : "Unable to delete batch.";
        toast.error("Failed to delete", { description: message });
      }
    },
    [deleteMutation]
  );

  const handleDeleteSelectedBatches = useCallback(
    async (selectedBatches: HarvestBatch[]) => {
      if (selectedBatches.length === 0) {
        toast.error("No batches selected");
        return;
      }

      const validTargets = selectedBatches
        .map((batch) => ({
          id: Number(batch.id),
          seasonId: parseSeasonId(batch.seasonId ?? undefined),
        }))
        .filter((target) => Number.isFinite(target.id) && target.id > 0);

      if (validTargets.length === 0) {
        toast.error("No valid batch IDs found");
        return;
      }

      const results = await Promise.allSettled(
        validTargets.map((target) =>
          deleteMutation.mutateAsync({
            id: target.id,
            seasonId: target.seasonId,
          })
        )
      );

      const successCount = results.filter(
        (result) => result.status === "fulfilled"
      ).length;
      const failedResults = results.filter(
        (result): result is PromiseRejectedResult => result.status === "rejected"
      );

      if (successCount > 0) {
        toast.success("Batches Deleted", {
          description: `${successCount} batch(es) deleted.`,
        });
      }

      if (failedResults.length > 0) {
        const firstError = failedResults[0].reason;
        const message =
          firstError instanceof Error
            ? firstError.message
            : "Some batches could not be deleted.";
        toast.error("Partial delete failure", {
          description: `${failedResults.length} failed. ${message}`,
        });
      }

      setSelectedBatchIds([]);
    },
    [deleteMutation]
  );

  const handleToggleBatchSelection = useCallback(
    (id: string, checked: boolean) => {
      setSelectedBatchIds((previous) => {
        if (checked) {
          return previous.includes(id) ? previous : [...previous, id];
        }
        return previous.filter((selectedId) => selectedId !== id);
      });
    },
    []
  );

  const handleToggleAllSelection = useCallback(
    (checked: boolean, visibleBatchIds?: string[]) => {
      if (!checked) {
        setSelectedBatchIds([]);
        return;
      }
      const targetIds =
        visibleBatchIds && visibleBatchIds.length > 0
          ? visibleBatchIds
          : filteredBatches.map((batch) => batch.id);
      setSelectedBatchIds(targetIds);
    },
    [filteredBatches]
  );

  const handleViewDetails = useCallback((batch: HarvestBatch) => {
    setSelectedBatch(batch);
    setIsDetailsDrawerOpen(true);
  }, []);

  const handleQuickAction = useCallback((action: string) => {
    const messages: Record<string, [string, string]> = {
      qr: ["Generating QR", "Ready shortly"],
      qc: ["Record QC", "Opening..."],
      sale: ["Link Sale", "Opening..."],
      handover: ["Printing", "Preparing..."],
      weight: ["Scale Reading", "Opening..."],
    };
    const message = messages[action];
    if (message) {
      toast.success(message[0], { description: message[1] });
    }
  }, []);

  const handleExport = useCallback(
    (rows: HarvestBatch[]) => {
      if (rows.length === 0) {
        toast.error("No data to export");
        return;
      }
      if (typeof window === "undefined") {
        toast.error("Export is not available in this environment");
        return;
      }

      const headers = [
        "Harvest ID",
        "Batch ID",
        "Season ID",
        "Season Name",
        "Harvest Date",
        "Quantity (kg)",
        "Unit Price",
        "Revenue",
        "Grade",
        "Notes",
        "Created At",
      ];

      const bodyRows = rows.map((batch) =>
        [
          batch.id,
          batch.batchId,
          batch.seasonId ?? "",
          batch.seasonName ?? "",
          batch.date,
          batch.quantity,
          batch.unitPrice ?? "",
          batch.revenue ?? "",
          batch.grade ?? "",
          batch.notes ?? "",
          batch.createdAt ?? "",
        ]
          .map(escapeCsvCell)
          .join(",")
      );

      const csv = [headers.map(escapeCsvCell).join(","), ...bodyRows].join("\n");
      const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      const date = new Date().toISOString().split("T")[0];
      const seasonToken = effectiveSeasonId
        ? `season-${effectiveSeasonId}`
        : "all-seasons";
      link.href = url;
      link.download = `harvest-${seasonToken}-${date}.csv`;
      link.click();
      window.URL.revokeObjectURL(url);

      toast.success("CSV Export Complete", {
        description: `${rows.length} row(s) exported.`,
      });
    },
    [effectiveSeasonId]
  );

  const handlePrint = useCallback(
    (rows: HarvestBatch[]) => {
      if (rows.length === 0) {
        toast.error("No data to print");
        return;
      }
      if (typeof window === "undefined") {
        toast.error("Print is not available in this environment");
        return;
      }

      const printWindow = window.open("", "_blank", "width=1200,height=800");
      if (!printWindow) {
        toast.error("Print window blocked", {
          description: "Please allow pop-ups to print harvest summary.",
        });
        return;
      }

      const generatedAt = new Date().toLocaleString();
      const title = effectiveSeasonId
        ? `Harvest Summary - Season #${effectiveSeasonId}`
        : "Harvest Summary - All Seasons";
      const rowsHtml = rows
        .map(
          (batch) => `
          <tr>
            <td>${escapeHtml(batch.id)}</td>
            <td>${escapeHtml(batch.batchId)}</td>
            <td>${escapeHtml(batch.seasonName ?? batch.seasonId ?? "—")}</td>
            <td>${escapeHtml(batch.date)}</td>
            <td style="text-align:right;">${escapeHtml(batch.quantity)}</td>
            <td>${escapeHtml(batch.grade ?? "—")}</td>
            <td style="text-align:right;">${escapeHtml(batch.revenue ?? "—")}</td>
            <td>${escapeHtml(batch.notes ?? "—")}</td>
          </tr>`
        )
        .join("");

      printWindow.document.write(`<!doctype html>
<html>
  <head>
    <meta charset="utf-8" />
    <title>${escapeHtml(title)}</title>
    <style>
      body { font-family: Arial, sans-serif; margin: 24px; color: #111; }
      h1 { margin: 0 0 8px; font-size: 20px; }
      p { margin: 0 0 16px; color: #555; font-size: 12px; }
      table { width: 100%; border-collapse: collapse; font-size: 12px; }
      th, td { border: 1px solid #d1d5db; padding: 8px; vertical-align: top; }
      th { background: #f3f4f6; text-align: left; }
    </style>
  </head>
  <body>
    <h1>${escapeHtml(title)}</h1>
    <p>Generated at ${escapeHtml(generatedAt)}</p>
    <table>
      <thead>
        <tr>
          <th>Harvest ID</th>
          <th>Batch ID</th>
          <th>Season</th>
          <th>Harvest Date</th>
          <th style="text-align:right;">Quantity (kg)</th>
          <th>Grade</th>
          <th style="text-align:right;">Revenue</th>
          <th>Notes</th>
        </tr>
      </thead>
      <tbody>
        ${rowsHtml}
      </tbody>
    </table>
  </body>
</html>`);
      printWindow.document.close();
      printWindow.focus();
      printWindow.print();
    },
    [effectiveSeasonId]
  );

  return {
    effectiveSeasonId,
    hasSeasonContext: !!seasonContext,
    selectedSeason,
    setSelectedSeason,
    isAddBatchOpen,
    setIsAddBatchOpen,
    selectedBatch,
    setSelectedBatch,
    isDetailsDrawerOpen,
    setIsDetailsDrawerOpen,
    batches,
    formData,
    setFormData,
    isLoading: apiLoading,
    error: apiError ?? null,
    refetch,
    filteredBatches,
    totalHarvested,
    lotsCount,
    avgGrade,
    avgMoisture,
    yieldVsPlan,
    dailyTrend,
    gradeDistribution,
    summaryStats,
    selectedBatchIds,
    setSelectedBatchIds,
    getStatusBadge,
    getGradeBadge,
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
    isCreating: createMutation.isPending,
    isDeleting: deleteMutation.isPending,
  };
}
