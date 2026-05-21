import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { harvestKeys } from "../model/keys";
import { useAllFarmerHarvests, useCreateHarvest, useDeleteHarvest } from "./hooks";

const httpMocks = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  delete: vi.fn(),
}));

vi.mock("@/shared/api/http", () => ({
  default: {
    get: httpMocks.get,
    post: httpMocks.post,
    delete: httpMocks.delete,
  },
}));

const harvestRow = {
  id: 77,
  seasonId: 33,
  seasonName: "Winter 2026",
  harvestDate: "2026-04-18",
  quantity: 120.5,
  unit: 1,
  grade: "A",
  revenue: 120.5,
  note: "first lot",
  createdAt: "2026-04-18T10:00:00",
};

function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, staleTime: 0, gcTime: 0 },
      mutations: { retry: false },
    },
  });
}

describe("harvest hooks flow", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("invalidates the same season-scoped list base used by read queries", async () => {
    httpMocks.get.mockResolvedValueOnce({
      data: {
        status: 200,
        code: "SUCCESS",
        message: "OK",
        result: {
          items: [harvestRow],
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
          ...harvestRow,
          id: 88,
          lotCode: "LOT-88",
        },
      },
    });

    const queryClient = createQueryClient();
    const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const list = renderHook(
      () => useAllFarmerHarvests({ seasonId: 33, page: 0, size: 20 }),
      { wrapper }
    );

    await waitFor(() => expect(list.result.current.isSuccess).toBe(true));
    expect(
      queryClient.getQueryCache().findAll({
        queryKey: harvestKeys.listBySeasonBase(33),
      }).length
    ).toBeGreaterThan(0);

    const create = renderHook(() => useCreateHarvest(), { wrapper });
    await create.result.current.mutateAsync({
      seasonId: 33,
      data: {
        harvestDate: "2026-05-01",
        quantity: 30,
        unit: 1,
        warehouseId: 10,
        productName: "Rice",
        lotCode: "LOT-NEW-1",
      },
    });

    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: harvestKeys.listBySeasonBase(33),
    });
  });

  it("invalidates season-scoped list base after delete", async () => {
    httpMocks.delete.mockResolvedValueOnce({ data: { status: 200 } });

    const queryClient = createQueryClient();
    const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const deletion = renderHook(() => useDeleteHarvest(), { wrapper });
    await deletion.result.current.mutateAsync({ id: 77, seasonId: 33 });

    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: harvestKeys.listBySeasonBase(33),
    });
  });
});
