import { isAxiosError } from "axios";
import { useCallback, useMemo, useState } from "react";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";
import { useOptionalSeason } from "@/shared/contexts";
import {
    expenseApi,
    expenseKeys,
    useAllFarmerExpenses,
    useCreateExpense,
    useDeleteExpense,
    useUpdateExpense,
    type Expense as ApiExpense,
} from "@/entities/expense";
import { useTasksBySeason, type Task as ApiTask } from "@/entities/task";
import { useAllSuppliers } from "@/entities/supplies";
import type { Expense, ExpenseFormData, TaskOption } from "../types";

interface UseExpenseManagementOptions {
    scopedSeasonId?: number | null;
}

const INITIAL_FORM_DATA: ExpenseFormData = {
    date: "",
    category: "",
    description: "",
    linkedTask: "",
    linkedTaskId: undefined,
    linkedSeason: "",
    linkedSeasonId: undefined,
    linkedPlotId: undefined,
    amount: "",
    status: "PENDING",
    notes: "",
    vendor: "",
    vendorId: undefined,
    attachmentFile: null,
    attachmentName: undefined,
    attachmentUrl: undefined,
};

const LOCKED_SEASON_STATUSES = new Set(["COMPLETED", "CANCELLED", "ARCHIVED"]);

const CATEGORY_KEYWORDS: Array<{ keywords: string[]; category: string }> = [
    { keywords: ["fertilizer", "npk", "urea", "compost"], category: "Fertilizer" },
    { keywords: ["seed", "seedling", "seedlings"], category: "Seeds" },
    { keywords: ["labor", "wage", "salary"], category: "Labor" },
    { keywords: ["tractor", "equipment", "tool", "machinery"], category: "Equipment" },
    { keywords: ["pesticide", "spray", "herbicide", "fungicide"], category: "Pesticide" },
    { keywords: ["transport", "delivery", "shipping", "logistics"], category: "Transportation" },
    { keywords: ["utility", "electric", "water", "fuel"], category: "Utilities" },
    { keywords: ["repair", "maintenance", "service"], category: "Maintenance" },
];

const isValidSeasonId = (value: unknown): value is number =>
    typeof value === "number" && Number.isInteger(value) && value > 0;

const toLocalDate = (value?: string | null) => {
    if (!value) {
        return null;
    }
    const parsed = new Date(`${value}T00:00:00`);
    if (Number.isNaN(parsed.getTime())) {
        return null;
    }
    return parsed;
};

const inferCategory = (itemName: string | null | undefined): string => {
    if (!itemName) return "Other";
    const value = itemName.toLowerCase();
    for (const entry of CATEGORY_KEYWORDS) {
        if (entry.keywords.some((keyword) => value.includes(keyword))) {
            return entry.category;
        }
    }
    return "Other";
};

const mapApiExpense = (expense: ApiExpense, fallbackSeasonName: string): Expense => {
    const amount = expense.amount ?? expense.totalCost ?? ((expense.unitPrice ?? 0) * (expense.quantity ?? 1));
    const status = expense.paymentStatus ?? "PENDING";
    return {
        id: expense.id,
        date: expense.expenseDate,
        category: expense.category ?? inferCategory(expense.itemName),
        description: expense.itemName ?? expense.category ?? "Expense",
        linkedTask: expense.taskTitle ?? "",
        linkedTaskId: expense.taskId ?? undefined,
        linkedSeason: expense.seasonName ?? fallbackSeasonName,
        linkedSeasonId: expense.seasonId ?? undefined,
        linkedPlotId: expense.plotId ?? undefined,
        linkedPlotName: expense.plotName ?? undefined,
        amount: amount ?? 0,
        status: status as Expense["status"],
        notes: expense.note ?? "",
        vendor: expense.vendorName ?? "",
        vendorId: expense.vendorId ?? undefined,
        attachmentUrl: expense.attachmentUrl ?? undefined,
        attachmentName: expense.attachmentName ?? undefined,
    };
};

const toReadableApiError = (error: unknown, fallback: string) => {
    if (isAxiosError(error)) {
        const payload = error.response?.data as { message?: string; code?: string } | undefined;
        if (payload?.message) {
            return payload.message;
        }
        if (typeof error.message === "string" && error.message.length > 0) {
            return error.message;
        }
    }

    if (error instanceof Error && error.message) {
        return error.message;
    }

    return fallback;
};

export function useExpenseManagement(options: UseExpenseManagementOptions = {}) {
    const queryClient = useQueryClient();
    const seasonContext = useOptionalSeason();
    const seasons = seasonContext?.seasons ?? [];
    const fixedSeasonId = isValidSeasonId(options.scopedSeasonId) ? options.scopedSeasonId : null;
    const contextSeasonId = seasonContext?.selectedSeasonId ?? null;
    const seasonId = fixedSeasonId ?? contextSeasonId;
    const setSeasonIdFromContext = seasonContext?.setSelectedSeasonId ?? (() => { });

    const selectedSeasonData = useMemo(() => {
        if (!isValidSeasonId(seasonId)) {
            return null;
        }
        return seasons.find((season) => season.id === seasonId) ?? null;
    }, [seasonId, seasons]);

    const selectedSeason = isValidSeasonId(seasonId) ? String(seasonId) : "";
    const selectedSeasonName = selectedSeasonData?.seasonName ?? "";
    const selectedSeasonStatus = selectedSeasonData?.status ?? null;
    const isSeasonWriteLocked =
        !!selectedSeasonStatus && LOCKED_SEASON_STATUSES.has(selectedSeasonStatus);

    const seasonOptions = useMemo(() => {
        return seasons.map((season) => ({
            value: String(season.id),
            label: season.seasonName,
            plotId: season.plotId,
        }));
    }, [seasons]);

    // Tab State
    const [activeTab, setActiveTab] = useState("list");

    // Modal State
    const [isAddExpenseOpen, setIsAddExpenseOpen] = useState(false);
    const [selectedExpense, setSelectedExpense] = useState<Expense | null>(null);

    // Filter States
    const [searchQuery, setSearchQuery] = useState("");
    const [selectedCategory, setSelectedCategory] = useState("all");
    const [selectedStatus, setSelectedStatus] = useState("all");

    // Form State
    const [formData, setFormData] = useState<ExpenseFormData>(INITIAL_FORM_DATA);
    const [showValidationErrors, setShowValidationErrors] = useState(false);

    const resetForm = useCallback(() => {
        setFormData({
            ...INITIAL_FORM_DATA,
            linkedSeason: selectedSeasonName,
            linkedSeasonId: selectedSeasonData?.id,
            linkedPlotId: selectedSeasonData?.plotId,
        });
        setSelectedExpense(null);
        setShowValidationErrors(false);
    }, [selectedSeasonData?.id, selectedSeasonData?.plotId, selectedSeasonName]);

    const hasSeason = isValidSeasonId(seasonId);

    const listParams = useMemo(() => ({
        seasonId: seasonId ?? undefined,
        q: searchQuery.trim() || undefined,
        category: selectedCategory !== "all" ? selectedCategory : undefined,
        paymentStatus: selectedStatus !== "all" ? (selectedStatus as Expense["status"]) : undefined,
        page: 0,
        size: 200,
    }), [seasonId, searchQuery, selectedCategory, selectedStatus]);

    const {
        data: expenseData,
        isLoading,
        error,
        refetch,
    } = useAllFarmerExpenses(listParams, { enabled: hasSeason });

    const {
        data: taskData,
        isLoading: isLoadingTasks,
    } = useTasksBySeason(
        seasonId ?? 0,
        {
            page: 0,
            size: 100,
            sortBy: "title",
            sortDirection: "asc",
        },
        { enabled: hasSeason }
    );

    const { data: supplierData } = useAllSuppliers();

    const taskOptions: TaskOption[] = useMemo(() => {
        const tasks = taskData?.items ?? [];
        return tasks.map((task: ApiTask) => ({
            value: String(task.taskId),
            label: task.title,
            id: task.taskId,
        }));
    }, [taskData]);

    const supplierOptions = useMemo(() => {
        const items = supplierData ?? [];
        return items.map((supplier) => ({
            value: String(supplier.id),
            label: supplier.name,
            id: supplier.id,
        }));
    }, [supplierData]);

    const createMutation = useCreateExpense();
    const updateMutation = useUpdateExpense();
    const deleteMutation = useDeleteExpense();

    const expenses = useMemo(() => {
        const items = expenseData?.items ?? [];
        return items.map((expense) => mapApiExpense(expense, selectedSeasonName));
    }, [expenseData, selectedSeasonName]);

    const totalCount = expenseData?.totalElements ?? expenses.length;

    const totalExpenses = useMemo(
        () => expenses.reduce((sum, expense) => sum + (Number.isFinite(expense.amount) ? expense.amount : 0), 0),
        [expenses]
    );

    const paidExpenses = useMemo(
        () => expenses.filter((expense) => expense.status === "PAID").reduce((sum, expense) => sum + expense.amount, 0),
        [expenses]
    );
    const unpaidExpenses = useMemo(
        () => expenses.filter((expense) => expense.status === "UNPAID").reduce((sum, expense) => sum + expense.amount, 0),
        [expenses]
    );

    const budgetAmount = selectedSeasonData?.budgetAmount ?? null;
    const budgetUsagePercentage =
        budgetAmount && budgetAmount > 0
            ? (totalExpenses / budgetAmount) * 100
            : null;
    const remainingBudget =
        budgetAmount && budgetAmount > 0
            ? budgetAmount - totalExpenses
            : null;

    const pendingExpenses = useMemo(
        () => expenses.filter((expense) => expense.status === "PENDING" || expense.status === "UNPAID"),
        [expenses]
    );

    const filteredExpenses = expenses;

    const validateExpenseDateInSeason = useCallback((dateValue: string, selectedSeasonId: number) => {
        const selectedSeasonRecord = seasons.find((season) => season.id === selectedSeasonId);
        if (!selectedSeasonRecord) {
            return "Selected season does not exist.";
        }

        const expenseDate = toLocalDate(dateValue);
        if (!expenseDate) {
            return "Expense date is invalid.";
        }

        const seasonStartDate = toLocalDate(selectedSeasonRecord.startDate);
        const seasonEndDate = toLocalDate(
            selectedSeasonRecord.endDate ?? selectedSeasonRecord.plannedHarvestDate ?? null
        );

        if (!seasonStartDate) {
            return "Season start date is invalid.";
        }

        if (expenseDate < seasonStartDate) {
            return `Expense date must be on or after ${selectedSeasonRecord.startDate}.`;
        }

        if (seasonEndDate && expenseDate > seasonEndDate) {
            return `Expense date must be on or before ${selectedSeasonRecord.endDate ?? selectedSeasonRecord.plannedHarvestDate}.`;
        }

        return null;
    }, [seasons]);

    const handleAddExpense = useCallback(async () => {
        if (isSeasonWriteLocked) {
            toast.error("Season is locked", {
                description: "Expense write actions are disabled for this season.",
            });
            return;
        }

        if (!hasSeason) {
            toast.error("Select a season", {
                description: "Choose a season before recording expenses.",
            });
            return;
        }

        if (!formData.date || !formData.category || !formData.amount) {
            setShowValidationErrors(true);
            toast.error("Missing required fields", {
                description: "Date, category, and amount are required.",
            });
            return;
        }

        const parsedDate = toLocalDate(formData.date);
        if (!parsedDate) {
            setShowValidationErrors(true);
            toast.error("Invalid date", {
                description: "Expense date must be a valid calendar date.",
            });
            return;
        }

        const amount = Number(formData.amount);
        if (!Number.isFinite(amount) || amount <= 0) {
            setShowValidationErrors(true);
            toast.error("Invalid amount", {
                description: "Enter a valid amount greater than 0.",
            });
            return;
        }

        const selectedSeasonId = formData.linkedSeasonId ?? seasonId;
        if (!isValidSeasonId(selectedSeasonId)) {
            setShowValidationErrors(true);
            toast.error("Season required", {
                description: "Select a valid season for this expense.",
            });
            return;
        }

        const selectedSeasonRecord = seasons.find((season) => season.id === selectedSeasonId);
        if (!selectedSeasonRecord) {
            setShowValidationErrors(true);
            toast.error("Invalid season", {
                description: "The selected season was not found.",
            });
            return;
        }

        const selectedSeasonStatus = selectedSeasonRecord.status ?? "";
        if (LOCKED_SEASON_STATUSES.has(selectedSeasonStatus)) {
            toast.error("Season is locked", {
                description: "Expense write actions are disabled for this season.",
            });
            return;
        }

        const dateValidationError = validateExpenseDateInSeason(formData.date, selectedSeasonId);
        if (dateValidationError) {
            setShowValidationErrors(true);
            toast.error("Invalid date", {
                description: dateValidationError,
            });
            return;
        }

        const plotId = formData.linkedPlotId ?? selectedSeasonRecord.plotId;
        if (!plotId) {
            setShowValidationErrors(true);
            toast.error("Missing plot information", {
                description: "Season must have an associated plot.",
            });
            return;
        }

        const taskId = formData.linkedTaskId ??
            (formData.linkedTask ? Number.parseInt(formData.linkedTask, 10) : undefined);
        const payload = {
            amount,
            expenseDate: formData.date,
            category: formData.category.trim(),
            plotId,
            taskId: taskId && !Number.isNaN(taskId) ? taskId : undefined,
            note: formData.notes || undefined,
            itemName: formData.description.trim() || formData.category.trim(),
            unitPrice: amount,
            quantity: 1,
        };

        try {
            const savedExpense = selectedExpense
                ? await updateMutation.mutateAsync({
                    id: selectedExpense.id,
                    data: {
                        ...payload,
                        seasonId: selectedSeasonId,
                    },
                })
                : await createMutation.mutateAsync({
                    seasonId: selectedSeasonId,
                    data: payload,
                });

            if (formData.attachmentFile) {
                try {
                    await expenseApi.uploadAttachment(savedExpense.id, formData.attachmentFile);
                    await queryClient.invalidateQueries({ queryKey: expenseKeys.detail(savedExpense.id) });
                    await queryClient.invalidateQueries({ queryKey: expenseKeys.lists() });
                } catch (uploadError) {
                    toast.warning("Expense saved, but attachment upload failed", {
                        description: toReadableApiError(uploadError, "You can upload the receipt later."),
                    });
                }
            }

            toast.success(selectedExpense ? "Expense updated" : "Expense added", {
                description: `${formData.description || formData.category} has been recorded.`,
            });

            if (!selectedExpense && budgetAmount && budgetAmount > 0) {
                const newTotal = totalExpenses + amount;
                const newUsagePercent = (newTotal / budgetAmount) * 100;

                if (newUsagePercent >= 100) {
                    toast.warning("Budget exceeded", {
                        description: `Current spending is ${newUsagePercent.toFixed(1)}% of season budget.`,
                        duration: 6000,
                    });
                } else if (newUsagePercent >= 80) {
                    toast.warning("Budget warning", {
                        description: `You have used ${newUsagePercent.toFixed(1)}% of season budget.`,
                        duration: 5000,
                    });
                }
            }

            setIsAddExpenseOpen(false);
            setShowValidationErrors(false);
            resetForm();
            refetch();
        } catch (err: unknown) {
            toast.error(selectedExpense ? "Failed to update expense" : "Failed to add expense", {
                description: toReadableApiError(err, "Please try again."),
            });
        }
    }, [
        budgetAmount,
        createMutation,
        formData,
        hasSeason,
        isSeasonWriteLocked,
        queryClient,
        refetch,
        resetForm,
        seasonId,
        selectedExpense,
        seasons,
        totalExpenses,
        updateMutation,
        validateExpenseDateInSeason,
    ]);

    const handleEditExpense = useCallback((expense: Expense) => {
        setSelectedExpense(expense);
        setFormData({
            date: expense.date,
            category: expense.category,
            description: expense.description,
            linkedTask: expense.linkedTaskId ? String(expense.linkedTaskId) : "",
            linkedTaskId: expense.linkedTaskId,
            linkedSeason: expense.linkedSeason || "",
            linkedSeasonId: expense.linkedSeasonId,
            linkedPlotId: expense.linkedPlotId,
            amount: String(expense.amount),
            status: expense.status,
            notes: expense.notes || "",
            vendor: expense.vendor || "",
            vendorId: expense.vendorId,
            attachmentFile: null,
            attachmentName: expense.attachmentName,
            attachmentUrl: expense.attachmentUrl,
        });
        setShowValidationErrors(false);
        setIsAddExpenseOpen(true);
    }, []);

    const handleDeleteExpense = useCallback((id: number) => {
        if (!hasSeason || !isValidSeasonId(seasonId)) {
            toast.error("Invalid season", {
                description: "Select a valid season before deleting expenses.",
            });
            return;
        }

        if (isSeasonWriteLocked) {
            toast.error("Season is locked", {
                description: "Expense write actions are disabled for this season.",
            });
            return;
        }

        deleteMutation.mutate(
            { id, seasonId },
            {
                onSuccess: () => toast.success("Expense deleted"),
                onError: (err) => toast.error("Failed to delete expense", {
                    description: toReadableApiError(err, "Please try again."),
                }),
            }
        );
    }, [deleteMutation, hasSeason, isSeasonWriteLocked, seasonId]);

    const handleOpenAddExpense = useCallback(() => {
        resetForm();
        setShowValidationErrors(false);
        setIsAddExpenseOpen(true);
    }, [resetForm]);

    const handleSeasonChange = useCallback((value: string) => {
        if (fixedSeasonId) {
            return;
        }
        const numericId = Number.parseInt(value, 10);
        if (!Number.isNaN(numericId) && numericId > 0) {
            setSeasonIdFromContext(numericId);
        }
    }, [fixedSeasonId, setSeasonIdFromContext]);

    const handleTaskChange = useCallback((taskIdStr: string) => {
        const taskId = Number.parseInt(taskIdStr, 10);
        setFormData((prev) => ({
            ...prev,
            linkedTask: taskIdStr,
            linkedTaskId: Number.isNaN(taskId) ? undefined : taskId,
        }));
    }, []);

    const handleExportExpenses = useCallback(async () => {
        if (!hasSeason) {
            toast.error("Select a season", {
                description: "Choose a season before exporting expenses.",
            });
            return;
        }

        try {
            const { page, size, ...filters } = listParams;
            const blob = await expenseApi.exportCsv({ page, size, ...filters });
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement("a");
            const date = new Date().toISOString().split("T")[0];
            const seasonLabel = selectedSeasonName ? selectedSeasonName.replace(/\s+/g, "-") : "season";
            link.href = url;
            link.download = `expenses-${seasonLabel}-${date}.csv`;
            link.click();
            window.URL.revokeObjectURL(url);
        } catch (err: unknown) {
            if (isAxiosError(err) && err.response?.status === 404) {
                toast.error("Export is not available", {
                    description: "The backend does not expose expense CSV export in this environment.",
                });
                return;
            }
            toast.error("Export failed", {
                description: toReadableApiError(err, "Please try again."),
            });
        }
    }, [hasSeason, listParams, selectedSeasonName]);

    const handleQuickUpdate = useCallback(async (
        expense: Expense,
        updates: { status?: Expense["status"]; notes?: string }
    ) => {
        if (isSeasonWriteLocked) {
            toast.error("Season is locked", {
                description: "Expense write actions are disabled for this season.",
            });
            return;
        }

        if (!isValidSeasonId(expense.linkedSeasonId) || !expense.linkedPlotId) {
            toast.error("Cannot update expense", {
                description: "Missing season or plot information.",
            });
            return;
        }

        try {
            await updateMutation.mutateAsync({
                id: expense.id,
                data: {
                    amount: expense.amount,
                    expenseDate: expense.date,
                    category: expense.category,
                    seasonId: expense.linkedSeasonId,
                    plotId: expense.linkedPlotId,
                    taskId: expense.linkedTaskId,
                    note: updates.notes ?? expense.notes,
                    itemName: expense.description,
                    unitPrice: expense.amount,
                    quantity: 1,
                },
            });
            if (updates.status && updates.status !== expense.status) {
                toast.info("Payment status is local-only", {
                    description: "This backend does not persist payment status on expense update.",
                });
            }
            refetch();
        } catch (err: unknown) {
            toast.error("Failed to update expense", {
                description: toReadableApiError(err, "Please try again."),
            });
        }
    }, [isSeasonWriteLocked, refetch, updateMutation]);

    return {
        // Tab State
        activeTab,
        setActiveTab,

        // Modal State
        isAddExpenseOpen,
        setIsAddExpenseOpen,
        selectedExpense,

        // Filter States
        searchQuery,
        setSearchQuery,
        selectedSeason,
        setSelectedSeason: handleSeasonChange,
        selectedCategory,
        setSelectedCategory,
        selectedStatus,
        setSelectedStatus,
        seasonOptions,

        // Task options for dropdown
        taskOptions,
        isLoadingTasks,
        handleTaskChange,

        // Supplier options for dropdown
        supplierOptions,

        // Expenses Data
        expenses,
        filteredExpenses,
        totalCount,

        // Form State
        formData,
        setFormData,
        showValidationErrors,

        // Computed Values
        totalExpenses,
        budgetUsagePercentage,
        remainingBudget,
        paidExpenses,
        unpaidExpenses,
        budgetAmount,
        pendingExpenses,

        // Handlers
        handleAddExpense,
        handleEditExpense,
        handleDeleteExpense,
        resetForm,
        handleOpenAddExpense,
        handleExportExpenses,
        handleQuickUpdate,
        isSeasonWriteLocked,

        // API state
        isLoading,
        isLoadingTracker: false,
        error: error ?? null,
        refetch,
        isCreating: createMutation.isPending,
        isUpdating: updateMutation.isPending,
        isDeleting: deleteMutation.isPending,
        hasSeason,
    };
}
