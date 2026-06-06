import { X, Trash2, Download } from "lucide-react";
import { Button } from "@/shared/ui/button";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/shared/ui/select";
import { useTranslation } from "react-i18next";
import type { PlotStatus } from "../types";

interface PlotBulkActionBarProps {
    selectedCount: number;
    onClearSelection: () => void;
    onBulkDelete: () => void;
    onBulkStatusChange: (status: PlotStatus) => void;
    onBulkExport?: () => void;
}

/**
 * PlotBulkActionBar Component
 * 
 * Floating action bar that appears when one or more plots are selected.
 * Provides bulk operations: delete, change status, and export.
 */
export function PlotBulkActionBar({
    selectedCount,
    onClearSelection,
    onBulkDelete,
    onBulkStatusChange,
    onBulkExport,
}: PlotBulkActionBarProps) {
    const { t } = useTranslation();

    if (selectedCount === 0) return null;

    return (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 animate-in slide-in-from-bottom-5">
            <div className="bg-card rounded-lg shadow-lg border border-border px-4 py-3 flex items-center gap-4">
                {/* Selected Count */}
                <div className="flex items-center gap-2 px-3 py-1 bg-muted rounded-md">
                    <span className="text-sm font-medium text-foreground">
                        {t("plots.selectedCount", { count: selectedCount })}
                    </span>
                </div>

                {/* Divider */}
                <div className="h-6 w-px bg-border" />

                {/* Change Status */}
                <Select onValueChange={(value) => onBulkStatusChange(value as PlotStatus)}>
                    <SelectTrigger className="w-[160px] h-9">
                        <SelectValue placeholder={t("plots.bulk.changeStatus")} />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="active">{t("plots.status.active")}</SelectItem>
                        <SelectItem value="dormant">{t("plots.status.dormant")}</SelectItem>
                        <SelectItem value="planned">{t("plots.status.planned")}</SelectItem>
                    </SelectContent>
                </Select>

                {/* Export (Optional) */}
                {onBulkExport && (
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={onBulkExport}
                        className="h-9"
                    >
                        <Download className="w-4 h-4 mr-2" />
                        {t("common.export")}
                    </Button>
                )}

                {/* Delete */}
                <Button
                    variant="destructive"
                    size="sm"
                    onClick={onBulkDelete}
                    className="h-9"
                >
                    <Trash2 className="w-4 h-4 mr-2" />
                    {t("common.delete")}
                </Button>

                {/* Divider */}
                <div className="h-6 w-px bg-border" />

                {/* Clear Selection */}
                <Button
                    variant="ghost"
                    size="sm"
                    onClick={onClearSelection}
                    className="h-9 w-9 p-0"
                >
                    <X className="w-4 h-4" />
                    <span className="sr-only">{t("plots.actions.clearSelection")}</span>
                </Button>
            </div>
        </div>
    );
}



