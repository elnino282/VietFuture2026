import { beforeEach, describe, expect, it, vi } from "vitest";
import { renderHook, act } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { useExpenseManagement } from "@/features/farmer/expense-management/hooks/useExpenseManagement";

const expenseMocks = vi.hoisted(() => ({
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    uploadAttachment: vi.fn(),
    refetch: vi.fn(),
    exportCsv: vi.fn(),
}));
const sonnerToast = vi.hoisted(() => ({
    error: vi.fn(),
    success: vi.fn(),
    warning: vi.fn(),
    info: vi.fn(),
}));

let mockSeasonContext: {
    seasons: Array<any>;
    selectedSeasonId: number | null;
    setSelectedSeasonId: ReturnType<typeof vi.fn>;
} = {
    seasons: [],
    selectedSeasonId: 1,
    setSelectedSeasonId: vi.fn(),
};

let mockExpenseListData: {
    items: Array<any>;
    totalElements: number;
} = {
    items: [],
    totalElements: 0,
};

vi.mock("sonner", () => ({
    toast: sonnerToast,
}));

vi.mock("@/shared/contexts", () => ({
    useOptionalSeason: () => mockSeasonContext,
}));

vi.mock("@/entities/expense", () => ({
    expenseApi: {
        uploadAttachment: expenseMocks.uploadAttachment,
        exportCsv: expenseMocks.exportCsv,
    },
    expenseKeys: {
        detail: (id: number) => ["expense", "detail", id],
        lists: () => ["expense", "list"],
    },
    useAllFarmerExpenses: () => ({
        data: mockExpenseListData,
        isLoading: false,
        error: null,
        refetch: expenseMocks.refetch,
    }),
    useCreateExpense: () => ({
        mutateAsync: expenseMocks.create,
        isPending: false,
    }),
    useUpdateExpense: () => ({
        mutateAsync: expenseMocks.update,
        isPending: false,
    }),
    useDeleteExpense: () => ({
        mutate: expenseMocks.delete,
        isPending: false,
    }),
}));

vi.mock("@/entities/task", () => ({
    useTasksBySeason: () => ({
        data: { items: [] },
        isLoading: false,
    }),
}));

vi.mock("@/entities/supplies", () => ({
    useAllSuppliers: () => ({
        data: [],
    }),
}));

const buildSeason = (overrides?: Partial<any>) => ({
    id: 1,
    seasonName: "Spring 2026",
    plotId: 101,
    startDate: "2026-01-01",
    endDate: "2026-12-31",
    plannedHarvestDate: "2026-12-31",
    status: "ACTIVE",
    budgetAmount: 10_000,
    ...overrides,
});

const buildExpenseRow = (overrides?: Partial<any>) => ({
    id: 11,
    seasonId: 1,
    seasonName: "Spring 2026",
    plotId: 101,
    taskId: null,
    taskTitle: null,
    category: "Fertilizer",
    amount: 500,
    note: "base note",
    expenseDate: "2026-03-05",
    itemName: "NPK",
    unitPrice: 500,
    quantity: 1,
    totalCost: 500,
    attachmentUrl: null,
    attachmentName: null,
    ...overrides,
});

function wrapperFactory() {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
        },
    });
    return ({ children }: { children: ReactNode }) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
}

describe("useExpenseManagement", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockSeasonContext = {
            seasons: [buildSeason()],
            selectedSeasonId: 1,
            setSelectedSeasonId: vi.fn(),
        };
        mockExpenseListData = {
            items: [],
            totalElements: 0,
        };
        expenseMocks.create.mockResolvedValue({ id: 1001 });
        expenseMocks.update.mockResolvedValue({ id: 11 });
        expenseMocks.delete.mockImplementation((_vars: unknown, opts?: { onSuccess?: () => void }) => {
            opts?.onSuccess?.();
        });
        expenseMocks.uploadAttachment.mockResolvedValue({ id: 1001 });
    });

    it("calls create mutation for a valid new expense", async () => {
        const { result } = renderHook(() => useExpenseManagement(), { wrapper: wrapperFactory() });

        act(() => {
            result.current.setFormData({
                ...result.current.formData,
                date: "2026-03-10",
                category: "Fertilizer",
                amount: "1200",
                linkedSeasonId: 1,
                linkedPlotId: 101,
            });
        });

        await act(async () => {
            await result.current.handleAddExpense();
        });

        expect(expenseMocks.create).toHaveBeenCalledWith(
            expect.objectContaining({
                seasonId: 1,
                data: expect.objectContaining({
                    amount: 1200,
                    category: "Fertilizer",
                    expenseDate: "2026-03-10",
                    plotId: 101,
                }),
            })
        );
        expect(expenseMocks.refetch).toHaveBeenCalled();
    });

    it("calls update mutation when editing an expense", async () => {
        mockExpenseListData = {
            items: [buildExpenseRow()],
            totalElements: 1,
        };

        const { result } = renderHook(() => useExpenseManagement(), { wrapper: wrapperFactory() });

        act(() => {
            result.current.handleEditExpense(result.current.expenses[0]);
        });

        await act(async () => {
            await result.current.handleAddExpense();
        });

        expect(expenseMocks.update).toHaveBeenCalledWith(
            expect.objectContaining({
                id: 11,
                data: expect.objectContaining({
                    seasonId: 1,
                    plotId: 101,
                    amount: 500,
                }),
            })
        );
    });

    it("calls delete mutation with season context", () => {
        const { result } = renderHook(() => useExpenseManagement(), { wrapper: wrapperFactory() });

        act(() => {
            result.current.handleDeleteExpense(11);
        });

        expect(expenseMocks.delete).toHaveBeenCalledWith(
            { id: 11, seasonId: 1 },
            expect.any(Object)
        );
    });

    it("blocks create and delete when season is invalid", async () => {
        mockSeasonContext = {
            seasons: [buildSeason()],
            selectedSeasonId: null,
            setSelectedSeasonId: vi.fn(),
        };

        const { result } = renderHook(() => useExpenseManagement(), { wrapper: wrapperFactory() });

        act(() => {
            result.current.setFormData({
                ...result.current.formData,
                date: "2026-03-10",
                category: "Fertilizer",
                amount: "1200",
            });
        });

        await act(async () => {
            await result.current.handleAddExpense();
        });

        act(() => {
            result.current.handleDeleteExpense(11);
        });

        expect(expenseMocks.create).not.toHaveBeenCalled();
        expect(expenseMocks.delete).not.toHaveBeenCalled();
    });

    it("blocks create and delete when season is locked", async () => {
        mockSeasonContext = {
            seasons: [buildSeason({ status: "COMPLETED" })],
            selectedSeasonId: 1,
            setSelectedSeasonId: vi.fn(),
        };

        const { result } = renderHook(() => useExpenseManagement(), { wrapper: wrapperFactory() });

        act(() => {
            result.current.setFormData({
                ...result.current.formData,
                date: "2026-03-10",
                category: "Fertilizer",
                amount: "1200",
                linkedSeasonId: 1,
                linkedPlotId: 101,
            });
        });

        await act(async () => {
            await result.current.handleAddExpense();
        });

        act(() => {
            result.current.handleDeleteExpense(11);
        });

        expect(expenseMocks.create).not.toHaveBeenCalled();
        expect(expenseMocks.delete).not.toHaveBeenCalled();
    });

    it("validates amount/date/category before create", async () => {
        const { result } = renderHook(() => useExpenseManagement(), { wrapper: wrapperFactory() });

        act(() => {
            result.current.setFormData({
                ...result.current.formData,
                date: "2025-12-31",
                category: "",
                amount: "-10",
                linkedSeasonId: 1,
                linkedPlotId: 101,
            });
        });

        await act(async () => {
            await result.current.handleAddExpense();
        });

        expect(expenseMocks.create).not.toHaveBeenCalled();
        expect(sonnerToast.error).toHaveBeenCalled();
    });
});
