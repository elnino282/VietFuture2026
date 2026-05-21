import { act, renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useHarvestManagement } from "./useHarvestManagement";

const mocks = vi.hoisted(() => ({
  useAllFarmerHarvests: vi.fn(),
  useHarvestSummary: vi.fn(),
  useCreateHarvest: vi.fn(),
  useDeleteHarvest: vi.fn(),
  useOptionalSeason: vi.fn(),
  useWeightUnit: vi.fn(),
  createMutate: vi.fn(),
  deleteMutateAsync: vi.fn(),
  refetch: vi.fn(),
  toastSuccess: vi.fn(),
  toastError: vi.fn(),
}));

vi.mock("@/entities/harvest", () => ({
  useAllFarmerHarvests: mocks.useAllFarmerHarvests,
  useHarvestSummary: mocks.useHarvestSummary,
  useCreateHarvest: mocks.useCreateHarvest,
  useDeleteHarvest: mocks.useDeleteHarvest,
}));

vi.mock("@/shared/contexts", () => ({
  useOptionalSeason: mocks.useOptionalSeason,
  useWeightUnit: mocks.useWeightUnit,
}));

vi.mock("sonner", () => ({
  toast: {
    success: mocks.toastSuccess,
    error: mocks.toastError,
  },
}));

const apiHarvestRows = [
  {
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
  },
];

describe("useHarvestManagement", () => {
  beforeEach(() => {
    vi.clearAllMocks();

    mocks.useOptionalSeason.mockReturnValue({
      selectedSeasonId: 11,
      seasons: [{ id: 11, status: "ACTIVE", seasonName: "Spring 2026" }],
    });
    mocks.useWeightUnit.mockReturnValue("kg");
    mocks.useAllFarmerHarvests.mockReturnValue({
      data: {
        items: apiHarvestRows,
        page: 0,
        size: 100,
        totalElements: apiHarvestRows.length,
        totalPages: 1,
      },
      isLoading: false,
      error: null,
      refetch: mocks.refetch,
    });
    mocks.useHarvestSummary.mockReturnValue({
      data: { yieldVsPlanPercent: 95.5 },
    });
    mocks.useCreateHarvest.mockReturnValue({
      mutate: mocks.createMutate,
      isPending: false,
    });
    mocks.useDeleteHarvest.mockReturnValue({
      mutateAsync: mocks.deleteMutateAsync,
      isPending: false,
    });

    mocks.deleteMutateAsync.mockResolvedValue(undefined);
  });

  it("maps only backend fields without placeholder transform values", () => {
    const { result } = renderHook(() => useHarvestManagement());

    expect(result.current.batches).toHaveLength(1);
    expect(result.current.batches[0]).toMatchObject({
      id: "77",
      batchId: "77",
      seasonId: 33,
      seasonName: "Winter 2026",
      quantity: 120.5,
      grade: "A",
      revenue: 120.5,
      notes: "first lot",
    });
    expect(result.current.batches[0].moisture).toBeUndefined();
    expect(result.current.batches[0].status).toBeUndefined();
    expect(result.current.batches[0].plot).toBeUndefined();
    expect(result.current.batches[0].crop).toBeUndefined();
  });

  it("does not show success or call create mutation when season is invalid", () => {
    mocks.useOptionalSeason.mockReturnValue(null);
    const { result } = renderHook(() => useHarvestManagement());

    act(() => {
      result.current.setFormData((previous) => ({
        ...previous,
        date: "2026-05-10",
        quantity: "50",
        warehouseId: "3",
        productName: "Rice",
        lotCode: "LOT-100",
      }));
    });

    act(() => {
      result.current.handleAddBatch();
    });

    expect(mocks.createMutate).not.toHaveBeenCalled();
    expect(mocks.toastSuccess).not.toHaveBeenCalledWith("Harvest Added", expect.anything());
    expect(mocks.toastError).toHaveBeenCalledWith(
      "Season required",
      expect.objectContaining({
        description: expect.stringContaining("Select a valid season"),
      })
    );
  });

  it("calls create mutation with validated payload for valid season", () => {
    const { result } = renderHook(() => useHarvestManagement());

    act(() => {
      result.current.setFormData((previous) => ({
        ...previous,
        date: "2026-05-10",
        quantity: "50",
        warehouseId: "3",
        productName: "Rice",
        productVariant: "Premium",
        lotCode: "LOT-100",
        inventoryUnit: "kg",
        notes: "note",
      }));
    });

    act(() => {
      result.current.handleAddBatch();
    });

    expect(mocks.createMutate).toHaveBeenCalledWith(
      expect.objectContaining({
        seasonId: 11,
        data: expect.objectContaining({
          harvestDate: "2026-05-10",
          quantity: 50,
          warehouseId: 3,
          productName: "Rice",
          productVariant: "Premium",
          lotCode: "LOT-100",
          inventoryUnit: "kg",
          note: "note",
        }),
      })
    );
  });

  it("calls delete mutation for a real batch", async () => {
    const { result } = renderHook(() => useHarvestManagement());

    await act(async () => {
      await result.current.handleDeleteBatch(result.current.batches[0]);
    });

    expect(mocks.deleteMutateAsync).toHaveBeenCalledWith({
      id: 77,
      seasonId: 33,
    });
  });

  it("exports CSV from fetched real batch data", async () => {
    const originalCreateElement = document.createElement.bind(document);
    const link = originalCreateElement("a");
    const clickSpy = vi.spyOn(link, "click").mockImplementation(() => {});
    const createElementSpy = vi.spyOn(document, "createElement").mockImplementation((tagName: string) => {
      if (tagName.toLowerCase() === "a") {
        return link;
      }
      return originalCreateElement(tagName);
    });

    const createObjectURLSpy = vi.fn(() => "blob:mock");
    const revokeObjectURLSpy = vi.fn();
    const originalURL = window.URL;
    const originalBlob = globalThis.Blob;
    const blobPayloads: string[] = [];
    class MockBlob {
      private readonly payload: string;
      constructor(parts: Array<string | ArrayBuffer | ArrayBufferView>) {
        this.payload = parts
          .map((part) => {
            if (typeof part === "string") return part;
            if (part instanceof ArrayBuffer) {
              return new TextDecoder().decode(part);
            }
            return new TextDecoder().decode(part.buffer);
          })
          .join("");
        blobPayloads.push(this.payload);
      }
      async text() {
        return this.payload;
      }
    }
    Object.defineProperty(globalThis, "Blob", {
      configurable: true,
      value: MockBlob,
    });
    Object.defineProperty(window, "URL", {
      configurable: true,
      value: {
        ...window.URL,
        createObjectURL: createObjectURLSpy,
        revokeObjectURL: revokeObjectURLSpy,
      },
    });

    const { result } = renderHook(() => useHarvestManagement());
    act(() => {
      result.current.handleExport(result.current.batches);
    });

    expect(createObjectURLSpy).toHaveBeenCalledTimes(1);
    const csv = blobPayloads[0] ?? "";
    expect(csv).toContain('"Harvest ID"');
    expect(csv).toContain('"77"');
    expect(csv).toContain('"Winter 2026"');
    expect(csv).not.toContain("Plot A");
    expect(csv).not.toContain("Crop");
    expect(clickSpy).toHaveBeenCalled();
    Object.defineProperty(globalThis, "Blob", {
      configurable: true,
      value: originalBlob,
    });
    Object.defineProperty(window, "URL", {
      configurable: true,
      value: originalURL,
    });
    createElementSpy.mockRestore();
    clickSpy.mockRestore();
  });
});
