import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { expenseKeys } from "../model/keys";
import { useAllFarmerExpenses, useCreateExpense, useDeleteExpense, useUpdateExpense } from "./hooks";

const httpMocks = vi.hoisted(() => ({
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
}));

vi.mock("@/shared/api/http", () => ({
    default: {
        get: httpMocks.get,
        post: httpMocks.post,
        put: httpMocks.put,
        delete: httpMocks.delete,
    },
}));

const expenseRow = {
    id: 11,
    seasonId: 33,
    seasonName: "Winter 2026",
    plotId: 15,
    category: "Fertilizer",
    amount: 1200,
    expenseDate: "2026-03-08",
    itemName: "NPK 20-20-20",
    unitPrice: 1200,
    quantity: 1,
    totalCost: 1200,
};

function createQueryClient() {
    return new QueryClient({
        defaultOptions: {
            queries: { retry: false, staleTime: 0, gcTime: 0 },
            mutations: { retry: false },
        },
    });
}

describe("expense hooks flow", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("create invalidates list keys used by visible expense list queries", async () => {
        httpMocks.get.mockResolvedValueOnce({
            data: {
                status: 200,
                code: "SUCCESS",
                message: "OK",
                result: {
                    items: [expenseRow],
                    page: 0,
                    size: 20,
                    totalElements: 1,
                    totalPages: 1,
                },
            },
        });
        httpMocks.post.mockResolvedValueOnce({
            data: {
                status: 200,
                code: "SUCCESS",
                message: "OK",
                result: {
                    ...expenseRow,
                    id: 99,
                },
            },
        });

        const queryClient = createQueryClient();
        const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
        const wrapper = ({ children }: { children: ReactNode }) => (
            <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
        );

        const list = renderHook(
            () => useAllFarmerExpenses({ seasonId: 33, page: 0, size: 20 }),
            { wrapper }
        );
        await waitFor(() => expect(list.result.current.isSuccess).toBe(true));

        const create = renderHook(() => useCreateExpense(), { wrapper });
        await create.result.current.mutateAsync({
            seasonId: 33,
            data: {
                amount: 300,
                expenseDate: "2026-04-02",
                category: "Labor",
                plotId: 15,
                itemName: "Labor day 1",
                unitPrice: 300,
                quantity: 1,
            },
        });

        expect(invalidateSpy).toHaveBeenCalledWith({
            queryKey: expenseKeys.lists(),
        });
    });

    it("update invalidates list keys used by visible expense list queries", async () => {
        httpMocks.put.mockResolvedValueOnce({
            data: {
                status: 200,
                code: "SUCCESS",
                message: "OK",
                result: {
                    ...expenseRow,
                    amount: 1500,
                },
            },
        });

        const queryClient = createQueryClient();
        const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
        const wrapper = ({ children }: { children: ReactNode }) => (
            <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
        );

        const update = renderHook(() => useUpdateExpense(), { wrapper });
        await update.result.current.mutateAsync({
            id: 11,
            data: {
                amount: 1500,
                expenseDate: "2026-03-08",
                category: "Fertilizer",
                seasonId: 33,
                plotId: 15,
                itemName: "NPK 20-20-20",
                unitPrice: 1500,
                quantity: 1,
            },
        });

        expect(invalidateSpy).toHaveBeenCalledWith({
            queryKey: expenseKeys.lists(),
        });
    });

    it("delete invalidates list keys used by visible expense list queries", async () => {
        httpMocks.delete.mockResolvedValueOnce({
            data: {
                status: 200,
                code: "SUCCESS",
                message: "OK",
            },
        });

        const queryClient = createQueryClient();
        const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
        const wrapper = ({ children }: { children: ReactNode }) => (
            <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
        );

        const deletion = renderHook(() => useDeleteExpense(), { wrapper });
        await deletion.result.current.mutateAsync({ id: 11, seasonId: 33 });

        expect(invalidateSpy).toHaveBeenCalledWith({
            queryKey: expenseKeys.lists(),
        });
    });
});
