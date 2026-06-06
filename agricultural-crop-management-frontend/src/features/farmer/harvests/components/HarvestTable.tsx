import {
  AlertCircle,
  Download,
  Edit,
  Eye,
  MoreVertical,
  Printer,
  QrCode,
  Trash2,
} from "lucide-react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/shared/ui/table";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu";
import { Checkbox } from "@/shared/ui/checkbox";
import { usePreferences } from "@/shared/contexts";
import { formatWeight, getWeightUnitLabel } from "@/shared/lib";
import { useI18n } from "@/shared/lib/hooks/useI18n";
import type { HarvestBatch, HarvestGrade, HarvestStatus } from "../types";

interface HarvestTableProps {
  batches: HarvestBatch[];
  totalBatches: number;
  selectedBatchIds: string[];
  onViewDetails: (batch: HarvestBatch) => void;
  onEditBatch: (batch: HarvestBatch) => void;
  onDeleteBatch: (batch: HarvestBatch) => void;
  onDeleteSelected: (selectedBatches: HarvestBatch[]) => void;
  onToggleBatchSelection: (id: string, checked: boolean) => void;
  onToggleAllSelection: (checked: boolean, visibleBatchIds: string[]) => void;
  onExport: (rows: HarvestBatch[]) => void;
  onPrint: (rows: HarvestBatch[]) => void;
  getStatusBadge: (status?: HarvestStatus | null) => JSX.Element | null;
  getGradeBadge: (grade?: HarvestGrade | null) => JSX.Element;
  disableMutations?: boolean;
}

export function HarvestTable({
  batches,
  totalBatches,
  selectedBatchIds,
  onViewDetails,
  onEditBatch,
  onDeleteBatch,
  onDeleteSelected,
  onToggleBatchSelection,
  onToggleAllSelection,
  onExport,
  onPrint,
  getStatusBadge,
  getGradeBadge,
  disableMutations = false,
}: HarvestTableProps) {
  const { preferences } = usePreferences();
  const { t } = useI18n();
  const unitLabel = getWeightUnitLabel(preferences.weightUnit);
  const selectedVisibleIds = batches
    .map((batch) => batch.id)
    .filter((id) => selectedBatchIds.includes(id));
  const selectedVisibleCount = selectedVisibleIds.length;
  const allSelected = batches.length > 0 && selectedVisibleCount === batches.length;
  const partiallySelected =
    selectedVisibleCount > 0 && selectedVisibleCount < batches.length;
  const selectedBatches = batches.filter((batch) => selectedBatchIds.includes(batch.id));

  return (
    <Card className="border-border rounded-2xl shadow-sm">
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="text-foreground">{t("harvests.table.title")}</CardTitle>
            <CardDescription className="text-sm text-muted-foreground">
              {t("harvests.table.showingCount", {
                shown: batches.length,
                total: totalBatches,
              })}
            </CardDescription>
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              className="rounded-xl border-border"
              onClick={() => onDeleteSelected(selectedBatches)}
              disabled={disableMutations || selectedVisibleCount === 0}
            >
              <Trash2 className="w-4 h-4 mr-2" />
              {t("harvests.table.deleteSelected", { count: selectedVisibleCount })}
            </Button>
            <Button
              variant="outline"
              size="sm"
              className="rounded-xl border-border"
              onClick={() => onExport(batches)}
            >
              <Download className="w-4 h-4 mr-2" />
              {t("harvests.table.exportCsv")}
            </Button>
            <Button
              variant="outline"
              size="sm"
              className="rounded-xl border-border"
              onClick={() => onPrint(batches)}
            >
              <Printer className="w-4 h-4 mr-2" />
              {t("harvests.table.printSummary")}
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent className="px-6 py-4">
        <div className="overflow-x-auto rounded-xl border border-border">
          <Table>
            <TableHeader>
              <TableRow className="bg-muted hover:bg-muted">
                <TableHead className="w-12 text-center">
                  <Checkbox
                    checked={allSelected ? true : partiallySelected ? "indeterminate" : false}
                    onCheckedChange={(checked) =>
                      onToggleAllSelection(
                        checked === true,
                        batches.map((batch) => batch.id)
                      )
                    }
                    aria-label={t("harvests.table.selectAllBatches")}
                  />
                </TableHead>
                <TableHead className="text-foreground">{t("harvests.table.columns.date")}</TableHead>
                <TableHead className="text-foreground">{t("harvests.table.columns.batchId")}</TableHead>
                <TableHead className="text-foreground text-right">
                  {t("harvests.table.columns.quantity", { unit: unitLabel })}
                </TableHead>
                <TableHead className="text-foreground">{t("harvests.table.columns.grade")}</TableHead>
                <TableHead className="text-foreground">{t("harvests.table.columns.status")}</TableHead>
                <TableHead className="text-foreground">{t("harvests.table.columns.createdAt")}</TableHead>
                <TableHead className="text-foreground">{t("harvests.table.columns.actions")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {batches.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} className="text-center py-12 text-muted-foreground">
                    <AlertCircle className="w-12 h-12 mx-auto mb-3 text-muted-foreground" />
                    <p>{t("harvests.table.emptyTitle")}</p>
                    <p className="text-sm mt-1">{t("harvests.table.emptyDescription")}</p>
                  </TableCell>
                </TableRow>
              ) : (
                batches.map((batch) => (
                  <TableRow
                    key={batch.id}
                    className="hover:bg-muted/50 cursor-pointer"
                    onClick={() => onViewDetails(batch)}
                  >
                    <TableCell
                      className="text-center"
                      onClick={(event) => event.stopPropagation()}
                    >
                      <Checkbox
                        checked={selectedBatchIds.includes(batch.id)}
                        onCheckedChange={(checked) =>
                          onToggleBatchSelection(batch.id, checked === true)
                        }
                        aria-label={t("harvests.table.selectBatch", { batchId: batch.batchId })}
                      />
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {new Date(batch.date).toLocaleDateString(preferences.locale, {
                        month: "short",
                        day: "numeric",
                        year: "numeric",
                      })}
                    </TableCell>
                    <TableCell className="numeric text-foreground">
                      {batch.batchId}
                    </TableCell>
                    <TableCell className="text-right numeric text-foreground">
                      {formatWeight(batch.quantity, preferences.weightUnit, preferences.locale)}
                    </TableCell>
                    <TableCell>{getGradeBadge(batch.grade)}</TableCell>
                    <TableCell>
                      {getStatusBadge(batch.status) ?? (
                        <span className="text-xs text-muted-foreground">{t("common.notAvailable", "—")}</span>
                      )}
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {batch.createdAt
                        ? new Date(batch.createdAt).toLocaleString(preferences.locale)
                        : "-"}
                    </TableCell>
                    <TableCell onClick={(event) => event.stopPropagation()}>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-8 w-8 p-0 hover:bg-muted"
                            aria-label={t("harvests.table.actions.openMenu", {
                              batchId: batch.batchId,
                              defaultValue: `Open actions for batch ${batch.batchId}`,
                            })}
                          >
                            <MoreVertical className="w-4 h-4 text-muted-foreground" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end" className="w-48">
                          <DropdownMenuItem onClick={() => onViewDetails(batch)}>
                            <Eye className="w-4 h-4 mr-2" />
                            {t("harvests.table.actions.viewDetails")}
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={() => !disableMutations && onEditBatch(batch)}
                            disabled={disableMutations}
                          >
                            <Edit className="w-4 h-4 mr-2" />
                            {t("harvests.table.actions.editBatch")}
                          </DropdownMenuItem>
                          <DropdownMenuItem>
                            <QrCode className="w-4 h-4 mr-2" />
                            {t("harvests.table.actions.generateQr")}
                          </DropdownMenuItem>
                          <DropdownMenuItem>
                            <Printer className="w-4 h-4 mr-2" />
                            {t("harvests.table.actions.printHandover")}
                          </DropdownMenuItem>
                          <DropdownMenuSeparator />
                          <DropdownMenuItem
                            onClick={() => !disableMutations && onDeleteBatch(batch)}
                            className="text-destructive"
                            disabled={disableMutations}
                          >
                            <Trash2 className="w-4 h-4 mr-2" />
                            {t("common.delete")}
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>
      </CardContent>
    </Card>
  );
}
