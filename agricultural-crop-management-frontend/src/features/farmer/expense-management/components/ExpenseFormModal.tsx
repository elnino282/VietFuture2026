import {
    Upload,
    Save,
    Edit,
    Plus,
    DollarSign,
    Loader2,
    ListTodo,
} from "lucide-react";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
    DialogFooter,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { BackButton } from "@/shared/ui/back-button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Textarea } from "@/shared/ui/textarea";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/shared/ui/select";
import type { Expense, ExpenseFormData, ExpenseStatus, TaskOption } from "../types";
import { usePreferences } from "@/shared/contexts";
import { formatMoney } from "@/shared/lib";
import { useTranslation } from "react-i18next";
import { useEffect, useRef } from "react";

interface ExpenseFormModalProps {
    isOpen: boolean;
    setIsOpen: (open: boolean) => void;
    selectedExpense: Expense | null;
    formData: ExpenseFormData;
    setFormData: (data: ExpenseFormData) => void;
    handleAddExpense: () => void;
    resetForm: () => void;
    showValidationErrors?: boolean;
    seasonOptions: { value: string; label: string; plotId?: number }[];
    taskOptions?: TaskOption[];
    supplierOptions?: { value: string; label: string; id: number }[];
    isLoadingTasks?: boolean;
    onTaskChange?: (taskId: string) => void;
    isSeasonLocked?: boolean;
    lockedSeasonLabel?: string;
    isSubmitting?: boolean;
}

export function ExpenseFormModal({
    isOpen,
    setIsOpen,
    selectedExpense,
    formData,
    setFormData,
    handleAddExpense,
    resetForm,
    showValidationErrors = false,
    seasonOptions,
    taskOptions = [],
    supplierOptions = [],
    isLoadingTasks = false,
    onTaskChange,
    isSeasonLocked = false,
    lockedSeasonLabel,
    isSubmitting = false,
}: ExpenseFormModalProps) {
    const { t } = useTranslation();
    const { preferences } = usePreferences();
    const amountValue = Number(formData.amount);
    const amountInvalid = !Number.isFinite(amountValue) || amountValue <= 0;
    const dateError = showValidationErrors && !formData.date ? t("expenses.form.dateRequired") : undefined;
    const categoryError = showValidationErrors && !formData.category ? t("expenses.form.categoryRequired") : undefined;
    const seasonError = showValidationErrors && !formData.linkedSeasonId ? t("expenses.form.seasonRequired") : undefined;
    const amountError = showValidationErrors && (formData.amount === "" || amountInvalid)
        ? t("expenses.form.amountRequired")
        : undefined;
    const statusError = showValidationErrors && !formData.status ? t("expenses.form.statusRequired") : undefined;
    const isFormDisabled = isSubmitting;
    const initialFormRef = useRef<string | null>(null);
    const normalizeFormData = (data: ExpenseFormData) => JSON.stringify({
        ...data,
        attachmentFile: data.attachmentFile?.name ?? null,
    });

    useEffect(() => {
        if (!isOpen) {
            initialFormRef.current = null;
            return;
        }

        if (!initialFormRef.current) {
            initialFormRef.current = normalizeFormData(formData);
        }
    }, [formData, isOpen]);

    const isDirty =
        isOpen &&
        !!initialFormRef.current &&
        initialFormRef.current !== normalizeFormData(formData);
    const confirmMessage = t("common.unsavedChangesConfirm", "You have unsaved changes. Leave this page?");

    const handleClose = () => {
        if (isSubmitting) return;
        if (isDirty && !window.confirm(confirmMessage)) return;
        setIsOpen(false);
        resetForm();
    };

    const handleTaskSelection = (value: string) => {
        if (onTaskChange) {
            onTaskChange(value);
        } else {
            const taskId = parseInt(value, 10);
            setFormData({
                ...formData,
                linkedTask: value,
                linkedTaskId: isNaN(taskId) ? undefined : taskId,
            });
        }
    };

    const handleSeasonSelection = (value: string) => {
        const season = seasonOptions.find((option) => option.value === value);
        const seasonId = parseInt(value, 10);
        setFormData({
            ...formData,
            linkedSeason: season?.label ?? "",
            linkedSeasonId: isNaN(seasonId) ? undefined : seasonId,
            linkedPlotId: season?.plotId,
        });
    };

    const handleVendorSelection = (value: string) => {
        if (value === "none") {
            setFormData({
                ...formData,
                vendor: "",
                vendorId: undefined,
            });
            return;
        }
        const vendorId = parseInt(value, 10);
        const vendor = supplierOptions.find((option) => option.value === value);
        setFormData({
            ...formData,
            vendor: vendor?.label ?? "",
            vendorId: isNaN(vendorId) ? undefined : vendorId,
        });
    };

    return (
        <Dialog
            open={isOpen}
            onOpenChange={(open) => {
                if (isSubmitting && !open) return;
                if (!open) {
                    handleClose();
                    return;
                }
                setIsOpen(true);
            }}
        >
            <DialogContent className="sm:max-w-[720px]" closeDisabled={isSubmitting}>
                <DialogHeader>
                    <BackButton onClick={handleClose} className="w-fit" />
                    <DialogTitle className="flex items-center gap-2 text-foreground">
                        {selectedExpense ? (
                            <>
                                <Edit className="w-5 h-5 text-secondary" />
                                {t("expenses.dialog.editTitle")}
                            </>
                        ) : (
                            <>
                                <Plus className="w-5 h-5 text-primary" />
                                {t("expenses.dialog.createTitle")}
                            </>
                        )}
                    </DialogTitle>
                    <DialogDescription>
                        {t("expenses.dialog.description")}
                    </DialogDescription>
                </DialogHeader>

                <div className="space-y-6 py-2">
                    {/* Date & Category */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <Label htmlFor="date" className="text-foreground">
                                {t("expenses.form.date")} <span className="text-destructive">*</span>
                            </Label>
                            <Input
                                id="date"
                                type="date"
                                value={formData.date}
                                onChange={(e) =>
                                    setFormData({ ...formData, date: e.target.value })
                                }
                                className={`rounded-xl border-border focus:border-primary ${dateError ? "border-destructive" : ""}`}
                                aria-invalid={!!dateError}
                                aria-describedby={dateError ? "expense-date-error" : undefined}
                                disabled={isFormDisabled}
                            />
                            {dateError && (
                                <p id="expense-date-error" className="text-xs text-destructive">{dateError}</p>
                            )}
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="category" className="text-foreground">
                                {t("expenses.form.category")} <span className="text-destructive">*</span>
                            </Label>
                            <Select
                                value={formData.category}
                                onValueChange={(value) =>
                                    setFormData({ ...formData, category: value })
                                }
                                disabled={isFormDisabled}
                            >
                                <SelectTrigger
                                    className={`rounded-xl border-border ${categoryError ? "border-destructive" : ""}`}
                                    aria-invalid={!!categoryError}
                                    aria-describedby={categoryError ? "expense-category-error" : undefined}
                                >
                                    <SelectValue placeholder={t("expenses.form.selectCategory")} />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="Fertilizer">Fertilizer</SelectItem>
                                    <SelectItem value="Seeds">Seeds</SelectItem>
                                    <SelectItem value="Labor">Labor</SelectItem>
                                    <SelectItem value="Equipment">Equipment</SelectItem>
                                    <SelectItem value="Pesticide">Pesticide</SelectItem>
                                    <SelectItem value="Transportation">Transportation</SelectItem>
                                    <SelectItem value="Utilities">Utilities</SelectItem>
                                    <SelectItem value="Maintenance">Maintenance</SelectItem>
                                    <SelectItem value="Other">Other</SelectItem>
                                </SelectContent>
                            </Select>
                            {categoryError && (
                                <p id="expense-category-error" className="text-xs text-destructive">{categoryError}</p>
                            )}
                        </div>
                    </div>

                    {/* Description */}
                    <div className="space-y-2">
                        <Label htmlFor="description" className="text-foreground">
                            {t("expenses.form.description")}
                        </Label>
                        <Input
                            id="description"
                            placeholder={t("expenses.form.descriptionPlaceholder")}
                            value={formData.description}
                            onChange={(e) =>
                                setFormData({ ...formData, description: e.target.value })
                            }
                            className="rounded-xl border-border focus:border-primary"
                            disabled={isFormDisabled}
                        />
                    </div>

                    {/* Vendor */}
                    <div className="space-y-2">
                        <Label htmlFor="vendor" className="text-foreground">
                            {t("expenses.form.vendor")}
                        </Label>
                        <Select
                            value={formData.vendorId ? String(formData.vendorId) : "none"}
                            onValueChange={handleVendorSelection}
                            disabled={isFormDisabled}
                        >
                            <SelectTrigger className="rounded-xl border-border">
                                <SelectValue placeholder={t("expenses.form.selectVendor")} />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="none">{t("expenses.form.noVendor")}</SelectItem>
                                {supplierOptions.map((option) => (
                                    <SelectItem key={option.value} value={option.value}>
                                        {option.label}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    {/* Linked Task & Season */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {/* LINKED TASK */}
                        <div className="space-y-2">
                            <Label htmlFor="linkedTask" className="text-foreground">
                                <div className="flex items-center gap-2">
                                    <ListTodo className="w-4 h-4" />
                                    {t("expenses.form.linkedTask")}
                                </div>
                            </Label>
                            <Select
                                value={formData.linkedTask || "none"}
                                onValueChange={(value) => {
                                    if (value === "none") {
                                        setFormData({
                                            ...formData,
                                            linkedTask: "",
                                            linkedTaskId: undefined,
                                        });
                                    } else {
                                        handleTaskSelection(value);
                                    }
                                }}
                                disabled={isFormDisabled || isLoadingTasks || taskOptions.length === 0}
                            >
                                <SelectTrigger className="rounded-xl border-border">
                                    <SelectValue placeholder={
                                        isLoadingTasks
                                            ? t("expenses.form.loadingTasks")
                                            : taskOptions.length === 0
                                                ? t("expenses.form.noTasks")
                                                : t("expenses.form.selectTask")
                                    } />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="none">
                                        <span className="text-muted-foreground">{t("expenses.form.noLinkedTask")}</span>
                                    </SelectItem>
                                    {taskOptions.map((task) => (
                                        <SelectItem key={task.value} value={task.value}>
                                            {task.label}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                            {taskOptions.length === 0 && !isLoadingTasks && (
                                <p className="text-xs text-muted-foreground">
                                    {t("expenses.form.noTasksFound")}
                                </p>
                            )}
                        </div>

                        {/* LINKED SEASON */}
                        <div className="space-y-2">
                            <Label htmlFor="linkedSeason" className="text-foreground">
                                {t("expenses.form.linkedSeason")} <span className="text-destructive">*</span>
                            </Label>
                            {isSeasonLocked ? (
                                <div className={`rounded-xl border border-border px-3 py-2 text-sm bg-muted/30 ${seasonError ? "border-destructive" : ""}`}>
                                    {lockedSeasonLabel ?? formData.linkedSeason ?? t("expenses.form.currentSeason")}
                                </div>
                            ) : (
                                <Select
                                    value={formData.linkedSeasonId ? String(formData.linkedSeasonId) : ""}
                                    onValueChange={handleSeasonSelection}
                                    disabled={isFormDisabled}
                                >
                                    <SelectTrigger
                                        className={`rounded-xl border-border ${seasonError ? "border-destructive" : ""}`}
                                        aria-invalid={!!seasonError}
                                        aria-describedby={seasonError ? "expense-season-error" : undefined}
                                    >
                                        <SelectValue placeholder={t("expenses.form.selectSeason")} />
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
                            {seasonError && (
                                <p id="expense-season-error" className="text-xs text-destructive">{seasonError}</p>
                            )}
                        </div>
                    </div>

                    {/* Amount & Status */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div className="space-y-2">
                            <Label htmlFor="amount" className="text-foreground">
                                {t("expenses.form.amount", { currency: preferences.currency })} <span className="text-destructive">*</span>
                            </Label>
                            <div className="relative">
                                <DollarSign className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                                <Input
                                    id="amount"
                                    type="number"
                                    step="0.01"
                                    placeholder="0.00"
                                    value={formData.amount}
                                    onChange={(e) =>
                                        setFormData({ ...formData, amount: e.target.value })
                                    }
                                    className={`pl-10 rounded-xl border-border focus:border-primary ${amountError ? "border-destructive" : ""}`}
                                    aria-invalid={!!amountError}
                                    aria-describedby={amountError ? "expense-amount-error" : undefined}
                                    disabled={isFormDisabled}
                                />
                            </div>
                            {amountError && (
                                <p id="expense-amount-error" className="text-xs text-destructive">{amountError}</p>
                            )}
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="status" className="text-foreground">
                                {t("expenses.form.paymentStatus")} <span className="text-destructive">*</span>
                            </Label>
                            <Select
                                value={formData.status}
                                onValueChange={(value: ExpenseStatus) =>
                                    setFormData({ ...formData, status: value })
                                }
                                disabled={isFormDisabled}
                            >
                                <SelectTrigger
                                    className={`rounded-xl border-border ${statusError ? "border-destructive" : ""}`}
                                    aria-invalid={!!statusError}
                                    aria-describedby={statusError ? "expense-status-error" : undefined}
                                >
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="PAID">{t("expenses.overview.paid")}</SelectItem>
                                    <SelectItem value="PENDING">{t("expenses.status.pending")}</SelectItem>
                                    <SelectItem value="UNPAID">{t("expenses.overview.unpaid")}</SelectItem>
                                </SelectContent>
                            </Select>
                            {statusError && (
                                <p id="expense-status-error" className="text-xs text-destructive">{statusError}</p>
                            )}
                        </div>
                    </div>

                    {/* Upload Attachment */}
                    <div className="space-y-2">
                        <Label className="text-foreground">
                            {t("expenses.form.attachment")}
                        </Label>
                        <div className="border-2 border-dashed border-border rounded-xl p-6 text-center hover:border-primary hover:bg-primary/5 transition-colors">
                            <Upload className="w-10 h-10 text-muted-foreground mx-auto mb-3" />
                            <p className="text-sm text-foreground mb-1">
                                {t("expenses.form.uploadHint")}
                            </p>
                            <p className="text-xs text-muted-foreground">{t("expenses.form.uploadLimits")}</p>
                            <Input
                                type="file"
                                accept=".pdf,.jpg,.jpeg,.png"
                                className="mt-4"
                                onChange={(event) => {
                                    const file = event.target.files?.[0] ?? null;
                                    setFormData({
                                        ...formData,
                                        attachmentFile: file,
                                    });
                                }}
                                disabled={isFormDisabled}
                            />
                            {formData.attachmentFile && (
                                <p className="text-xs text-muted-foreground mt-2">
                                    {t("expenses.form.selectedFile")} {formData.attachmentFile.name}
                                </p>
                            )}
                            {!formData.attachmentFile && formData.attachmentUrl && (
                                <p className="text-xs text-muted-foreground mt-2">
                                    {t("expenses.form.existingFile")} {formData.attachmentName ?? t("expenses.form.receiptAttached")}
                                </p>
                            )}
                        </div>
                    </div>

                    {/* Notes */}
                    <div className="space-y-2">
                        <Label htmlFor="notes" className="text-foreground">
                            {t("expenses.form.notes")}
                        </Label>
                        <Textarea
                            id="notes"
                            placeholder={t("expenses.form.notesPlaceholder")}
                            value={formData.notes}
                            onChange={(e) =>
                                setFormData({ ...formData, notes: e.target.value })
                            }
                            className="rounded-xl border-border focus:border-primary min-h-[100px]"
                            disabled={isFormDisabled}
                        />
                    </div>

                    {/* Amount Preview */}
                    {formData.amount && (
                        <div className="p-4 rounded-xl bg-primary/10 border-2 border-primary/20">
                            <div className="flex items-center justify-between">
                                <div className="flex items-center gap-2">
                                    <DollarSign className="w-5 h-5 text-primary" />
                                    <span className="text-foreground">{t("expenses.form.totalAmount")}</span>
                                </div>
                                <span className="text-2xl numeric text-primary">
                                    {formatMoney(Number(formData.amount), preferences.currency, preferences.locale)}
                                </span>
                            </div>
                        </div>
                    )}
                </div>

                <DialogFooter>
                    <Button
                        variant="outline"
                        onClick={handleClose}
                        disabled={isSubmitting}
                        disabledHint={t("expenses.dialog.savingHint")}
                    >
                        {t("common.cancel")}
                    </Button>
                    <Button
                        onClick={handleAddExpense}
                        disabled={isSubmitting}
                        disabledHint={t("expenses.dialog.savingHint")}
                    >
                        {isSubmitting ? (
                            <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                        ) : (
                            <Save className="w-4 h-4 mr-2" />
                        )}
                        {isSubmitting
                            ? t("common.saving")
                            : selectedExpense
                                ? t("expenses.dialog.updateButton")
                                : t("expenses.dialog.saveButton")}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
