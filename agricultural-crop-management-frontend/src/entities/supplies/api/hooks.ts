import { inventoryKeys } from "@/entities/inventory";
import type { PageResponse } from "@/shared/api/types";
import { useMutation, useQuery, useQueryClient, type UseQueryOptions } from "@tanstack/react-query";
import { suppliesKeys } from "../model/keys";
import type {
  CreateSupplyItemRequest,
  CreateSupplierRequest,
  StockInRequest,
  StockInResponse,
  Supplier,
  SuppliersParams,
  SupplyItem,
  SupplyItemsParams,
  SupplyLot,
  SupplyLotsParams,
  UpdateSupplyItemRequest,
  UpdateSupplierRequest,
} from "../model/types";
import { suppliesApi } from "./client";

// ═══════════════════════════════════════════════════════════════
// Context types for optimistic updates
// ═══════════════════════════════════════════════════════════════
type CreateSupplierContext = {
  previousSuppliers: PageResponse<Supplier> | undefined;
  previousAllSuppliers: Supplier[] | undefined;
};

type UpdateSupplierContext = {
  previousSuppliers: PageResponse<Supplier> | undefined;
  previousAllSuppliers: Supplier[] | undefined;
};

type DeleteSupplierContext = {
  previousSuppliers: PageResponse<Supplier> | undefined;
  previousAllSuppliers: Supplier[] | undefined;
};

type CreateSupplyItemContext = {
  previousItems: PageResponse<SupplyItem> | undefined;
  previousAllItems: SupplyItem[] | undefined;
};

type UpdateSupplyItemContext = {
  previousItems: PageResponse<SupplyItem> | undefined;
  previousAllItems: SupplyItem[] | undefined;
};

type DeleteSupplyItemContext = {
  previousItems: PageResponse<SupplyItem> | undefined;
  previousAllItems: SupplyItem[] | undefined;
};

// ═══════════════════════════════════════════════════════════════
// SUPPLIERS
// ═══════════════════════════════════════════════════════════════

export function useSuppliers(params?: SuppliersParams) {
  return useQuery({
    queryKey: suppliesKeys.suppliers(params),
    queryFn: () => suppliesApi.getSuppliers(params),
  });
}

export function useAllSuppliers() {
  return useQuery({
    queryKey: [...suppliesKeys.all, "all-suppliers"],
    queryFn: () => suppliesApi.getAllSuppliers(),
  });
}

/**
 * Create supplier with optimistic update
 */
export function useCreateSupplier() {
  const queryClient = useQueryClient();

  return useMutation<
    Supplier,
    Error,
    CreateSupplierRequest,
    CreateSupplierContext
  >({
    mutationFn: (data) => suppliesApi.createSupplier(data),
    onMutate: async (newSupplier) => {
      // Cancel any outgoing refetches
      await queryClient.cancelQueries({ queryKey: suppliesKeys.all });

      // Snapshot previous values
      const previousSuppliers = queryClient.getQueryData<
        PageResponse<Supplier>
      >(suppliesKeys.suppliers());
      const previousAllSuppliers = queryClient.getQueryData<Supplier[]>([
        ...suppliesKeys.all,
        "all-suppliers",
      ]);

      // Optimistic update - add new supplier with temp ID
      if (previousSuppliers) {
        queryClient.setQueryData<PageResponse<Supplier>>(
          suppliesKeys.suppliers(),
          {
            ...previousSuppliers,
            items: [
              { id: Date.now(), ...newSupplier } as Supplier,
              ...previousSuppliers.items,
            ],
            totalElements: previousSuppliers.totalElements + 1,
          },
        );
      }

      if (previousAllSuppliers) {
        queryClient.setQueryData<Supplier[]>(
          [...suppliesKeys.all, "all-suppliers"],
          [
            { id: Date.now(), ...newSupplier } as Supplier,
            ...previousAllSuppliers,
          ],
        );
      }

      return { previousSuppliers, previousAllSuppliers };
    },
    onError: (_err, _newSupplier, context) => {
      // Rollback on error
      if (context?.previousSuppliers) {
        queryClient.setQueryData(
          suppliesKeys.suppliers(),
          context.previousSuppliers,
        );
      }
      if (context?.previousAllSuppliers) {
        queryClient.setQueryData(
          [...suppliesKeys.all, "all-suppliers"],
          context.previousAllSuppliers,
        );
      }
    },
    onSettled: () => {
      // Always refetch to sync with server
      queryClient.invalidateQueries({ queryKey: suppliesKeys.all });
    },
  });
}

/**
 * Update supplier with optimistic update
 */
export function useUpdateSupplier() {
  const queryClient = useQueryClient();

  return useMutation<
    Supplier,
    Error,
    { id: number; data: UpdateSupplierRequest },
    UpdateSupplierContext
  >({
    mutationFn: ({ id, data }) => suppliesApi.updateSupplier(id, data),
    onMutate: async ({ id, data }) => {
      await queryClient.cancelQueries({ queryKey: suppliesKeys.all });

      const previousSuppliers = queryClient.getQueryData<
        PageResponse<Supplier>
      >(suppliesKeys.suppliers());
      const previousAllSuppliers = queryClient.getQueryData<Supplier[]>([
        ...suppliesKeys.all,
        "all-suppliers",
      ]);

      // Optimistic update in paginated list
      if (previousSuppliers) {
        queryClient.setQueryData<PageResponse<Supplier>>(
          suppliesKeys.suppliers(),
          {
            ...previousSuppliers,
            items: previousSuppliers.items.map((supplier) =>
              supplier.id === id ? { ...supplier, ...data } : supplier,
            ),
          },
        );
      }

      // Optimistic update in all suppliers list
      if (previousAllSuppliers) {
        queryClient.setQueryData<Supplier[]>(
          [...suppliesKeys.all, "all-suppliers"],
          previousAllSuppliers.map((supplier) =>
            supplier.id === id ? { ...supplier, ...data } : supplier,
          ),
        );
      }

      return { previousSuppliers, previousAllSuppliers };
    },
    onError: (_err, _variables, context) => {
      if (context?.previousSuppliers) {
        queryClient.setQueryData(
          suppliesKeys.suppliers(),
          context.previousSuppliers,
        );
      }
      if (context?.previousAllSuppliers) {
        queryClient.setQueryData(
          [...suppliesKeys.all, "all-suppliers"],
          context.previousAllSuppliers,
        );
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: suppliesKeys.all });
    },
  });
}

/**
 * Delete supplier with optimistic update
 */
export function useDeleteSupplier() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, number, DeleteSupplierContext>({
    mutationFn: (id) => suppliesApi.deleteSupplier(id),
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: suppliesKeys.all });

      const previousSuppliers = queryClient.getQueryData<
        PageResponse<Supplier>
      >(suppliesKeys.suppliers());
      const previousAllSuppliers = queryClient.getQueryData<Supplier[]>([
        ...suppliesKeys.all,
        "all-suppliers",
      ]);

      // Optimistic remove from paginated list
      if (previousSuppliers) {
        queryClient.setQueryData<PageResponse<Supplier>>(
          suppliesKeys.suppliers(),
          {
            ...previousSuppliers,
            items: previousSuppliers.items.filter(
              (supplier) => supplier.id !== id,
            ),
            totalElements: Math.max(0, previousSuppliers.totalElements - 1),
          },
        );
      }

      // Optimistic remove from all suppliers list
      if (previousAllSuppliers) {
        queryClient.setQueryData<Supplier[]>(
          [...suppliesKeys.all, "all-suppliers"],
          previousAllSuppliers.filter((supplier) => supplier.id !== id),
        );
      }

      return { previousSuppliers, previousAllSuppliers };
    },
    onError: (_err, _id, context) => {
      if (context?.previousSuppliers) {
        queryClient.setQueryData(
          suppliesKeys.suppliers(),
          context.previousSuppliers,
        );
      }
      if (context?.previousAllSuppliers) {
        queryClient.setQueryData(
          [...suppliesKeys.all, "all-suppliers"],
          context.previousAllSuppliers,
        );
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: suppliesKeys.all });
    },
  });
}

// ═══════════════════════════════════════════════════════════════
// SUPPLY ITEMS
// ═══════════════════════════════════════════════════════════════

export function useSupplyItems(params?: SupplyItemsParams) {
  return useQuery({
    queryKey: suppliesKeys.items(params),
    queryFn: () => suppliesApi.getSupplyItems(params),
  });
}

export function useAllSupplyItems(
  options?: Omit<UseQueryOptions<SupplyItem[], Error>, "queryKey" | "queryFn">,
) {
  return useQuery({
    queryKey: [...suppliesKeys.all, "all-items"],
    queryFn: () => suppliesApi.getAllSupplyItems(),
    ...options,
  });
}

export function useEmployeeSeasonSupplyItems(
  seasonId: number,
  params?: SupplyItemsParams,
  options?: Omit<UseQueryOptions<PageResponse<SupplyItem>, Error>, "queryKey" | "queryFn">,
) {
  return useQuery({
    queryKey: suppliesKeys.employeeSeasonItems(seasonId, params),
    queryFn: () => suppliesApi.getEmployeeSeasonSupplyItems(seasonId, params),
    enabled: seasonId > 0,
    staleTime: 60 * 1000,
    refetchOnWindowFocus: true,
    ...options,
  });
}

export function useCreateSupplyItem() {
  const queryClient = useQueryClient();

  return useMutation<
    SupplyItem,
    Error,
    CreateSupplyItemRequest,
    CreateSupplyItemContext
  >({
    mutationFn: (data) => suppliesApi.createSupplyItem(data),
    onMutate: async (newItem) => {
      await queryClient.cancelQueries({ queryKey: suppliesKeys.all });

      const previousItems = queryClient.getQueryData<PageResponse<SupplyItem>>(
        suppliesKeys.items(),
      );
      const previousAllItems = queryClient.getQueryData<SupplyItem[]>([
        ...suppliesKeys.all,
        "all-items",
      ]);

      if (previousItems) {
        queryClient.setQueryData<PageResponse<SupplyItem>>(
          suppliesKeys.items(),
          {
            ...previousItems,
            items: [{ id: Date.now(), ...newItem } as SupplyItem, ...previousItems.items],
            totalElements: previousItems.totalElements + 1,
          },
        );
      }

      if (previousAllItems) {
        queryClient.setQueryData<SupplyItem[]>(
          [...suppliesKeys.all, "all-items"],
          [{ id: Date.now(), ...newItem } as SupplyItem, ...previousAllItems],
        );
      }

      return { previousItems, previousAllItems };
    },
    onError: (_err, _newItem, context) => {
      if (context?.previousItems) {
        queryClient.setQueryData(suppliesKeys.items(), context.previousItems);
      }
      if (context?.previousAllItems) {
        queryClient.setQueryData(
          [...suppliesKeys.all, "all-items"],
          context.previousAllItems,
        );
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: suppliesKeys.all });
    },
  });
}

export function useUpdateSupplyItem() {
  const queryClient = useQueryClient();

  return useMutation<
    SupplyItem,
    Error,
    { id: number; data: UpdateSupplyItemRequest },
    UpdateSupplyItemContext
  >({
    mutationFn: ({ id, data }) => suppliesApi.updateSupplyItem(id, data),
    onMutate: async ({ id, data }) => {
      await queryClient.cancelQueries({ queryKey: suppliesKeys.all });

      const previousItems = queryClient.getQueryData<PageResponse<SupplyItem>>(
        suppliesKeys.items(),
      );
      const previousAllItems = queryClient.getQueryData<SupplyItem[]>([
        ...suppliesKeys.all,
        "all-items",
      ]);

      if (previousItems) {
        queryClient.setQueryData<PageResponse<SupplyItem>>(
          suppliesKeys.items(),
          {
            ...previousItems,
            items: previousItems.items.map((item) =>
              item.id === id ? { ...item, ...data } : item,
            ),
          },
        );
      }

      if (previousAllItems) {
        queryClient.setQueryData<SupplyItem[]>(
          [...suppliesKeys.all, "all-items"],
          previousAllItems.map((item) =>
            item.id === id ? { ...item, ...data } : item,
          ),
        );
      }

      return { previousItems, previousAllItems };
    },
    onError: (_err, _variables, context) => {
      if (context?.previousItems) {
        queryClient.setQueryData(suppliesKeys.items(), context.previousItems);
      }
      if (context?.previousAllItems) {
        queryClient.setQueryData(
          [...suppliesKeys.all, "all-items"],
          context.previousAllItems,
        );
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: suppliesKeys.all });
    },
  });
}

export function useDeleteSupplyItem() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, number, DeleteSupplyItemContext>({
    mutationFn: (id) => suppliesApi.deleteSupplyItem(id),
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: suppliesKeys.all });

      const previousItems = queryClient.getQueryData<PageResponse<SupplyItem>>(
        suppliesKeys.items(),
      );
      const previousAllItems = queryClient.getQueryData<SupplyItem[]>([
        ...suppliesKeys.all,
        "all-items",
      ]);

      if (previousItems) {
        queryClient.setQueryData<PageResponse<SupplyItem>>(
          suppliesKeys.items(),
          {
            ...previousItems,
            items: previousItems.items.filter((item) => item.id !== id),
            totalElements: Math.max(0, previousItems.totalElements - 1),
          },
        );
      }

      if (previousAllItems) {
        queryClient.setQueryData<SupplyItem[]>(
          [...suppliesKeys.all, "all-items"],
          previousAllItems.filter((item) => item.id !== id),
        );
      }

      return { previousItems, previousAllItems };
    },
    onError: (_err, _id, context) => {
      if (context?.previousItems) {
        queryClient.setQueryData(suppliesKeys.items(), context.previousItems);
      }
      if (context?.previousAllItems) {
        queryClient.setQueryData(
          [...suppliesKeys.all, "all-items"],
          context.previousAllItems,
        );
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: suppliesKeys.all });
    },
  });
}

// ═══════════════════════════════════════════════════════════════
// SUPPLY LOTS
// ═══════════════════════════════════════════════════════════════

export function useSupplyLots(
  params?: SupplyLotsParams,
  options?: Omit<UseQueryOptions<PageResponse<SupplyLot>, Error>, "queryKey" | "queryFn">,
) {
  return useQuery({
    queryKey: suppliesKeys.lots(params),
    queryFn: () => suppliesApi.getSupplyLots(params),
    ...options,
  });
}

export function useEmployeeSeasonSupplyLots(
  seasonId: number,
  params?: SupplyLotsParams,
  options?: Omit<UseQueryOptions<PageResponse<SupplyLot>, Error>, "queryKey" | "queryFn">,
) {
  return useQuery({
    queryKey: suppliesKeys.employeeSeasonLots(seasonId, params),
    queryFn: () => suppliesApi.getEmployeeSeasonSupplyLots(seasonId, params),
    enabled: seasonId > 0,
    staleTime: 60 * 1000,
    refetchOnWindowFocus: true,
    ...options,
  });
}

// ═══════════════════════════════════════════════════════════════
// STOCK IN (Mutation)
// ═══════════════════════════════════════════════════════════════

/**
 * Stock in with invalidation (no optimistic update needed - creates new records)
 */
export function useStockIn() {
  const queryClient = useQueryClient();

  return useMutation<StockInResponse, Error, StockInRequest>({
    mutationFn: (data) => suppliesApi.stockIn(data),
    onSuccess: () => {
      // Invalidate supplies lots and inventory queries
      queryClient.invalidateQueries({ queryKey: suppliesKeys.all });
      queryClient.invalidateQueries({ queryKey: inventoryKeys.all });
    },
  });
}
