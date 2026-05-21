import {
    useMutation,
    useQuery,
    useQueryClient,
    type UseMutationOptions,
    type UseQueryOptions,
} from '@tanstack/react-query';
import type { PageResponse } from '@/shared/api/types';
import { harvestKeys } from '../model/keys';
import { harvestApi } from './client';
import type {
    HarvestListParams,
    Harvest,
    HarvestSummary,
    HarvestStockContext,
    HarvestCreateRequest,
    HarvestUpdateRequest,
} from '../model/types';

const isValidSeasonId = (seasonId?: number | null): seasonId is number =>
    typeof seasonId === 'number' && Number.isFinite(seasonId) && seasonId > 0;

type CreateHarvestVariables = {
    seasonId: number;
    data: HarvestCreateRequest;
};

type UpdateHarvestVariables = {
    id: number;
    data: HarvestUpdateRequest;
    seasonId?: number;
};

type DeleteHarvestVariables = {
    id: number;
    seasonId?: number;
};

const invalidateHarvestLists = (
    queryClient: ReturnType<typeof useQueryClient>,
    seasonId?: number
) => {
    if (isValidSeasonId(seasonId)) {
        queryClient.invalidateQueries({ queryKey: harvestKeys.listBySeasonBase(seasonId) });
        queryClient.invalidateQueries({ queryKey: harvestKeys.summary(seasonId) });
    }
    queryClient.invalidateQueries({ queryKey: harvestKeys.listAllBase() });
    queryClient.invalidateQueries({ queryKey: harvestKeys.summaries() });
};

/**
 * Hook to fetch all farmer harvests with optional seasonId filter.
 * When seasonId is provided, query key is season-scoped for reliable mutation invalidation.
 */
export const useAllFarmerHarvests = (
    params?: HarvestListParams,
    options?: Omit<UseQueryOptions<PageResponse<Harvest>, Error>, 'queryKey' | 'queryFn'>
) => {
    const seasonId = params?.seasonId;
    return useQuery({
        queryKey: isValidSeasonId(seasonId)
            ? harvestKeys.listBySeason(seasonId, params)
            : harvestKeys.listAll(params),
    queryFn: () => harvestApi.listAll(params),
    staleTime: 5 * 60 * 1000,
    ...options,
    });
};

/**
 * Hook to fetch harvest summary/KPI data
 */
export const useHarvestSummary = (
    seasonId?: number,
    options?: Omit<UseQueryOptions<HarvestSummary, Error>, 'queryKey' | 'queryFn'>
) => useQuery({
    queryKey: harvestKeys.summary(seasonId),
    queryFn: () => harvestApi.getSummary(seasonId),
    staleTime: 5 * 60 * 1000,
    ...options,
});

/**
 * Hook to fetch paginated list of harvests for a season
 * @deprecated Use useAllFarmerHarvests with seasonId in params instead
 */
export const useHarvestsBySeason = (
    seasonId: number,
    params?: HarvestListParams,
    options?: Omit<UseQueryOptions<PageResponse<Harvest>, Error>, 'queryKey' | 'queryFn'>
) => useQuery({
    queryKey: harvestKeys.listBySeason(seasonId, params),
    queryFn: () => harvestApi.listBySeason(seasonId, params),
    enabled: seasonId > 0,
    staleTime: 5 * 60 * 1000,
    ...options,
});

/**
 * Hook to fetch a single harvest by ID
 */
export const useHarvestById = (
    id: number,
    options?: Omit<UseQueryOptions<Harvest, Error>, 'queryKey' | 'queryFn'>
) => useQuery({
    queryKey: harvestKeys.detail(id),
    queryFn: () => harvestApi.getById(id),
    enabled: id > 0,
    staleTime: 5 * 60 * 1000,
    ...options,
});

export const useHarvestStockContext = (
    seasonId: number | null | undefined,
    params?: { warehouseId?: number; productName?: string; lotCode?: string },
    options?: Omit<UseQueryOptions<HarvestStockContext, Error>, 'queryKey' | 'queryFn'>
) => useQuery({
    queryKey: harvestKeys.stockContext(
        seasonId ?? 0,
        params?.warehouseId ?? 0,
        params?.productName ?? '',
        params?.lotCode ?? ''
    ),
    queryFn: () => harvestApi.getStockContext(seasonId!, {
        warehouseId: params!.warehouseId!,
        productName: params!.productName!,
        lotCode: params!.lotCode!,
    }),
    enabled: !!seasonId
        && seasonId > 0
        && !!params?.warehouseId
        && !!params?.productName
        && !!params?.lotCode,
    staleTime: 30 * 1000,
    ...options,
});

/**
 * Hook to create a new harvest.
 */
export const useCreateHarvest = (
    options?: Omit<UseMutationOptions<Harvest, Error, CreateHarvestVariables, unknown>, 'mutationFn'>
) => {
    const queryClient = useQueryClient();
    return useMutation<Harvest, Error, CreateHarvestVariables, unknown>({
        ...options,
        mutationFn: ({ seasonId, data }) => harvestApi.create(seasonId, data),
        onSuccess: (data, variables, onMutateResult, context) => {
            options?.onSuccess?.(data, variables, onMutateResult, context);
        },
        onError: (error, variables, onMutateResult, context) => {
            options?.onError?.(error, variables, onMutateResult, context);
        },
        onSettled: (data, error, variables, onMutateResult, context) => {
            invalidateHarvestLists(queryClient, variables?.seasonId);
            options?.onSettled?.(data, error, variables, onMutateResult, context);
        },
    });
};

/**
 * Hook to update a harvest.
 */
export const useUpdateHarvest = (
    options?: Omit<UseMutationOptions<Harvest, Error, UpdateHarvestVariables, unknown>, 'mutationFn'>
) => {
    const queryClient = useQueryClient();
    return useMutation<Harvest, Error, UpdateHarvestVariables, unknown>({
        ...options,
        mutationFn: ({ id, data }) => harvestApi.update(id, data),
        onSuccess: (data, variables, onMutateResult, context) => {
            options?.onSuccess?.(data, variables, onMutateResult, context);
        },
        onError: (error, variables, onMutateResult, context) => {
            options?.onError?.(error, variables, onMutateResult, context);
        },
        onSettled: (data, error, variables, onMutateResult, context) => {
            if (variables?.id) {
                queryClient.invalidateQueries({ queryKey: harvestKeys.detail(variables.id) });
            }
            invalidateHarvestLists(queryClient, variables?.seasonId);
            options?.onSettled?.(data, error, variables, onMutateResult, context);
        },
    });
};

/**
 * Hook to delete a harvest.
 */
export const useDeleteHarvest = (
    options?: Omit<UseMutationOptions<void, Error, DeleteHarvestVariables, unknown>, 'mutationFn'>
) => {
    const queryClient = useQueryClient();
    return useMutation<void, Error, DeleteHarvestVariables, unknown>({
        ...options,
        mutationFn: ({ id }) => harvestApi.delete(id),
        onSuccess: (data, variables, onMutateResult, context) => {
            options?.onSuccess?.(data, variables, onMutateResult, context);
        },
        onError: (error, variables, onMutateResult, context) => {
            options?.onError?.(error, variables, onMutateResult, context);
        },
        onSettled: (data, error, variables, onMutateResult, context) => {
            if (variables?.id) {
                queryClient.invalidateQueries({ queryKey: harvestKeys.detail(variables.id) });
            }
            invalidateHarvestLists(queryClient, variables?.seasonId);
            options?.onSettled?.(data, error, variables, onMutateResult, context);
        },
    });
};
